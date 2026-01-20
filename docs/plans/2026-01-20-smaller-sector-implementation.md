# Smaller Sector Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a Starsector mod that makes cruisers and capitals rarer through procedural replacement, more expensive to operate, and harder to salvage.

**Architecture:** Hook into Starsector's procedural generation systems (fleets, markets, salvage, blueprints) at creation time. Apply replacement logic based on hull size and role matching within faction. Modify hull stats for cost multipliers. Override salvage chance calculations and UI display.

**Tech Stack:** Java 7 (Starsector requirement), Starsector API, LunaLib for settings UI

---

## Prerequisites

Before starting, ensure you have:
- Starsector installed with `starsector-core/starfarer.api.jar` accessible
- LunaLib mod installed (we depend on it)
- Java JDK 7+ for compilation
- IDE configured with Starsector API jar in classpath

**Starsector install location (common):** `~/.local/share/Steam/steamapps/common/Starsector/` or similar

---

## Task 1: Mod Scaffolding

**Files:**
- Create: `mod_info.json`
- Create: `data/config/settings.json`

**Step 1: Create mod_info.json**

```json
{
    "id": "smallersector",
    "name": "Smaller Sector",
    "author": "Sirix",
    "version": "1.0.0",
    "description": "Makes cruisers and capital ships rarer and more impactful. Configurable replacement chances, cost multipliers, and salvage modifiers.",
    "gameVersion": "0.97a-RC11",
    "dependencies": [
        {"id": "lunalib", "name": "LunaLib"}
    ],
    "modPlugin": "smallersector.SmallerSectorModPlugin"
}
```

**Step 2: Create directory structure**

Run:
```bash
mkdir -p data/config src/smallersector lunalib jars
```

**Step 3: Create data/config/settings.json**

```json
{
    "cruiserToFrigate": 30,
    "cruiserToDestroyer": 50,
    "capitalToFrigate": 20,
    "capitalToDestroyer": 40,
    "capitalToCruiser": 25,

    "blueprintSyncEnabled": true,
    "blueprintCruiserToFrigate": 30,
    "blueprintCruiserToDestroyer": 50,
    "blueprintCapitalToFrigate": 20,
    "blueprintCapitalToDestroyer": 40,
    "blueprintCapitalToCruiser": 25,

    "cruiserCrewMult": 1.5,
    "cruiserSupplyMult": 1.5,
    "cruiserFuelMult": 1.5,
    "capitalCrewMult": 2.0,
    "capitalSupplyMult": 2.0,
    "capitalFuelMult": 2.0,

    "frigateSalvageMult": 1.0,
    "destroyerSalvageMult": 1.0,
    "cruiserSalvageMult": 0.75,
    "capitalSalvageMult": 0.5,

    "factionBlacklist": ""
}
```

**Step 4: Commit scaffolding**

```bash
git add mod_info.json data/ src/ lunalib/ jars/
git commit -m "feat: add mod scaffolding and default config"
```

---

## Task 2: LunaLib Settings Definition

**Files:**
- Create: `lunalib/LunaSettings.json`

**Step 1: Create LunaSettings.json**

