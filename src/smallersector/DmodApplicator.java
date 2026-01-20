package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Applies D-mods to cruisers and capitals when they are added to the player fleet.
 * This simulates the difficulty of maintaining large ships in a resource-scarce sector.
 */
public class DmodApplicator {

    private static final Logger log = Global.getLogger(DmodApplicator.class);
    private static final String DMOD_APPLIED_TAG = "smallersector_dmods_applied";
    private static final Random random = new Random();

    /**
     * Check a fleet member and apply D-mods if needed.
     * Call this when a ship is added to the player fleet.
     */
    public static void applyDmodsIfNeeded(FleetMemberAPI member) {
        if (member == null) return;
        if (member.getVariant() == null) return;
        if (member.getHullSpec() == null) return;

        // Skip if we've already processed this ship
        if (member.getVariant().hasTag(DMOD_APPLIED_TAG)) {
            return;
        }

        HullSize size = member.getHullSpec().getHullSize();
        int targetDmods = Settings.getDmodCount(size);

        // Only apply to cruisers and capitals
        if (targetDmods <= 0) return;

        // Count existing D-mods
        int existingDmods = DModManager.getNumDMods(member.getVariant());

        // Calculate how many to add
        int toAdd = targetDmods - existingDmods;

        if (toAdd > 0) {
            log.info("Smaller Sector: Applying " + toAdd + " D-mods to " +
                member.getShipName() + " (" + member.getHullSpec().getHullName() + ")");

            // Apply random D-mods
            DModManager.addDMods(member, false, toAdd, random);
        }

        // Mark as processed so we don't re-apply
        member.getVariant().addTag(DMOD_APPLIED_TAG);
    }

    /**
     * Process all ships in the player fleet.
     * Call this on game load and periodically.
     */
    public static void processPlayerFleet() {
        if (Global.getSector() == null) return;

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) return;
        if (playerFleet.getFleetData() == null) return;

        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            applyDmodsIfNeeded(member);
        }
    }

    /**
     * Check if a ship already has the D-mod tag (has been processed).
     */
    public static boolean hasBeenProcessed(FleetMemberAPI member) {
        if (member == null || member.getVariant() == null) return true;
        return member.getVariant().hasTag(DMOD_APPLIED_TAG);
    }
}
