package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import java.util.*;

public class RoleMatcher {

    private static final Random RANDOM = new Random();

    // Role categories based on ship hints/tags
    public enum Role {
        COMBAT,
        CARRIER,
        PHASE,
        CIVILIAN,
        UTILITY
    }

    public static Set<Role> getRoles(ShipHullSpecAPI hull) {
        if (hull == null) {
            Set<Role> empty = new HashSet<Role>();
            empty.add(Role.COMBAT);
            return empty;
        }

        Set<Role> roles = new HashSet<Role>();

        Set<String> tags = hull.getTags();
        Set<String> hints = hull.getHints();

        // Check for carrier
        if (hull.getFighterBays() > 0) {
            roles.add(Role.CARRIER);
        }

        // Check for phase
        if (hull.isPhase()) {
            roles.add(Role.PHASE);
        }

        // Check for civilian/utility
        if (tags.contains("civilian") || hints.contains("CIVILIAN")) {
            roles.add(Role.CIVILIAN);
        }

        // Check hints for combat role
        if (hints.contains("COMBAT") || hints.contains("CARRIER") ||
            hints.contains("LINER") || hints.contains("ASSAULT")) {
            roles.add(Role.COMBAT);
        }

        // Check for freighter/tanker/transport
        if (hull.getCargoFraction() > 0 || hull.getFuelFraction() > 0 ||
            hull.getCrewFraction() > 0.5f) {
            roles.add(Role.UTILITY);
        }

        // Default to combat if no other role identified
        if (roles.isEmpty()) {
            roles.add(Role.COMBAT);
        }

        return roles;
    }

    public static boolean rolesMatch(Set<Role> a, Set<Role> b) {
        for (Role role : a) {
            if (b.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find a replacement ship from the same faction with matching role.
     * Falls back through priority chain as defined in design doc.
     */
    public static ShipHullSpecAPI findReplacement(
            ShipHullSpecAPI original,
            String factionId,
            HullSize targetSize) {

        if (original == null || factionId == null || targetSize == null) {
            return null;
        }

        Set<Role> originalRoles = getRoles(original);
        List<ShipHullSpecAPI> factionShips = getFactionShips(factionId, targetSize);

        // Priority 1: Same faction + target size + matching role
        List<ShipHullSpecAPI> roleMatches = filterByRole(factionShips, originalRoles);
        if (!roleMatches.isEmpty()) {
            return pickRandom(roleMatches);
        }

        // Priority 2: Same faction + target size + any role
        if (!factionShips.isEmpty()) {
            return pickRandom(factionShips);
        }

        // Priority 3 & 4: Try alternate size
        HullSize alternateSize = getAlternateSize(targetSize);
        if (alternateSize != null) {
            List<ShipHullSpecAPI> alternateShips = getFactionShips(factionId, alternateSize);

            // Priority 3: Alternate size + matching role
            List<ShipHullSpecAPI> alternateRoleMatches = filterByRole(alternateShips, originalRoles);
            if (!alternateRoleMatches.isEmpty()) {
                return pickRandom(alternateRoleMatches);
            }

            // Priority 4: Alternate size + any role
            if (!alternateShips.isEmpty()) {
                return pickRandom(alternateShips);
            }
        }

        // Priority 5: No valid replacement found
        return null;
    }

    private static HullSize getAlternateSize(HullSize targetSize) {
        if (targetSize == HullSize.DESTROYER) {
            return HullSize.FRIGATE;
        } else if (targetSize == HullSize.FRIGATE) {
            return HullSize.DESTROYER;
        }
        return null;
    }

    private static List<ShipHullSpecAPI> getFactionShips(String factionId, HullSize size) {
        List<ShipHullSpecAPI> result = new ArrayList<ShipHullSpecAPI>();

        if (factionId == null || Global.getSector() == null) {
            return result;
        }

        com.fs.starfarer.api.campaign.FactionAPI faction = Global.getSector().getFaction(factionId);
        if (faction == null) {
            return result;
        }

        Set<String> knownShips = faction.getKnownShips();

        for (String hullId : knownShips) {
            ShipHullSpecAPI hull = Global.getSettings().getHullSpec(hullId);
            if (hull != null && hull.getHullSize() == size && !hull.isDefaultDHull()) {
                result.add(hull);
            }
        }

        return result;
    }

    private static List<ShipHullSpecAPI> filterByRole(
            List<ShipHullSpecAPI> ships,
            Set<Role> targetRoles) {
        List<ShipHullSpecAPI> result = new ArrayList<ShipHullSpecAPI>();

        for (ShipHullSpecAPI ship : ships) {
            if (rolesMatch(getRoles(ship), targetRoles)) {
                result.add(ship);
            }
        }

        return result;
    }

    private static ShipHullSpecAPI pickRandom(List<ShipHullSpecAPI> ships) {
        if (ships.isEmpty()) return null;
        return ships.get(RANDOM.nextInt(ships.size()));
    }
}
