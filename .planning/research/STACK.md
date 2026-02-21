# STACK.md -- Tabbed LunaLib Settings UI for Smaller Sector

## Research Question

What is the standard approach for building complex, multi-tab settings UIs in Starsector mods using LunaLib? Specifically: can LunaLib's native CSV-based tab system support preset preview tabs (read-only), editable custom settings, and a faction management panel with per-faction toggles?

## Executive Summary

LunaLib's settings system provides native tab support through the `tab` column in `LunaSettings.csv`, plus `Text` and `Header` field types for display-only content. This covers ~70% of the desired UI. The remaining 30%--specifically read-only preset value previews and interactive faction management with source-mod identification--exceeds LunaLib's declarative CSV model and requires either creative CSV workarounds or a hybrid approach using Starsector's `CustomPanelAPI` via the existing `FactionManagerIntel` plugin.

## Findings

### 1. LunaLib's Native Tab System (CSV-Based)

**Confidence: HIGH** -- Documented in wiki, confirmed by changelog and multiple mods.

LunaLib supports tabs natively through the `tab` column in `LunaSettings.csv`. The system works as follows:

- **Tab column**: Any setting row with a value in the `tab` column is grouped under that tab name. Settings sharing identical tab names appear together.
- **General tab**: Rows with an empty `tab` column are assigned to a "General" tab, which only appears if such rows exist.
- **Tab persistence**: As of v1.7.6, the UI preserves scrollbar position and selected tab across reopenings.
- **Cross-tab saving**: As of v1.4.0, users can modify settings across multiple tabs before saving once (no per-tab save required).

**Supported field types** (all can be assigned to any tab):

| Type | Purpose | Notes |
|------|---------|-------|
| `Int` | Integer slider | min/max bounds |
| `Double` | Decimal slider | min/max bounds |
| `String` | Text input | Free-form text |
| `Boolean` | Toggle button | true/false |
| `Radio` | Dropdown/single-select | Comma-separated choices in `secondaryValue` |
| `Keycode` | Key binding | Java `Keyboard` class values |
| `Color` | Color picker | Slider + hex input |
| `Text` | Display-only row | Supports `[bracket]` highlighting |
| `Header` | Section header | Uses `defaultValue` as title text |

**Rationale for using native tabs**: The CSV tab system requires zero Java code for layout. It is battle-tested across many mods (e.g., VayraMerged uses 4 tabs: General, Popular Front, Colonial Factions, D-Mods). The tab names are arbitrary strings, so "Vanilla Preset", "Sirix Recommended", "Custom Settings" are all valid.

### 2. LunaSettings Java API

**Confidence: HIGH** -- Documented in wiki integration guide, confirmed by existing Smaller Sector code.

Reading settings programmatically:

```java
import lunalib.lunaSettings.LunaSettings;

// Type-specific getters (all take modID, fieldID)
String  val = LunaSettings.getString("smaller_sector", "ss_preset");
Integer val = LunaSettings.getInt("smaller_sector", "ss_cruiser_replace_chance");
Double  val = LunaSettings.getDouble("smaller_sector", "ss_supply_cost_mult");
Boolean val = LunaSettings.getBoolean("smaller_sector", "ss_apply_dmods");
```

Note: Getter methods return nullable types (Integer, Double, etc., not primitives). Null indicates the setting was not found.

**Listener system**:

```java
// Register in onApplicationLoad()
LunaSettings.addSettingsListener(new LunaSettingsListener() {
    @Override
    public void settingsChanged(String modID) {
        // Called when ANY mod's settings are saved (campaign or main menu)
        // Use Global.getCurrentState() to determine context
        if ("smaller_sector".equals(modID)) {
            reloadSettings();
        }
    }
});
```

As of v1.7.4, `reportSettingsChanged()` is available to manually trigger listener callbacks. The deprecated non-JvmStatic listener methods have been replaced.

**Data persistence**: Settings auto-save to `saves/common/LunaSettings/modname.json.data`. New CSV entries are merged into existing save data on launch.

### 3. Preset Tabs with Read-Only Value Display

**Confidence: MEDIUM** -- Requires creative use of `Text` field type; no native "read-only slider" exists.

LunaLib has no native concept of a "read-only setting" or "disabled slider". The `Text` field type is the closest equivalent--it displays a non-editable row with optional `[bracket]` highlighting. This means preset tabs cannot show actual slider widgets with locked values.

**Recommended approach for preset tabs**:

Each preset tab (Vanilla, Sirix Recommended, Sirix Hardcore) should use:

- A `Header` row for the preset name
- `Text` rows to display each preset value in a readable format, e.g.:
  ```
  Cruiser Replacement Chance: [75%]
  Capital Replacement Chance: [90%]
  Supply Cost Multiplier: [1.5x]
  ```
  The `[bracket]` syntax highlights the values visually.
