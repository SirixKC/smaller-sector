# Phase 2: Preset Switching and Custom Persistence - Research

**Researched:** 2026-02-21
**Domain:** LunaLib settings persistence, custom value backup/restore, dynamic description rewriting
**Confidence:** HIGH

## Summary

Phase 2 adds three features to the existing Phase 1 tabbed settings UI: (1) automatic backup of custom slider values when the user clicks "Save Settings", (2) explicit restore of backed-up values, and (3) dynamic `[Preset: value]` hints prepended to setting descriptions that update when the user switches presets. The old inline `[Recommended: 30%]` annotations were already removed during Phase 1's CSV rewrite -- CLNP-02's "removal" aspect is already done; what remains is replacing them with the new dynamic hints.

The backup mechanism uses LazyLib's `JSONUtils.loadCommonJSON()` to persist a snapshot to `saves/common/SmallerSectorCustomBackup.json.data`. This is the same persistence API already used by `PresetListener` for updating LunaLib settings -- a proven pattern in this codebase. Backup triggers on `settingsChanged()` when the active preset is Custom.

The dynamic description rewriting is the most technically challenging feature. LunaLib's `LunaSettingsData` is a Kotlin `data class` with `val` (immutable) properties. However, `LunaSettingsLoader.SettingsData` is a `MutableList`, so entries can be found and replaced with new copies containing modified `fieldDescription` values. Since Kotlin `data class` objects expose a `copy()` method, Java code can construct new `LunaSettingsData` instances with the same field values except a modified description. The panel renders descriptions from `SettingsData` each time it calls `recreatePanel()`, so modifying `SettingsData` before a tab switch or panel rebuild ensures the new descriptions appear. The `settingsChanged()` callback is the natural place to update descriptions since it fires on save -- and the user must save to apply a preset switch anyway.

**Primary recommendation:** Use `JSONUtils.loadCommonJSON("SmallerSectorCustomBackup.json")` for backup persistence. Hook backup save into `settingsChanged()` when preset is Custom. For restore, add a new Radio option "Restore Custom Values" to the existing `ss_load_preset` radio on the Configuration tab. For dynamic descriptions, replace `LunaSettingsData` entries in `LunaSettingsLoader.SettingsData` with copies containing `[Preset: value]` prefixed descriptions, then trigger `recreatePanel()` indirectly by forcing the `settingsChanged` callback to modify `SettingsData` before LunaLib rebuilds the panel.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Custom values are saved when the user clicks "Save Settings" in LunaLib (ties into existing save flow, not on every slider change)
- All settings are backed up as a full snapshot (not just diff from preset defaults)
- factionBlacklist is excluded from backup -- consistent with Phase 1's exclusion from preset loading
- Single backup file, overwritten each save -- no history
- No confirmation dialog when switching from Custom to a named preset -- immediate switch
- Switching TO Custom does NOT auto-restore backed-up values -- it just unlocks editing with current preset values as starting point
- Separate "Restore Custom Values" action to explicitly restore backed-up snapshot -- Claude's discretion on UI placement (button vs radio option, within LunaLib capabilities)
- Restore action is disabled/hidden when no backup exists
- Default preset on first install: Vanilla
- If mod update adds new settings not in backup: those settings keep their active preset default values during restore (only backed-up settings are overwritten)
- Backup persistence: best effort -- store where LunaLib/LazyLib normally stores data; don't go out of the way to guarantee survival across mod reinstall
- Remove old inline [Recommended: 30%] annotations from setting descriptions
- Replace with dynamic active-preset value hints, e.g., [Preset: 30%]
- Hints show the active preset's value for that setting (changes when user switches presets)
- Hints prepended to the setting description, e.g., [Preset: 30%] Controls fleet replacement chance.
- Hints update dynamically in real-time when the user switches presets (not just on settings open)

