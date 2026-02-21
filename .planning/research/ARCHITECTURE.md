# Architecture Patterns

**Domain:** LunaLib Tabbed Settings UI for Starsector Mod
**Researched:** 2026-02-21

## Recommended Architecture

The revamp restructures settings from a flat slider list into a tabbed LunaLib UI with four tabs: three read-only preset tabs and one editable custom tab. The faction management tab is deferred from the LunaLib UI (explained below). The core architectural change is moving preset values from hardcoded Java maps into the CSV declaration layer while keeping `Settings.java` as the single read point.

### High-Level Component Diagram

```
LunaSettings.csv (declarative)
  |
  +-- Tab: "Vanilla"          -- read-only Text fields showing preset values + Boolean enable checkbox
  +-- Tab: "Recommended"      -- read-only Text fields showing preset values + Boolean enable checkbox
  +-- Tab: "Hardcore"         -- read-only Text fields showing preset values + Boolean enable checkbox
  +-- Tab: "Custom"           -- editable sliders/doubles (same field IDs as current)
  |
  v
LunaLib Engine (renders tabs, stores values in JSON)
  |
  v
PresetListener.java (settingsChanged callback)
  |
  +-- Reads which preset Boolean is checked
  +-- Enforces radio behavior (uncheck others)
  +-- If Custom: sliders are live, values read directly
  +-- If preset: values served from PresetValues constants
  |
  v
Settings.java (static getters -- single source of truth for all consumers)
  |
  +-- ShipReplacer, CostModifier, BaseValueModifier, DmodApplicator, etc.
```

### Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| `data/config/LunaSettings.csv` | Declares all UI fields, tabs, types, defaults, tooltips. The tab column assigns each row to its tab. | LunaLib engine (read at app load) |
| `lunalib/LunaSettings.json` | Legacy format -- **remove**. The CSV is the canonical settings definition since LunaLib 1.5.6. Keeping both causes confusion. | LunaLib engine |
| `Settings.java` | Single read point for all setting values. Every consumer (ShipReplacer, CostModifier, etc.) calls `Settings.getX()`. Determines which preset is active, returns appropriate value. | LunaLib (reads values), all consumers (provides values) |
| `PresetListener.java` | Responds to `settingsChanged()`. Enforces radio behavior across preset enable checkboxes. Syncs custom slider values when switching to/from Custom. Triggers downstream effects (BaseValueModifier, blacklist reload). | LunaLib (receives events), Settings (triggers reload), BaseValueModifier (triggers reapply) |
| `PresetValues.java` (new) | Static constants class holding all preset value maps. Extracted from current PresetListener static blocks. Single source of truth for what each preset contains. | Settings.java (reads values), PresetListener (reads for checkbox sync) |
| `FactionManagerDialog.java` | In-game dialog for faction blacklist (unchanged). | Settings (reads/writes blacklist) |
| `FactionManagerIntel.java` | Intel panel entry point (unchanged). | FactionManagerDialog (launches it) |

### Why No Faction Tab in LunaLib

LunaLib's CSV-driven settings UI supports these field types: Int, Double, String, Boolean, Radio, Text, Header, Color, Keycode. There is no list/table component, no dynamic row generation, and no way to enumerate loaded factions at CSV declaration time. A faction tab showing every loaded faction with source mod and toggle would require either:

1. **Hardcoding faction IDs in CSV** -- defeats the purpose (factions are mod-dependent)
2. **Custom LunaLib UI panel** -- LunaLib does not expose a custom panel API for the settings screen
3. **Keeping the existing approach** -- FactionManagerDialog uses Starsector's InteractionDialogPlugin which supports dynamic content

**Decision:** Keep faction management in the existing in-game FactionManagerDialog/Intel system. The LunaLib settings UI handles presets and custom values only. The faction blacklist string field stays in the Custom tab for power users who want to manually type faction IDs, but the primary UX is through the in-game dialog.

**Confidence:** HIGH -- verified against LunaLib wiki documentation for field types and the CSV column specification.

## Data Flow

### Preset Selection Flow