- A single `Boolean` toggle at the top: "Enable this preset"
- The `settingsChanged` listener detects which preset boolean was toggled and programmatically applies values to the Custom tab's settings

**Limitation**: There is no public `LunaSettings.set*()` API for writing values programmatically. The existing `PresetListener` in Smaller Sector already works around this--when a preset is selected via the Radio dropdown, the listener presumably writes to the underlying JSON data file or uses internal LunaLib APIs.

**Alternative**: Keep the existing `Radio` dropdown for preset selection on a "General" or "Presets" tab, then display preset values as `Text` rows below it. The radio selection triggers the listener, which copies preset values into the working settings. This is simpler and avoids the need for per-preset boolean radio behavior.

**Rationale**: The `Text` + `Header` approach is the only way to show preset values without editable controls in LunaLib's CSV model. It trades interactive sliders for clarity and simplicity.

### 4. Custom Settings Tab (Editable Sliders)

**Confidence: HIGH** -- This is LunaLib's core use case.

A "Custom" tab with full editable sliders is straightforward:

```csv
fieldID,fieldName,fieldType,defaultValue,secondaryValue,fieldDescription,minValue,maxValue,tab
ss_custom_header,Custom Settings,Header,Configure your own values,,Adjust all replacement and cost parameters.,,,Custom
ss_cruiser_chance,Cruiser Replacement %,Int,50,,Chance to replace cruisers in NPC fleets.,0,100,Custom
ss_capital_chance,Capital Replacement %,Int,75,,Chance to replace capitals in NPC fleets.,0,100,Custom
ss_supply_mult,Supply Cost Multiplier,Double,1.5,,Multiplier for supply costs on large ships.,1.0,5.0,Custom
ss_apply_dmods,Apply D-Mods,Boolean,true,,Apply D-mods to player-acquired large ships.,,,Custom
```

The `settingsChanged` listener should check if the user is on the Custom tab (or if the custom override boolean is active) and apply these values directly.

**Interaction with presets**: When a preset is active, the Custom tab sliders should either:
1. Be visually annotated with `Text` rows explaining "Preset X is active -- switch to Custom to edit", or
2. Simply coexist, where selecting the Custom override boolean disables the preset and uses Custom tab values.

The existing `PresetListener` pattern already handles this kind of switching logic.

### 5. Faction Management Tab

**Confidence: LOW for CSV-only approach; HIGH for hybrid approach.**

A faction management tab with per-faction toggles, source-mod identification, and blacklist management **cannot be built** with LunaLib's CSV system alone. The CSV model requires all settings to be statically declared at mod packaging time. Factions are dynamic--they depend on which mods the user has installed.

**Why CSV fails here**:
- No way to enumerate installed factions at CSV parse time
- No dynamic row generation
- No way to show faction icons or source mod information
- Boolean toggles would need one row per faction, but the faction list varies per user

**Recommended hybrid approach**:

The existing `FactionManagerIntel` and `FactionManagerDialog` in Smaller Sector already solve this problem using Starsector's native UI APIs. The faction management tab should remain as an in-game intel panel, not migrate into LunaLib settings.

However, a "Factions" tab in LunaLib can serve as a **navigation entry point**:

```csv
ss_faction_header,Faction Management,Header,Faction Blacklist,,,,Factions
ss_faction_info,Info,Text,[Open the Faction Manager in-game via the Intel panel (F2 key)],,The faction blacklist controls which factions have their ships replaced. Access it from the Intel screen during gameplay.,,,Factions
ss_default_blacklist,Default Blacklist,Text,"Default excluded: [remnant] [derelict] [pirates_remnant] [omega]",,These AI/special factions are excluded by default.,,,Factions
```

This provides discoverability within LunaLib settings while keeping the actual faction management in the purpose-built intel dialog where `CustomPanelAPI` and `TooltipMakerAPI` provide the interactivity needed.

**Starsector's Custom UI API** (for reference, already used by Smaller Sector):
- `BaseIntelPlugin.createLargeDescription(CustomPanelAPI, float width, float height)` -- renders the main intel panel
- `CustomPanelAPI.createUIElement(float width, float height, boolean scrollable)` -- creates a `TooltipMakerAPI` for adding UI elements
- `TooltipMakerAPI.addButton(...)` / `addCheckbox(...)` -- interactive controls
- `BaseIntelPlugin.buttonPressConfirmed(Object buttonId, IntelUIAPI ui)` -- handles button clicks
- `panel.addUIElement(tooltip).inTL(x, y)` -- positions elements within the panel

### 6. Configuration File

**Confidence: HIGH** -- Simple JSON, documented.

`data/config/LunaSettingsConfig.json` controls the mod's appearance in the LunaLib settings menu:

```json
{
    "smaller_sector": {
        "iconPath": "graphics/icons/smaller_sector_icon.png"
    }
}
```

