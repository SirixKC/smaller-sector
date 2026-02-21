# Phase 1: Tab Structure and Preset Display - Research

**Researched:** 2026-02-21
**Domain:** LunaLib CSV Settings UI -- Tabs, Radio Fields, Text Display, Slider Disabling
**Confidence:** HIGH

## Summary

Phase 1 restructures Smaller Sector's flat LunaLib settings list into a three-tab UI (Presets, Configuration, Factions) with a Radio preset selector and comparison-table-style value summaries. The core technical vehicle is LunaLib's `tab` column in `LunaSettings.csv`, which natively groups settings into named tabs with zero Java code. The Radio field type natively handles single-selection for preset choice. Text fields with `[bracket]` highlighting provide read-only value display.

The primary technical challenge is the user decision to disable/gray out sliders when a named preset is active. LunaLib has no native mechanism for runtime field disabling -- the CSV format has no `disabled` column, and the UI components (`LunaUITextFieldWithSlider`, `LunaUIButton`) have no `setEnabled(false)` method. The existing `"disabled": true` property in the `LunaSettings.json` file is not recognized by LunaLib at all (confirmed by source code inspection). This requires a workaround: using the PresetListener to manipulate slider UI elements at the backend level (setting alpha, intercepting input) or accepting that sliders remain interactive but are annotated with explanatory text and functionally ignored by `Settings.java` when a named preset is active.

The secondary challenge is the "load preset values into sliders" dropdown on the Configuration tab. LunaLib's Radio field type is the closest match to a dropdown selector -- it renders as a vertical list of mutually exclusive buttons. An alternative is to use a dedicated Radio field with choices like "-- Select --", "Load Vanilla", "Load Recommended", "Load Hardcore" that triggers the PresetListener to copy preset values into the slider fields. This requires the same internal API usage the existing PresetListener already employs (`LunaSettingsUISettingsPanel.Companion.getAddedElements()`).

**Primary recommendation:** Use the CSV `tab` column for three-tab layout, Radio field for preset selection on Presets tab, Text fields for comparison summaries, and a prominent Text field warning on the Configuration tab when sliders are non-authoritative. For slider disabling, use the PresetListener's existing internal API access pattern to reduce slider opacity and overlay a dimming effect, with a fallback to text-only annotation if the UI manipulation proves too fragile. For the "load preset values" control, use a Radio field with a sentinel "-- Select --" default option.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Three tabs: **Presets**, **Configuration**, **Factions**
- Presets tab opens by default when user opens mod settings (F3)
- Presets tab contains: radio selector + grouped value comparison summaries for all presets
- Configuration tab contains: all individual sliders/fields, grouped with headers and sub-headers
- Factions tab contains: blacklist string field + guidance text pointing to the in-game Faction Manager dialog
- Preset radio selector lives only on the Presets tab (not duplicated elsewhere)
- Summaries grouped by category (Replacement, Costs, D-mods) with all preset values inline -- comparison-table style in text form
- Full detail: show every value for every preset, not abbreviated
- Custom preset shows its own summary block reflecting current slider values (same treatment as named presets)
- Currently active preset's summary is visually highlighted (marker, arrow, or prefix) to stand out from others
- Sliders on Configuration tab stay at their Custom values -- they do NOT update when a named preset is selected
- Sliders are disabled/grayed out when a named preset (Vanilla/Recommended/Hardcore) is active; only enabled when Custom is selected
- When sliders are disabled, a text element at the top of Configuration tab explains why (e.g., "Select Custom preset to edit values")
- Configuration tab includes a dropdown/selector to load a preset's values into the sliders as a starting point -- only visible/available when Custom is active
- No auto-detection: if Custom values happen to match a named preset, nothing happens -- stays on Custom
- Named presets override slider values internally via Settings.java (existing behavior preserved)
- Configuration tab has 2 main groups with Header separators: **Ship Replacement** and **Costs & Penalties**
- Ship Replacement group has sub-headers (Text separators): "Cruiser" and "Capital", with the stays fields alongside their respective sliders
- Costs & Penalties group has sub-headers for "Cruiser" and "Capital" (covering operating costs, build costs per class). D-mods flow at the end without their own sub-header
- No intro text on the Presets tab -- radio selector and summaries are self-explanatory
- User wants the Configuration tab slider-loading control to be a single dropdown/selector (not one button per preset) -- compact and clean
- "Stays as cruiser/capital" auto-calculated disabled fields should be kept alongside replacement sliders
- Factions tab guidance text should direct users to the in-game Faction Manager dialog even though dialog improvements are Phase 3

