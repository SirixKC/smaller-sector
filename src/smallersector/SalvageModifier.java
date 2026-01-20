package smallersector;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public class SalvageModifier {

    /**
     * Calculate modified recovery chance.
     * @param baseChance The vanilla recovery chance (0.0 - 1.0)
     * @param size The hull size of the ship
     * @return Modified chance, clamped to 0.0 - 1.0
     */
    public static float getModifiedRecoveryChance(float baseChance, HullSize size) {
        if (size == null) return baseChance;

        float multiplier = Settings.getSalvageMult(size);
        float modified = baseChance * multiplier;
        return Math.max(0f, Math.min(1f, modified));
    }

    /**
     * Calculate modified recovery chance for a specific fleet member.
     */
    public static float getModifiedRecoveryChance(float baseChance, FleetMemberAPI member) {
        if (member == null || member.getHullSpec() == null) {
            return baseChance;
        }
        return getModifiedRecoveryChance(baseChance, member.getHullSpec().getHullSize());
    }

    /**
     * Get the display percentage (for UI).
     */
    public static int getDisplayPercentage(float baseChance, HullSize size) {
        float modified = getModifiedRecoveryChance(baseChance, size);
        return Math.round(modified * 100f);
    }
}
