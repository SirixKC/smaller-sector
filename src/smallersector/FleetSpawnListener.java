package smallersector;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;

public class FleetSpawnListener extends BaseCampaignEventListener {

    public FleetSpawnListener(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportFleetSpawned(CampaignFleetAPI fleet) {
        // Null safety check
        if (fleet == null) return;

        // Skip player fleet
        if (fleet.isPlayerFleet()) return;

        // Skip fleets with memory flags indicating scripted/story spawns
        com.fs.starfarer.api.campaign.rules.MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        if (memory != null &&
            (memory.contains("$defenderFleet") ||
             memory.contains("$storyFleet") ||
             memory.contains("$missionFleet"))) {
            return;
        }

        FleetInterceptor.processFleet(fleet);
    }
}