### Claude's Discretion
- Exact text formatting for comparison summaries (line breaks, alignment, symbols)
- How to visually highlight the active preset (arrow prefix, brackets, etc.) within LunaLib text constraints
- LunaSettings.json deletion verification approach
- Exact wording of the "sliders disabled" explanation text
- Dropdown vs radio for the "load preset values" control (based on LunaLib field type support)

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| LunaLib | 2.0.5 | Settings UI framework -- CSV tab system, Radio fields, Text display, listener callbacks | Only settings framework in Starsector modding ecosystem; all mods use it |
| LazyLib | latest | JSON utilities (`JSONUtils.loadCommonJSON`) for persistent data read/write | Standard companion to LunaLib; already used by PresetListener for JSON file operations |
| Starsector API | 0.98a-RC8 | Game engine providing `Global`, `ModSpecAPI`, `ShipHullSpecAPI` | Runtime environment |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Log4j | 1.2.9 | Logging via `Global.getLogger()` | All lifecycle events, preset switching, error conditions |
| MagicLib | latest | ReflectionUtils for `BaseValueModifier` | Only used by BaseValueModifier (not directly relevant to this phase, but called from PresetListener) |

### Alternatives Considered
None -- LunaLib is the only settings framework for Starsector mods. There is no alternative.

**Installation:**
No new dependencies. All libraries are already in the build classpath via `build.xml`.

## Architecture Patterns

### Recommended File Structure
```
data/config/
  LunaSettings.csv          # MODIFIED: Add tab column values, reorganize rows, add Text summaries
src/smallersector/
  Settings.java              # MODIFIED: No structural changes, preserve existing preset override logic
  PresetListener.java        # MODIFIED: Add slider disable/enable logic, add "load preset" handling
lunalib/
  LunaSettings.json          # DELETED: Duplicate of CSV, not used by LunaLib or mod code
```

### Pattern 1: Tab Assignment via CSV Column
**What:** The ninth column (`tab`) in `LunaSettings.csv` assigns each setting row to a named tab. Settings with the same tab name are grouped together. Empty tab = "General" tab (auto-created only if empty values exist).

**When to use:** Every non-blank row in the CSV must have a tab value.

**Example:**
```csv
fieldID,fieldName,fieldType,defaultValue,secondaryValue,fieldDescription,minValue,maxValue,tab
preset,Difficulty Preset,Radio,Sirix Recommended,"Custom,Vanilla,Sirix Recommended,Sirix Hardcore",Select a difficulty preset.,,,Presets
presetVanillaSummary,Vanilla,Text,"All replacement [disabled]. Ships spawn at vanilla rates with [no cost modifications].",,,,,Presets
cruiserToFrigate,Cruiser to Frigate,Int,30,,Percentage chance for cruisers to be replaced with frigates,0,100,Configuration
factionBlacklist,Blacklisted Factions,String,,,"Comma-separated faction IDs exempt from replacement.",,,Factions
```

**Verified from:** LunaLib source `LunaSettingsLoader.kt` line 84: `tab = row.getString("tab")`. LunaLib wiki: "The Tab column generates tabs."

**Confidence:** HIGH

### Pattern 2: Tab Ordering (First Tab Opens by Default)
**What:** LunaLib determines tab order from the CSV row order. Specifically, `LunaSettingsUISettingsPanel.recreateTabs()` calls `LunaSettingsLoader.SettingsData.filter { it.modID == selectedMod!!.id }.map { it.tab }.distinct()`. The first unique tab name encountered becomes the first tab. When `lastSelectedTab` is empty (first time opening), the first tab is auto-selected.

**When to use:** The Presets tab must be the FIRST tab encountered in the CSV to ensure it opens by default.

**Implication:** The CSV must start with Presets tab rows, then Configuration tab rows, then Factions tab rows.

