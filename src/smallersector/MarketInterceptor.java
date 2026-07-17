package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.listeners.SubmarketInteractionListener;
import com.fs.starfarer.api.campaign.listeners.SubmarketUpdateListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Intercepts market stock and replaces cruisers/capitals with smaller ships.
 *
 * Processes submarkets once per natural stock refresh cycle:
 * - On first encounter (player opens market)
 * - After the game naturally refreshes stock (~30 days)
 *
 * Uses timestamp tracking to prevent re-rolling ships on save/reload.
 */
public class MarketInterceptor implements EconomyTickListener, ColonyInteractionListener,
        SubmarketInteractionListener, SubmarketUpdateListener {

    private static final String PERSISTENCE_KEY = "smallersector_market_processed";
    private static final String PROCESSED_SHIP_TAG =
            "smallersector_market_ship_processed";
    private static final String MIGRATION_KEY =
            "smallersector_market_processing_initialized_v2";
    private static final String STORAGE_KNOWN_SHIPS_KEY =
            "smallersector_storage_known_ship_ids";
    private static final String STORAGE_PENDING_ACQUISITIONS_KEY =
            "smallersector_storage_pending_acquisition_ids";
    private static final float REFRESH_COMPARISON_EPSILON_DAYS = 0.001f;

    // ========== Persistence ==========

    /**
     * Get or create the map tracking when each submarket was last processed.
     * Stored in sector persistent data so it survives save/load.
     */
    private static Map<String, Long> getProcessedMap() {
        Map<String, Object> persistent = Global.getSector().getPersistentData();
        Object saved = persistent.get(PERSISTENCE_KEY);

        if (saved instanceof Map<?, ?>) {
            Map<?, ?> rawMap = (Map<?, ?>) saved;
            boolean compatible = true;

            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof Long)) {
                    compatible = false;
                    break;
                }
            }

            if (compatible) {
                @SuppressWarnings("unchecked")
                Map<String, Long> processed = (Map<String, Long>) rawMap;
                return processed;
            }

            Map<String, Long> migrated = new HashMap<String, Long>();
            long migrationTimestamp = Global.getSector().getClock().getTimestamp();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (!(entry.getKey() instanceof String)) continue;

                String key = (String) entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Long) {
                    migrated.put(key, (Long) value);
                } else if (value instanceof Number) {
                    // Older versions stored a Float derived from a millisecond
                    // timestamp using the wrong units. Preserve the fact that
                    // the stock was processed, but rebase it to a valid timestamp.
                    migrated.put(key, migrationTimestamp);
                }
            }

            persistent.put(PERSISTENCE_KEY, migrated);
            return migrated;
        }

        Map<String, Long> map = new HashMap<String, Long>();
        persistent.put(PERSISTENCE_KEY, map);
        return map;
    }

    /**
     * Generate a unique key for a submarket (market + submarket type).
     */
    private static String getSubmarketKey(MarketAPI market, SubmarketAPI submarket) {
        return market.getId() + "_" + submarket.getSpecId();
    }

    /**
     * Check if a submarket should be processed.
     * Returns true if:
     * - Never processed before, OR
     * - Stock has refreshed since last processing
     *
     * For BaseSubmarketPlugin, the refresh happened after our timestamp when
     * the elapsed days since processing exceed the age of the current ship stock.
     */
    private static boolean shouldProcess(MarketAPI market, SubmarketAPI submarket,
            Map<String, Long> processed) {
        String key = getSubmarketKey(market, submarket);
        SubmarketPlugin plugin = submarket.getPlugin();
        Long lastProcessed = processed.get(key);

        if (lastProcessed == null) {
            // Do not process stock that the game is about to replace. After the
            // natural update, okToUpdateShipsAndWeapons() becomes false and the
            // post-update market callback processes the newly generated ships.
            return !plugin.okToUpdateShipsAndWeapons();
        }

        if (!(plugin instanceof BaseSubmarketPlugin)) {
            // The public SubmarketPlugin API does not expose how long it has been
            // since a ship-stock refresh. Process exotic implementations once;
            // repeatedly guessing would reroll their inventory on every tick.
            return false;
        }

        BaseSubmarketPlugin basePlugin = (BaseSubmarketPlugin) plugin;
        float sinceLastShipUpdate = basePlugin.getSinceSWUpdate();
        if (Float.isNaN(sinceLastShipUpdate) || Float.isInfinite(sinceLastShipUpdate)) {
            return false;
        }

        CampaignClockAPI clock = Global.getSector().getClock();
        float daysSinceProcessed = clock.getElapsedDaysSince(lastProcessed.longValue());

        // A refresh happened after our timestamp when its age is less than the
        // time since we processed this submarket.
        return daysSinceProcessed > Math.max(0f, sinceLastShipUpdate)
            + REFRESH_COMPARISON_EPSILON_DAYS;
    }

    /**
     * Mark a submarket as processed at the current time.
     */
    private static void markProcessed(MarketAPI market, SubmarketAPI submarket,
            Map<String, Long> processed) {
        String key = getSubmarketKey(market, submarket);
        long timestamp = Global.getSector().getClock().getTimestamp();
        processed.put(key, timestamp);
    }

    /**
     * Whether this submarket owns sale inventory rather than player-owned cargo.
     */
    private static boolean isProcessableSubmarket(SubmarketAPI submarket) {
        if (submarket == null || submarket.getPlugin() == null) return false;

        // Check both the vanilla id and plugin behavior. The latter protects
        // modded storage, local-resource, and other free-transfer submarkets.
        if (Submarkets.SUBMARKET_STORAGE.equals(submarket.getSpecId())) return false;
        return !submarket.getPlugin().isFreeTransfer();
    }

    // ========== Colony Interaction Listener ==========

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
        // Don't process here - cargo may not be loaded yet
        // Use reportPlayerOpenedMarketAndCargoUpdated instead
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
        // Not needed
    }

    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
        if (transaction == null || transaction.getSubmarket() == null) return;

        SubmarketPlugin plugin = transaction.getSubmarket().getPlugin();
        if (plugin == null) return;

        // A player-sold ship is not procedural stock. Preserve its exact identity
        // if it remains in a shop across a later natural refresh. Free-transfer
        // deposits are tracked by ID without mutating the stored variant.
        for (PlayerMarketTransaction.ShipSaleInfo ship : transaction.getShipsSold()) {
            if (ship != null && ship.getMember() != null) {
                if (plugin.isFreeTransfer()) {
                    recordStoredShip(ship.getMember(),
                            !DmodApplicator.hasBeenProcessed(ship.getMember()));
                } else {
                    markShipProcessed(ship.getMember());
                }
            }
        }

        // Market transactions happen while the campaign monitor is paused. Handle
        // purchases synchronously so there is no acquisition/store exploit window.
        for (PlayerMarketTransaction.ShipSaleInfo ship : transaction.getShipsBought()) {
            if (ship == null || ship.getMember() == null) continue;

            CostModifier.applyHullModIfNeeded(ship.getMember());
            if (plugin.isFreeTransfer()) {
                String memberId = ship.getMember().getId();
                if (memberId != null && getPendingStorageAcquisitions().remove(memberId)) {
                    // Direct-to-storage production was not mutated while stored;
                    // apply its acquisition penalty when it first enters the fleet.
                    DmodApplicator.applyDmodsIfNeeded(ship.getMember());
                } else {
                    // Taking an already-owned ship out of storage is not a new acquisition.
                    DmodApplicator.markAsProcessedWithoutApplying(ship.getMember());
                }
                if (memberId != null) {
                    getKnownStorageShipIds().remove(memberId);
                }
            } else {
                DmodApplicator.applyDmodsIfNeeded(ship.getMember());
            }
        }
    }

    @Override
    public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {
        detectNewStoredShips(market);
        processMarket(market);
    }

    @Override
    public void reportPlayerOpenedSubmarket(
            SubmarketAPI submarket, SubmarketInteractionType type) {
        detectNewStoredShips(submarket);
    }

    @Override
    public void reportSubmarketCargoAndShipsUpdated(SubmarketAPI submarket) {
        detectNewStoredShips(submarket);
    }

    // ========== Economy Tick Listener ==========

    @Override
    public void reportEconomyTick(int iterIndex) {
        if (Global.getSector() == null) return;
        if (Global.getSector().getEconomy() == null) return;

        // Process all markets on economy tick (catches refreshes while playing)
        Map<String, Long> processed = getProcessedMap();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            processMarket(market, processed);
        }
    }

    @Override
    public void reportEconomyMonthEnd() {
        detectNewStoredAcquisitions();
    }

    // ========== Processing ==========

    /**
     * Initialize all materialized market stock for a campaign. Existing stock is
     * grandfathered once on upgrade; new-game and subsequently missed stock is
     * processed normally.
     *
     * @return number of markets visited
     */
    public static int initializeMarkets(boolean newGame) {
        if (Global.getSector() == null || Global.getSector().getEconomy() == null) return 0;

        // Every game load establishes a no-penalty baseline for ships already in
        // storage. Previously detected production deliveries remain pending.
        grandfatherCurrentStorage();

        boolean migrationComplete = Boolean.TRUE.equals(
                Global.getSector().getPersistentData().get(MIGRATION_KEY));
        boolean grandfather = !newGame && !migrationComplete;
        Map<String, Long> processed = getProcessedMap();
        int count = 0;

        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (grandfather) {
                grandfatherMarket(market, processed);
            } else {
                processMarket(market, processed);
            }
            count += 1;
        }

        Global.getSector().getPersistentData().put(MIGRATION_KEY, Boolean.TRUE);
        return count;
    }

    private static void grandfatherMarket(MarketAPI market, Map<String, Long> processed) {
        if (market == null) return;

        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            if (!isProcessableSubmarket(submarket)) continue;
            CargoAPI cargo = submarket.getCargoNullOk();
            if (cargo == null) continue;

            // Vanilla refreshes prune existing ships probabilistically rather
            // than clearing all of them. Mark the current objects before a due
            // refresh so survivors remain grandfathered while newly added stock
            // is still eligible.
            markExistingShipsWithoutReplacement(cargo);

            // If a refresh is already due, leave this unmarked. The open-market
            // update will generate genuinely new stock and the post-update hook
            // will process it before the player sees it.
            String key = getSubmarketKey(market, submarket);
            if (submarket.getPlugin().okToUpdateShipsAndWeapons()) {
                // Also clear a legacy Float-map entry rebased by getProcessedMap();
                // otherwise an immediately refreshed shop looks just processed.
                processed.remove(key);
                continue;
            }
            if (processed.containsKey(key)) continue;
            markProcessed(market, submarket, processed);
        }
    }

    /**
     * Process all submarkets in a market, respecting the refresh tracking.
     */
    public static void processMarket(MarketAPI market) {
        if (Global.getSector() == null) return;
        processMarket(market, getProcessedMap());
    }

    private static void processMarket(MarketAPI market, Map<String, Long> processed) {
        if (market == null || market.getFaction() == null) return;

        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            if (!isProcessableSubmarket(submarket)) {
                continue;
            }

            // Do not call getCargo(): it creates lazy cargo and can make us mark
            // a shop before the game generates its stock for player interaction.
            CargoAPI cargo = submarket.getCargoNullOk();
            if (cargo == null) {
                continue;
            }

            // Skip if already processed this refresh cycle
            if (!shouldProcess(market, submarket, processed)) {
                continue;
            }

            String factionId = submarket.getFaction() != null
                    ? submarket.getFaction().getId()
                    : market.getFaction().getId();
            processSubmarket(cargo, factionId);
            markProcessed(market, submarket, processed);
        }
    }

    /**
     * Process ships in a submarket, replacing cruisers/capitals with smaller ships.
     */
    private static void processSubmarket(CargoAPI cargo, String factionId) {
        if (cargo == null) return;

        com.fs.starfarer.api.campaign.FleetDataAPI mothballedShips = cargo.getMothballedShips();
        if (mothballedShips == null) return;

        // Process ships for sale
        List<FleetMemberAPI> ships = mothballedShips.getMembersListCopy();
        List<FleetMemberAPI> toRemove = new ArrayList<FleetMemberAPI>();
        List<FleetMemberAPI> toAdd = new ArrayList<FleetMemberAPI>();

        for (FleetMemberAPI member : ships) {
            if (hasShipBeenProcessed(member)) continue;

            // Mark before rolling. This clone-and-tag also makes an unchanged
            // ship stable if vanilla retains it through later stock refreshes.
            markShipProcessed(member);
            FleetMemberAPI replacement = ShipReplacer.tryReplaceForMarket(member, factionId);
            if (replacement != null) {
                markShipProcessed(replacement);
                toRemove.add(member);
                toAdd.add(replacement);
            }
        }

        // Apply changes
        for (FleetMemberAPI member : toRemove) {
            mothballedShips.removeFleetMember(member);
        }
        for (FleetMemberAPI member : toAdd) {
            mothballedShips.addFleetMember(member);
        }
    }

    private static void markExistingShipsWithoutReplacement(CargoAPI cargo) {
        if (cargo == null || cargo.getMothballedShips() == null) return;
        for (FleetMemberAPI member : cargo.getMothballedShips().getMembersListCopy()) {
            markShipProcessed(member);
        }
    }

    private static boolean hasShipBeenProcessed(FleetMemberAPI member) {
        return member == null || member.getVariant() == null
                || member.getVariant().hasTag(PROCESSED_SHIP_TAG);
    }

    private static void markShipProcessed(FleetMemberAPI member) {
        com.fs.starfarer.api.combat.ShipVariantAPI variant =
                VariantUtils.getMutableVariant(member);
        if (variant != null) {
            variant.addTag(PROCESSED_SHIP_TAG);
        }
    }

    private static Set<String> getKnownStorageShipIds() {
        return getPersistentStringSet(STORAGE_KNOWN_SHIPS_KEY);
    }

    private static Set<String> getPendingStorageAcquisitions() {
        return getPersistentStringSet(STORAGE_PENDING_ACQUISITIONS_KEY);
    }

    private static Set<String> getPersistentStringSet(String key) {
        Map<String, Object> persistent = Global.getSector().getPersistentData();
        Object saved = persistent.get(key);
        if (saved instanceof Set<?>) {
            Set<String> migrated = new HashSet<String>();
            for (Object value : (Set<?>) saved) {
                if (value instanceof String) {
                    migrated.add((String) value);
                }
            }
            if (migrated.size() == ((Set<?>) saved).size()) {
                @SuppressWarnings("unchecked")
                Set<String> compatible = (Set<String>) saved;
                return compatible;
            }
            persistent.put(key, migrated);
            return migrated;
        }

        Set<String> created = new HashSet<String>();
        persistent.put(key, created);
        return created;
    }

    private static void grandfatherCurrentStorage() {
        Set<String> known = getKnownStorageShipIds();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (market == null) continue;
            for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
                if (submarket == null || submarket.getPlugin() == null
                        || !submarket.getPlugin().isFreeTransfer()) {
                    continue;
                }
                CargoAPI cargo = submarket.getCargoNullOk();
                if (cargo == null || cargo.getMothballedShips() == null) continue;
                for (FleetMemberAPI member :
                        cargo.getMothballedShips().getMembersListCopy()) {
                    if (member != null && member.getId() != null) {
                        known.add(member.getId());
                    }
                }
            }
        }
    }

    private static void detectNewStoredShips(MarketAPI market) {
        if (market == null) return;
        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            detectNewStoredShips(submarket);
        }
    }

    /** Close listener-ordering and save windows for direct-to-storage deliveries. */
    public static void detectNewStoredAcquisitions() {
        if (Global.getSector() == null || Global.getSector().getEconomy() == null) return;
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            detectNewStoredShips(market);
        }
    }

    private static void detectNewStoredShips(SubmarketAPI submarket) {
        if (submarket == null || submarket.getPlugin() == null
                || !submarket.getPlugin().isFreeTransfer()) {
            return;
        }

        CargoAPI cargo = submarket.getCargoNullOk();
        if (cargo == null || cargo.getMothballedShips() == null) return;

        Set<String> known = getKnownStorageShipIds();
        Set<String> pending = getPendingStorageAcquisitions();
        for (FleetMemberAPI member : cargo.getMothballedShips().getMembersListCopy()) {
            if (member == null || member.getId() == null) continue;
            if (!known.add(member.getId())) continue;

            // An unmarked ship appearing directly in storage is a new acquisition
            // (normally colony production). Defer mutation until withdrawal.
            if (!DmodApplicator.hasBeenProcessed(member)) {
                pending.add(member.getId());
            }
        }
    }

    private static void recordStoredShip(FleetMemberAPI member, boolean pendingAcquisition) {
        if (member == null || member.getId() == null) return;
        getKnownStorageShipIds().add(member.getId());
        if (pendingAcquisition) {
            getPendingStorageAcquisitions().add(member.getId());
        } else {
            getPendingStorageAcquisitions().remove(member.getId());
        }
    }
}
