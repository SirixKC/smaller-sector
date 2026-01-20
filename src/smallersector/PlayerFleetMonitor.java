package smallersector;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.HashSet;
import java.util.Set;

/**
 * Monitors the player fleet for new cruisers and capitals.
 * Applies the cost hull mod and D-mods to newly added ships.
 */
public class PlayerFleetMonitor implements EveryFrameScript {

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

    private void checkPlayerFleet() {
        if (Global.getSector() == null) return;

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) return;
        if (playerFleet.getFleetData() == null) return;

        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (member == null) continue;

            String shipId = member.getId();

            // Skip if we've already processed this ship
            if (processedShips.contains(shipId)) {
                continue;
            }

            // Process new ship
            processNewShip(member);
            processedShips.add(shipId);
        }

        // Clean up removed ships from tracking set (prevent memory leak)
        cleanupRemovedShips(playerFleet);
    }

    private void processNewShip(FleetMemberAPI member) {
        if (member.getHullSpec() == null) return;
        if (member.getVariant() == null) return;

        HullSize size = member.getHullSpec().getHullSize();

        // Only process cruisers and capitals
        if (size != HullSize.CRUISER && size != HullSize.CAPITAL_SHIP) {
            return;
        }

        // Apply cost hull mod if not present
        String hullModId = CostModifier.HULLMOD_ID;
        if (!member.getVariant().hasHullMod(hullModId)) {
            member.getVariant().addPermaMod(hullModId, false);
        }

        // Apply D-mods
        DmodApplicator.applyDmodsIfNeeded(member);
    }

    private void cleanupRemovedShips(CampaignFleetAPI playerFleet) {
        // Build set of current ship IDs
        Set<String> currentIds = new HashSet<String>();
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (member != null) {
                currentIds.add(member.getId());
            }
        }

        // Remove IDs that are no longer in the fleet
        processedShips.retainAll(currentIds);
    }
}
