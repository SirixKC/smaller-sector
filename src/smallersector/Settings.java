package smallersector;

import lunalib.lunaSettings.LunaSettings;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
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

    // Operating cost multipliers
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

    // Build/purchase cost multipliers
    public static float getCruiserBuildCostMult() {
        return LunaSettings.getFloat(MOD_ID, "cruiserBuildCostMult");
    }

    public static float getCapitalBuildCostMult() {
        return LunaSettings.getFloat(MOD_ID, "capitalBuildCostMult");
    }

    public static float getBuildCostMult(HullSize size) {
        if (size == null) return 1.0f;
        switch (size) {
            case CRUISER:
                return getCruiserBuildCostMult();
            case CAPITAL_SHIP:
                return getCapitalBuildCostMult();
            default:
                return 1.0f;
        }
    }

    // D-mod counts for player-built ships
    public static int getCruiserDmodCount() {
        return LunaSettings.getInt(MOD_ID, "cruiserDmodCount");
    }

    public static int getCapitalDmodCount() {
        return LunaSettings.getInt(MOD_ID, "capitalDmodCount");
    }

    public static int getDmodCount(HullSize size) {
        if (size == null) return 0;
        switch (size) {
            case CRUISER:
                return getCruiserDmodCount();
            case CAPITAL_SHIP:
                return getCapitalDmodCount();
            default:
                return 0;
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