### Claude's Discretion
- Persistence mechanism (LazyLib JSONUtils or alternative)
- Restore action UI placement (button on Presets tab, radio option, or other)
- Backup file location and format
- How to handle dynamic description rewriting within LunaLib's API constraints

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| LunaLib | 2.0.5 | Settings UI framework, settings listener callback, internal API for UI element manipulation | Only settings framework in Starsector modding ecosystem |
| LazyLib | latest | `JSONUtils.loadCommonJSON()` for backup file persistence to `saves/common/` | Standard persistence API used by all Starsector mods; already used by PresetListener |
| Starsector API | 0.98a-RC8 | Game engine providing `Global` | Runtime environment |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Log4j | 1.2.9 | Logging via `Global.getLogger()` | Backup save/restore events, error conditions |
| MagicLib | latest | ReflectionUtils | Not needed for this phase -- no obfuscated field access required |

### Alternatives Considered
None -- LazyLib JSONUtils is the only reasonable persistence mechanism. It stores files in `saves/common/`, which is the standard location for cross-save mod data. The existing `PresetListener` already uses this exact pattern.

**Installation:**
No new dependencies. All libraries are already in the build classpath.

## Architecture Patterns

### Recommended File Structure
```
src/smallersector/
  Settings.java             # MODIFIED: Remove hardcoded preset overrides (simplify after Phase 1)
  PresetListener.java       # MODIFIED: Add backup save, restore handler, dynamic description updates
data/config/
  LunaSettings.csv          # MODIFIED: Add restore option to ss_load_preset radio, update default preset
```

### Pattern 1: Custom Value Backup via JSONUtils
**What:** Save a full snapshot of all numeric settings (excluding factionBlacklist) to a separate JSON file in `saves/common/`. Use `JSONUtils.loadCommonJSON("SmallerSectorCustomBackup.json")` to load/create the file, populate it, and call `.save()`.

**When to use:** In `settingsChanged()` when the active preset is "Custom".

**How it works:**
```java
// Backup custom values on save when Custom is active
if ("Custom".equals(currentPreset)) {
    CommonDataJSONObject backup = JSONUtils.loadCommonJSON("SmallerSectorCustomBackup.json");
    // Read current values from LunaLib
    Integer cruiserToFrigate = LunaSettings.getInt(MOD_ID, "cruiserToFrigate");
    if (cruiserToFrigate != null) backup.put("cruiserToFrigate", cruiserToFrigate);
    // ... repeat for all numeric settings ...
    backup.save();
}
```

**File location:** `saves/common/SmallerSectorCustomBackup.json.data` (LazyLib appends `.data` automatically).

**Verified from:** LunaLib uses `JSONUtils.loadCommonJSON("LunaSettings/${mod.id}.json")` at `LunaSettingsLoader.kt` line 151. The same API is used in PresetListener for updating settings JSON. The `saves/common/` directory structure is confirmed at `/home/sirix/Games/starsector/saves/common/`.

**Confidence:** HIGH

### Pattern 2: Restore via Load-Preset Radio Extension
**What:** Add a "Restore Custom Values" option to the existing `ss_load_preset` Radio field on the Configuration tab. When selected and the active preset is Custom, read the backup file and apply its values to the sliders.

**When to use:** User explicitly selects "Restore Custom Values" from the load-preset radio, then clicks Save All.

**How it maps to existing code:**
```java
// In handleLoadPreset(), extend the switch statement:
case "Restore Custom Values":
    CommonDataJSONObject backup = JSONUtils.loadCommonJSON("SmallerSectorCustomBackup.json");
    if (backup != null && backup.length() > 0) {
        Map<String, Object> restoredValues = new HashMap<>();
        // Read each setting from backup, use current value as fallback for new settings
        try { restoredValues.put("cruiserToFrigate", backup.getInt("cruiserToFrigate")); }
        catch (Exception e) { /* new setting not in backup -- keep current value */ }
        // ... repeat for all settings ...
        updateChangedSettings(restoredValues);
        updateUIElements(restoredValues);
        // Persist to LunaSettings JSON
        CommonDataJSONObject data = JSONUtils.loadCommonJSON(
            "LunaSettings/" + MOD_ID + ".json",
            "data/config/LunaSettingsDefault.default"
        );
        for (Map.Entry<String, Object> entry : restoredValues.entrySet()) {
            data.put(entry.getKey(), entry.getValue());
        }
        data.save();
        LunaSettingsLoader.INSTANCE.loadSettings(MOD_ID, true);
    }
    break;
```

