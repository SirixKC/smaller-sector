package smallersector;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import lunalib.backend.ui.settings.LunaSettingsLoader;
import lunalib.backend.ui.settings.LunaSettingsData;
import lunalib.backend.ui.settings.LunaSettingsUISettingsPanel;
import lunalib.backend.ui.settings.ChangedSetting;
import lunalib.backend.ui.components.base.LunaUIBaseElement;
import lunalib.backend.ui.components.LunaUITextFieldWithSlider;
import lunalib.backend.ui.components.base.LunaUITextField;
import org.lazywizard.lazylib.JSONUtils;
import org.lazywizard.lazylib.JSONUtils.CommonDataJSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Listens for preset changes and updates all individual settings to match.
 * This ensures the UI sliders reflect the actual preset values.
 */
public class PresetListener implements LunaSettingsListener {

    private static final String MOD_ID = "smallersector";

    // Track the last preset to detect changes
    private String lastPreset = null;

    // Preset values stored as maps for easy lookup
    private static final Map<String, Object> VANILLA_VALUES = new HashMap<>();
    private static final Map<String, Object> RECOMMENDED_VALUES = new HashMap<>();
    private static final Map<String, Object> HARDCORE_VALUES = new HashMap<>();

    private static final String SIRIX_BLACKLIST = "remnant,omega,derelict,hmi_nightmare,mess,jdp_deadcomm,threat,rat_abyssals,rat_abyssals_primordials,rat_abyssals_deep,rat_abyssals_harmony,rat_abyssals_serenity,rat_abyssals_sim,rat_abyssals_solitude,rat_abyssals_wastes,sotf_dreaminggestalt,zea_elysians,zea_dusk,zea_dawn,khewarpedomega,ml_bounty,nex_derelict";

    static {
        // Vanilla preset - no modifications
        VANILLA_VALUES.put("cruiserToFrigate", 0);
        VANILLA_VALUES.put("cruiserToDestroyer", 0);
        VANILLA_VALUES.put("capitalToFrigate", 0);
        VANILLA_VALUES.put("capitalToDestroyer", 0);
        VANILLA_VALUES.put("capitalToCruiser", 0);
        VANILLA_VALUES.put("cruiserCrewMult", 1.0);
        VANILLA_VALUES.put("cruiserSupplyMult", 1.0);
        VANILLA_VALUES.put("cruiserFuelMult", 1.0);
        VANILLA_VALUES.put("capitalCrewMult", 1.0);
        VANILLA_VALUES.put("capitalSupplyMult", 1.0);
        VANILLA_VALUES.put("capitalFuelMult", 1.0);
        VANILLA_VALUES.put("cruiserBuildCostMult", 1.0);
        VANILLA_VALUES.put("capitalBuildCostMult", 1.0);
        VANILLA_VALUES.put("cruiserDmodCount", 0);
        VANILLA_VALUES.put("capitalDmodCount", 0);
        VANILLA_VALUES.put("factionBlacklist", "");

        // Sirix Recommended preset
        RECOMMENDED_VALUES.put("cruiserToFrigate", 30);
        RECOMMENDED_VALUES.put("cruiserToDestroyer", 50);
        RECOMMENDED_VALUES.put("capitalToFrigate", 20);
        RECOMMENDED_VALUES.put("capitalToDestroyer", 40);
        RECOMMENDED_VALUES.put("capitalToCruiser", 25);
        RECOMMENDED_VALUES.put("cruiserCrewMult", 1.5);
        RECOMMENDED_VALUES.put("cruiserSupplyMult", 1.5);
        RECOMMENDED_VALUES.put("cruiserFuelMult", 1.5);
        RECOMMENDED_VALUES.put("capitalCrewMult", 2.0);
        RECOMMENDED_VALUES.put("capitalSupplyMult", 2.0);
        RECOMMENDED_VALUES.put("capitalFuelMult", 2.0);
        RECOMMENDED_VALUES.put("cruiserBuildCostMult", 1.5);
        RECOMMENDED_VALUES.put("capitalBuildCostMult", 2.0);
        RECOMMENDED_VALUES.put("cruiserDmodCount", 2);
        RECOMMENDED_VALUES.put("capitalDmodCount", 3);
        RECOMMENDED_VALUES.put("factionBlacklist", SIRIX_BLACKLIST);

        // Sirix Hardcore preset
        HARDCORE_VALUES.put("cruiserToFrigate", 50);
        HARDCORE_VALUES.put("cruiserToDestroyer", 45);
        HARDCORE_VALUES.put("capitalToFrigate", 40);
        HARDCORE_VALUES.put("capitalToDestroyer", 40);
        HARDCORE_VALUES.put("capitalToCruiser", 18);
        HARDCORE_VALUES.put("cruiserCrewMult", 2.0);
        HARDCORE_VALUES.put("cruiserSupplyMult", 3.0);
        HARDCORE_VALUES.put("cruiserFuelMult", 3.0);
        HARDCORE_VALUES.put("capitalCrewMult", 3.0);
        HARDCORE_VALUES.put("capitalSupplyMult", 4.0);
        HARDCORE_VALUES.put("capitalFuelMult", 4.0);
        HARDCORE_VALUES.put("cruiserBuildCostMult", 3.0);
        HARDCORE_VALUES.put("capitalBuildCostMult", 5.0);
        HARDCORE_VALUES.put("cruiserDmodCount", 3);
        HARDCORE_VALUES.put("capitalDmodCount", 5);
        HARDCORE_VALUES.put("factionBlacklist", SIRIX_BLACKLIST);
    }

