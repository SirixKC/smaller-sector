package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import org.apache.log4j.Logger;

import java.util.Random;

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

        // Skip if we've already processed this ship
        if (member.getVariant().hasTag(DMOD_APPLIED_TAG)) {
            return;
        }

        // A member that cannot be evaluated must still not become eligible later
        // due to a refit or another mod changing its variant.
        if (member.getHullSpec() == null) {
            markProcessed(member);
            return;
        }

        // Story, unique, and station ships are never damaged by this mechanic.
        // Mark them too, so removing a tag later cannot apply D-mods retroactively.
        if (isProtected(member)) {
            markProcessed(member);
            return;
        }

        HullSize size = member.getHullSpec().getHullSize();
        int targetDmods = Settings.getDmodCount(size);

        // This includes small hulls and the Vanilla/zero-D-mod setting. Persisting
        // the decision makes a ship's acquisition result independent of later
        // settings changes.
        if (targetDmods <= 0) {
            markProcessed(member);
            return;
        }

        // Recovery and market APIs can return members backed by globally shared
        // stock variants. Clone before DModManager has any opportunity to mutate
        // that template for every ship using it.
        ShipVariantAPI variant = VariantUtils.getMutableVariant(member);
        if (variant == null) return;

        // Count existing D-mods
        int existingDmods = DModManager.getNumDMods(variant);

        // Calculate how many to add
        int toAdd = targetDmods - existingDmods;

        if (toAdd > 0) {
            log.info("Smaller Sector: Applying " + toAdd + " D-mods to " +
                member.getShipName() + " (" + member.getHullSpec().getHullName() + ")");

            // Apply random D-mods
            DModManager.addDMods(member, false, toAdd, random);
        }

        // DModManager may replace or clone the variant internally, so fetch the
        // member's current variant when writing the marker.
        markProcessed(member);
    }

    /**
     * Process all ships in the player fleet.
     * Call this periodically and immediately before saving. Game-load migration
     * must call grandfatherPlayerFleet() instead.
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
     * Grandfather every ship already in the active player fleet on game load.
     * This never touches ships in market storage.
     */
    public static void grandfatherPlayerFleet() {
        if (Global.getSector() == null) return;

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null || playerFleet.getFleetData() == null) return;

        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            markProcessed(member);
        }
    }

    private static void markProcessed(FleetMemberAPI member) {
        ShipVariantAPI variant = VariantUtils.getMutableVariant(member);
        if (variant != null) {
            variant.addTag(DMOD_APPLIED_TAG);
        }
    }

    /** Mark one owned ship as processed without applying any D-mods. */
    public static void markAsProcessedWithoutApplying(FleetMemberAPI member) {
        markProcessed(member);
    }

    /**
     * Check if a ship already has the D-mod tag (has been processed).
     */
    public static boolean hasBeenProcessed(FleetMemberAPI member) {
        if (member == null || member.getVariant() == null) return true;
        return member.getVariant().hasTag(DMOD_APPLIED_TAG);
    }

    private static boolean isProtected(FleetMemberAPI member) {
        return ShipReplacer.isProtectedShip(member);
    }
}