```
User clicks Boolean checkbox on "Recommended" tab
  |
  v
LunaLib saves Boolean value, fires settingsChanged("smallersector")
  |
  v
PresetListener.settingsChanged()
  |
  +-- Read all three preset Booleans: enableVanilla, enableRecommended, enableHardcore
  +-- Detect which one changed to true (compare with last state)
  +-- Write false to the other two Booleans (radio enforcement)
  +-- Write false to forceCustom Boolean
  +-- Update LunaLib's changedSettings list + UI elements (existing pattern)
  +-- Call Settings.reloadBlacklist() to refresh faction cache
  +-- Call BaseValueModifier.applyMultipliers() to refresh build costs
  |
  v
Settings.getActivePreset() (new method)
  |
  +-- Check forceCustom Boolean first (highest priority)
  +-- Then check enableVanilla, enableRecommended, enableHardcore
  +-- Return enum: VANILLA | RECOMMENDED | HARDCORE | CUSTOM
  |
  v
Settings.getCruiserToFrigate() etc.
  |
  +-- If CUSTOM: read from LunaLib slider value (cruiserToFrigate field)
  +-- If preset: return from PresetValues.RECOMMENDED.get("cruiserToFrigate")
```

### Custom Override Flow

```
User navigates to "Custom" tab
  |
  +-- Sees "Force Custom Values" Boolean checkbox (default: unchecked)
  +-- Sliders are always visible and editable (LunaLib has no disable mechanism per-element from CSV)
  +-- But Settings.java only reads slider values when forceCustom is true
  |
  v
User checks "Force Custom Values"
  |
  v
PresetListener.settingsChanged()
  |
  +-- Detects forceCustom changed to true
  +-- Unchecks all three preset Booleans (radio enforcement)
  +-- Custom slider values are now live
  |
  v
Settings.getActivePreset() returns CUSTOM
  |
  v
All getters now read from LunaLib slider values instead of preset maps
```

### Settings Value Resolution (Settings.java)

```
Settings.getCruiserToFrigate():
  1. Call getActivePreset()
  2. Switch on result:
     - VANILLA:     return PresetValues.VANILLA.cruiserToFrigate     // 0
     - RECOMMENDED: return PresetValues.RECOMMENDED.cruiserToFrigate // 30
     - HARDCORE:    return PresetValues.HARDCORE.cruiserToFrigate    // 50
     - CUSTOM:      return LunaSettings.getInt(MOD_ID, "cruiserToFrigate") ?? 30
```

### Preset Values Persistence

Custom slider values persist in LunaLib's save JSON (`saves/common/LunaSettings/smallersector.json.data`) regardless of which preset is active. Switching from Custom to Recommended and back to Custom preserves the user's custom values because LunaLib stores all field values independently.

**Key insight:** LunaLib stores ALL settings values, not just the active ones. The preset system is purely a read-time selection mechanism in `Settings.java`, not a write-time mechanism.

## Patterns to Follow

### Pattern 1: Tab Assignment via CSV Column

**What:** The `tab` column in LunaSettings.csv assigns each setting row to a named tab. Empty tab column = "General" tab.

**When:** Every settings row needs a tab assignment.

**Example (CSV rows):**
```csv
fieldID,fieldName,fieldType,defaultValue,secondaryValue,fieldDescription,minValue,maxValue,tab
enableVanilla,Enable Vanilla Preset,Boolean,false,,,,,Vanilla
vanillaNote,Description,Text,"[All replacement disabled.] Ships spawn at vanilla rates with no cost modifications.",,,,,Vanilla
vanillaCruiserToFrigate,Cruiser to Frigate,Text,"[0%]",,,,,Vanilla
...
enableRecommended,Enable Recommended Preset,Boolean,true,,,,,Recommended
...
cruiserToFrigate,Cruiser to Frigate,Int,30,,Percentage chance for cruisers to be replaced with frigates,0,100,Custom
...
```

**Confidence:** HIGH -- verified from LunaLib wiki: "The Tab column generates tabs. Any Config under the same assigned Tab will be displayed under it together."

### Pattern 2: Radio Behavior via Listener (not Radio type)

**What:** LunaLib's Radio field type creates mutually exclusive options within a single field. But we need radio behavior ACROSS TABS (one checkbox per tab, only one active). This requires implementing radio logic in `PresetListener.settingsChanged()` by programmatically unchecking other Booleans when one is checked.