    @Override
    public void settingsChanged(String modID) {
        if (!MOD_ID.equals(modID)) return;

        String currentPreset = LunaSettings.getString(MOD_ID, "preset");
        if (currentPreset == null) currentPreset = "Sirix Recommended";

        // Only act if preset actually changed
        if (currentPreset.equals(lastPreset)) {
            lastPreset = currentPreset;
            return;
        }

        lastPreset = currentPreset;

        // Don't modify values for Custom preset - user controls those
        if ("Custom".equals(currentPreset)) {
            return;
        }

        // Get the preset values map
        Map<String, Object> presetValues;
        switch (currentPreset) {
            case "Vanilla":
                presetValues = VANILLA_VALUES;
                break;
            case "Sirix Recommended":
                presetValues = RECOMMENDED_VALUES;
                break;
            case "Sirix Hardcore":
                presetValues = HARDCORE_VALUES;
                break;
            default:
                return;
        }

        try {
            // 1. Update the JSON file
            CommonDataJSONObject data = JSONUtils.loadCommonJSON(
                "LunaSettings/" + MOD_ID + ".json",
                "data/config/LunaSettingsDefault.default"
            );

            if (data != null) {
                for (Map.Entry<String, Object> entry : presetValues.entrySet()) {
                    data.put(entry.getKey(), entry.getValue());
                }
                data.save();
            }

            // 2. Reload LunaLib's in-memory settings
            LunaSettingsLoader.INSTANCE.loadSettings(MOD_ID, true);

            // 3. Update changedSettings so UI knows about pending values
            updateChangedSettings(presetValues);

            // 4. Update the live UI elements directly
            updateUIElements(presetValues);

            // 5. Reload our blacklist cache
            Settings.reloadBlacklist();

            com.fs.starfarer.api.Global.getLogger(this.getClass()).info(
                "Applied preset '" + currentPreset + "' - all settings updated");

        } catch (Exception e) {
            com.fs.starfarer.api.Global.getLogger(this.getClass()).warn(
                "Failed to apply preset: " + e.getMessage(), e);
        }
    }

    /**
     * Update LunaLib's changedSettings list with our preset values.
     * This ensures the values persist if the user clicks Save again.
     */
    private void updateChangedSettings(Map<String, Object> presetValues) {
        try {
            List<ChangedSetting> changedSettings = LunaSettingsUISettingsPanel.Companion.getChangedSettings();

            // Remove existing entries for our mod
            Iterator<ChangedSetting> iter = changedSettings.iterator();
            while (iter.hasNext()) {
                ChangedSetting cs = iter.next();
                if (MOD_ID.equals(cs.getModID())) {
                    iter.remove();
                }
            }

            // Add our preset values
            for (Map.Entry<String, Object> entry : presetValues.entrySet()) {
                changedSettings.add(new ChangedSetting(MOD_ID, entry.getKey(), entry.getValue()));
            }

            // Mark as having changes
            LunaSettingsUISettingsPanel.Companion.setUnsaved(true);

        } catch (Exception e) {
            com.fs.starfarer.api.Global.getLogger(this.getClass()).debug(
                "Could not update changedSettings: " + e.getMessage());
        }
    }

    /**
     * Directly update the live UI elements with preset values.
     * This makes the sliders visually update immediately.
     */
    private void updateUIElements(Map<String, Object> presetValues) {
        try {
            List<LunaUIBaseElement> elements = LunaSettingsUISettingsPanel.Companion.getAddedElements();

            for (LunaUIBaseElement element : elements) {
                Object key = element.getKey();
                if (!(key instanceof LunaSettingsData)) continue;

                LunaSettingsData settingsData = (LunaSettingsData) key;
                if (!MOD_ID.equals(settingsData.getModID())) continue;

                String fieldId = settingsData.getFieldID();
                Object newValue = presetValues.get(fieldId);
                if (newValue == null) continue;

                // Update the element based on its type
                if (element instanceof LunaUITextFieldWithSlider) {
                    LunaUITextFieldWithSlider<?> slider = (LunaUITextFieldWithSlider<?>) element;
                    if (newValue instanceof Integer) {
                        ((LunaUITextFieldWithSlider<Integer>) slider).setValue((Integer) newValue);
                    } else if (newValue instanceof Double) {
                        ((LunaUITextFieldWithSlider<Double>) slider).setValue((Double) newValue);
                    }
                } else if (element instanceof LunaUITextField) {
                    LunaUITextField<?> textField = (LunaUITextField<?>) element;
                    if (newValue instanceof String) {
                        ((LunaUITextField<String>) textField).setValue((String) newValue);
                    }
                }
            }

        } catch (Exception e) {
            com.fs.starfarer.api.Global.getLogger(this.getClass()).debug(
                "Could not update UI elements: " + e.getMessage());
        }
    }
}
