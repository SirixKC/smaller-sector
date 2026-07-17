package smallersector;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.FleetStubAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;
import java.util.ArrayList;

/**
 * Main mod plugin for Smaller Sector.
 *
 * Registers all listeners and ensures cost modifiers are applied.
 */
public class SmallerSectorModPlugin extends BaseModPlugin {

    public static final Logger log = Global.getLogger(SmallerSectorModPlugin.class);
    private static final String FLEET_MIGRATION_KEY =
            "smallersector_fleet_processing_initialized_v1";

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

        removeOldTransientRegistrations(listeners);

        // Fleet spawn listener
        fleetSpawnListener = new FleetSpawnListener(false);
        Global.getSector().addTransientListener(fleetSpawnListener);
        Global.getSector().addTransientScript(fleetSpawnListener);

        // Market/economy listener
        marketInterceptor = new MarketInterceptor();
        listeners.addListener(marketInterceptor, true);

        // Derelict ship interceptor - replaces cruisers/capitals in derelicts
        derelictInterceptor = new DerelictInterceptor();
        listeners.addListener(derelictInterceptor, true);
        Global.getSector().addTransientScript(derelictInterceptor);

        // Player fleet monitor for D-mods and cost hull mod
        playerFleetMonitor = new PlayerFleetMonitor();
        listeners.addListener(playerFleetMonitor, true);
        Global.getSector().addTransientScript(playerFleetMonitor);

        // Add Faction Manager intel item (if not already present)
        if (!FactionManagerIntel.exists()) {
            Global.getSector().getIntelManager().addIntel(new FactionManagerIntel());
            log.info("Smaller Sector: Added Faction Manager intel item.");
        }

        // Existing player ships are an explicit migration baseline. Only ships
        // acquired after this point are eligible for acquisition-time D-mods.
        applyCostHullMod();
        DmodApplicator.grandfatherPlayerFleet();

        // Existing saves must not reroll fleets that the old release may already
        // have altered. A new campaign can safely process procgen fleets now,
        // before control is handed to the player.
        initializeExistingFleets(newGame);

        int derelictCount = derelictInterceptor.initializeExistingEntities(newGame);
        log.info("Smaller Sector: Initialized " + derelictCount + " derelicts.");

        // Initial processing of all markets (don't wait for economy tick)
        processAllMarkets(newGame);

        log.info("Smaller Sector: Game initialization complete.");
    }

    @Override
    public void beforeGameSave() {
        if (fleetSpawnListener != null) {
            fleetSpawnListener.processPendingFleets();
        }

        MarketInterceptor.detectNewStoredAcquisitions();

        // Close the small polling window for a ship acquired immediately before
        // saving, so its persistent marker and any D-mods enter the save together.
        applyCostHullMod();
        DmodApplicator.processPlayerFleet();
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
            CostModifier.applyHullModIfNeeded(member);
        }
    }

    private void removeOldTransientRegistrations(ListenerManagerAPI listeners) {
        listeners.removeListenerOfClass(MarketInterceptor.class);
        listeners.removeListenerOfClass(DerelictInterceptor.class);
        listeners.removeListenerOfClass(PlayerFleetMonitor.class);
        Global.getSector().removeTransientScriptsOfClass(PlayerFleetMonitor.class);
        Global.getSector().removeTransientScriptsOfClass(FleetSpawnListener.class);
        Global.getSector().removeTransientScriptsOfClass(DerelictInterceptor.class);

        for (CampaignEventListener listener :
                new ArrayList<CampaignEventListener>(Global.getSector().getAllListeners())) {
            if (listener instanceof FleetSpawnListener) {
                Global.getSector().removeListener(listener);
            }
        }
    }

    /**
     * Process all markets immediately on game load.
     * This ensures ships are replaced right away, not just on economy ticks.
     */
    private void processAllMarkets(boolean newGame) {
        if (Global.getSector() == null) return;
        if (Global.getSector().getEconomy() == null) return;

        int count = MarketInterceptor.initializeMarkets(newGame);
        log.info("Smaller Sector: Initialized " + count + " markets.");
    }

    /**
     * Establish the fleet migration baseline for a loaded campaign.
     *
     * On an existing save, all fleets already present are marked without rolling;
     * the prior mod version did not persist whether it had processed them. On a
     * new game, procgen fleets are processed once before the campaign is shown.
     */
    private void initializeExistingFleets(boolean newGame) {
        if (Global.getSector() == null) return;

        boolean migrationComplete = Boolean.TRUE.equals(
                Global.getSector().getPersistentData().get(FLEET_MIGRATION_KEY));
        boolean grandfather = !newGame && !migrationComplete;

        if (grandfather) {
            log.info("Smaller Sector: Grandfathering existing fleets...");
        } else if (newGame) {
            log.info("Smaller Sector: Processing new-game fleets...");
        } else {
            log.info("Smaller Sector: Processing previously missed fleets...");
        }
        int count = 0;

        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI fleet : location.getFleets()) {
                if (fleet == null || fleet.isPlayerFleet()) continue;
                if (FleetInterceptor.hasBeenProcessed(fleet)) continue;

                if (grandfather) {
                    FleetInterceptor.grandfatherFleet(fleet);
                } else {
                    FleetInterceptor.processFleet(fleet);
                }
                count += 1;
            }

            // Deflated route fleets are not returned by getFleets(). On migration
            // loads, tag their persistent memory too so materializing one later
            // cannot reroll a fleet that belonged to the old save.
            if (grandfather) {
                for (FleetStubAPI stub : location.getFleetStubs()) {
                    FleetInterceptor.grandfatherFleetStub(stub);
                }
            }
        }

        Global.getSector().getPersistentData().put(FLEET_MIGRATION_KEY, Boolean.TRUE);

        log.info("Smaller Sector: Initialized " + count + " previously unmarked fleets.");
    }
}
