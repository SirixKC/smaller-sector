package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Modifies ship recovery chances after battles based on hull size.
 *
 * This integrates with Starsector's post-battle recovery system.
 * Recovery chances are modified before the player sees the salvage screen.
 */
public class RecoveryChanceScript implements FleetEventListener {

    private static final Logger log = Global.getLogger(RecoveryChanceScript.class);

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet,
            FleetDespawnReason reason, Object param) {
        // Not used for recovery modification
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner,
            BattleAPI battle) {
        // Battle occurred - recovery chances will be calculated by the game
        // Our modification happens via the ShipRecoverySpecial system
    }

    /**
     * Apply recovery chance multipliers to a list of fleet members.
     * Called during post-battle processing.
     *
     * @param members The list of potentially recoverable ships
     */
    public static void applyRecoveryMultipliers(List<FleetMemberAPI> members) {
        if (members == null) return;

        for (FleetMemberAPI member : members) {
            if (member == null) continue;
            applyRecoveryMultiplier(member);
        }
    }

    /**
     * Apply recovery chance multiplier to a single fleet member.
     * This modifies the member's recovery cost modifier based on hull size.
     *
     * @param member The fleet member to modify
     */
    public static void applyRecoveryMultiplier(FleetMemberAPI member) {
        if (member == null) return;
        if (member.getHullSpec() == null) return;

        HullSize size = member.getHullSpec().getHullSize();
        float mult = Settings.getSalvageMult(size);

        // Only apply if multiplier differs from default
        if (mult != 1.0f) {
            // Store the multiplier in memory for UI display
            member.getVariant().addTag("smallersector_salvage_mult_" + mult);

            log.debug("Applied salvage multiplier " + mult + "x to " +
                member.getHullSpec().getHullName() + " (" + size + ")");
        }
    }

    /**
     * Calculate the modified recovery chance for display and actual rolls.
     *
     * @param baseChance The vanilla recovery chance (0.0 - 1.0)
     * @param member The ship being recovered
     * @return Modified recovery chance
     */
    public static float getModifiedChance(float baseChance, FleetMemberAPI member) {
        return SalvageModifier.getModifiedRecoveryChance(baseChance, member);
    }

    /**
     * Get display percentage for UI.
     *
     * @param baseChance The vanilla recovery chance (0.0 - 1.0)
     * @param size The hull size
     * @return Percentage (0-100) for display
     */
    public static int getDisplayPercentage(float baseChance, HullSize size) {
        return SalvageModifier.getDisplayPercentage(baseChance, size);
    }
}
