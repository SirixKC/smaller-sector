package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Intercepts market stock and replaces cruisers/capitals with smaller ships.
 *
 * Processes submarkets once per natural stock refresh cycle:
 * - On first encounter (player opens market)
 * - After the game naturally refreshes stock (~30 days)
 *
 * Uses timestamp tracking to prevent re-rolling ships on save/reload.
 */
public class MarketInterceptor implements EconomyTickListener, ColonyInteractionListener {

    private static final String PERSISTENCE_KEY = "smallersector_market_processed";

    // ========== Persistence ==========

    /**
     * Get or create the map tracking when each submarket was last processed.
     * Stored in sector persistent data so it survives save/load.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Float> getProcessedMap() {
        Map<String, Object> persistent = Global.getSector().getPersistentData();
        Map<String, Float> map = (Map<String, Float>) persistent.get(PERSISTENCE_KEY);
        if (map == null) {
            map = new HashMap<String, Float>();
            persistent.put(PERSISTENCE_KEY, map);
        }
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
     * Formula: (currentDay - lastProcessedDay) > sinceLastCargoUpdate
     * This works because sinceLastCargoUpdate tells us when the last refresh was.
     */
    private static boolean shouldProcess(MarketAPI market, SubmarketAPI submarket) {
        String key = getSubmarketKey(market, submarket);
        Map<String, Float> processed = getProcessedMap();

        // Get current absolute day (timestamp is in seconds, convert to days)
        float currentDay = Global.getSector().getClock().getTimestamp() / (24f * 60f * 60f);

        // Get sinceLastCargoUpdate from the plugin (requires cast to BaseSubmarketPlugin)
        float sinceLastUpdate = 0f;
        if (submarket.getPlugin() instanceof BaseSubmarketPlugin) {
            sinceLastUpdate = ((BaseSubmarketPlugin) submarket.getPlugin()).getSinceLastCargoUpdate();
        } else {
            // Unknown plugin type - always process to be safe
            return true;
        }

        Float lastProcessed = processed.get(key);
        if (lastProcessed == null) {
            return true; // Never processed
        }

        // If time since we processed exceeds time since last refresh, a refresh happened
        return (currentDay - lastProcessed) > sinceLastUpdate;
    }

    /**
     * Mark a submarket as processed at the current time.
     */
    private static void markProcessed(MarketAPI market, SubmarketAPI submarket) {
        String key = getSubmarketKey(market, submarket);
        float currentDay = Global.getSector().getClock().getTimestamp() / (24f * 60f * 60f);
        getProcessedMap().put(key, currentDay);
    }

    // ========== Colony Interaction Listener ==========

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
        // Process when player first opens a market (catches first encounters)
        processMarket(market);
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {
        // Not needed
    }

    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
        // Not needed - we process on market open, not on each transaction
    }

    @Override
    public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {
        // This fires after cargo is updated - also a good hook point
        processMarket(market);
    }

    // ========== Economy Tick Listener ==========

    @Override
    public void reportEconomyTick(int iterIndex) {
        if (Global.getSector() == null) return;
        if (Global.getSector().getEconomy() == null) return;

        // Process all markets on economy tick (catches refreshes while playing)
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            processMarket(market);
        }
    }

    @Override
    public void reportEconomyMonthEnd() {
        // Not needed
    }

    // ========== Processing ==========

    /**
     * Process all submarkets in a market, respecting the refresh tracking.
     */
    public static void processMarket(MarketAPI market) {
        if (market == null || market.getFaction() == null) return;

        String factionId = market.getFaction().getId();

        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            // Skip if already processed this refresh cycle
            if (!shouldProcess(market, submarket)) {
                continue;
            }

            processSubmarket(submarket, factionId);
            markProcessed(market, submarket);
        }
    }

    /**
     * Process ships in a submarket, replacing cruisers/capitals with smaller ships.
     */
    private static void processSubmarket(SubmarketAPI submarket, String factionId) {
        if (submarket == null || submarket.getCargo() == null) return;

        com.fs.starfarer.api.campaign.FleetDataAPI mothballedShips = submarket.getCargo().getMothballedShips();
        if (mothballedShips == null) return;

        // Process ships for sale
        List<FleetMemberAPI> ships = mothballedShips.getMembersListCopy();
        List<FleetMemberAPI> toRemove = new ArrayList<FleetMemberAPI>();
        List<FleetMemberAPI> toAdd = new ArrayList<FleetMemberAPI>();

        for (FleetMemberAPI member : ships) {
            FleetMemberAPI replacement = ShipReplacer.tryReplace(member, factionId);
            if (replacement != null) {
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
}
