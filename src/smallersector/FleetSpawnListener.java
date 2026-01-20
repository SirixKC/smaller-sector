package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;

public class FleetSpawnListener extends BaseCampaignEventListener {

    public FleetSpawnListener(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportFleetSpawned(CampaignFleetAPI fleet) {
        // Skip player fleet
        if (fleet.isPlayerFleet()) return;

        // Skip fleets with memory flags indicating scripted/story spawns
        if (fleet.getMemoryWithoutUpdate().contains("$defenderFleet") ||
            fleet.getMemoryWithoutUpdate().contains("$storyFleet") ||
            fleet.getMemoryWithoutUpdate().contains("$missionFleet")) {
            return;
        }

        FleetInterceptor.processFleet(fleet);
    }
}
