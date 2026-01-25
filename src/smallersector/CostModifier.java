package smallersector;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class CostModifier {

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

        if (size == HullSize.CRUISER) {
            float crewMult = Settings.getCruiserCrewMult();
            float supplyMult = Settings.getCruiserSupplyMult();
            float fuelMult = Settings.getCruiserFuelMult();

            stats.getMinCrewMod().modifyMult(HULLMOD_ID, crewMult);
            stats.getMaxCrewMod().modifyMult(HULLMOD_ID, crewMult);
            stats.getSuppliesPerMonth().modifyMult(HULLMOD_ID, supplyMult);
            stats.getSuppliesToRecover().modifyMult(HULLMOD_ID, supplyMult);
            stats.getFuelUseMod().modifyMult(HULLMOD_ID, fuelMult);

        } else if (size == HullSize.CAPITAL_SHIP) {
            float crewMult = Settings.getCapitalCrewMult();
            float supplyMult = Settings.getCapitalSupplyMult();
            float fuelMult = Settings.getCapitalFuelMult();

            stats.getMinCrewMod().modifyMult(HULLMOD_ID, crewMult);
            stats.getMaxCrewMod().modifyMult(HULLMOD_ID, crewMult);
            stats.getSuppliesPerMonth().modifyMult(HULLMOD_ID, supplyMult);
            stats.getSuppliesToRecover().modifyMult(HULLMOD_ID, supplyMult);
            stats.getFuelUseMod().modifyMult(HULLMOD_ID, fuelMult);
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