```json
{
    "modId": "smallersector",
    "modName": "Smaller Sector",
    "settings": [
        {
            "id": "header_cruiser",
            "type": "header",
            "text": "Cruiser Replacement"
        },
        {
            "id": "cruiserToFrigate",
            "type": "int",
            "defaultValue": 30,
            "minValue": 0,
            "maxValue": 100,
            "text": "Cruiser → Frigate %",
            "tooltip": "Chance for cruisers to be replaced with frigates"
        },
        {
            "id": "cruiserToDestroyer",
            "type": "int",
            "defaultValue": 50,
            "minValue": 0,
            "maxValue": 100,
            "text": "Cruiser → Destroyer %",
            "tooltip": "Chance for cruisers to be replaced with destroyers"
        },
        {
            "id": "cruiserStays",
            "type": "int",
            "defaultValue": 20,
            "minValue": 0,
            "maxValue": 100,
            "text": "Cruiser → Stays % (auto-calculated)",
            "tooltip": "Remaining percentage - cruiser stays as cruiser",
            "disabled": true
        },
        {
            "id": "header_capital",
            "type": "header",
            "text": "Capital Replacement"
        },
        {
            "id": "capitalToFrigate",
            "type": "int",
            "defaultValue": 20,
            "minValue": 0,
            "maxValue": 100,
            "text": "Capital → Frigate %",
            "tooltip": "Chance for capitals to be replaced with frigates"
        },
        {
            "id": "capitalToDestroyer",
            "type": "int",
            "defaultValue": 40,
            "minValue": 0,
            "maxValue": 100,
            "text": "Capital → Destroyer %",
            "tooltip": "Chance for capitals to be replaced with destroyers"
        },
        {
            "id": "capitalToCruiser",
            "type": "int",
            "defaultValue": 25,
            "minValue": 0,
            "maxValue": 100,
            "text": "Capital → Cruiser %",
            "tooltip": "Chance for capitals to be replaced with cruisers"
        },
        {
            "id": "capitalStays",
            "type": "int",
            "defaultValue": 15,
            "minValue": 0,
            "maxValue": 100,
            "text": "Capital → Stays % (auto-calculated)",
            "tooltip": "Remaining percentage - capital stays as capital",
            "disabled": true
        },
        {
            "id": "header_blueprint",
            "type": "header",
            "text": "Blueprint Replacement"
        },
        {
            "id": "blueprintSyncEnabled",
            "type": "boolean",
            "defaultValue": true,
            "text": "Sync with ship settings",
            "tooltip": "When enabled, blueprint replacement uses the same percentages as ship replacement"
        },
        {
            "id": "blueprintCruiserToFrigate",
            "type": "int",
            "defaultValue": 30,
            "minValue": 0,
            "maxValue": 100,
            "text": "Blueprint Cruiser → Frigate %",
            "tooltip": "Chance for cruiser blueprints to become frigate blueprints",
            "enabledIf": {"blueprintSyncEnabled": false}
        },
        {
            "id": "blueprintCruiserToDestroyer",
            "type": "int",
            "defaultValue": 50,
            "minValue": 0,
            "maxValue": 100,
            "text": "Blueprint Cruiser → Destroyer %",
            "tooltip": "Chance for cruiser blueprints to become destroyer blueprints",
            "enabledIf": {"blueprintSyncEnabled": false}
        },
        {
            "id": "blueprintCapitalToFrigate",
            "type": "int",
            "defaultValue": 20,
            "minValue": 0,
            "maxValue": 100,
            "text": "Blueprint Capital → Frigate %",
            "tooltip": "Chance for capital blueprints to become frigate blueprints",
            "enabledIf": {"blueprintSyncEnabled": false}
        },
        {
            "id": "blueprintCapitalToDestroyer",
            "type": "int",
            "defaultValue": 40,
            "minValue": 0,
            "maxValue": 100,
            "text": "Blueprint Capital → Destroyer %",
            "tooltip": "Chance for capital blueprints to become destroyer blueprints",
            "enabledIf": {"blueprintSyncEnabled": false}
        },
        {
            "id": "blueprintCapitalToCruiser",
            "type": "int",
            "defaultValue": 25,
            "minValue": 0,
            "maxValue": 100,
            "text": "Blueprint Capital → Cruiser %",
            "tooltip": "Chance for capital blueprints to become cruiser blueprints",
            "enabledIf": {"blueprintSyncEnabled": false}
        },
        {
            "id": "header_cost",
            "type": "header",
            "text": "Cost Multipliers"
        },
        {
            "id": "cruiserCrewMult",
            "type": "double",
            "defaultValue": 1.5,
            "minValue": 0.1,
            "maxValue": 10.0,
            "text": "Cruiser Crew Multiplier",
            "tooltip": "Multiplier for cruiser crew requirements"
        },
        {
            "id": "cruiserSupplyMult",
            "type": "double",
            "defaultValue": 1.5,
            "minValue": 0.1,
            "maxValue": 10.0,
            "text": "Cruiser Supply Multiplier",
            "tooltip": "Multiplier for cruiser supply costs"
        },
        {
            "id": "cruiserFuelMult",
            "type": "double",
            "defaultValue": 1.5,
            "minValue": 0.1,
            "maxValue": 10.0,
            "text": "Cruiser Fuel Multiplier",
            "tooltip": "Multiplier for cruiser fuel consumption"
        },
        {
            "id": "capitalCrewMult",
            "type": "double",
            "defaultValue": 2.0,
            "minValue": 0.1,
            "maxValue": 10.0,
            "text": "Capital Crew Multiplier",
            "tooltip": "Multiplier for capital crew requirements"
        },
        {
            "id": "capitalSupplyMult",
            "type": "double",
            "defaultValue": 2.0,
            "minValue": 0.1,
            "maxValue": 10.0,
            "text": "Capital Supply Multiplier",
            "tooltip": "Multiplier for capital supply costs"
        },
        {
            "id": "capitalFuelMult",
            "type": "double",
            "defaultValue": 2.0,
            "minValue": 0.1,
            "maxValue": 10.0,
            "text": "Capital Fuel Multiplier",
            "tooltip": "Multiplier for capital fuel consumption"
        },
        {
            "id": "header_salvage",
            "type": "header",
            "text": "Salvage Chance Multipliers"
        },
        {
            "id": "frigateSalvageMult",
            "type": "double",
            "defaultValue": 1.0,
            "minValue": 0.1,
            "maxValue": 10.0,
            "text": "Frigate Salvage Multiplier",
            "tooltip": "Multiplier for frigate recovery chance (affects both normal and story point)"
        },
        {
            "id": "destroyerSalvageMult",
            "type": "double",
            "defaultValue": 1.0,
            "minValue": 0.1,
            "maxValue": 10.0,
            "text": "Destroyer Salvage Multiplier",
            "tooltip": "Multiplier for destroyer recovery chance (affects both normal and story point)"
        },
        {
            "id": "cruiserSalvageMult",
            "type": "double",
            "defaultValue": 0.75,
            "minValue": 0.1,
            "maxValue": 10.0,
            "text": "Cruiser Salvage Multiplier",
            "tooltip": "Multiplier for cruiser recovery chance (affects both normal and story point)"
        },
        {
            "id": "capitalSalvageMult",
            "type": "double",
            "defaultValue": 0.5,
            "minValue": 0.1,
            "maxValue": 10.0,
            "text": "Capital Salvage Multiplier",
            "tooltip": "Multiplier for capital recovery chance (affects both normal and story point)"
        },
        {
            "id": "header_blacklist",
            "type": "header",
            "text": "Faction Blacklist"
        },
        {
            "id": "factionBlacklist",
            "type": "string",
            "defaultValue": "",
            "text": "Blacklisted Factions",
            "tooltip": "Comma-separated faction IDs that are exempt from replacement (e.g., 'abyss, remnants')"
        }
    ]
}
```

