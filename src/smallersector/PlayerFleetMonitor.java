package smallersector;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Monitors the player fleet for new cruisers and capitals.
 * Applies the cost hull mod and D-mods to newly added ships.
 */
public class PlayerFleetMonitor implements EveryFrameScript, ShipRecoveryListener {

    // Track ships we've already processed by their unique ID
    private Set<String> processedShips = new HashSet<String>();

    // Check every second instead of every frame for performance
    private float timeSinceLastCheck = 0f;
    private static final float CHECK_INTERVAL = 1.0f;

    @Override
    public boolean isDone() {
        return false; // Never done - runs continuously
    }

    @Override
    public boolean runWhilePaused() {
        return false; // No need to run while paused
    }

    @Override
    public void advance(float amount) {
        timeSinceLastCheck += amount;

        if (timeSinceLastCheck < CHECK_INTERVAL) {
            return;
        }
        timeSinceLastCheck = 0f;

        checkPlayerFleet();
    }

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> ships, InteractionDialogAPI dialog) {
        if (ships == null) return;

        // Recovery occurs while an interaction dialog is paused. Process the
        // returned members synchronously so there is no acquisition/save window.
        for (FleetMemberAPI member : ships) {
            if (member != null && processNewShip(member)) {
                String memberId = member.getId();
                if (memberId != null) {
                    processedShips.add(memberId);
                }
            }
        }
    }

    private void checkPlayerFleet() {
        if (Global.getSector() == null) return;

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) return;
        if (playerFleet.getFleetData() == null) return;

        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (member == null) continue;

            String shipId = member.getId();

            // Skip if we've already processed this ship
            if (shipId != null && processedShips.contains(shipId)) {
                continue;
            }

            // Only cache fully initialized members. A newly-created member can be
            // visible to the campaign API one frame before its variant is ready.
            if (processNewShip(member)) {
                if (shipId != null) {
                    processedShips.add(shipId);
                }
            }
        }

        // Clean up removed ships from tracking set (prevent memory leak)
        cleanupRemovedShips(playerFleet);
    }

    private boolean processNewShip(FleetMemberAPI member) {
        if (member.getHullSpec() == null) return false;
        if (member.getVariant() == null) return false;

        CostModifier.applyHullModIfNeeded(member);

        // Check every hull size. DmodApplicator persistently records zero-D-mod,
        // station, story, and unique exclusions as well as successful rolls.
        DmodApplicator.applyDmodsIfNeeded(member);
        return true;
    }

    private void cleanupRemovedShips(CampaignFleetAPI playerFleet) {
        // Build set of current ship IDs
        Set<String> currentIds = new HashSet<String>();
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (member != null) {
                String memberId = member.getId();
                if (memberId != null) {
                    currentIds.add(memberId);
                }
            }
        }

        // Remove IDs that are no longer in the fleet
        processedShips.retainAll(currentIds);
    }
}