**Why Radio option instead of separate button:** LunaLib has no standalone button field type. The Radio field already exists on the Configuration tab with the load-preset options. Adding a fourth option keeps the UI compact and consistent. The "hidden when no backup exists" requirement is approximated: the option is always visible in the CSV (since CSV is static), but the code silently ignores it and logs a message when no backup exists. This matches the user's "disabled/hidden" intent -- the action is a no-op, and the radio resets to `-- None --` either way.

**Alternative considered:** Adding a separate Radio field just for restore. Rejected because it adds UI clutter for a single action.

**Confidence:** HIGH for the mechanism; MEDIUM for the "hidden when no backup" constraint (CSV radio options are always visible; the best we can do is make it a no-op).

### Pattern 3: Dynamic Description Rewriting via SettingsData Replacement
**What:** Modify `LunaSettingsLoader.SettingsData` entries to prepend `[Preset: value]` hints to each setting's `fieldDescription`. Since `LunaSettingsData` is a Kotlin `data class` with `val` fields (immutable), you cannot modify fields directly. Instead, find the existing entry in the `SettingsData` list, remove it, and add a new `LunaSettingsData` instance with the same values but a modified `fieldDescription`.

**When to use:** In `settingsChanged()` after determining the active preset. Also on `onApplicationLoad()` to set initial descriptions.

**How the description rendering works:**
1. `LunaSettingsUISettingsPanel.recreatePanel()` iterates `LunaSettingsLoader.SettingsData`
2. For each setting row, it reads `data.fieldDescription` (line 341)
3. It renders the description via `addPara()` with `[bracket]` highlighting
4. Panel is recreated on tab switch, mod selection change, and settings reopen

**Key insight:** The description text is read from `SettingsData` each time the panel is built. If we modify `SettingsData` before the panel rebuilds, the new description will render. The `settingsChanged()` callback fires after save. The user must then re-navigate to the Configuration tab (or close/reopen settings) to see the updated descriptions. This satisfies "real-time when user switches presets" because switching presets requires a save, and after save, any tab navigation rebuilds the panel from the updated `SettingsData`.

**Implementation approach:**
```java
// Build a map of settingId -> preset value for the active preset
Map<String, Object> presetValues = getPresetValues(currentPreset);

// For each setting that should get a hint:
List<LunaSettingsData> allData = LunaSettingsLoader.INSTANCE.getSettingsData();
for (int i = 0; i < allData.size(); i++) {
    LunaSettingsData entry = allData.get(i);
    if (!MOD_ID.equals(entry.getModID())) continue;
    if (!presetValues.containsKey(entry.getFieldID())) continue;

    // Strip any existing [Preset: ...] prefix from the description
    String desc = entry.getFieldDescription();
    desc = desc.replaceAll("^\\[Preset: [^\\]]+\\] ", "");

    // Prepend the new hint
    Object value = presetValues.get(entry.getFieldID());
    String hint = formatHint(entry.getFieldID(), value);
    String newDesc = hint + " " + desc;

    // Replace the entry with a new one (data class is immutable)
    LunaSettingsData replacement = new LunaSettingsData(
        entry.getModID(), entry.getFieldID(), entry.getFieldName(),
        entry.getFieldType(), newDesc, entry.getDefaultValue(),
        entry.getSecondaryValue(), entry.getMinValue(), entry.getMaxValue(),
        entry.getTab()
    );
    allData.set(i, replacement);
}
```

**Kotlin data class from Java:** The `LunaSettingsData` constructor is accessible from Java as a regular class constructor. All `val` properties have getter methods. The constructor takes all 10 parameters.