**When:** Any time a preset Boolean changes.

**Why not use Radio type:** Radio creates a single dropdown/radio group within one tab. We need a checkbox on each separate tab.

**Example:**
```java
public void settingsChanged(String modID) {
    if (!MOD_ID.equals(modID)) return;

    Boolean vanilla = LunaSettings.getBoolean(MOD_ID, "enableVanilla");
    Boolean recommended = LunaSettings.getBoolean(MOD_ID, "enableRecommended");
    Boolean hardcore = LunaSettings.getBoolean(MOD_ID, "enableHardcore");
    Boolean custom = LunaSettings.getBoolean(MOD_ID, "forceCustom");

    // Detect which changed to true, uncheck others
    // Write back via LunaLib JSON + update UI elements
}
```

**Confidence:** MEDIUM -- the radio enforcement pattern via listener is proven in the existing PresetListener (it already writes to LunaLib JSON and updates UI elements). The cross-tab Boolean approach is architecturally sound but untested with this specific UX.

### Pattern 3: Read-Only Display via Text Fields

**What:** Preset tabs show values as Text type fields (display only, not editable). The text content includes `[bracketed]` values for LunaLib's highlight rendering.

**When:** Showing preset values that users should see but not edit.

**Example:**
```csv
vanillaCruiserToFrigate,Cruiser to Frigate,Text,"[0%] chance to replace with frigates",,,,,Vanilla
recCruiserToFrigate,Cruiser to Frigate,Text,"[30%] chance to replace with frigates",,,,,Recommended
```

**Confidence:** HIGH -- Text type confirmed in LunaLib docs: "defaultValue sets content, used for contextual information." Bracket highlighting confirmed: "surround text with brackets like [this] to apply standard highlighting."

### Pattern 4: Single Source of Truth for Preset Values

**What:** Extract preset value maps from `PresetListener.java` into a dedicated `PresetValues.java` class. Both `Settings.java` (for reading) and the CSV Text fields (hardcoded strings matching these values) reference the same conceptual data.

**When:** Any time preset values need to be defined or referenced.

**Risk:** The CSV Text field strings and the Java preset maps can drift out of sync. Mitigation: add a comment block in both files cross-referencing each other.

## Anti-Patterns to Avoid

### Anti-Pattern 1: Dynamic CSV Generation

**What:** Trying to generate LunaSettings.csv rows programmatically at runtime to list factions or dynamically change text.

**Why bad:** LunaLib loads the CSV once at application start. The CSV is a static declaration. Runtime modifications require going through LunaLib's internal APIs (LunaSettingsLoader), which are backend classes not part of the public API and may break between versions.

**Instead:** Use static CSV declarations for all settings. For dynamic content (faction lists), use Starsector's InteractionDialogPlugin (the existing FactionManagerDialog pattern).

### Anti-Pattern 2: Using LunaSettings.json AND LunaSettings.csv

**What:** Defining settings in both `lunalib/LunaSettings.json` and `data/config/LunaSettings.csv`.

**Why bad:** The current project has BOTH files. LunaLib's CSV format is the canonical approach since v1.5.6. The JSON format (`lunalib/LunaSettings.json`) is an older approach. Having both creates confusion about which is authoritative and may cause field ID conflicts or duplicate UI entries.

**Instead:** Use only `data/config/LunaSettings.csv`. Delete `lunalib/LunaSettings.json`. The CSV provides all needed features: tabs, field types, headers, text fields, sliders.

### Anti-Pattern 3: Coupling Preset Logic to UI Layer

**What:** Having `PresetListener` contain both preset value definitions AND UI synchronization logic (as it does currently).

**Why bad:** When adding tabs, the listener grows more complex (radio enforcement, cross-tab sync). Mixing "what are the values" with "how to sync the UI" makes the class harder to maintain.

**Instead:** Extract preset definitions into `PresetValues.java`. Keep `PresetListener` focused on: detecting changes, enforcing radio behavior, triggering downstream effects.

### Anti-Pattern 4: Disabling Sliders Based on Preset

**What:** Trying to make Custom tab sliders appear disabled/grayed when a preset is active.