**Verified from:** LunaLib source `LunaSettingsUISettingsPanel.kt` lines 99-100 (tab ordering) and lines 160-166 (first tab auto-selection).

**Confidence:** HIGH

### Pattern 3: Radio Field for Preset Selection
**What:** The Radio field type renders as a vertical list of mutually exclusive buttons. It stores the selected option as a String. Options are defined in the `secondaryValue` column as comma-separated values. The `defaultValue` column sets the initially selected option.

**When to use:** Preset selector on the Presets tab, and the "load preset values" control on the Configuration tab.

**Example:**
```csv
preset,Difficulty Preset,Radio,Sirix Recommended,"Custom,Vanilla,Sirix Recommended,Sirix Hardcore",Select a difficulty preset.,,,Presets
```

**How it works internally:** `LunaUIRadioButton` creates one `LunaUIButton` per choice. Clicking a button calls `setSelected()` which deselects others within the same group. The value is stored via `LunaSettings.getString(modID, fieldID)`.

**Verified from:** LunaLib source `LunaUIRadioButton.kt` lines 33-59 and `LunaSettingsUISettingsPanel.kt` lines 579-622.

**Confidence:** HIGH

### Pattern 4: Text Fields for Read-Only Display with Highlighting
**What:** The Text field type renders a non-editable paragraph of text. Content comes from `defaultValue`. Text within `[brackets]` is highlighted in the standard highlight color (bright yellow/gold). Bracket markers are stripped from the display -- only the enclosed text is highlighted.

**When to use:** Preset comparison summaries, explanation text, sub-headers.

**Rendering details:** Text fields are rendered as full-width paragraphs using `TooltipMakerAPI.addPara()`. The highlight extraction loop in `LunaSettingsUISettingsPanel.recreatePanel()` (lines 288-302) processes `[bracket]` syntax. Multiple bracketed segments per Text field are supported.

**Example:**
```csv
recCruiserInfo,Ship Replacement (Cruiser),Text,"Frigate [30%] | Destroyer [50%] | Stays [20%]",,,,,Presets
```

**Verified from:** LunaLib source `LunaSettingsUISettingsPanel.kt` lines 274-317.

**Confidence:** HIGH

### Pattern 5: Header Fields for Section Breaks
**What:** The Header field type renders as a section heading using `TooltipMakerAPI.addSectionHeading()`. The `defaultValue` provides the heading text. Headers render centered with a horizontal line background.

**When to use:** "Ship Replacement" and "Costs & Penalties" group separators on the Configuration tab.

**Verified from:** LunaLib source `LunaSettingsUISettingsPanel.kt` lines 263-272.

**Confidence:** HIGH

### Pattern 6: Slider Disabling via PresetListener Internal API Access
**What:** LunaLib has NO native mechanism for disabling settings fields at runtime. The CSV has no `disabled` column. UI components have no `setEnabled()` method. The `"disabled": true` property in `LunaSettings.json` is completely ignored by LunaLib (not present in source code at all).

**To achieve the user's requirement of "sliders disabled/grayed out when a named preset is active"**, the PresetListener must manipulate UI elements directly via `LunaSettingsUISettingsPanel.Companion.getAddedElements()`. The existing code already does this to update slider VALUES (lines 211-246 of `PresetListener.java`). To disable, the listener would need to:

1. Iterate `addedElements` and find sliders belonging to this mod
2. Set `backgroundAlpha` to a low value (e.g., 0.2f) to create a dimmed appearance
3. Potentially override `processInput` on elements to swallow input events

**However**, this approach has significant fragility:
- `addedElements` is only populated for the CURRENTLY VISIBLE tab. Switching tabs calls `addedElements.clear()` and `recreatePanel()`. So the PresetListener can only affect sliders on the Configuration tab when the user is LOOKING at the Configuration tab.
- The `settingsChanged()` callback fires when the user clicks "Save All", not when they switch tabs. So there is no hook to re-apply the dimming when the user navigates to the Configuration tab.
- There is no "tab changed" callback in LunaLib's listener system.