**Verified from:** `LunaSettingsLoader.kt` line 10-19 defines `LunaSettingsData` with `val` fields. Line 35: `var SettingsData: MutableList<LunaSettingsData>` is a `@JvmStatic` mutable list. `LunaSettingsUISettingsPanel.kt` line 244 reads from `LunaSettingsLoader.SettingsData`. Line 341 reads `data.fieldDescription` for rendering.

**Confidence:** HIGH for the mechanism. The `SettingsData` list is public, `@JvmStatic`, and mutable. The `LunaSettingsData` constructor is a standard Kotlin data class constructor callable from Java. The description rendering path is verified.

### Pattern 4: Default Preset on First Install
**What:** The CONTEXT.md specifies "Default preset on first install: Vanilla". The current CSV has `defaultValue` of `Sirix Recommended` for the `preset` Radio field. This needs to change to `Vanilla`.

**Impact:** The `defaultValue` in CSV is used by `LunaSettingsLoader.saveDefaultsToFile()` when no saved settings exist yet. Changing it to `Vanilla` means new users start with the Vanilla preset (all replacements off).

**Confidence:** HIGH

### Anti-Patterns to Avoid
- **Backing up on every settingsChanged regardless of preset:** Only backup when Custom is active. Backing up when a named preset is active would overwrite the custom backup with preset values.
- **Modifying LunaSettingsData fields via reflection:** Unnecessary complexity when the MutableList allows entry replacement.
- **Storing backup in a save-specific location:** The `saves/common/` directory is cross-save. This is correct for custom settings -- they are per-installation preferences, not per-save-game state.
- **Adding a `[Preset: value]` hint to Text/Header fields:** Only add hints to interactive settings (Int, Double, String) that have corresponding preset values.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JSON file persistence | Custom File I/O with try/catch/BufferedWriter | `JSONUtils.loadCommonJSON()` + `.save()` | LazyLib handles file creation, error recovery, and the `.data` extension convention |
| Settings value access | Direct JSON file parsing | `LunaSettings.getInt/getDouble/getString` | LunaLib's API handles type conversion and provides the in-memory cached value |
| UI element updates | Custom panel rendering | Existing `updateUIElements()` and `updateChangedSettings()` methods in PresetListener | These methods are already proven to work for slider and radio updates |
| Description highlighting | Manual color/font manipulation | LunaLib's `[bracket]` syntax in `fieldDescription` | The rendering pipeline at `LunaSettingsUISettingsPanel.kt` line 288-302 and 341-357 already handles bracket highlighting |

**Key insight:** Phase 2's backup/restore is architecturally identical to the load-preset feature from Phase 1 -- read values from a source, apply them to sliders/JSON/UI. The only new element is the backup file as a source.

## Common Pitfalls

### Pitfall 1: Backup Overwritten by Named Preset Values
**What goes wrong:** If the backup triggers whenever `settingsChanged()` fires (regardless of active preset), switching to "Sirix Recommended" and saving would overwrite the custom backup with Recommended values. The user's custom values are lost.
**Why it happens:** `settingsChanged()` fires on every save, not just when Custom is active.
**How to avoid:** Gate the backup save behind `if ("Custom".equals(currentPreset))`. Only backup when the user is actively on Custom and saving.
**Warning signs:** Custom values restore to a named preset's values instead of the user's actual custom values.