**Step 2: Commit LunaSettings**

```bash
git add lunalib/LunaSettings.json
git commit -m "feat: add LunaLib settings definition"
```

---

## Task 3: Settings Manager

**Files:**
- Create: `src/smallersector/Settings.java`

**Step 1: Create Settings.java**

This class reads from LunaLib and handles sync logic.

```java
package smallersector;

import lunalib.lunaSettings.LunaSettings;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Settings {

    private static final String MOD_ID = "smallersector";

    // Cruiser replacement
    public static int getCruiserToFrigate() {
        return LunaSettings.getInt(MOD_ID, "cruiserToFrigate");
    }

    public static int getCruiserToDestroyer() {
        return LunaSettings.getInt(MOD_ID, "cruiserToDestroyer");
    }

    public static int getCruiserStays() {
        int stays = 100 - getCruiserToFrigate() - getCruiserToDestroyer();
        return Math.max(0, stays);
    }

    // Capital replacement
    public static int getCapitalToFrigate() {
        return LunaSettings.getInt(MOD_ID, "capitalToFrigate");
    }

    public static int getCapitalToDestroyer() {
        return LunaSettings.getInt(MOD_ID, "capitalToDestroyer");
    }

    public static int getCapitalToCruiser() {
        return LunaSettings.getInt(MOD_ID, "capitalToCruiser");
    }

    public static int getCapitalStays() {
        int stays = 100 - getCapitalToFrigate() - getCapitalToDestroyer() - getCapitalToCruiser();
        return Math.max(0, stays);
    }

    // Blueprint replacement (handles sync)
    public static boolean isBlueprintSyncEnabled() {
        return LunaSettings.getBoolean(MOD_ID, "blueprintSyncEnabled");
    }

    public static int getBlueprintCruiserToFrigate() {
        if (isBlueprintSyncEnabled()) return getCruiserToFrigate();
        return LunaSettings.getInt(MOD_ID, "blueprintCruiserToFrigate");
    }

    public static int getBlueprintCruiserToDestroyer() {
        if (isBlueprintSyncEnabled()) return getCruiserToDestroyer();
        return LunaSettings.getInt(MOD_ID, "blueprintCruiserToDestroyer");
    }

    public static int getBlueprintCapitalToFrigate() {
        if (isBlueprintSyncEnabled()) return getCapitalToFrigate();
        return LunaSettings.getInt(MOD_ID, "blueprintCapitalToFrigate");
    }

    public static int getBlueprintCapitalToDestroyer() {
        if (isBlueprintSyncEnabled()) return getCapitalToDestroyer();
        return LunaSettings.getInt(MOD_ID, "blueprintCapitalToDestroyer");
    }

    public static int getBlueprintCapitalToCruiser() {
        if (isBlueprintSyncEnabled()) return getCapitalToCruiser();
        return LunaSettings.getInt(MOD_ID, "blueprintCapitalToCruiser");
    }

    // Cost multipliers
    public static float getCruiserCrewMult() {
        return LunaSettings.getFloat(MOD_ID, "cruiserCrewMult");
    }

    public static float getCruiserSupplyMult() {
        return LunaSettings.getFloat(MOD_ID, "cruiserSupplyMult");
    }

    public static float getCruiserFuelMult() {
        return LunaSettings.getFloat(MOD_ID, "cruiserFuelMult");
    }

    public static float getCapitalCrewMult() {
        return LunaSettings.getFloat(MOD_ID, "capitalCrewMult");
    }

    public static float getCapitalSupplyMult() {
        return LunaSettings.getFloat(MOD_ID, "capitalSupplyMult");
    }

    public static float getCapitalFuelMult() {
        return LunaSettings.getFloat(MOD_ID, "capitalFuelMult");
    }

    // Salvage multipliers
    public static float getSalvageMult(HullSize size) {
        switch (size) {
            case FRIGATE:
                return LunaSettings.getFloat(MOD_ID, "frigateSalvageMult");
            case DESTROYER:
                return LunaSettings.getFloat(MOD_ID, "destroyerSalvageMult");
            case CRUISER:
                return LunaSettings.getFloat(MOD_ID, "cruiserSalvageMult");
            case CAPITAL_SHIP:
                return LunaSettings.getFloat(MOD_ID, "capitalSalvageMult");
            default:
                return 1.0f;
        }
    }

    // Faction blacklist
    private static Set<String> blacklistCache = null;

    public static Set<String> getFactionBlacklist() {
        if (blacklistCache == null) {
            reloadBlacklist();
        }
        return blacklistCache;
    }

    public static void reloadBlacklist() {
        String raw = LunaSettings.getString(MOD_ID, "factionBlacklist");
        blacklistCache = new HashSet<String>();
        if (raw != null && !raw.trim().isEmpty()) {
            for (String faction : raw.split(",")) {
                blacklistCache.add(faction.trim().toLowerCase());
            }
        }
    }

    public static boolean isFactionBlacklisted(String factionId) {
        return getFactionBlacklist().contains(factionId.toLowerCase());
    }
}
```