**Recommended approach:** Accept that true visual disabling is not feasible within LunaLib's architecture. Instead:
1. Add a prominent Text field at the top of the Configuration tab: "These settings only take effect when [Custom] is selected. To edit, switch to [Custom] on the Presets tab."
2. Have `Settings.java` continue to ignore slider values when a named preset is active (existing behavior).
3. When the PresetListener detects a save while a named preset is active, attempt to dim the currently visible sliders via alpha manipulation as a best-effort visual cue. Accept that this dimming may not persist across tab switches.

**Verified from:** LunaLib source inspection -- no `disabled`, `setEnabled`, or `isEnabled` in any settings-related source file. `addedElements.clear()` called in `recreatePanel()` (line 226). No tab-change callback exists.

**Confidence:** HIGH for the limitation; MEDIUM for the workaround approach.

### Pattern 7: "Load Preset Values" Control
**What:** The user wants a dropdown/selector on the Configuration tab that loads a preset's values into the Custom sliders as a starting point (only available when Custom is active).

**LunaLib field type options:**
- **Radio field:** Renders as a vertical button list. Can have a sentinel option like "-- Select --" to indicate no action. When the user selects "Load Vanilla" etc., the PresetListener detects the change and copies preset values into the slider fields. This is the best match.
- **String field:** Free-text input -- not suitable for a selector.
- **Boolean fields:** Would need one per preset -- user rejected this (wants single compact control).

**Recommended:** Use a Radio field with options: `-- Select --,Load Vanilla,Load Recommended,Load Hardcore`. Default to `-- Select --`. In the PresetListener, when this field changes from "-- Select --" to a preset name, load the values into sliders, then reset the field back to "-- Select --" so it acts as a trigger.

**Visibility control (only when Custom is active):** LunaLib CSV fields cannot be conditionally shown/hidden. The Radio field will always be visible on the Configuration tab regardless of the active preset. However, when a named preset is active, the sliders are (visually) disabled and the load control is irrelevant. Add a Text field note below it: "This control only works when [Custom] is the active preset."

**Confidence:** MEDIUM -- the Radio field as a trigger is architecturally sound but resetting it to "-- Select --" after use requires the same internal API access pattern. If the reset fails, the field stays on "Load Vanilla" etc., which is cosmetically wrong but functionally harmless.

### Anti-Patterns to Avoid
- **Renaming existing field IDs:** Breaks saved user settings. Keep ALL existing IDs (`cruiserToFrigate`, `preset`, etc.) exactly as-is. Add NEW fields for new functionality.
- **Leaving blank tab columns:** Any CSV row with an empty `tab` column creates an unwanted "General" tab. Every non-blank row must have a tab value.
- **Using the `lunalib/LunaSettings.json` alongside CSV:** This is a legacy file not recognized by LunaLib's current CSV-based loading system. It must be deleted, not maintained in parallel.
- **Dynamic CSV generation at runtime:** LunaLib loads CSV once at application start. Runtime modifications are not supported.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Tab-based settings UI | Custom panel API, InteractionDialogPlugin for settings | LunaLib CSV `tab` column | LunaLib renders tabs, handles scrolling, saving, persistence automatically |
| Single-select preset picker | Boolean checkboxes with cross-field radio enforcement | LunaLib Radio field type | Radio field natively enforces single selection; no listener logic needed for mutual exclusion |
| Settings persistence | Custom JSON file management | LunaLib's built-in persistence to `saves/common/LunaSettings/` | LunaLib handles file creation, default merging, save/load automatically |
| Highlighted text display | Custom rendering code | LunaLib Text field with `[bracket]` syntax | LunaLib's text rendering already parses brackets and applies highlight colors |

**Key insight:** LunaLib handles 90% of this phase's requirements through its declarative CSV system. The remaining 10% (slider disabling, preset value loading) requires internal API access that the codebase already employs.

## Common Pitfalls

### Pitfall 1: Orphaned "General" Tab from Missing Tab Values
**What goes wrong:** Any CSV row with an empty `tab` column creates a "General" tab. During the migration from zero tabs to three tabs, it is easy to miss a row -- especially blank spacer rows or the Text/Header rows that had no tab value before.
**Why it happens:** The existing CSV has NO tab values on any row. Every row must gain a tab value.
**How to avoid:** After writing the new CSV, audit every non-blank row to confirm column 9 (tab) is populated. Remove blank spacer rows (use Header fields for visual separation instead). Test in-game: if you see a "General" tab, a row is missing its tab.
**Warning signs:** Four tabs visible instead of three when opening settings.