Requirements: 40x40px square image, must be preloaded via `settings.json` graphics entries.

## Recommendations

### Approach: CSV Tabs + Existing Intel Dialog Hybrid

| Component | Implementation | Confidence |
|-----------|---------------|------------|
| Preset display | LunaLib CSV tabs with `Text`/`Header` rows | HIGH |
| Preset selection | `Radio` dropdown on General tab (existing pattern) | HIGH |
| Custom settings | LunaLib CSV tab with editable `Int`/`Double`/`Boolean` fields | HIGH |
| Faction management | Keep existing `FactionManagerIntel`; add "Factions" info tab in LunaLib | HIGH |
| Preset-to-custom sync | `LunaSettingsListener` callback (existing `PresetListener` pattern) | HIGH |

### Why NOT a fully custom UI

Building the entire settings UI as a custom `InteractionDialogPlugin` or `CustomPanelAPI` panel would:
1. **Lose LunaLib integration** -- users expect F2 settings to work; a separate dialog breaks conventions
2. **Require reimplementing** slider controls, save/load, tab rendering, scrolling
3. **Add maintenance burden** -- LunaLib handles persistence, defaults, and UI rendering for free
4. **Break user expectations** -- the Starsector modding community has standardized on LunaLib settings

The hybrid approach gets the best of both: LunaLib for standard settings + the existing intel dialog for dynamic faction management.

### Proposed Tab Structure

1. **General** -- Preset selector (Radio), mod version info (Text), quick-start description
2. **Vanilla Preset** -- Header + Text rows showing Vanilla preset values (read-only display)
3. **Sirix Recommended** -- Header + Text rows showing recommended preset values
4. **Sirix Hardcore** -- Header + Text rows showing hardcore preset values
5. **Custom Settings** -- Force-override Boolean + editable Int/Double/Boolean sliders
6. **Factions** -- Info text directing users to the in-game Intel panel for faction management

**Alternative (fewer tabs)**: Consolidate presets into a single "Presets" tab with Headers separating each preset's values, and a Radio selector at the top. This reduces tab count from 6 to 4 (General, Presets, Custom, Factions) and may be cleaner UX.

### Migration Path from Current Code

The existing `Settings.java` already reads from LunaLib. The existing `PresetListener` handles preset switching. The main work is:

1. Restructure `LunaSettings.csv` to add tab assignments to existing fields
2. Add new `Text`/`Header` rows for preset value display
3. Move editable settings to a "Custom" tab
4. Add a "Factions" informational tab
5. Update `PresetListener` if the preset selection mechanism changes (Radio on General tab vs. per-preset Booleans)
6. No changes needed to `FactionManagerIntel`/`FactionManagerDialog`

## Key Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| No public `LunaSettings.set*()` API for programmatic value writes | MEDIUM | Existing PresetListener already works around this; investigate how it writes preset values |
| `Text` field display may not look polished enough for preset previews | LOW | Test with bracket highlighting; iterate on formatting |
| Too many tabs may overwhelm users (6 tabs for a settings panel) | MEDIUM | Use consolidated 4-tab layout (General, Presets, Custom, Factions) |
| LunaLib updates could break assumptions about internal APIs | LOW | Stick to documented public API; avoid reflection into LunaLib internals |

## Sources

- [LunaLib GitHub Repository](https://github.com/Lukas22041/LunaLib)
- [LunaSettings.CSV Wiki](https://github.com/Lukas22041/LunaLib/wiki/LunaSettings.CSV)
- [Integrating LunaSettings Wiki](https://github.com/Lukas22041/LunaLib/wiki/Integrating-LunaSettings)
- [LunaSettingsListener Wiki](https://github.com/Lukas22041/LunaLib/wiki/LunaSettingsListener)
- [LunaSettingsConfig.json Wiki](https://github.com/Lukas22041/LunaLib/wiki/LunaSettingsConfig.json)
- [LunaLib Changelog](https://github.com/Lukas22041/LunaLib/blob/main/changelog.txt)
- [LunaLib Forum Thread](https://fractalsoftworks.com/forum/index.php?topic=25658.0)
- [Custom UI Guide: Necessary Basics (Forum)](https://fractalsoftworks.com/forum/index.php?topic=28425.0)
- [Custom UI Panels and Handling Input (Forum)](https://fractalsoftworks.com/forum/index.php?topic=25284.0)
- [TooltipMakerAPI Javadoc](https://fractalsoftworks.com/starfarer.api/com/fs/starfarer/api/ui/TooltipMakerAPI.html)
- [BaseIntelPlugin Javadoc](https://fractalsoftworks.com/starfarer.api/com/fs/starfarer/api/impl/campaign/intel/BaseIntelPlugin.html)
- [FactionAPI Javadoc](https://fractalsoftworks.com/starfarer.api/com/fs/starfarer/api/campaign/FactionAPI.html)