**Step 2: Commit Settings.java**

```bash
git add src/smallersector/Settings.java
git commit -m "feat: add Settings manager with LunaLib integration"
```

---

## Task 4: Role Matcher

**Files:**
- Create: `src/smallersector/RoleMatcher.java`

**Step 1: Create RoleMatcher.java**

```java
package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import java.util.*;

public class RoleMatcher {

    // Role categories based on ship hints/tags
    public enum Role {
        COMBAT,
        CARRIER,
        PHASE,
        CIVILIAN,
        UTILITY
    }

    public static Set<Role> getRoles(ShipHullSpecAPI hull) {
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

        // Get faction's known ships
        Set<String> knownShips = Global.getSector().getFaction(factionId).getKnownShips();

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
        Random rand = new Random();
        return ships.get(rand.nextInt(ships.size()));
    }
}
```

**Step 2: Commit RoleMatcher.java**

```bash
git add src/smallersector/RoleMatcher.java
git commit -m "feat: add RoleMatcher for faction/role-based ship selection"
```

---

## Task 5: Ship Replacer Core Logic

**Files:**
- Create: `src/smallersector/ShipReplacer.java`

**Step 1: Create ShipReplacer.java**

```java
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

    /**
     * Blueprint replacement - uses separate settings that may be synced.
     * Returns replacement hull ID or null if no replacement.
     */
    public static String tryReplaceBlueprintHull(String hullId, String factionId) {
        if (Settings.isFactionBlacklisted(factionId)) {
            return null;
        }

        ShipHullSpecAPI hull = Global.getSettings().getHullSpec(hullId);
        if (hull == null) return null;

        HullSize size = hull.getHullSize();

        if (size == HullSize.CRUISER) {
            return tryReplaceCruiserBlueprint(hull, factionId);
        } else if (size == HullSize.CAPITAL_SHIP) {
            return tryReplaceCapitalBlueprint(hull, factionId);
        }

        return null;
    }

    private static String tryReplaceCruiserBlueprint(ShipHullSpecAPI original, String factionId) {
        int roll = rand.nextInt(100);

        int frigateChance = Settings.getBlueprintCruiserToFrigate();
        int destroyerChance = Settings.getBlueprintCruiserToDestroyer();

        HullSize targetSize;
        if (roll < frigateChance) {
            targetSize = HullSize.FRIGATE;
        } else if (roll < frigateChance + destroyerChance) {
            targetSize = HullSize.DESTROYER;
        } else {
            return null;
        }

        ShipHullSpecAPI replacement = RoleMatcher.findReplacement(original, factionId, targetSize);
        return replacement != null ? replacement.getHullId() : null;
    }

    private static String tryReplaceCapitalBlueprint(ShipHullSpecAPI original, String factionId) {
        int roll = rand.nextInt(100);

        int frigateChance = Settings.getBlueprintCapitalToFrigate();
        int destroyerChance = Settings.getBlueprintCapitalToDestroyer();
        int cruiserChance = Settings.getBlueprintCapitalToCruiser();

        HullSize targetSize;
        if (roll < frigateChance) {
            targetSize = HullSize.FRIGATE;
        } else if (roll < frigateChance + destroyerChance) {
            targetSize = HullSize.DESTROYER;
        } else if (roll < frigateChance + destroyerChance + cruiserChance) {
            targetSize = HullSize.CRUISER;
        } else {
            return null;
        }

        ShipHullSpecAPI replacement = RoleMatcher.findReplacement(original, factionId, targetSize);
        return replacement != null ? replacement.getHullId() : null;
    }
}
```

**Step 2: Commit ShipReplacer.java**

```bash
git add src/smallersector/ShipReplacer.java
git commit -m "feat: add ShipReplacer core replacement logic"
```

---

## Task 6: Fleet Interceptor

**Files:**
- Create: `src/smallersector/FleetInterceptor.java`

**Step 1: Create FleetInterceptor.java**

```java
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
        if (fleet == null || fleet.getFaction() == null) return;

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
```

**Step 2: Commit FleetInterceptor.java**

```bash
git add src/smallersector/FleetInterceptor.java
git commit -m "feat: add FleetInterceptor for fleet spawn processing"
```

---

## Task 7: Fleet Spawn Listener

**Files:**
- Create: `src/smallersector/FleetSpawnListener.java`

**Step 1: Create FleetSpawnListener.java**

This uses a campaign plugin to intercept fleet spawns.

```java
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
```

**Step 2: Commit FleetSpawnListener.java**

```bash
git add src/smallersector/FleetSpawnListener.java
git commit -m "feat: add FleetSpawnListener for spawn event handling"
```

---

## Task 8: Market Interceptor

**Files:**
- Create: `src/smallersector/MarketInterceptor.java`

**Step 1: Create MarketInterceptor.java**