### Pitfall 2: Field ID Rename Destroys Saved Custom Values
**What goes wrong:** LunaLib persists settings keyed by `fieldID`. Renaming `cruiserToFrigate` to anything else causes LunaLib to treat it as a new field and load the CSV default instead of the user's saved value.
**Why it happens:** It is tempting to rename IDs to reflect the new organization. The `fieldID` column is the persistence key -- there is no migration hook.
**How to avoid:** Keep ALL 17 existing field IDs unchanged: `preset`, `cruiserToFrigate`, `cruiserToDestroyer`, `capitalToFrigate`, `capitalToDestroyer`, `capitalToCruiser`, `cruiserCrewMult`, `cruiserSupplyMult`, `cruiserFuelMult`, `capitalCrewMult`, `capitalSupplyMult`, `capitalFuelMult`, `cruiserBuildCostMult`, `capitalBuildCostMult`, `cruiserDmodCount`, `capitalDmodCount`, `factionBlacklist`. Diff old and new CSV on the `fieldID` column before committing.
**Warning signs:** Users report "all my settings reset" after updating the mod.

### Pitfall 3: Text and Header Fields Are NOT Persisted
**What goes wrong:** LunaLib's `saveDefaultsToFile()` explicitly skips Text and Header fields: `if (default.fieldType == "Text" || default.fieldType == "Header") continue` (LunaSettingsLoader.kt line 155). This means Text field content is always read from the CSV, never from the saved JSON. This is GOOD for static summaries (they always show the latest CSV content). But it means you CANNOT use Text fields for dynamically updated content (e.g., "Custom preset shows its own summary block reflecting current slider values").
**Why it happens:** Text/Header are display-only types with no user-editable value; LunaLib correctly treats them as static.
**How to avoid:** For the Custom preset summary that must reflect current slider values: this cannot be a CSV Text field. Options: (a) use a String field set to read-only appearance via the PresetListener, (b) accept that the Custom summary shows fixed text like "Custom: uses your configured slider values" rather than live values, (c) use a String-type field that the PresetListener dynamically updates with formatted text. Option (b) is recommended for Phase 1 simplicity.
**Warning signs:** Custom summary text never changes despite slider value changes.

### Pitfall 4: Tab Button Layout (2 per Row)
**What goes wrong:** LunaLib's tab rendering code (`recreateTabs()`) places up to 2 tabs per row, then wraps to a new row. With 3 tabs (Presets, Configuration, Factions), the layout will be: Row 1: [Presets] [Configuration], Row 2: [Factions]. This means the Factions tab sits alone on a second row, taking up vertical space.
**Why it happens:** The code `if (row!!.size >= 2) { rowCount++ }` caps each row at 2 tabs. Three tabs always creates 2 rows.
**How to avoid:** This is a cosmetic issue, not a bug. Accept the 2-row tab layout. The user may want to verify this looks acceptable. If the tab names are short, they will fit well in the 2-per-row layout. "Presets" (7 chars), "Configuration" (13 chars), "Factions" (8 chars) -- reasonable lengths.
**Warning signs:** Two rows of tab buttons instead of one. Expected behavior for 3 tabs.

### Pitfall 5: PresetListener settingsChanged Fires on Save, Not on Tab Switch
**What goes wrong:** The `settingsChanged()` callback fires when the user clicks "Save All" (verified from `LunaSettingsUIModsPanel.kt` line 624: `LunaSettings.reportSettingsChanged(mod)`). It does NOT fire when the user switches tabs, changes a slider value, or selects a different Radio option. This means any "live" UI updates (dimming sliders, showing/hiding the load-preset control) cannot be triggered by tab navigation events.
**Why it happens:** LunaLib's architecture separates UI interaction (immediate, per-element) from persistence (batched save). The listener system hooks into persistence.
**How to avoid:** Accept that "disabled sliders" state can only be applied on save events. Between saves, sliders remain interactive but `Settings.java` ignores their values when a named preset is active. The explanatory Text field at the top of Configuration tab provides the user-facing guard.
**Warning signs:** Sliders appear enabled after tab switch even though a named preset is active.

