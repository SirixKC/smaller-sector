package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import java.util.Random;

public class ShipReplacer {

    private static final Random rand = new Random();

    /**
     * Attempts to replace a fleet member with a smaller ship.
     * Returns null if ship should not be replaced.
     */
    public static FleetMemberAPI tryReplace(FleetMemberAPI original, String factionId) {
        if (original == null || original.getHullSpec() == null) {
            return null;
        }

        // Check faction blacklist
        if (Settings.isFactionBlacklisted(factionId)) {
            return null;
        }

        HullSize size = original.getHullSpec().getHullSize();

        if (size == HullSize.CRUISER) {
            return tryReplaceCruiser(original, factionId);
        } else if (size == HullSize.CAPITAL_SHIP) {
            return tryReplaceCapital(original, factionId);
        }

        return null;
    }

    private static FleetMemberAPI tryReplaceCruiser(FleetMemberAPI original, String factionId) {
        int roll = rand.nextInt(100);

        int frigateChance = Settings.getCruiserToFrigate();
        int destroyerChance = Settings.getCruiserToDestroyer();

        HullSize targetSize;
        if (roll < frigateChance) {
            targetSize = HullSize.FRIGATE;
        } else if (roll < frigateChance + destroyerChance) {
            targetSize = HullSize.DESTROYER;
        } else {
            return null; // Stays cruiser
        }

        return createReplacement(original, factionId, targetSize);
    }

    private static FleetMemberAPI tryReplaceCapital(FleetMemberAPI original, String factionId) {
        int roll = rand.nextInt(100);

        int frigateChance = Settings.getCapitalToFrigate();
        int destroyerChance = Settings.getCapitalToDestroyer();
        int cruiserChance = Settings.getCapitalToCruiser();

        HullSize targetSize;
        if (roll < frigateChance) {
            targetSize = HullSize.FRIGATE;
        } else if (roll < frigateChance + destroyerChance) {
            targetSize = HullSize.DESTROYER;
        } else if (roll < frigateChance + destroyerChance + cruiserChance) {
            targetSize = HullSize.CRUISER;
            // Note: Capitals becoming cruisers are NOT re-checked
        } else {
            return null; // Stays capital
        }

        return createReplacement(original, factionId, targetSize);
    }

    private static FleetMemberAPI createReplacement(
            FleetMemberAPI original,
            String factionId,
            HullSize targetSize) {

        if (original == null || original.getHullSpec() == null) {
            return null;
        }

        ShipHullSpecAPI replacement = RoleMatcher.findReplacement(
            original.getHullSpec(),
            factionId,
            targetSize
        );

        if (replacement == null) {
            return null; // No valid replacement, keep original
        }

        // Create new fleet member with replacement hull
        FleetMemberAPI newMember = Global.getFactory().createFleetMember(
            FleetMemberType.SHIP,
            replacement.getHullId() + "_Hull"
        );

        // Copy relevant properties from original
        if (original.getCaptain() != null) {
            newMember.setCaptain(original.getCaptain());
        }

        return newMember;
    }

}