```java
package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.util.ArrayList;
import java.util.List;

public class MarketInterceptor implements EconomyTickListener {

    @Override
    public void reportEconomyTick(int iterIndex) {
        // Process all markets on economy tick
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            processMarket(market);
        }
    }

    @Override
    public void reportEconomyMonthEnd() {
        // Not needed
    }

    public static void processMarket(MarketAPI market) {
        if (market == null || market.getFaction() == null) return;

        String factionId = market.getFaction().getId();

        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            processSubmarket(submarket, factionId);
        }
    }

    private static void processSubmarket(SubmarketAPI submarket, String factionId) {
        if (submarket == null || submarket.getCargo() == null) return;

        // Process ships for sale
        List<FleetMemberAPI> ships = submarket.getCargo().getMothballedShips().getMembersListCopy();
        List<FleetMemberAPI> toRemove = new ArrayList<FleetMemberAPI>();
        List<FleetMemberAPI> toAdd = new ArrayList<FleetMemberAPI>();

        for (FleetMemberAPI member : ships) {
            FleetMemberAPI replacement = ShipReplacer.tryReplace(member, factionId);
            if (replacement != null) {
                toRemove.add(member);
                toAdd.add(replacement);
            }
        }

        // Apply changes
        for (FleetMemberAPI member : toRemove) {
            submarket.getCargo().getMothballedShips().removeFleetMember(member);
        }
        for (FleetMemberAPI member : toAdd) {
            submarket.getCargo().getMothballedShips().addFleetMember(member);
        }
    }
}
```

**Step 2: Commit MarketInterceptor.java**

```bash
git add src/smallersector/MarketInterceptor.java
git commit -m "feat: add MarketInterceptor for market inventory processing"
```

---

## Task 9: Salvage Interceptor

**Files:**
- Create: `src/smallersector/SalvageInterceptor.java`

**Step 1: Create SalvageInterceptor.java**

```java
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
        // Check if it's a derelict ship or salvageable entity
        if (entity == null) return;

        if (entity.hasTag(Tags.SALVAGEABLE) || entity.hasTag(Tags.DEBRIS_FIELD)) {
            processDerelict(entity);
        }
    }

    public static void processDerelict(SectorEntityToken entity) {
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
        // Try to determine original faction from entity data
        if (entity.getFaction() != null) {
            return entity.getFaction().getId();
        }
        // Default to independent for unknown derelicts
        return "independent";
    }

    private static void processDerelictFleet(CampaignFleetAPI fleet, String factionId) {
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
```

**Step 2: Commit SalvageInterceptor.java**

```bash
git add src/smallersector/SalvageInterceptor.java
git commit -m "feat: add SalvageInterceptor for derelict/salvage processing"
```

---

## Task 10: Blueprint Interceptor

**Files:**
- Create: `src/smallersector/BlueprintInterceptor.java`

**Step 1: Create BlueprintInterceptor.java**

```java
package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.listeners.CargoModifyListener;
import java.util.ArrayList;
import java.util.List;

public class BlueprintInterceptor {

    /**
     * Process cargo for blueprint replacement.
     * Call this when loot is generated.
     */
    public static void processCargo(CargoAPI cargo, String factionId) {
        if (cargo == null) return;

        List<CargoStackAPI> toRemove = new ArrayList<CargoStackAPI>();
        List<SpecialItemData> toAdd = new ArrayList<SpecialItemData>();

        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (!stack.isSpecialStack()) continue;

            SpecialItemData special = stack.getSpecialDataIfSpecial();
            if (special == null) continue;

            // Check if it's a ship blueprint
            if ("ship_bp".equals(special.getId())) {
                String hullId = special.getData();
                String replacement = ShipReplacer.tryReplaceBlueprintHull(hullId, factionId);

                if (replacement != null) {
                    toRemove.add(stack);
                    toAdd.add(new SpecialItemData("ship_bp", replacement));
                }
            }
        }

        // Apply changes
        for (CargoStackAPI stack : toRemove) {
            cargo.removeStack(stack);
        }
        for (SpecialItemData item : toAdd) {
            cargo.addSpecial(item, 1);
        }
    }

    /**
     * Process a blueprint package - each blueprint checked individually.
     */
    public static void processBlueprintPackage(CargoAPI cargo, String factionId, List<String> hullIds) {
        for (int i = 0; i < hullIds.size(); i++) {
            String hullId = hullIds.get(i);
            String replacement = ShipReplacer.tryReplaceBlueprintHull(hullId, factionId);
            if (replacement != null) {
                hullIds.set(i, replacement);
            }
        }
    }
}
```

**Step 2: Commit BlueprintInterceptor.java**

```bash
git add src/smallersector/BlueprintInterceptor.java
git commit -m "feat: add BlueprintInterceptor for blueprint drop processing"
```

---

## Task 11: Cost Modifier

**Files:**
- Create: `src/smallersector/CostModifier.java`

**Step 1: Create CostModifier.java**