## Code Examples

### CSV Tab Assignment (Complete Row)
```csv
fieldID,fieldName,fieldType,defaultValue,secondaryValue,fieldDescription,minValue,maxValue,tab
preset,Difficulty Preset,Radio,Sirix Recommended,"Custom,Vanilla,Sirix Recommended,Sirix Hardcore",Select a difficulty preset to apply.,,,Presets
```
Source: LunaLib CSV column spec from wiki + verified in `LunaSettingsLoader.kt`

### Text Field with Multiple Highlights
```csv
recReplacementSummary,>> Sirix Recommended <<,Text,"Cruiser: Frigate [30%] Destroyer [50%] Stays [20%] | Capital: Frigate [20%] Destroyer [40%] Cruiser [25%] Stays [15%]",,,,,Presets
```
Source: LunaLib Text rendering in `LunaSettingsUISettingsPanel.kt` lines 288-302

### Header Field
```csv
header_replacement,Ship Replacement,Header,Ship Replacement,,,,,Configuration
```
Source: LunaLib Header rendering in `LunaSettingsUISettingsPanel.kt` lines 263-272

### Existing Slider with Tab Assignment (No ID Change)
```csv
cruiserToFrigate,Cruiser to Frigate,Int,30,,Percentage chance for cruisers to be replaced with frigates,0,100,Configuration
```
Source: Existing `LunaSettings.csv` field with tab column added

### PresetListener UI Element Access (Existing Pattern)
```java
// Already in PresetListener.java -- this pattern is proven to work
List<LunaUIBaseElement> elements = LunaSettingsUISettingsPanel.Companion.getAddedElements();
for (LunaUIBaseElement element : elements) {
    Object key = element.getKey();
    if (!(key instanceof LunaSettingsData)) continue;
    LunaSettingsData settingsData = (LunaSettingsData) key;
    if (!MOD_ID.equals(settingsData.getModID())) continue;
    // Manipulate element...
}
```
Source: `PresetListener.java` lines 213-222

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `lunalib/LunaSettings.json` (JSON settings definition) | `data/config/LunaSettings.csv` (CSV settings definition) | LunaLib v1.5.6 | CSV is canonical; JSON file in mod root is legacy. Must delete. |
| Flat settings list (no tabs) | Tab column in CSV (since v1.4.0) | LunaLib v1.4.0 | Tabs available since 2023; many mods use them. Smaller Sector should adopt. |
| Radio type absent | Radio field type available | LunaLib v1.5.0 | Enables single-select preset picker without custom checkbox logic |
| Tab state lost on reopen | Tab selection remembered | LunaLib v1.7.6 | Users return to last-viewed tab after closing/reopening settings |

**Deprecated/outdated:**
- `lunalib/LunaSettings.json`: Not loaded by LunaLib. Not referenced by mod code. Safe to delete. This is CLNP-01.
- Inline preset annotation Text rows (e.g., `cruiserToFrigatePresets`): Replaced by comparison summaries on Presets tab. However, removing these is CLNP-02 which is Phase 2 scope. Phase 1 should ADD the new summaries but MAY leave old annotations in place if removing them is deemed Phase 2 work. Per the context decisions, Phase 1 does not mention removing old annotations -- it focuses on adding the new structure.

## Discretion Recommendations

### Text Formatting for Comparison Summaries

**Recommended format:** Use one Text field per preset, per category. Group by category with category name as a Text sub-header. Use `[brackets]` for all numeric values.

Example Presets tab layout (each line is one CSV Text field):
```
Radio: [preset selector]

Header: "Preset Comparison"

Text: "--- Ship Replacement ---"

Text: ">> Vanilla <<"
Text: "  Cruiser:  Frigate [0%]  Destroyer [0%]  Stays [100%]"
Text: "  Capital:  Frigate [0%]  Destroyer [0%]  Cruiser [0%]  Stays [100%]"

Text: ">> Sirix Recommended <<"
Text: "  Cruiser:  Frigate [30%]  Destroyer [50%]  Stays [20%]"
Text: "  Capital:  Frigate [20%]  Destroyer [40%]  Cruiser [25%]  Stays [15%]"

Text: ">> Sirix Hardcore <<"
Text: "  Cruiser:  Frigate [50%]  Destroyer [45%]  Stays [5%]"
Text: "  Capital:  Frigate [40%]  Destroyer [40%]  Cruiser [18%]  Stays [2%]"

Text: ">> Custom <<"
Text: "  Uses your configured values from the Configuration tab."

Text: "--- Costs & Penalties ---"
[same pattern for costs per preset]

Text: "--- D-Mods ---"
[same pattern for dmods per preset]
```

