package smallersector;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class CostModifier {

    public static final String HULLMOD_ID = "smallersector_cost_modifier";

    /**
     * Apply all cost multipliers to a ship's stats.
     * Called via hull mod that's added to all cruisers/capitals.
     */
    public static void applyToStats(MutableShipStatsAPI stats, HullSize size) {
        if (stats == null || size == null) return;

        if (size == HullSize.CRUISER) {
            // Operating costs
            float crewMult = Settings.getCruiserCrewMult();
            float supplyMult = Settings.getCruiserSupplyMult();
            float fuelMult = Settings.getCruiserFuelMult();

            stats.getMinCrewMod().modifyMult(HULLMOD_ID, crewMult);
            stats.getMaxCrewMod().modifyMult(HULLMOD_ID, crewMult);
            stats.getSuppliesPerMonth().modifyMult(HULLMOD_ID, supplyMult);
            stats.getSuppliesToRecover().modifyMult(HULLMOD_ID, supplyMult);
            stats.getFuelUseMod().modifyMult(HULLMOD_ID, fuelMult);

            // Build/purchase cost - affects base value which determines price
            float buildMult = Settings.getCruiserBuildCostMult();
            stats.getDynamic().getMod(Stats.PRODUCTION_COST_MOD).modifyMult(HULLMOD_ID, buildMult);

        } else if (size == HullSize.CAPITAL_SHIP) {
            // Operating costs
            float crewMult = Settings.getCapitalCrewMult();
            float supplyMult = Settings.getCapitalSupplyMult();
            float fuelMult = Settings.getCapitalFuelMult();

            stats.getMinCrewMod().modifyMult(HULLMOD_ID, crewMult);
            stats.getMaxCrewMod().modifyMult(HULLMOD_ID, crewMult);
            stats.getSuppliesPerMonth().modifyMult(HULLMOD_ID, supplyMult);
            stats.getSuppliesToRecover().modifyMult(HULLMOD_ID, supplyMult);
            stats.getFuelUseMod().modifyMult(HULLMOD_ID, fuelMult);

            // Build/purchase cost
            float buildMult = Settings.getCapitalBuildCostMult();
            stats.getDynamic().getMod(Stats.PRODUCTION_COST_MOD).modifyMult(HULLMOD_ID, buildMult);
        }
    }

    /**
     * Remove all cost multipliers from a ship's stats.
     */
    public static void removeFromStats(MutableShipStatsAPI stats) {
        if (stats == null) return;

        stats.getMinCrewMod().unmodify(HULLMOD_ID);
        stats.getMaxCrewMod().unmodify(HULLMOD_ID);
        stats.getSuppliesPerMonth().unmodify(HULLMOD_ID);
        stats.getSuppliesToRecover().unmodify(HULLMOD_ID);
        stats.getFuelUseMod().unmodify(HULLMOD_ID);
        stats.getDynamic().getMod(Stats.PRODUCTION_COST_MOD).unmodify(HULLMOD_ID);
    }
}