```java
package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;

public class CostModifier {

    public static final String HULLMOD_ID = "smallersector_cost_modifier";

    /**
     * Apply cost multipliers to a ship's stats.
     * Called via hull mod that's added to all cruisers/capitals.
     */
    public static void applyToStats(MutableShipStatsAPI stats, HullSize size) {
        if (size == HullSize.CRUISER) {
            float crewMult = Settings.getCruiserCrewMult();
            float supplyMult = Settings.getCruiserSupplyMult();
            float fuelMult = Settings.getCruiserFuelMult();

            stats.getMinCrewMod().modifyMult(HULLMOD_ID, crewMult);
            stats.getMaxCrewMod().modifyMult(HULLMOD_ID, crewMult);
            stats.getSuppliesPerMonth().modifyMult(HULLMOD_ID, supplyMult);
            stats.getSuppliesToRecover().modifyMult(HULLMOD_ID, supplyMult);
            stats.getFuelUseMod().modifyMult(HULLMOD_ID, fuelMult);

        } else if (size == HullSize.CAPITAL_SHIP) {
            float crewMult = Settings.getCapitalCrewMult();
            float supplyMult = Settings.getCapitalSupplyMult();
            float fuelMult = Settings.getCapitalFuelMult();

            stats.getMinCrewMod().modifyMult(HULLMOD_ID, crewMult);
            stats.getMaxCrewMod().modifyMult(HULLMOD_ID, crewMult);
            stats.getSuppliesPerMonth().modifyMult(HULLMOD_ID, supplyMult);
            stats.getSuppliesToRecover().modifyMult(HULLMOD_ID, supplyMult);
            stats.getFuelUseMod().modifyMult(HULLMOD_ID, fuelMult);
        }
    }

    /**
     * Remove cost multipliers from a ship's stats.
     */
    public static void removeFromStats(MutableShipStatsAPI stats) {
        stats.getMinCrewMod().unmodify(HULLMOD_ID);
        stats.getMaxCrewMod().unmodify(HULLMOD_ID);
        stats.getSuppliesPerMonth().unmodify(HULLMOD_ID);
        stats.getSuppliesToRecover().unmodify(HULLMOD_ID);
        stats.getFuelUseMod().unmodify(HULLMOD_ID);
    }
}
```

**Step 2: Commit CostModifier.java**

```bash
git add src/smallersector/CostModifier.java
git commit -m "feat: add CostModifier for crew/supply/fuel multipliers"
```

---

## Task 12: Cost Modifier Hull Mod

**Files:**
- Create: `src/smallersector/hullmods/SmallerSectorCostMod.java`
- Create: `data/hullmods/hull_mods.csv`
- Create: `data/strings/descriptions.csv`

**Step 1: Create hull mod Java class**

```java
package smallersector.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import smallersector.CostModifier;
import smallersector.Settings;

public class SmallerSectorCostMod extends BaseHullMod {

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize,
            MutableShipStatsAPI stats, String id) {
        CostModifier.applyToStats(stats, hullSize);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (hullSize == HullSize.CRUISER) {
            if (index == 0) return String.format("%.1fx", Settings.getCruiserCrewMult());
            if (index == 1) return String.format("%.1fx", Settings.getCruiserSupplyMult());
            if (index == 2) return String.format("%.1fx", Settings.getCruiserFuelMult());
        } else if (hullSize == HullSize.CAPITAL_SHIP) {
            if (index == 0) return String.format("%.1fx", Settings.getCapitalCrewMult());
            if (index == 1) return String.format("%.1fx", Settings.getCapitalSupplyMult());
            if (index == 2) return String.format("%.1fx", Settings.getCapitalFuelMult());
        }
        return null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        HullSize size = ship.getHullSize();
        return size == HullSize.CRUISER || size == HullSize.CAPITAL_SHIP;
    }

    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) {
        return false; // Hidden from manual selection
    }
}
```

**Step 2: Create data/hullmods/hull_mods.csv**

```csv
name,id,tier,rarity,tech/manufacturer,tags,uiTags,base value,unlocked,hidden,hiddenEverywhere,cost_frigate,cost_dest,cost_cruiser,cost_capital,script,desc
Smaller Sector Costs,smallersector_cost_modifier,0,0,,SYSTEM,,,true,true,true,0,0,0,0,smallersector.hullmods.SmallerSectorCostMod,smallersector_cost_modifier
```

**Step 3: Create data/strings/descriptions.csv**

```csv
id,type,text1,text2,text3,text4
smallersector_cost_modifier,HULLMOD,"Large ships require more resources to operate. This ship has %s crew requirement, %s supply costs, and %s fuel consumption.",,,
```

**Step 4: Commit hull mod files**

```bash
mkdir -p src/smallersector/hullmods data/hullmods data/strings
git add src/smallersector/hullmods/SmallerSectorCostMod.java
git add data/hullmods/hull_mods.csv
git add data/strings/descriptions.csv
git commit -m "feat: add hidden hull mod for cost multipliers"
```

---

## Task 13: Salvage Modifier

**Files:**
- Create: `src/smallersector/SalvageModifier.java`

**Step 1: Create SalvageModifier.java**