**Active preset highlighting:** Prefix the active preset's name with `>>` arrows and highlight the name in brackets: `[>> Sirix Recommended <<]`. Non-active presets use plain `>>` without brackets. This leverages LunaLib's `[bracket]` highlighting to make the active one stand out in a different color.

**Challenge with dynamic highlighting:** Text field content is static (loaded from CSV at app start). The `>>` markers cannot change at runtime based on which preset is selected. To truly highlight the active preset dynamically, the PresetListener would need to update Text field content at save-time via the internal API -- but Text fields are not in `addedElements` (they are rendered as plain `addPara` elements, not as `LunaUIBaseElement` instances).

**Practical recommendation:** Accept static text. All presets get identical formatting. The Radio selector above clearly shows which is active. Users can compare values without needing active-preset highlighting in the summaries. If dynamic highlighting is essential, consider using String fields instead of Text fields (String fields ARE persisted and appear in `addedElements`), but this adds complexity for marginal UX gain.

### Active Preset Visual Highlighting

Given the static nature of Text fields, recommend:
- The Radio selector itself shows the active preset (this is native Radio behavior -- the selected button is visually distinct)
- Text summaries use uniform formatting without dynamic active markers
- If the user insists on a marker, use a separate String field at the top of the Presets tab that the PresetListener updates to show: "Active: [Sirix Recommended]". This field IS persisted and CAN be dynamically updated.

### LunaSettings.json Deletion Verification

Approach:
1. Delete `lunalib/LunaSettings.json`
2. Run `ant build` -- compilation should succeed (no code references this file)
3. Launch Starsector with the mod enabled
4. Open mod settings (F2 in campaign) -- verify all settings appear correctly from CSV
5. Change a setting, save, close settings, reopen -- verify persistence works
6. Verify `saves/common/LunaSettings/smallersector.json.data` still exists and contains correct values

No code references `lunalib/LunaSettings.json` (verified by grep of entire src directory).

### "Sliders Disabled" Explanation Text

Recommended wording (as a Text field at top of Configuration tab):
```
[NOTE:] A preset is currently active. These sliders will not take effect. To customize values, select [Custom] on the [Presets] tab.
```

When Custom IS active, alternative text:
```
[Custom] preset is active. Your slider values below are being used.
```

Since Text field content is static (loaded from CSV), only one message can be shown. Use the "preset active" warning, since that is the non-obvious case. When Custom is active, users intuitively understand that sliders are live.

Recommended static text:
```
[NOTE:] Slider values below only apply when the [Custom] preset is selected on the [Presets] tab. Other presets use their own hardcoded values.
```

### Dropdown vs Radio for "Load Preset Values" Control

**Recommendation: Radio field.**

LunaLib has no dropdown/select field type. The available types are: Int, Double, String, Boolean, Radio, Text, Header, Color, Keycode. Radio is the closest to a selector -- it renders as a vertical list of exclusive buttons.

Use a Radio field with options: `-- None --,Vanilla Values,Recommended Values,Hardcore Values`
- Default: `-- None --`
- fieldName: "Load Preset as Starting Point"
- fieldDescription: "Select a preset to copy its values into the sliders below. Only works when Custom is the active preset."
- Tab: Configuration

When the PresetListener detects this field changed to a non-sentinel value AND the active preset is Custom, it copies the preset's values into the slider fields (using the existing `updateUIElements()` pattern), then resets this field back to `-- None --`.

## Open Questions

1. **Dynamic Custom Preset Summary**
   - What we know: Text fields are static (loaded from CSV, not persisted). The user decision says "Custom preset shows its own summary block reflecting current slider values."
   - What's unclear: Whether a truly dynamic summary (updating when sliders change) is achievable within LunaLib constraints. Text fields cannot be dynamically updated. String fields CAN be updated but render with an editable text input appearance, not as clean display text.
   - Recommendation: For Phase 1, show static text: "Custom: uses your configured values from the Configuration tab." Defer dynamic summary to a future phase or accept the limitation. If the user requires live values, investigate using the String field type with the PresetListener updating its content on save events.

