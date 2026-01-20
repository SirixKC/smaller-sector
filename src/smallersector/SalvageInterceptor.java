package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.listeners.DiscoverEntityListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import java.util.ArrayList;
import java.util.List;

public class SalvageInterceptor implements DiscoverEntityListener {

    @Override
    public void reportEntityDiscovered(SectorEntityToken entity) {
        // Null safety check
        if (entity == null) return;

        // Check if it's a derelict ship or salvageable entity
        if (entity.hasTag(Tags.SALVAGEABLE) || entity.hasTag(Tags.DEBRIS_FIELD)) {
            processDerelict(entity);
        }
    }

    public static void processDerelict(SectorEntityToken entity) {
        if (entity == null) return;

        // Get associated fleet data if present
        CampaignFleetAPI fleet = entity.getContainingFleet();
        if (fleet != null) {
            String factionId = getFactionForDerelict(entity);
            if (factionId != null) {
                processDerelictFleet(fleet, factionId);
            }
        }
    }

    private static String getFactionForDerelict(SectorEntityToken entity) {
        if (entity == null) return null;

        // Try to determine original faction from entity data
        if (entity.getFaction() != null) {
            return entity.getFaction().getId();
        }
        // Default to independent for unknown derelicts
        return "independent";
    }

    private static void processDerelictFleet(CampaignFleetAPI fleet, String factionId) {
        if (fleet == null || fleet.getFleetData() == null) return;

        List<FleetMemberAPI> toRemove = new ArrayList<FleetMemberAPI>();
        List<FleetMemberAPI> toAdd = new ArrayList<FleetMemberAPI>();

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            FleetMemberAPI replacement = ShipReplacer.tryReplace(member, factionId);
            if (replacement != null) {
                toRemove.add(member);
                toAdd.add(replacement);
            }
        }

        for (FleetMemberAPI member : toRemove) {
            fleet.getFleetData().removeFleetMember(member);
        }
        for (FleetMemberAPI member : toAdd) {
            fleet.getFleetData().addFleetMember(member);
        }
    }
}