**Why bad:** LunaLib CSV supports a `disabled` property but it is a static declaration, not runtime-togglable. There is no documented way to dynamically enable/disable fields based on another field's value from the CSV layer. The PresetListener CAN update UI elements programmatically, but disabling/enabling elements is not part of the documented `LunaUIBaseElement` API surface used in the existing code.

**Instead:** Always show sliders as editable. Use a prominent Text field warning: "[These values only apply when Force Custom Values is checked above.]" The `Settings.java` layer handles which values to actually use.

## Component Build Order

The dependency chain determines build order. Each phase depends on the previous one being complete.

### Phase 1: PresetValues Extraction + Settings Refactor

**Build first because:** Everything else depends on `Settings.java` knowing about the new preset mechanism.

**Components:**
1. `PresetValues.java` (new) -- Extract static preset maps from PresetListener
2. `Settings.java` -- Refactor to use `getActivePreset()` method that reads Boolean fields instead of Radio field; all getters switch on active preset
3. Remove dependency on `preset` Radio field (old approach)

**Dependencies:** None -- this is a pure refactor of existing logic.

**Validates:** The core value resolution works before any UI changes.

### Phase 2: CSV Tab Structure

**Build second because:** The tab layout is the visual foundation for everything else.

**Components:**
1. `data/config/LunaSettings.csv` -- Complete rewrite with tab column assignments
2. Delete `lunalib/LunaSettings.json` -- Remove duplicate settings definition

**Tab structure:**
- **Vanilla tab:** enableVanilla Boolean + Text fields showing all Vanilla values + description
- **Recommended tab:** enableRecommended Boolean + Text fields showing all Recommended values + description
- **Hardcore tab:** enableHardcore Boolean + Text fields showing all Hardcore values + description
- **Custom tab:** forceCustom Boolean + warning Text + all editable sliders (Int/Double) + faction blacklist String

**Dependencies:** Phase 1 (Settings.java must read new field IDs).

**Validates:** Tabs render correctly in LunaLib, fields appear in right tabs.

### Phase 3: PresetListener Radio Enforcement

**Build third because:** Needs both the new Settings logic and the new CSV fields to exist.

**Components:**
1. `PresetListener.java` -- Rewrite to:
   - Read four Boolean fields (enableVanilla, enableRecommended, enableHardcore, forceCustom)
   - Enforce mutual exclusivity (radio behavior across tabs)
   - Update LunaLib JSON + changedSettings + UI elements (existing patterns)
   - Trigger BaseValueModifier.applyMultipliers() and Settings.reloadBlacklist()

**Dependencies:** Phase 1 (PresetValues), Phase 2 (new field IDs in CSV).

**Validates:** Checking one preset unchecks others; switching presets updates downstream systems.

### Phase 4: Cleanup + Polish

**Build last because:** Everything functional must work first.

**Components:**
1. Remove old `preset` Radio field handling from Settings.java
2. Remove old flat-list preset note Text fields from CSV
3. Update SmallerSectorModPlugin.java if registration changes needed
4. Verify FactionManagerDialog still works (should be unchanged)
5. Test save/load compatibility (existing settings JSON should still load)

**Dependencies:** Phases 1-3 complete.

**Validates:** Full end-to-end flow, save compatibility, no regressions.

## Scalability Considerations

| Concern | Current (4 presets) | At 8+ presets | Mitigation |
|---------|--------------------|--------------|-|
| CSV size | ~80 rows (manageable) | 150+ rows (still manageable for CSV) | Each preset tab adds ~20 Text rows. CSV has no practical size limit. |
| PresetListener complexity | 4-way radio enforcement | N-way radio enforcement | Extract radio logic into helper method; iterate preset list instead of hardcoding |
| PresetValues maps | 3 static maps + Custom | More static maps | Move to enum with constructor-initialized maps |
| Settings.java getters | 4-way switch per getter | N-way switch per getter | Use PresetValues enum lookup instead of switch statements |

## Key Technical Details

### LunaLib CSV Column Reference

