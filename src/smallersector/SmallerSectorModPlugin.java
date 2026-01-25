package smallersector;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;

/**
 * Main mod plugin for Smaller Sector.
 *
 * Registers all listeners and ensures cost modifiers are applied.
 */
public class SmallerSectorModPlugin extends BaseModPlugin {

    public static final Logger log = Global.getLogger(SmallerSectorModPlugin.class);

    private FleetSpawnListener fleetSpawnListener;
    private MarketInterceptor marketInterceptor;
    private DerelictInterceptor derelictInterceptor;
    private PlayerFleetMonitor playerFleetMonitor;

    @Override
    public void onApplicationLoad() throws Exception {
        log.info("Smaller Sector: Loading mod...");

        // Verify LunaLib is present
        try {
            Class.forName("lunalib.lunaSettings.LunaSettings");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "Smaller Sector requires LunaLib! Please install LunaLib and restart.");
        }

        // Register preset listener to sync UI sliders when preset changes
        if (!LunaSettings.hasSettingsListenerOfClass(PresetListener.class)) {
            LunaSettings.addSettingsListener(new PresetListener());
            log.info("Smaller Sector: Registered preset listener.");
        }

        // Store original base values and apply build cost multipliers
        BaseValueModifier.storeOriginalValues();
        BaseValueModifier.applyMultipliers();

        log.info("Smaller Sector: Mod loaded successfully.");
    }

    @Override
    public void onGameLoad(boolean newGame) {
        log.info("Smaller Sector: Initializing for game...");

        // Reload settings
        Settings.reloadBlacklist();

        // Register listeners
        if (Global.getSector() == null) {
            log.error("Smaller Sector: Global.getSector() is null, cannot register listeners.");
            return;
        }

        ListenerManagerAPI listeners = Global.getSector().getListenerManager();
        if (listeners == null) {
            log.error("Smaller Sector: ListenerManager is null, cannot register listeners.");
            return;
        }

        // Fleet spawn listener
        fleetSpawnListener = new FleetSpawnListener(false);
        Global.getSector().addTransientListener(fleetSpawnListener);

        // Market/economy listener
        marketInterceptor = new MarketInterceptor();
        listeners.addListener(marketInterceptor, true);

        // Derelict ship interceptor - replaces cruisers/capitals in derelicts
        derelictInterceptor = new DerelictInterceptor();
        listeners.addListener(derelictInterceptor, true);

        // Player fleet monitor for D-mods and cost hull mod
        playerFleetMonitor = new PlayerFleetMonitor();
        Global.getSector().addTransientScript(playerFleetMonitor);

        // Add Faction Manager intel item (if not already present)
        if (!FactionManagerIntel.exists()) {
            Global.getSector().getIntelManager().addIntel(new FactionManagerIntel());
            log.info("Smaller Sector: Added Faction Manager intel item.");
        }

        // Initial processing of player fleet
        applyCostHullMod();
        DmodApplicator.processPlayerFleet();

        // Process all existing fleets (for existing saves)
        processAllExistingFleets();

        // Initial processing of all markets (don't wait for economy tick)
        processAllMarkets();

        log.info("Smaller Sector: Game initialization complete.");
    }

    @Override
    public void beforeGameSave() {
        // Nothing to save - mod is stateless
    }

    @Override
    public void afterGameSave() {
        // Re-apply modifiers in case of any issues
        applyCostHullMod();
        DmodApplicator.processPlayerFleet();
    }

    /**
     * Ensure all cruisers and capitals in the player fleet have the cost modifier hull mod.
     */
    private void applyCostHullMod() {
        if (Global.getSector() == null) return;

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) return;
        if (playerFleet.getFleetData() == null) return;

        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            applyHullModIfNeeded(member);
        }
    }

    /**
     * Apply the cost modifier hull mod to a fleet member if it's a cruiser or capital.
     */
    private void applyHullModIfNeeded(FleetMemberAPI member) {
        if (member == null) return;
        if (member.getHullSpec() == null) return;
        if (member.getVariant() == null) return;

        HullSize size = member.getHullSpec().getHullSize();
        if (size != HullSize.CRUISER && size != HullSize.CAPITAL_SHIP) return;

        String hullModId = CostModifier.HULLMOD_ID;
        if (!member.getVariant().hasHullMod(hullModId)) {
            member.getVariant().addPermaMod(hullModId, false);
        }
    }

    /**
     * Process all markets immediately on game load.
     * This ensures ships are replaced right away, not just on economy ticks.
     */
    private void processAllMarkets() {
        if (Global.getSector() == null) return;
        if (Global.getSector().getEconomy() == null) return;

        log.info("Smaller Sector: Processing all markets on game load...");
        int count = 0;
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            MarketInterceptor.processMarket(market);
            count++;
        }
        log.info("Smaller Sector: Processed " + count + " markets.");
    }

    /**
     * Process all existing fleets in the sector.
     * This allows the mod to work on existing saves without requiring a new game.
     * Fleets are tagged after processing to prevent re-rolling on subsequent loads.
     */
    private void processAllExistingFleets() {
        if (Global.getSector() == null) return;

        log.info("Smaller Sector: Processing all existing fleets...");
        int count = 0;

        // Process fleets in all locations (star systems, hyperspace, etc.)
        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI fleet : location.getFleets()) {
                if (fleet == null || fleet.isPlayerFleet()) continue;
                FleetInterceptor.processFleet(fleet);
                count++;
            }
        }

        log.info("Smaller Sector: Processed " + count + " existing fleets.");
    }
}
