package smallersector;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public class CostModifier {

    public static final String HULLMOD_ID = "smallersector_cost_modifier";

    /**
     * Apply operating cost multipliers to a ship's stats.
     * Called via hull mod that's added to all cruisers/capitals.
     *
     * Production cost and time modifiers are not implemented yet.
     */
    public static void applyToStats(MutableShipStatsAPI stats, HullSize size) {
        if (stats == null || size == null) return;

        if (size == HullSize.CRUISER) {
            float crewMult = Settings.getCruiserCrewMult();
            float supplyMult = Settings.getCruiserSupplyMult();
            float fuelMult = Settings.getCruiserFuelMult();

            applyCrewMultiplier(stats, crewMult);
            stats.getSuppliesPerMonth().modifyMult(HULLMOD_ID, supplyMult);
            stats.getSuppliesToRecover().modifyMult(HULLMOD_ID, supplyMult);
            stats.getFuelUseMod().modifyMult(HULLMOD_ID, fuelMult);

        } else if (size == HullSize.CAPITAL_SHIP) {
            float crewMult = Settings.getCapitalCrewMult();
            float supplyMult = Settings.getCapitalSupplyMult();
            float fuelMult = Settings.getCapitalFuelMult();

            applyCrewMultiplier(stats, crewMult);
            stats.getSuppliesPerMonth().modifyMult(HULLMOD_ID, supplyMult);
            stats.getSuppliesToRecover().modifyMult(HULLMOD_ID, supplyMult);
            stats.getFuelUseMod().modifyMult(HULLMOD_ID, fuelMult);

        }
    }

    /**
     * Apply crew multiplier to min crew only, capping at max crew.
     */
    private static void applyCrewMultiplier(MutableShipStatsAPI stats, float crewMult) {
        // The capped path uses a flat modifier while the normal path uses a
        // multiplier. Clear either previous representation when settings change.
        stats.getMinCrewMod().unmodify(HULLMOD_ID);

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

    /** Add the hidden operating-cost hullmod without mutating a stock template. */
    public static void applyHullModIfNeeded(FleetMemberAPI member) {
        if (member == null || member.getHullSpec() == null || member.getVariant() == null) return;
        if (member.isStation()) return;

        HullSize size = member.getHullSpec().getHullSize();
        if (size != HullSize.CRUISER && size != HullSize.CAPITAL_SHIP) return;
        if (member.getVariant().hasHullMod(HULLMOD_ID)) return;

        ShipVariantAPI variant = VariantUtils.getMutableVariant(member);
        if (variant != null && !variant.hasHullMod(HULLMOD_ID)) {
            variant.addPermaMod(HULLMOD_ID, false);
        }
    }
}
