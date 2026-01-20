package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.util.ArrayList;
import java.util.List;

public class FleetInterceptor implements FleetEventListener {

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet,
            FleetDespawnReason reason, Object param) {
        // Not needed
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet,
            CampaignFleetAPI primaryWinner, BattleAPI battle) {
        // Not needed
    }

    /**
     * Process a newly spawned fleet, replacing cruisers/capitals.
     */
    public static void processFleet(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getFaction() == null || fleet.getFleetData() == null) return;

        String factionId = fleet.getFaction().getId();
        List<FleetMemberAPI> toRemove = new ArrayList<FleetMemberAPI>();
        List<FleetMemberAPI> toAdd = new ArrayList<FleetMemberAPI>();

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            FleetMemberAPI replacement = ShipReplacer.tryReplace(member, factionId);
            if (replacement != null) {
                toRemove.add(member);
                toAdd.add(replacement);
            }
        }

        // Apply changes
        for (FleetMemberAPI member : toRemove) {
            fleet.getFleetData().removeFleetMember(member);
        }
        for (FleetMemberAPI member : toAdd) {
            fleet.getFleetData().addFleetMember(member);
        }
    }
}
