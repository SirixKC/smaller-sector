package smallersector;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Queues newly added fleets until their spawning call has finished configuring
 * story/mission state. Fleets remain hidden during that short window.
 */
public class FleetSpawnListener extends BaseCampaignEventListener implements EveryFrameScript {

    private final Map<CampaignFleetAPI, Boolean> pendingFleets =
            new LinkedHashMap<CampaignFleetAPI, Boolean>();

    public FleetSpawnListener(boolean permaRegister) {
        super(permaRegister);
    }

    @Override
    public void reportFleetSpawned(CampaignFleetAPI fleet) {
        if (fleet == null) return;
        if (fleet.isPlayerFleet()) return;
        if (FleetInterceptor.hasBeenProcessed(fleet)) return;

        // Starsector reports a fleet from inside LocationAPI.addEntity(), before
        // the caller can attach post-spawn story flags or defender references.
        // Hide it until that call returns, then use the canonical protection path.
        if (!pendingFleets.containsKey(fleet)) {
            pendingFleets.put(fleet, fleet.isHidden());
            fleet.setHidden(true);
        }
    }

    @Override
    public void advance(float amount) {
        processPendingFleets();
    }

    /**
     * Processes the current spawn queue after callers have attached story state.
     */
    public void processPendingFleets() {
        if (pendingFleets.isEmpty()) return;

        Map<CampaignFleetAPI, Boolean> ready =
                new LinkedHashMap<CampaignFleetAPI, Boolean>(pendingFleets);
        pendingFleets.clear();

        for (Map.Entry<CampaignFleetAPI, Boolean> entry : ready.entrySet()) {
            CampaignFleetAPI fleet = entry.getKey();
            try {
                if (fleet != null && fleet.isAlive() && fleet.getContainingLocation() != null) {
                    FleetInterceptor.processFleet(fleet);
                }
            } finally {
                if (fleet != null && fleet.isAlive()) {
                    fleet.setHidden(entry.getValue());
                }
            }
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }
}