2. **Slider Disabling Fidelity**
   - What we know: LunaLib has no disable mechanism. The PresetListener CAN manipulate element alpha for dimming, but only for the currently visible tab, and only after a save event.
   - What's unclear: Whether alpha manipulation on `LunaUITextFieldWithSlider` produces an acceptable "grayed out" appearance, and whether user input is actually blocked or just visually dimmed.
   - Recommendation: Implement text-based guard (explanation text at top of Configuration tab) as the primary mechanism. Attempt alpha-based dimming as a best-effort enhancement. Accept that sliders remain functionally interactive but are ignored by `Settings.java`.

3. **"Load Preset" Radio Reset**
   - What we know: The PresetListener can update Radio values via the internal API after detecting a change. The existing code updates sliders and text fields successfully.
   - What's unclear: Whether `LunaUIRadioButton.setValue()` exists or if the reset requires finding the radio element in `addedElements` and calling `setSelected()` on the correct sub-button.
   - Recommendation: Investigate `LunaUIRadioButton` API during implementation. The `value` field is public (`var value: String?`), so setting it directly may work. The visual update (re-selecting the "-- None --" button) requires calling `setSelected()` on the correct sub-button.

## Sources

### Primary (HIGH confidence)
- LunaLib source code at `/home/sirix/Games/starsector/mods/LunaLib/src/` -- Direct inspection of:
  - `LunaSettingsLoader.kt` -- CSV loading, tab column parsing, field type handling, persistence skipping for Text/Header
  - `LunaSettingsUISettingsPanel.kt` -- Tab rendering, panel creation, field rendering, `addedElements` management
  - `LunaUIRadioButton.kt` -- Radio button rendering, single-selection enforcement
  - `LunaSettingsUIModsPanel.kt` -- Save flow, `settingsChanged` trigger timing
- Existing mod source code at `/home/sirix/Games/Modding/smaller-sector/src/smallersector/` -- Direct inspection of:
  - `Settings.java` -- Current preset override logic, field ID usage
  - `PresetListener.java` -- Internal API usage patterns, UI element manipulation
  - `SmallerSectorModPlugin.java` -- Listener registration
- Existing mod data at `/home/sirix/Games/Modding/smaller-sector/data/config/LunaSettings.csv` -- Current CSV structure and field IDs
- Existing mod data at `/home/sirix/Games/Modding/smaller-sector/lunalib/LunaSettings.json` -- Duplicate file to be deleted

### Secondary (MEDIUM confidence)
- [LunaLib Wiki - LunaSettings.CSV](https://github.com/Lukas22041/LunaLib/wiki/LunaSettings.CSV) -- Tab column spec, field types, bracket highlighting
- [LunaLib Wiki - LunaSettingsListener](https://github.com/Lukas22041/LunaLib/wiki/LunaSettingsListener) -- Listener registration, callback timing
- [LunaLib Wiki - Integrating LunaSettings](https://github.com/Lukas22041/LunaLib/wiki/Integrating-LunaSettings) -- Persistence location, soft dependency
- [LunaLib Changelog](https://raw.githubusercontent.com/Lukas22041/LunaLib/main/changelog.txt) -- Version history for tabs (v1.4.0), Radio (v1.5.0), tab persistence (v1.7.6)

### Tertiary (LOW confidence)
None -- all findings verified against source code.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- LunaLib is the only option; confirmed via source code inspection
- Architecture patterns: HIGH for CSV/Tab/Radio/Text mechanics (verified from source); MEDIUM for slider disabling (limitation confirmed, workaround unverified)
- Pitfalls: HIGH -- verified from source code (tab ordering, field persistence, Text field skipping, addedElements lifecycle)
- Discretion items: MEDIUM -- formatting recommendations are subjective; technical feasibility verified

**Research date:** 2026-02-21
**Valid until:** 2026-04-21 (LunaLib is stable; 2.0.x series has been maintenance releases only)