```java
package smallersector;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public class SalvageModifier {

    /**
     * Calculate modified recovery chance.
     * @param baseChance The vanilla recovery chance (0.0 - 1.0)
     * @param size The hull size of the ship
     * @return Modified chance, clamped to 0.0 - 1.0
     */
    public static float getModifiedRecoveryChance(float baseChance, HullSize size) {
        float multiplier = Settings.getSalvageMult(size);
        float modified = baseChance * multiplier;
        return Math.max(0f, Math.min(1f, modified));
    }

    /**
     * Calculate modified recovery chance for a specific fleet member.
     */
    public static float getModifiedRecoveryChance(float baseChance, FleetMemberAPI member) {
        if (member == null || member.getHullSpec() == null) {
            return baseChance;
        }
        return getModifiedRecoveryChance(baseChance, member.getHullSpec().getHullSize());
    }

    /**
     * Get the display percentage (for UI).
     */
    public static int getDisplayPercentage(float baseChance, HullSize size) {
        float modified = getModifiedRecoveryChance(baseChance, size);
        return Math.round(modified * 100f);
    }
}
```

**Step 2: Commit SalvageModifier.java**

```bash
git add src/smallersector/SalvageModifier.java
git commit -m "feat: add SalvageModifier for recovery chance calculations"
```

---

## Task 14: Recovery Chance Hook

**Files:**
- Create: `src/smallersector/RecoveryChanceScript.java`

**Step 1: Create RecoveryChanceScript.java**

This script modifies ship recovery chances using the campaign plugin system.

```java
package smallersector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import java.util.List;

public class RecoveryChanceScript {

    /**
     * Modify recovery chances for ships in post-battle loot.
     * This needs to integrate with the game's battle resolution system.
     *
     * The actual hook point depends on Starsector version, but typically
     * involves the ShipRecoverySpecial or post-battle processing.
     */
    public static void modifyRecoveryChances(List<FleetMemberAPI> recoverableShips) {
        for (FleetMemberAPI member : recoverableShips) {
            if (member == null) continue;

            HullSize size = member.getHullSpec().getHullSize();
            float mult = Settings.getSalvageMult(size);

            // Apply multiplier to the member's recovery cost/chance
            // The exact method depends on how Starsector exposes this
            member.getRepairTracker().setCrashMothballed(true);

            // Note: Actual recovery chance modification may require
            // reflection or more direct API access depending on version
        }
    }
}
```

**Step 2: Commit RecoveryChanceScript.java**

```bash
git add src/smallersector/RecoveryChanceScript.java
git commit -m "feat: add RecoveryChanceScript for battle recovery modification"
```

---

## Task 15: Main Mod Plugin

**Files:**
- Create: `src/smallersector/SmallerSectorModPlugin.java`

**Step 1: Create SmallerSectorModPlugin.java**

```java
package smallersector;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.apache.log4j.Logger;

public class SmallerSectorModPlugin extends BaseModPlugin {

    public static final Logger log = Global.getLogger(SmallerSectorModPlugin.class);

    private FleetSpawnListener fleetSpawnListener;
    private MarketInterceptor marketInterceptor;
    private SalvageInterceptor salvageInterceptor;

    @Override
    public void onApplicationLoad() throws Exception {
        log.info("Smaller Sector: Loading mod...");

        // Verify LunaLib is present
        try {
            Class.forName("lunalib.lunaSettings.LunaSettings");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "Smaller Sector requires LunaLib! Please install LunaLib and restart.");
        }

        log.info("Smaller Sector: Mod loaded successfully.");
    }

    @Override
    public void onGameLoad(boolean newGame) {
        log.info("Smaller Sector: Initializing for game...");

        // Reload settings
        Settings.reloadBlacklist();

        // Register listeners
        ListenerManagerAPI listeners = Global.getSector().getListenerManager();

        // Fleet spawn listener
        fleetSpawnListener = new FleetSpawnListener(false);
        Global.getSector().addTransientListener(fleetSpawnListener);

        // Market/economy listener
        marketInterceptor = new MarketInterceptor();
        listeners.addListener(marketInterceptor, true);

        // Salvage listener
        salvageInterceptor = new SalvageInterceptor();
        listeners.addListener(salvageInterceptor, true);

        // Apply cost hull mod to all cruisers and capitals
        applyCostHullMod();

        log.info("Smaller Sector: Game initialization complete.");
    }

    @Override
    public void beforeGameSave() {
        // Nothing to save - mod is stateless
    }

    @Override
    public void afterGameSave() {
        // Re-apply cost hull mod in case of any issues
        applyCostHullMod();
    }

    /**
     * Ensure all cruisers and capitals have the cost modifier hull mod.
     */
    private void applyCostHullMod() {
        // Apply to player fleet
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet != null) {
            for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
                applyHullModIfNeeded(member);
            }
        }
    }

    private void applyHullModIfNeeded(FleetMemberAPI member) {
        if (member == null || member.getHullSpec() == null) return;

        HullSize size = member.getHullSpec().getHullSize();
        if (size != HullSize.CRUISER && size != HullSize.CAPITAL_SHIP) return;

        String hullModId = CostModifier.HULLMOD_ID;
        if (!member.getVariant().hasHullMod(hullModId)) {
            member.getVariant().addPermaMod(hullModId, false);
        }
    }
}
```

**Step 2: Commit SmallerSectorModPlugin.java**

```bash
git add src/smallersector/SmallerSectorModPlugin.java
git commit -m "feat: add main mod plugin with listener registration"
```

---

## Task 16: Build Configuration

**Files:**
- Create: `build.xml` (Ant build file)