```
fieldID        -- Unique ID used in LunaSettings.getString/getInt/getDouble/getBoolean calls
fieldName      -- Display label in the UI
fieldType      -- Int|Double|String|Boolean|Radio|Text|Header|Color|Keycode
defaultValue   -- Initial value (also sets Text content, Header name)
secondaryValue -- Only used by Radio type (comma-separated options)
fieldDescription -- Description text below the field (supports [bracket] highlighting)
minValue       -- Min for Int/Double sliders
maxValue       -- Max for Int/Double sliders
tab            -- Tab name. Empty = "General" tab. Same name = same tab.
```

### LunaLib Internal APIs Used by PresetListener

The existing `PresetListener.java` already uses these LunaLib backend classes (not public API, but stable across recent versions):

```java
import lunalib.backend.ui.settings.LunaSettingsLoader;        // Reload settings from JSON
import lunalib.backend.ui.settings.LunaSettingsData;           // Settings data model
import lunalib.backend.ui.settings.LunaSettingsUISettingsPanel; // Access changedSettings + UI elements
import lunalib.backend.ui.settings.ChangedSetting;             // Represent a pending change
import lunalib.backend.ui.components.base.LunaUIBaseElement;   // Base UI element
import lunalib.backend.ui.components.LunaUITextFieldWithSlider; // Slider component
import lunalib.backend.ui.components.base.LunaUITextField;     // Text field component
```

**Risk:** These are `backend` package classes. They work now but could change in future LunaLib updates. The tab-based approach reduces dependency on these internals because preset tabs use read-only Text fields (no need to programmatically update sliders when switching presets -- the user simply checks a different tab's checkbox).

### Save Compatibility

LunaLib stores settings in `saves/common/LunaSettings/smallersector.json.data`. The migration plan:

1. **New fields** (enableVanilla, enableRecommended, enableHardcore, forceCustom): Get default values from CSV on first load. No migration needed.
2. **Old field** (preset Radio): Settings.java can check if old `preset` field exists and map to new Booleans on first run. Or simply ignore -- default is `enableRecommended = true`.
3. **Existing slider values** (cruiserToFrigate, etc.): Same field IDs in new CSV. Values persist automatically.
4. **Faction blacklist** (factionBlacklist String): Same field ID. Persists automatically.

**Net effect:** Upgrading from old flat UI to new tabbed UI is seamless. The only visible change is the UI layout.

## Sources

- [LunaLib Wiki - LunaSettings.CSV](https://github.com/Lukas22041/LunaLib/wiki/LunaSettings.CSV) -- Tab column spec, field types, highlighting syntax (HIGH confidence)
- [LunaLib Wiki - LunaSettingsListener](https://github.com/Lukas22041/LunaLib/wiki/LunaSettingsListener) -- Listener registration, settingsChanged callback (HIGH confidence)
- [LunaLib Wiki - Integrating LunaSettings](https://github.com/Lukas22041/LunaLib/wiki/Integrating-LunaSettings) -- Setup steps, reading values (HIGH confidence)
- [LunaLib Wiki - LunaSettingsConfig.json](https://github.com/Lukas22041/LunaLib/wiki/LunaSettingsConfig.json) -- Icon customization (HIGH confidence)
- [LunaLib GitHub - Changelog](https://github.com/Lukas22041/LunaLib/blob/main/changelog.txt) -- Tab support since v1.4.0, Radio since v1.5.0 (HIGH confidence)
- Existing codebase: `PresetListener.java`, `Settings.java`, `LunaSettings.csv` -- Current implementation patterns (HIGH confidence, direct source reading)

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Tab column in CSV | HIGH | Verified in LunaLib wiki, confirmed in existing CSV header |
| Text field for read-only display | HIGH | Documented field type with bracket highlighting |
| Boolean for enable checkboxes | HIGH | Standard LunaLib field type |
| Radio enforcement via listener | MEDIUM | Pattern proven in existing code; cross-tab Boolean sync is new but uses same mechanisms |
| Dynamic field disable/enable | LOW | No evidence this is possible from CSV; anti-pattern documented above |
| Faction tab feasibility | HIGH | Confirmed NOT feasible in LunaLib CSV; existing dialog approach is correct |
| Save compatibility | HIGH | Same field IDs, LunaLib handles defaults for new fields |
