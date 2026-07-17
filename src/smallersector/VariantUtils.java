package smallersector;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;

/** Utilities for safely mutating variants owned by campaign fleet members. */
public final class VariantUtils {

    private VariantUtils() {
    }

    /**
     * Return a member-owned variant that can be mutated without changing a
     * globally shared stock, goal, or empty-hull template.
     */
    public static ShipVariantAPI getMutableVariant(FleetMemberAPI member) {
        if (member == null || member.getVariant() == null) return null;

        ShipVariantAPI variant = member.getVariant();
        if (variant.isStockVariant() || variant.isGoalVariant() || variant.isEmptyHullVariant()) {
            ShipVariantAPI copy = variant.clone();
            copy.setSource(VariantSource.REFIT);
            member.setVariant(copy, false, false);
            variant = member.getVariant();
        }

        return variant;
    }
}