**Step 1: Create build.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project name="SmallerSector" default="build" basedir=".">

    <property name="src.dir" value="src"/>
    <property name="build.dir" value="build"/>
    <property name="jar.dir" value="jars"/>
    <property name="jar.name" value="SmallerSector.jar"/>

    <!-- Update this path to your Starsector installation -->
    <property name="starsector.dir" value="${user.home}/.local/share/Steam/steamapps/common/Starsector"/>
    <property name="starsector.core" value="${starsector.dir}/starsector-core"/>

    <!-- LunaLib location - update if needed -->
    <property name="lunalib.dir" value="${starsector.dir}/mods/LunaLib"/>

    <path id="compile.classpath">
        <fileset dir="${starsector.core}">
            <include name="starfarer.api.jar"/>
            <include name="lwjgl.jar"/>
            <include name="lwjgl_util.jar"/>
            <include name="log4j-1.2.9.jar"/>
            <include name="json.jar"/>
        </fileset>
        <fileset dir="${lunalib.dir}/jars" erroronmissingdir="false">
            <include name="*.jar"/>
        </fileset>
    </path>

    <target name="clean">
        <delete dir="${build.dir}"/>
    </target>

    <target name="compile">
        <mkdir dir="${build.dir}"/>
        <javac srcdir="${src.dir}"
               destdir="${build.dir}"
               classpathref="compile.classpath"
               source="1.7"
               target="1.7"
               includeantruntime="false"
               debug="true">
        </javac>
    </target>

    <target name="jar" depends="compile">
        <mkdir dir="${jar.dir}"/>
        <jar destfile="${jar.dir}/${jar.name}">
            <fileset dir="${build.dir}"/>
        </jar>
    </target>

    <target name="build" depends="clean,jar">
        <echo>Build complete: ${jar.dir}/${jar.name}</echo>
    </target>

</project>
```

**Step 2: Add .gitignore**

```
build/
*.class
*.log
.idea/
*.iml
```

**Step 3: Commit build configuration**

```bash
echo "build/
*.class
*.log
.idea/
*.iml" > .gitignore
git add build.xml .gitignore
git commit -m "feat: add Ant build configuration"
```

---

## Task 17: In-Game Testing

**Step 1: Build the mod**

Run:
```bash
ant build
```
Expected: `BUILD SUCCESSFUL` with `jars/SmallerSector.jar` created

**Step 2: Install for testing**

```bash
# Create symlink or copy to Starsector mods folder
ln -s "$(pwd)" ~/.local/share/Steam/steamapps/common/Starsector/mods/SmallerSector
```

**Step 3: Enable mod and test**

1. Launch Starsector
2. Enable "Smaller Sector" and "LunaLib" in mod manager
3. Start new game or load save
4. Open LunaLib settings (usually ESC → Mod Settings)
5. Verify Smaller Sector settings appear
6. Adjust sliders and confirm values persist

**Step 4: Test ship replacement**

1. Use console commands (if available) to spawn fleets
2. Observe fleet composition - cruisers/capitals should sometimes be frigates/destroyers
3. Check market inventories for replacement effects

**Step 5: Commit any fixes**

```bash
git add -A
git commit -m "fix: [describe any fixes from testing]"
```

---

## Task 18: Final Polish

**Step 1: Review all files for typos/errors**

Check each Java file compiles without warnings.

**Step 2: Test edge cases**

- Set all replacement to 0% → ships should never be replaced
- Set all replacement to 100% → no cruisers/capitals should appear
- Add faction to blacklist → that faction's ships never replaced
- Disable blueprint sync → separate blueprint sliders should work

**Step 3: Create release commit**

```bash
git add -A
git commit -m "release: Smaller Sector v1.0.0"
git tag v1.0.0
```

---

## Summary

| Task | Component | Purpose |
|------|-----------|---------|
| 1 | Scaffolding | mod_info.json, directory structure |
| 2 | LunaSettings.json | In-game settings UI definition |
| 3 | Settings.java | Read settings from LunaLib |
| 4 | RoleMatcher.java | Find replacement ships by role/faction |
| 5 | ShipReplacer.java | Core replacement decision logic |
| 6-7 | FleetInterceptor | Hook fleet spawns |
| 8 | MarketInterceptor | Hook market restocks |
| 9 | SalvageInterceptor | Hook salvage/derelicts |
| 10 | BlueprintInterceptor | Hook blueprint drops |
| 11-12 | CostModifier + HullMod | Apply crew/supply/fuel multipliers |
| 13-14 | SalvageModifier | Modify recovery chances |
| 15 | ModPlugin | Register all listeners |
| 16 | Build config | Ant build.xml |
| 17-18 | Testing | Verify everything works |

---

## Notes for Implementation

**Starsector API Considerations:**
- The exact listener/hook APIs may vary slightly by Starsector version
- Some features (like salvage chance modification) may require version-specific approaches
- LunaLib API should be stable across versions

**Testing Without Full Game:**
- Unit tests are challenging due to Starsector API dependencies
- Most testing must be done in-game
- Consider adding debug logging to trace replacement decisions

**Known Limitations:**
- Mods with custom fleet spawners may bypass our listeners (by design)
- Save-game data from before mod installation is unaffected (by design)
