package smallersector;

import lunalib.lunaSettings.LunaSettings;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Settings {

    private static final String MOD_ID = "smallersector";
    // Preset names
    private static final String PRESET_CUSTOM = "Custom";
    private static final String PRESET_VANILLA = "Vanilla";
    private static final String PRESET_RECOMMENDED = "Sirix Recommended";
    private static final String PRESET_HARDCORE = "Sirix Hardcore";

    /**
     * Get the currently selected preset.
     */
    public static String getPreset() {
        String val = LunaSettings.getString(MOD_ID, "preset");
        return val != null ? val : PRESET_VANILLA;
    }

    // Cruiser replacement
    public static int getCruiserToFrigate() {
        String preset = getPreset();
        if (PRESET_VANILLA.equals(preset)) return 0;
        if (PRESET_RECOMMENDED.equals(preset)) return 30;
        if (PRESET_HARDCORE.equals(preset)) return 50;
        // Custom - use individual setting
        Integer val = LunaSettings.getInt(MOD_ID, "cruiserToFrigate");
        return val != null ? val : 30;
    }

    public static int getCruiserToDestroyer() {
        String preset = getPreset();
        if (PRESET_VANILLA.equals(preset)) return 0;
        if (PRESET_RECOMMENDED.equals(preset)) return 50;
        if (PRESET_HARDCORE.equals(preset)) return 45;
        // Custom
        Integer val = LunaSettings.getInt(MOD_ID, "cruiserToDestroyer");
        return val != null ? val : 50;
    }

    public static int getCruiserStays() {
        int stays = 100 - getCruiserToFrigate() - getCruiserToDestroyer();
        return Math.max(0, stays);
    }

    // Capital replacement
    public static int getCapitalToFrigate() {
        String preset = getPreset();
        if (PRESET_VANILLA.equals(preset)) return 0;
        if (PRESET_RECOMMENDED.equals(preset)) return 20;
        if (PRESET_HARDCORE.equals(preset)) return 40;
        // Custom
        Integer val = LunaSettings.getInt(MOD_ID, "capitalToFrigate");
        return val != null ? val : 20;
    }

    public static int getCapitalToDestroyer() {
        String preset = getPreset();
        if (PRESET_VANILLA.equals(preset)) return 0;
        if (PRESET_RECOMMENDED.equals(preset)) return 40;
        if (PRESET_HARDCORE.equals(preset)) return 40;
        // Custom
        Integer val = LunaSettings.getInt(MOD_ID, "capitalToDestroyer");
        return val != null ? val : 40;
    }

    public static int getCapitalToCruiser() {
        String preset = getPreset();
        if (PRESET_VANILLA.equals(preset)) return 0;
        if (PRESET_RECOMMENDED.equals(preset)) return 25;
        if (PRESET_HARDCORE.equals(preset)) return 18;
        // Custom
        Integer val = LunaSettings.getInt(MOD_ID, "capitalToCruiser");
        return val != null ? val : 25;
    }

    public static int getCapitalStays() {
        int stays = 100 - getCapitalToFrigate() - getCapitalToDestroyer() - getCapitalToCruiser();
        return Math.max(0, stays);
    }

    // Operating cost multipliers
    public static float getCruiserCrewMult() {
        String preset = getPreset();
        if (PRESET_VANILLA.equals(preset)) return 1.0f;
        if (PRESET_RECOMMENDED.equals(preset)) return 1.5f;
        if (PRESET_HARDCORE.equals(preset)) return 2.0f;
        // Custom
        Double val = LunaSettings.getDouble(MOD_ID, "cruiserCrewMult");
        return val != null ? val.floatValue() : 1.5f;
    }

    public static float getCruiserSupplyMult() {
        String preset = getPreset();
        if (PRESET_VANILLA.equals(preset)) return 1.0f;
        if (PRESET_RECOMMENDED.equals(preset)) return 1.5f;
        if (PRESET_HARDCORE.equals(preset)) return 3.0f;
        // Custom
        Double val = LunaSettings.getDouble(MOD_ID, "cruiserSupplyMult");
        return val != null ? val.floatValue() : 1.5f;
    }

    public static float getCruiserFuelMult() {
        String preset = getPreset();
        if (PRESET_VANILLA.equals(preset)) return 1.0f;
        if (PRESET_RECOMMENDED.equals(preset)) return 1.5f;
        if (PRESET_HARDCORE.equals(preset)) return 3.0f;
        // Custom
        Double val = LunaSettings.getDouble(MOD_ID, "cruiserFuelMult");
        return val != null ? val.floatValue() : 1.5f;
    }

    public static float getCapitalCrewMult() {
        String preset = getPreset();
        if (PRESET_VANILLA.equals(preset)) return 1.0f;
        if (PRESET_RECOMMENDED.equals(preset)) return 2.0f;
        if (PRESET_HARDCORE.equals(preset)) return 3.0f;
        // Custom
        Double val = LunaSettings.getDouble(MOD_ID, "capitalCrewMult");
        return val != null ? val.floatValue() : 2.0f;
    }

    public static float getCapitalSupplyMult() {
        String preset = getPreset();
        if (PRESET_VANILLA.equals(preset)) return 1.0f;
        if (PRESET_RECOMMENDED.equals(preset)) return 2.0f;
        if (PRESET_HARDCORE.equals(preset)) return 4.0f;
        // Custom
        Double val = LunaSettings.getDouble(MOD_ID, "capitalSupplyMult");
        return val != null ? val.floatValue() : 2.0f;
    }

    public static float getCapitalFuelMult() {
        String preset = getPreset();
        if (PRESET_VANILLA.equals(preset)) return 1.0f;
        if (PRESET_RECOMMENDED.equals(preset)) return 2.0f;
        if (PRESET_HARDCORE.equals(preset)) return 4.0f;
        // Custom
        Double val = LunaSettings.getDouble(MOD_ID, "capitalFuelMult");
        return val != null ? val.floatValue() : 2.0f;
    }

    // Build cost multipliers (affects base value)
    public static float getCruiserBuildCostMult() {
        String preset = getPreset();
        if (PRESET_VANILLA.equals(preset)) return 1.0f;
        if (PRESET_RECOMMENDED.equals(preset)) return 1.5f;
        if (PRESET_HARDCORE.equals(preset)) return 3.0f;
        // Custom
        Double val = LunaSettings.getDouble(MOD_ID, "cruiserBuildCostMult");
        return val != null ? val.floatValue() : 1.5f;
    }

    public static float getCapitalBuildCostMult() {
        String preset = getPreset();
        if (PRESET_VANILLA.equals(preset)) return 1.0f;
        if (PRESET_RECOMMENDED.equals(preset)) return 2.0f;
        if (PRESET_HARDCORE.equals(preset)) return 5.0f;
        // Custom
        Double val = LunaSettings.getDouble(MOD_ID, "capitalBuildCostMult");
        return val != null ? val.floatValue() : 2.0f;
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
        String preset = getPreset();
        if (PRESET_VANILLA.equals(preset)) return 0;
        if (PRESET_RECOMMENDED.equals(preset)) return 2;
        if (PRESET_HARDCORE.equals(preset)) return 3;
        // Custom
        Integer val = LunaSettings.getInt(MOD_ID, "cruiserDmodCount");
        return val != null ? val : 2;
    }

    public static int getCapitalDmodCount() {
        String preset = getPreset();
        if (PRESET_VANILLA.equals(preset)) return 0;
        if (PRESET_RECOMMENDED.equals(preset)) return 3;
        if (PRESET_HARDCORE.equals(preset)) return 5;
        // Custom
        Integer val = LunaSettings.getInt(MOD_ID, "capitalDmodCount");
        return val != null ? val : 3;
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
    private static Set<String> userBlacklistCache = null;

    // Default blacklist for Sirix presets
    // These are AI/special factions that shouldn't have their ships replaced
    private static final Set<String> SIRIX_DEFAULT_BLACKLIST;
    static {
        Set<String> defaults = new HashSet<String>();
        // Vanilla factions
        defaults.add("remnant");
        defaults.add("omega");
        defaults.add("derelict");
        // HMI - Nightmarish Horror
        defaults.add("hmi_nightmare");
        // HMI - Mess (Opuntia/Mansa)
        defaults.add("mess");
        // JaydeePiracy - ERROR: DATA INVALID (Deadcomm)
        defaults.add("jdp_deadcomm");
        // Emergent Threats - Threat
        defaults.add("threat");
        // Random Assortment of Things - Abyssals
        defaults.add("rat_abyssals");
        defaults.add("rat_abyssals_primordials");
        defaults.add("rat_abyssals_deep");
        defaults.add("rat_abyssals_harmony");
        defaults.add("rat_abyssals_serenity");
        defaults.add("rat_abyssals_sim");
        defaults.add("rat_abyssals_solitude");
        defaults.add("rat_abyssals_wastes");
        // Secrets of the Frontier - Dreaming Gestalt
        defaults.add("sotf_dreaminggestalt");
        // Elysians
        defaults.add("zea_elysians");
        // Duskborne
        defaults.add("zea_dusk");
        // Dawntide
        defaults.add("zea_dawn");
        // Phase-Warped Omega
        defaults.add("khewarpedomega");
        // MagicLib - Bounty Target
        defaults.add("ml_bounty");
        // Nexerelin - Derelict (Domain Exploration)
        defaults.add("nex_derelict");
        SIRIX_DEFAULT_BLACKLIST = Collections.unmodifiableSet(defaults);
    }

    public static synchronized Set<String> getFactionBlacklist() {
        if (blacklistCache == null) {
            reloadBlacklist();
        }
        return Collections.unmodifiableSet(blacklistCache);
    }

    /** Return only user-managed entries, excluding defaults contributed by a preset. */
    public static synchronized Set<String> getUserFactionBlacklist() {
        if (userBlacklistCache == null) {
            reloadBlacklist();
        }
        return Collections.unmodifiableSet(userBlacklistCache);
    }

    /** Whether the active named preset contributes this faction automatically. */
    public static boolean isPresetDefaultBlacklisted(String factionId) {
        if (factionId == null) return false;
        String preset = getPreset();
        if (!PRESET_RECOMMENDED.equals(preset) && !PRESET_HARDCORE.equals(preset)) {
            return false;
        }
        return SIRIX_DEFAULT_BLACKLIST.contains(factionId.toLowerCase());
    }

    public static synchronized void reloadBlacklist() {
        String preset = getPreset();
        blacklistCache = new HashSet<String>();
        userBlacklistCache = new HashSet<String>();

        // Sirix presets: start with default blacklist (AI/special factions)
        if (PRESET_RECOMMENDED.equals(preset) || PRESET_HARDCORE.equals(preset)) {
            blacklistCache.addAll(SIRIX_DEFAULT_BLACKLIST);
        }

        // Always add user-configured factions regardless of preset
        String raw = LunaSettings.getString(MOD_ID, "factionBlacklist");
        if (raw != null && !raw.trim().isEmpty()) {
            for (String faction : raw.split(",")) {
                String normalized = faction.trim().toLowerCase();
                if (!normalized.isEmpty()) {
                    userBlacklistCache.add(normalized);
                }
            }
        }
        blacklistCache.addAll(userBlacklistCache);
    }

    public static boolean isFactionBlacklisted(String factionId) {
        if (factionId == null) return false;
        return getFactionBlacklist().contains(factionId.toLowerCase());
    }
}