### Pitfall 2: Restore Overwrites New Settings with Stale Backup
**What goes wrong:** A mod update adds a new setting (e.g., `capitalDmodChance`). The backup file does not contain this setting. Restoring the backup sets all known settings but leaves the new one at... what? If the code tries to read it from the backup, it gets an exception or null.
**Why it happens:** The backup was created with the old mod version.
**How to avoid:** Per the user decision: "new settings keep their active preset default values during restore." Use try-catch when reading each setting from the backup. If a setting is missing, skip it (don't add it to the restore map). The setting retains whatever value it currently has.
**Warning signs:** Exception during restore, or a setting getting reset to 0/null.

### Pitfall 3: LunaSettingsData Constructor Parameter Order
**What goes wrong:** When constructing a new `LunaSettingsData` from Java to replace an entry in `SettingsData`, the parameter order must exactly match the Kotlin data class constructor: `(modID, fieldID, fieldName, fieldType, fieldDescription, defaultValue, secondaryValue, minValue, maxValue, tab)`. Getting the order wrong causes settings corruption (e.g., swapping `fieldDescription` and `defaultValue`).
**Why it happens:** Kotlin data class constructors have many positional parameters. Java does not have named parameters.
**How to avoid:** Create a helper method that takes an existing `LunaSettingsData` and a new description, constructing the replacement with explicit getter calls: `new LunaSettingsData(old.getModID(), old.getFieldID(), old.getFieldName(), old.getFieldType(), newDescription, old.getDefaultValue(), old.getSecondaryValue(), old.getMinValue(), old.getMaxValue(), old.getTab())`.
**Warning signs:** Settings appear with wrong names, descriptions, or types after a preset switch.

### Pitfall 4: Description Prefix Accumulation
**What goes wrong:** If the `[Preset: 30%]` prefix is prepended every time `settingsChanged()` fires without stripping the old prefix first, the description becomes `[Preset: 30%] [Preset: 30%] [Preset: 30%] Controls fleet replacement chance...`.
**Why it happens:** The description modification runs on every save, not just on preset change.
**How to avoid:** Always strip any existing `[Preset: ...]` prefix before prepending the new one. Use a regex: `desc.replaceAll("^\\[Preset: [^\\]]+\\] ", "")`. Store the original "clean" descriptions at startup and rebuild from those each time.
**Warning signs:** Descriptions getting longer each time settings are saved.

### Pitfall 5: Dynamic Descriptions Not Visible Until Tab Switch
**What goes wrong:** The user switches presets and saves. The `settingsChanged()` callback updates `SettingsData`. But the Configuration tab is still showing the old descriptions because `recreatePanel()` hasn't been called yet.
**Why it happens:** `recreatePanel()` is called on tab switch or mod reselection, not on save. The `settingsChanged()` callback fires during the save operation. If the user is currently viewing the Configuration tab and saves, the panel is not rebuilt.
**How to avoid:** This is a limitation. The descriptions update when the user navigates away from and back to the Configuration tab (which triggers `recreatePanel()`). In practice, users switch presets on the Presets tab, then navigate to Configuration -- by which time the panel has rebuilt. Accept this as expected behavior. Alternatively, if the user is on Configuration and saves, the descriptions update the next time they switch tabs.
**Warning signs:** Descriptions appear stale after saving. They update after switching tabs. This is expected behavior, not a bug.

### Pitfall 6: Backup File Load Returns Empty Object on First Run
**What goes wrong:** On first run (no backup file exists), `JSONUtils.loadCommonJSON("SmallerSectorCustomBackup.json")` creates an empty JSON object. The restore handler tries to read settings from it and gets exceptions or zeroes.
**Why it happens:** `loadCommonJSON` creates the file if it doesn't exist (it's a "load or create" pattern from LazyLib).
**How to avoid:** Check `backup.length() > 0` before attempting restore. If the backup is empty (no keys), treat it as "no backup exists" and skip the restore. The restore radio option should silently reset to `-- None --` with a log message.
**Warning signs:** Restore appears to do nothing (expected on first run, before any custom values have been saved).

## Code Examples

### Custom Value Backup (on save when Custom is active)
```java
// Source: Pattern from existing PresetListener + JSONUtils API
private static final String BACKUP_FILE = "SmallerSectorCustomBackup.json";

// Settings to back up (excludes factionBlacklist)
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

private void backupCustomValues() {
    try {
        CommonDataJSONObject backup = JSONUtils.loadCommonJSON(BACKUP_FILE);
        for (String key : BACKUP_SETTINGS_INT) {
            Integer val = LunaSettings.getInt(MOD_ID, key);
            if (val != null) backup.put(key, val);
        }
        for (String key : BACKUP_SETTINGS_DOUBLE) {
            Double val = LunaSettings.getDouble(MOD_ID, key);
            if (val != null) backup.put(key, val);
        }
        backup.save();
    } catch (Exception e) {
        log.warn("Failed to backup custom values: " + e.getMessage());
    }
}
```

