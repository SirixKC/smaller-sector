package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
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
        EnumSet<ShipTypeHints> hints = hull.getHints();

        // Check for carrier
        if (hull.getFighterBays() > 0) {
            roles.add(Role.CARRIER);
        }

        // Check for phase
        if (hull.isPhase() || hints.contains(ShipTypeHints.PHASE)) {
            roles.add(Role.PHASE);
        }

        // Check for civilian
        if (tags.contains("civilian") || hints.contains(ShipTypeHints.CIVILIAN)) {
            roles.add(Role.CIVILIAN);
        }

        // Check hints for combat role
        if (hints.contains(ShipTypeHints.COMBAT) || hints.contains(ShipTypeHints.CARRIER)) {
            roles.add(Role.COMBAT);
        }

        // Check for freighter/tanker/transport (utility ships)
        if (hints.contains(ShipTypeHints.FREIGHTER) ||
            hints.contains(ShipTypeHints.TANKER) ||
            hints.contains(ShipTypeHints.LINER) ||
            hints.contains(ShipTypeHints.TRANSPORT)) {
            roles.add(Role.UTILITY);
        }

        // Also check if ship has significant cargo/fuel capacity relative to combat stats
        if (hull.getCargo() > 100 || hull.getFuel() > 100) {
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
     * Get the design type of a hull (e.g., "Low Tech", "High Tech", "Midline", "XIV Battlegroup").
     * Returns null if design type cannot be determined.
     */
    public static String getDesignType(ShipHullSpecAPI hull) {
        if (hull == null) {
            return null;
        }
        String manufacturer = hull.getManufacturer();
        // Some hulls may have empty or null manufacturer
        if (manufacturer == null || manufacturer.isEmpty()) {
            return null;
        }
        return manufacturer;
    }

    /**
     * Check if two design types match.
     * Returns true if both are null/empty, or if they're equal (case-insensitive).
     */
    public static boolean designTypesMatch(String a, String b) {
        // If original has no design type, any replacement is fine
        if (a == null || a.isEmpty()) {
            return true;
        }
        // If candidate has no design type, it doesn't match a specific design type
        if (b == null || b.isEmpty()) {
            return false;
        }
        return a.equalsIgnoreCase(b);
    }

    /**
     * Find a replacement ship from the same faction with matching role and design type.
     * Falls back through priority chain, relaxing design type requirement if no match found.
     *
     * Priority chain:
     * 1. faction + size + role + designType
     * 2. faction + size + role (any design type)
     * 3. faction + size + designType
     * 4. faction + size (any design type)
     * 5. faction + altSize + role + designType
     * 6. faction + altSize + role (any design type)
     * 7. faction + altSize + designType
     * 8. faction + altSize (any design type)
     * 9. No valid replacement
     */
    public static ShipHullSpecAPI findReplacement(
            ShipHullSpecAPI original,
            String factionId,
            HullSize targetSize) {

        if (original == null || factionId == null || targetSize == null) {
            return null;
        }

        Set<Role> originalRoles = getRoles(original);
        String originalDesignType = getDesignType(original);
        List<ShipHullSpecAPI> factionShips = getFactionShips(factionId, targetSize);

        // Priority 1: Same faction + target size + matching role + matching design type
        List<ShipHullSpecAPI> roleMatches = filterByRole(factionShips, originalRoles);
        List<ShipHullSpecAPI> roleAndTypeMatches = filterByDesignType(roleMatches, originalDesignType);
        if (!roleAndTypeMatches.isEmpty()) {
            return pickRandom(roleAndTypeMatches);
        }

        // Priority 2: Same faction + target size + matching role (any design type)
        if (!roleMatches.isEmpty()) {
            return pickRandom(roleMatches);
        }

        // Priority 3: Same faction + target size + matching design type (any role)
        List<ShipHullSpecAPI> typeMatches = filterByDesignType(factionShips, originalDesignType);
        if (!typeMatches.isEmpty()) {
            return pickRandom(typeMatches);
        }

        // Priority 4: Same faction + target size + any role (any design type)
        if (!factionShips.isEmpty()) {
            return pickRandom(factionShips);
        }

        // Try alternate size
        HullSize alternateSize = getAlternateSize(targetSize);
        if (alternateSize != null) {
            List<ShipHullSpecAPI> alternateShips = getFactionShips(factionId, alternateSize);

            // Priority 5: Alternate size + matching role + matching design type
            List<ShipHullSpecAPI> altRoleMatches = filterByRole(alternateShips, originalRoles);
            List<ShipHullSpecAPI> altRoleAndTypeMatches = filterByDesignType(altRoleMatches, originalDesignType);
            if (!altRoleAndTypeMatches.isEmpty()) {
                return pickRandom(altRoleAndTypeMatches);
            }

            // Priority 6: Alternate size + matching role (any design type)
            if (!altRoleMatches.isEmpty()) {
                return pickRandom(altRoleMatches);
            }

            // Priority 7: Alternate size + matching design type (any role)
            List<ShipHullSpecAPI> altTypeMatches = filterByDesignType(alternateShips, originalDesignType);
            if (!altTypeMatches.isEmpty()) {
                return pickRandom(altTypeMatches);
            }

            // Priority 8: Alternate size + any role (any design type)
            if (!alternateShips.isEmpty()) {
                return pickRandom(alternateShips);
            }
        }

        // Priority 9: No valid replacement found
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

    private static List<ShipHullSpecAPI> filterByDesignType(
            List<ShipHullSpecAPI> ships,
            String targetDesignType) {
        List<ShipHullSpecAPI> result = new ArrayList<ShipHullSpecAPI>();

        for (ShipHullSpecAPI ship : ships) {
            if (designTypesMatch(targetDesignType, getDesignType(ship))) {
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
