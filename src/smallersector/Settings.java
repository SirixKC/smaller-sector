package smallersector;

import lunalib.lunaSettings.LunaSettings;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import java.util.Arrays;
import java.util.Collections;
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

    public static synchronized Set<String> getFactionBlacklist() {
        if (blacklistCache == null) {
            reloadBlacklist();
        }
        return Collections.unmodifiableSet(blacklistCache);
    }

    public static synchronized void reloadBlacklist() {
        String raw = LunaSettings.getString(MOD_ID, "factionBlacklist");
        blacklistCache = new HashSet<String>();
        if (raw != null && !raw.trim().isEmpty()) {
            for (String faction : raw.split(",")) {
                blacklistCache.add(faction.trim().toLowerCase());
            }
        }
    }

    public static boolean isFactionBlacklisted(String factionId) {
        if (factionId == null) return false;
        return getFactionBlacklist().contains(factionId.toLowerCase());
    }
}