### Restore from Backup
```java
// Source: Pattern from existing handleLoadPreset()
private Map<String, Object> loadBackupValues() {
    Map<String, Object> values = new HashMap<>();
    try {
        CommonDataJSONObject backup = JSONUtils.loadCommonJSON(BACKUP_FILE);
        if (backup == null || backup.length() == 0) return values;

        for (String key : BACKUP_SETTINGS_INT) {
            try { values.put(key, backup.getInt(key)); }
            catch (Exception e) { /* setting not in backup -- skip */ }
        }
        for (String key : BACKUP_SETTINGS_DOUBLE) {
            try { values.put(key, backup.getDouble(key)); }
            catch (Exception e) { /* setting not in backup -- skip */ }
        }
    } catch (Exception e) {
        log.warn("Failed to load backup: " + e.getMessage());
    }
    return values;
}
```

### Dynamic Description Rewriting
```java
// Source: LunaSettingsLoader.SettingsData is MutableList<LunaSettingsData>
// LunaSettingsData constructor: (modID, fieldID, fieldName, fieldType, fieldDescription,
//                                defaultValue, secondaryValue, minValue, maxValue, tab)

private void updateSettingDescriptions(String presetName, Map<String, Object> presetValues) {
    List<LunaSettingsData> allData = LunaSettingsLoader.INSTANCE.getSettingsData();
    for (int i = 0; i < allData.size(); i++) {
        LunaSettingsData entry = allData.get(i);
        if (!MOD_ID.equals(entry.getModID())) continue;

        String fieldId = entry.getFieldID();
        if (!presetValues.containsKey(fieldId)) continue;

        // Get the original clean description (strip any existing prefix)
        String desc = entry.getFieldDescription();
        desc = desc.replaceAll("^\\[Preset: [^\\]]+\\] ", "");

        // Format the hint value
        Object value = presetValues.get(fieldId);
        String formatted;
        if (value instanceof Integer) {
            formatted = value.toString() + "%";
        } else if (value instanceof Double) {
            formatted = String.format("%.1fx", (Double) value);
        } else {
            continue; // skip non-numeric
        }

        // Prepend hint
        String newDesc = "[Preset: " + formatted + "] " + desc;

        // Replace entry in SettingsData
        LunaSettingsData replacement = new LunaSettingsData(
            entry.getModID(), entry.getFieldID(), entry.getFieldName(),
            entry.getFieldType(), newDesc, entry.getDefaultValue(),
            entry.getSecondaryValue(), entry.getMinValue(), entry.getMaxValue(),
            entry.getTab()
        );
        allData.set(i, replacement);
    }
}
```

