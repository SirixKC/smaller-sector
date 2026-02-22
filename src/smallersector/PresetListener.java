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
import lunalib.backend.ui.components.LunaUIRadioButton;
import lunalib.backend.ui.components.base.LunaUIButton;
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
    private static final org.apache.log4j.Logger log = com.fs.starfarer.api.Global.getLogger(PresetListener.class);

    private static final String BACKUP_FILE = "SmallerSectorCustomBackup.json";

    private static final String[] BACKUP_SETTINGS_INT = {
        "cruiserToFrigate", "cruiserToDestroyer",
        "capitalToFrigate", "capitalToDestroyer", "capitalToCruiser",
        "cruiserDmodCount", "capitalDmodCount"
    };

    private static final String[] BACKUP_SETTINGS_DOUBLE = {
        "cruiserCrewMult", "cruiserSupplyMult", "cruiserFuelMult",
        "capitalCrewMult", "capitalSupplyMult", "capitalFuelMult",
        "cruiserBuildCostMult", "capitalBuildCostMult"
    };

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

        // Always update multipliers when settings change
        BaseValueModifier.applyMultipliers();

        // Show warning that save/reload is needed for operating costs
        if (com.fs.starfarer.api.Global.getSector() != null) {
            com.fs.starfarer.api.Global.getSector().getCampaignUI().addMessage(
                "Smaller Sector: Operating cost changes (crew/supply/fuel) require SAVE and RELOAD to take effect.",
                com.fs.starfarer.api.util.Misc.getNegativeHighlightColor()
            );
        }

        String currentPreset = LunaSettings.getString(MOD_ID, "preset");
        if (currentPreset == null) currentPreset = "Vanilla";

        // Update the active preset indicator on every save
        updateActivePresetIndicator(currentPreset);

        // Handle preset changes (existing logic)
        handlePresetChange(currentPreset);

        // Handle load-preset radio (new feature)
        handleLoadPreset(currentPreset);

        // Backup custom values when saving with Custom preset active
        if ("Custom".equals(currentPreset)) {
            backupCustomValues();
        }
    }

    /**
     * Updates the ss_active_preset String field to display the currently active preset.
     * Runs on every save so the indicator is always current.
     */
    private void updateActivePresetIndicator(String currentPreset) {
        try {
            String activeLabel = ">> Active: " + currentPreset + " <<";

            // Persist via JSON
            CommonDataJSONObject data = JSONUtils.loadCommonJSON(
                "LunaSettings/" + MOD_ID + ".json",
                "data/config/LunaSettingsDefault.default"
            );
            if (data != null) {
                data.put("ss_active_preset", activeLabel);
                data.save();
            }

            // Reload in-memory settings
            LunaSettingsLoader.INSTANCE.loadSettings(MOD_ID, true);

            // Update the live UI element
            List<LunaUIBaseElement> elements = LunaSettingsUISettingsPanel.Companion.getAddedElements();
            for (LunaUIBaseElement element : elements) {
                Object key = element.getKey();
                if (!(key instanceof LunaSettingsData)) continue;

                LunaSettingsData settingsData = (LunaSettingsData) key;
                if (!MOD_ID.equals(settingsData.getModID())) continue;

                if ("ss_active_preset".equals(settingsData.getFieldID())) {
                    if (element instanceof LunaUITextField) {
                        ((LunaUITextField<String>) element).setValue(activeLabel);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            com.fs.starfarer.api.Global.getLogger(this.getClass()).debug(
                "Could not update active preset indicator: " + e.getMessage());
        }
    }

    /**
     * Handles preset switching when the user changes the active preset radio.
     * Preserves the original behavior: named presets override slider values.
     */
    private void handlePresetChange(String currentPreset) {
        // Only update other values if preset actually changed
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
     * Handles the load-preset radio on the Configuration tab.
     * When a user selects a preset to load, copies that preset's values into
     * the Custom sliders (excluding factionBlacklist), then resets the radio
     * back to the sentinel "-- None --" value.
     *
     * Only works when the active preset is Custom.
     */
    private void handleLoadPreset(String currentPreset) {
        try {
            String loadPreset = LunaSettings.getString(MOD_ID, "ss_load_preset");

            // Skip if no load action requested
            if (loadPreset == null || loadPreset.isEmpty() || "-- None --".equals(loadPreset)) {
                return;
            }

            // Determine which preset values to load
            Map<String, Object> sourceValues;
            switch (loadPreset) {
                case "Vanilla Values":
                    sourceValues = VANILLA_VALUES;
                    break;
                case "Recommended Values":
                    sourceValues = RECOMMENDED_VALUES;
                    break;
                case "Hardcore Values":
                    sourceValues = HARDCORE_VALUES;
                    break;
                case "Restore Custom Values":
                    if ("Custom".equals(currentPreset)) {
                        Map<String, Object> restoredValues = loadBackupValues();
                        if (!restoredValues.isEmpty()) {
                            updateChangedSettings(restoredValues);
                            updateUIElements(restoredValues);
                            // Persist to LunaSettings JSON
                            CommonDataJSONObject data = JSONUtils.loadCommonJSON(
                                "LunaSettings/" + MOD_ID + ".json",
                                "data/config/LunaSettingsDefault.default"
                            );
                            if (data != null) {
                                for (Map.Entry<String, Object> entry : restoredValues.entrySet()) {
                                    data.put(entry.getKey(), entry.getValue());
                                }
                                data.save();
                            }
                            LunaSettingsLoader.INSTANCE.loadSettings(MOD_ID, true);
                            log.info("Restored custom values from backup");
                        } else {
                            log.info("No custom values backup found -- restore skipped");
                        }
                    } else {
                        log.info("Restore ignored: active preset is not Custom");
                    }
                    resetLoadPresetRadio();
                    return;
                default:
                    resetLoadPresetRadio();
                    return;
            }

            if ("Custom".equals(currentPreset)) {
                // Filter out factionBlacklist - only copy numeric settings
                Map<String, Object> filteredValues = new HashMap<>(sourceValues);
                filteredValues.remove("factionBlacklist");

                // Copy values into sliders
                updateChangedSettings(filteredValues);
                updateUIElements(filteredValues);

                // Persist the copied values
                CommonDataJSONObject data = JSONUtils.loadCommonJSON(
                    "LunaSettings/" + MOD_ID + ".json",
                    "data/config/LunaSettingsDefault.default"
                );
                if (data != null) {
                    for (Map.Entry<String, Object> entry : filteredValues.entrySet()) {
                        data.put(entry.getKey(), entry.getValue());
                    }
                    data.save();
                }
                LunaSettingsLoader.INSTANCE.loadSettings(MOD_ID, true);

                com.fs.starfarer.api.Global.getLogger(this.getClass()).info(
                    "Loaded " + loadPreset + " values into Custom sliders");
            } else {
                com.fs.starfarer.api.Global.getLogger(this.getClass()).warn(
                    "Load preset ignored: active preset is not Custom");
            }

            // Always reset the radio back to sentinel (even if not Custom)
            resetLoadPresetRadio();

        } catch (Exception e) {
            com.fs.starfarer.api.Global.getLogger(this.getClass()).warn(
                "Failed to handle load preset: " + e.getMessage(), e);
        }
    }

    /**
     * Resets the ss_load_preset radio back to "-- None --" sentinel value.
     * Updates JSON persistence, in-memory settings, and the live UI element.
     */
    private void resetLoadPresetRadio() {
        try {
            // 1. Update via JSON file
            CommonDataJSONObject data = JSONUtils.loadCommonJSON(
                "LunaSettings/" + MOD_ID + ".json",
                "data/config/LunaSettingsDefault.default"
            );
            if (data != null) {
                data.put("ss_load_preset", "-- None --");
                data.save();
            }

            // 2. Reload in-memory settings
            LunaSettingsLoader.INSTANCE.loadSettings(MOD_ID, true);

            // 3. Update the live UI radio element
            List<LunaUIBaseElement> elements = LunaSettingsUISettingsPanel.Companion.getAddedElements();
            for (LunaUIBaseElement element : elements) {
                Object key = element.getKey();
                if (!(key instanceof LunaSettingsData)) continue;

                LunaSettingsData settingsData = (LunaSettingsData) key;
                if (!MOD_ID.equals(settingsData.getModID())) continue;

                if ("ss_load_preset".equals(settingsData.getFieldID())) {
                    if (element instanceof LunaUIRadioButton) {
                        LunaUIRadioButton radio = (LunaUIRadioButton) element;
                        radio.setValue("-- None --");
                        for (LunaUIButton button : radio.getButtons()) {
                            if ("-- None --".equals(button.getButtonText().getText())) {
                                button.setSelected();
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            com.fs.starfarer.api.Global.getLogger(this.getClass()).debug(
                "Could not reset load preset radio: " + e.getMessage());
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

    /**
     * Backs up all custom numeric setting values to a common JSON file.
     * Called when the user saves settings with the Custom preset active.
     * Excludes factionBlacklist (user decision: blacklist is independent of presets).
     */
    private void backupCustomValues() {
        try {
            CommonDataJSONObject backup = JSONUtils.loadCommonJSON(BACKUP_FILE);
            for (String key : BACKUP_SETTINGS_INT) {
                Integer val = LunaSettings.getInt(MOD_ID, key);
                if (val != null) {
                    backup.put(key, val);
                }
            }
            for (String key : BACKUP_SETTINGS_DOUBLE) {
                Double val = LunaSettings.getDouble(MOD_ID, key);
                if (val != null) {
                    backup.put(key, val);
                }
            }
            backup.save();
            log.info("Backed up custom values to " + BACKUP_FILE);
        } catch (Exception e) {
            log.warn("Failed to backup custom values: " + e.getMessage(), e);
        }
    }

    /**
     * Loads previously backed-up custom values from the common JSON file.
     * Returns an empty map if no backup exists or the backup is empty.
     * Per-key error handling ensures new settings not in the backup are simply skipped
     * (they keep their current values).
     */
    private Map<String, Object> loadBackupValues() {
        Map<String, Object> result = new HashMap<>();
        try {
            CommonDataJSONObject backup = JSONUtils.loadCommonJSON(BACKUP_FILE);
            if (backup == null || backup.length() == 0) {
                return result;
            }
            for (String key : BACKUP_SETTINGS_INT) {
                try {
                    if (backup.has(key)) {
                        result.put(key, backup.getInt(key));
                    }
                } catch (Exception e) {
                    log.debug("Skipping backup key " + key + ": " + e.getMessage());
                }
            }
            for (String key : BACKUP_SETTINGS_DOUBLE) {
                try {
                    if (backup.has(key)) {
                        result.put(key, backup.getDouble(key));
                    }
                } catch (Exception e) {
                    log.debug("Skipping backup key " + key + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load custom values backup: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * Checks whether a custom values backup exists and is non-empty.
     */
    private boolean hasBackup() {
        try {
            CommonDataJSONObject backup = JSONUtils.loadCommonJSON(BACKUP_FILE);
            return backup != null && backup.length() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
