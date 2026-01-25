package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.apache.log4j.Logger;

public class CostModifier {

    private static final Logger log = Global.getLogger(CostModifier.class);
    public static final String HULLMOD_ID = "smallersector_cost_modifier";

    /**
     * Apply operating cost multipliers to a ship's stats.
     * Called via hull mod that's added to all cruisers/capitals.
     *
     * Note: Build/production costs are handled separately by BaseValueModifier,
     * which modifies the hull spec's base value directly.
     */
    public static void applyToStats(MutableShipStatsAPI stats, HullSize size) {
        if (stats == null || size == null) return;

        // Get ship name for logging
        String shipName = "unknown";
        if (stats.getFleetMember() != null) {
            shipName = stats.getFleetMember().getShipName();
        }

        if (size == HullSize.CRUISER) {
            float crewMult = Settings.getCruiserCrewMult();
            float supplyMult = Settings.getCruiserSupplyMult();
            float fuelMult = Settings.getCruiserFuelMult();

            applyCrewMultiplier(stats, crewMult);
            stats.getSuppliesPerMonth().modifyMult(HULLMOD_ID, supplyMult);
            stats.getSuppliesToRecover().modifyMult(HULLMOD_ID, supplyMult);
            stats.getFuelUseMod().modifyMult(HULLMOD_ID, fuelMult);

            log.info("Smaller Sector: [" + shipName + "] CRUISER - applied multipliers: crew=" + crewMult +
                     ", supply=" + supplyMult + ", fuel=" + fuelMult);

        } else if (size == HullSize.CAPITAL_SHIP) {
            float crewMult = Settings.getCapitalCrewMult();
            float supplyMult = Settings.getCapitalSupplyMult();
            float fuelMult = Settings.getCapitalFuelMult();

            applyCrewMultiplier(stats, crewMult);
            stats.getSuppliesPerMonth().modifyMult(HULLMOD_ID, supplyMult);
            stats.getSuppliesToRecover().modifyMult(HULLMOD_ID, supplyMult);
            stats.getFuelUseMod().modifyMult(HULLMOD_ID, fuelMult);

            log.info("Smaller Sector: [" + shipName + "] CAPITAL - applied multipliers: crew=" + crewMult +
                     ", supply=" + supplyMult + ", fuel=" + fuelMult);
        }
    }

    /**
     * Apply crew multiplier to min crew only, capping at max crew.
     */
    private static void applyCrewMultiplier(MutableShipStatsAPI stats, float crewMult) {
        // Get base values from hull spec if available
        float baseMinCrew = 0;
        float baseMaxCrew = 0;

        if (stats.getFleetMember() != null && stats.getFleetMember().getHullSpec() != null) {
            baseMinCrew = stats.getFleetMember().getHullSpec().getMinCrew();
            baseMaxCrew = stats.getFleetMember().getHullSpec().getMaxCrew();
        }

        // Calculate what min crew would be after multiplier
        float newMinCrew = baseMinCrew * crewMult;

        // Cap at max crew
        if (baseMaxCrew > 0 && newMinCrew > baseMaxCrew) {
            // Use flat modifier to set min crew to max crew
            float flatBonus = baseMaxCrew - baseMinCrew;
            stats.getMinCrewMod().modifyFlat(HULLMOD_ID, flatBonus);
            log.info("Smaller Sector: Capped min crew at max crew (" + baseMaxCrew + ")");
        } else {
            // Apply the multiplier normally
            stats.getMinCrewMod().modifyMult(HULLMOD_ID, crewMult);
        }
    }

    /**
     * Remove all operating cost multipliers from a ship's stats.
     */
    public static void removeFromStats(MutableShipStatsAPI stats) {
        if (stats == null) return;

        stats.getMinCrewMod().unmodify(HULLMOD_ID);
        stats.getMaxCrewMod().unmodify(HULLMOD_ID);
        stats.getSuppliesPerMonth().unmodify(HULLMOD_ID);
        stats.getSuppliesToRecover().unmodify(HULLMOD_ID);
        stats.getFuelUseMod().unmodify(HULLMOD_ID);
    }
}