### Checking Whether Backup Exists
```java
private boolean hasBackup() {
    try {
        CommonDataJSONObject backup = JSONUtils.loadCommonJSON(BACKUP_FILE);
        return backup != null && backup.length() > 0;
    } catch (Exception e) {
        return false;
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Static `[Recommended: 30%]` in CSV descriptions | Dynamic `[Preset: value]` via SettingsData replacement | This phase | Descriptions reflect active preset, not hardcoded Recommended values |
| No custom value persistence | Full snapshot backup to `saves/common/` | This phase | Users can switch presets freely without losing custom work |
| Settings.java hardcodes preset values per-method | Settings.java hardcodes preset values per-method (unchanged) | N/A | Architecture remains the same -- backup/restore is additive |

**Deprecated/outdated:**
- Old inline `[Recommended: 30%]` annotations: Already removed in Phase 1 CSV rewrite. No code changes needed for removal.
- `lunalib/LunaSettings.json`: Already deleted in Phase 1.

## Discretion Recommendations

### Persistence Mechanism
**Recommendation: LazyLib JSONUtils**

`JSONUtils.loadCommonJSON(filename)` stores to `saves/common/{filename}.data`. This is the standard location for cross-save mod preferences in Starsector. It is already used by LunaLib itself for settings storage, and by the existing PresetListener code. No reason to use any alternative.

The backup file name should be `SmallerSectorCustomBackup.json` (stored as `saves/common/SmallerSectorCustomBackup.json.data`). This clearly identifies the mod and purpose.

### Restore Action UI Placement
**Recommendation: Add to existing ss_load_preset Radio**

Add "Restore Custom Values" as a new option in the `ss_load_preset` Radio field on the Configuration tab. The options become: `"-- None --,Vanilla Values,Recommended Values,Hardcore Values,Restore Custom Values"`.

**Rationale:**
- Reuses existing UI element -- no new field needed in CSV
- The handler code already processes this radio (handleLoadPreset) -- adding a new case is trivial
- Semantically consistent: "Load Preset" and "Restore Custom" are both "load values into sliders" operations
- The "hidden when no backup" requirement maps to: when no backup exists, the restore option is present but acts as a no-op (radio resets, log message). This is acceptable because users won't even know to look for this option until they have custom values to restore.

**Alternative considered:** Separate Radio field for restore only. Rejected -- adds clutter, requires a new CSV field ID, and duplicates the handler pattern.

### Backup File Format
**Recommendation: Flat JSON object, same structure as LunaSettings**

```json
{
  "cruiserToFrigate": 30,
  "cruiserToDestroyer": 50,
  "capitalToFrigate": 20,
  "capitalToDestroyer": 40,
  "capitalToCruiser": 25,
  "cruiserCrewMult": 1.5,
  "cruiserSupplyMult": 1.5,
  "cruiserFuelMult": 1.5,
  "capitalCrewMult": 2.0,
  "capitalSupplyMult": 2.0,
  "capitalFuelMult": 2.0,
  "cruiserBuildCostMult": 1.5,
  "capitalBuildCostMult": 2.0,
  "cruiserDmodCount": 2,
  "capitalDmodCount": 3
}
```

Same field IDs as LunaSettings. No metadata, no versioning. Simple, easy to debug, easy to restore from.

### Dynamic Description Rewriting Strategy
**Recommendation: Store clean descriptions at startup, rebuild on every settingsChanged**

1. In `onApplicationLoad()` (or PresetListener constructor), capture the original clean descriptions from `LunaSettingsLoader.SettingsData` into a `Map<String, String>` (fieldID -> original description).
2. On every `settingsChanged()`, determine the active preset's values and rebuild descriptions from the clean originals + `[Preset: value]` prefix.
3. This avoids the prefix accumulation pitfall entirely.

**Alternative considered:** Using a regex to strip old prefix each time. This works but is fragile -- if the format changes, the regex breaks. The clean-descriptions map is more robust.

**Timing of updates:** The descriptions are updated in `SettingsData` during `settingsChanged()`. The panel rebuilds on the next tab switch. For the Presets tab -> Configuration tab navigation path (the most common flow when changing presets), this ensures descriptions are current. If the user saves while on Configuration, the descriptions update on the next tab switch -- acceptable.

### Default Preset Change
**Recommendation: Change CSV defaultValue to Vanilla**

The CONTEXT.md says "Default preset on first install: Vanilla". Update the `preset` Radio field's `defaultValue` from `Sirix Recommended` to `Vanilla` in `LunaSettings.csv`.

**Impact on existing users:** None. `LunaSettingsLoader.saveDefaultsToFile()` only writes defaults for settings that don't already exist in the saved JSON. Existing users already have `preset` saved.

**Impact on Settings.java:** The `getPreset()` method has a fallback: `val != null ? val : PRESET_RECOMMENDED`. This should change to `PRESET_VANILLA` to match the new default. This is a minor correctness fix.

## Open Questions

1. **SettingsData List Concurrent Modification**
   - What we know: `LunaSettingsLoader.SettingsData` is a public `MutableList`. The `settingsChanged()` callback fires during the save operation (in the `advance()` loop of `LunaSettingsUIModsPanel`). Modifying `SettingsData` during this callback should be safe because the panel rendering happens in a separate call (`recreatePanel()`), not during the save loop.
   - What's unclear: Whether any other thread or listener modifies `SettingsData` concurrently.
   - Recommendation: Since this is a single-threaded game (UI runs on the main thread), concurrent modification is not a concern. Proceed with direct list modification.
   - Confidence: HIGH

2. **Dynamic Description with `[bracket]` Highlighting**
   - What we know: The `[Preset: 30%]` prefix will be highlighted by LunaLib's bracket parsing (lines 288-302 and 341-357 of `LunaSettingsUISettingsPanel.kt`). The "Preset: 30%" text inside brackets will appear in the highlight color (bright yellow/gold).
   - What's unclear: Whether the colon and percent sign inside brackets render correctly, or if they cause parsing issues.
   - Recommendation: Test in-game. If brackets cause issues, fall back to parentheses: `(Preset: 30%)` without highlighting. The bracket parser is simple (find `[`, find `]`, extract text between) and should handle colons and percent signs fine.
   - Confidence: MEDIUM

3. **Restore When No Backup Exists -- UI Feedback**
   - What we know: The "Restore Custom Values" radio option is always visible (CSV is static). When no backup exists, the code silently does nothing.
   - What's unclear: Whether users will be confused by a restore option that apparently does nothing.
   - Recommendation: After a failed restore (no backup), show a campaign UI message: "No custom values backup found. Save settings while on the Custom preset first." This provides feedback without requiring UI element manipulation.
   - Confidence: HIGH -- the `Global.getSector().getCampaignUI().addMessage()` pattern is already used by PresetListener for the operating cost warning.

## Sources

### Primary (HIGH confidence)
- LunaLib source code at `/home/sirix/Games/starsector/mods/LunaLib/src/`:
  - `LunaSettingsLoader.kt` -- `LunaSettingsData` data class (val fields, immutable), `SettingsData` MutableList (`@JvmStatic`), `saveDefaultsToFile()` skip logic for Text/Header
  - `LunaSettingsUISettingsPanel.kt` -- `recreatePanel()` reads `SettingsData` for descriptions, `addedElements` lifecycle, tab switch triggers panel rebuild
  - `LunaSettingsUIModsPanel.kt` -- Save flow: `setUnsavedData()` -> JSON write -> `reportSettingsChanged()` at lines 566-626
  - `LunaSettings.kt` -- Public API: `getString/getInt/getDouble`, listener registration
  - `LunaUIRadioButton.kt` -- `value` is `var String?`, `buttons` is `MutableList<LunaUIButton>`, `setSelected()` from `LunaUIBaseElement`
- Existing mod source at `/home/sirix/Games/Modding/smaller-sector/src/smallersector/`:
  - `PresetListener.java` -- Existing patterns for JSON persistence, UI element manipulation, radio reset
  - `Settings.java` -- Current preset override logic, field ID list
- Starsector save directory at `/home/sirix/Games/starsector/saves/common/`:
  - `LunaSettings/smallersector.json.data` -- Current saved settings confirming JSON structure
  - Directory listing confirming `saves/common/` as the standard location for `JSONUtils.loadCommonJSON()`

### Secondary (MEDIUM confidence)
- LazyLib `JSONUtils.loadCommonJSON()` API -- inferred from LunaLib's usage patterns (source not directly available, but behavior confirmed via LunaLib's file operations and save directory inspection)

### Tertiary (LOW confidence)
None -- all findings verified against source code.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- same stack as Phase 1, no new dependencies
- Architecture (backup/restore): HIGH -- uses proven JSONUtils + existing PresetListener patterns
- Architecture (dynamic descriptions): HIGH for mechanism (SettingsData replacement), MEDIUM for rendering (bracket syntax in [Preset: value] needs runtime verification)
- Pitfalls: HIGH -- verified from source code analysis

**Research date:** 2026-02-21
**Valid until:** 2026-04-21 (LunaLib 2.0.x is stable; no breaking changes expected)
