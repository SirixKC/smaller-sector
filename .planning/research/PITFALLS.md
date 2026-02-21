# Domain Pitfalls

**Domain:** LunaLib Settings UI Revamp for Starsector Mod
**Researched:** 2026-02-21

## Critical Pitfalls

Mistakes that cause rewrites, broken saves, or major user-facing issues.

### Pitfall 1: Renaming Field IDs Destroys Existing User Settings

**What goes wrong:** LunaLib persists user settings to `saves/common/LunaSettings/smallersector.json.data` keyed by `fieldID` from `LunaSettings.csv`. If you rename a `fieldID` during the tabbed migration (e.g., `cruiserToFrigate` becomes `custom_cruiserToFrigate` to namespace it under a "Custom" tab), LunaLib treats the new ID as a brand-new setting and loads the CSV `defaultValue` instead of the user's saved value. The user's customizations silently vanish.

**Why it happens:** It is tempting to rename IDs to reflect the new tab structure or to avoid confusion between preset-display fields and editable custom fields. The persistence file is keyed by fieldID, not by any semantic mapping.

**Consequences:**
- Every existing user loses their custom probability values, cost multipliers, and faction blacklist string
- Users on "Custom" preset suddenly get "Sirix Recommended" default values applied to their active game
- Ship replacement rates change without the user's knowledge, potentially ruining ongoing campaigns
- LunaLib has no migration hook -- there is no `onSettingsUpgrade()` callback

**Prevention:**
- Keep ALL existing field IDs exactly as they are: `cruiserToFrigate`, `cruiserToDestroyer`, `capitalToFrigate`, `capitalToDestroyer`, `capitalToCruiser`, `cruiserCrewMult`, `cruiserSupplyMult`, `cruiserFuelMult`, `capitalCrewMult`, `capitalSupplyMult`, `capitalFuelMult`, `cruiserBuildCostMult`, `capitalBuildCostMult`, `cruiserDmodCount`, `capitalDmodCount`, `factionBlacklist`, `preset`
- Add NEW fields for tab-specific display purposes (e.g., read-only preview fields) with new unique IDs
- The `tab` column in the CSV controls which tab a field appears on -- use it to reorganize without changing IDs
- Test by: backing up `saves/common/LunaSettings/smallersector.json.data`, deploying the new CSV, launching the game, and verifying all values match the backup

**Detection:**
- Diff old and new `LunaSettings.csv` and confirm zero changes in the `fieldID` column for existing settings
- If any existing fieldID is absent from the new CSV, that is a settings-destroying change

**Phase relevance:** Phase 1 (CSV migration). Must be validated before any code changes.

**Confidence:** HIGH -- verified from LunaLib wiki documentation that settings are keyed by fieldID and persist to `saves/common/LunaSettings/<modId>.json.data`.

---

### Pitfall 2: Preset Checkbox Radio Behavior Is Not Native to LunaLib

**What goes wrong:** LunaLib's `Radio` field type provides single-selection from a list. LunaLib does NOT have a native "checkbox that unchecks other checkboxes" (radio-group-of-checkboxes) widget. Attempting to emulate radio behavior using multiple `Boolean` checkboxes requires manual synchronization in the `settingsChanged()` listener. If the sync logic has a bug or race condition, multiple presets can be "checked" simultaneously, leading to undefined behavior in `Settings.java` where the first matching preset wins.

**Why it happens:** The PROJECT.md specifies "Enable checkbox per preset tab -- radio behavior (checking one unchecks others)." This implies using Boolean checkboxes with manual exclusion logic. LunaLib's existing `Radio` type already provides single-selection, but it renders as a radio button list within a single field, not as a per-tab checkbox.

**Consequences:**
- If two preset checkboxes are true simultaneously, `Settings.getPreset()` returns whichever is checked first in the if-chain, not necessarily what the user last selected
- The `settingsChanged()` callback fires once per save, not per individual checkbox change. If the user checks "Hardcore" then unchecks "Recommended" in the same save action, the listener sees the final state but may process them in wrong order
- UI shows contradictory state (two tabs appear "active")

**Prevention:**
- Option A (recommended): Keep the existing `Radio` field type for preset selection. Place it on a "Presets" tab or at the top of all tabs. Use the `tab` column to put it where desired. The Radio type already handles single-selection natively.
- Option B: If per-tab checkboxes are required, use a single hidden `String` field to track the active preset, and Boolean checkboxes purely for display. In `settingsChanged()`, read all checkboxes, determine which one changed (compare against the hidden string), update the hidden string, then programmatically uncheck the others. This is fragile but workable.
- Option C: Use the `Radio` type but place descriptive `Text` fields on each preset tab that show the values. The Radio field on a "General" or "Presets" tab controls which preset is active. Preset tabs are informational only.
- Regardless of option: validate in `settingsChanged()` that exactly one preset state is active. If zero or multiple are detected, fall back to "Sirix Recommended" and log a warning.

**Detection:**
- Unit test (manual): Open settings, rapidly switch between presets, save, reload -- verify only one preset is active
- Add defensive logging: `if (activePresetCount != 1) log.warn("Multiple presets active: " + activePresets)`

**Phase relevance:** Phase 1 (UI architecture decision). Must be decided before writing any CSV or listener code.

**Confidence:** HIGH -- verified from LunaLib wiki that Radio is a native single-selection type, and Boolean is a simple true/false checkbox with no built-in exclusion.

---

### Pitfall 3: PresetListener Uses LunaLib Internal APIs That May Break

**What goes wrong:** The current `PresetListener.java` directly accesses LunaLib's internal backend classes: `LunaSettingsLoader.INSTANCE.loadSettings()`, `LunaSettingsUISettingsPanel.Companion.getChangedSettings()`, `LunaSettingsUISettingsPanel.Companion.getAddedElements()`, and `LunaSettingsUISettingsPanel.Companion.setUnsaved()`. These are NOT part of LunaLib's public API (`lunalib.lunaSettings.LunaSettings`). They are Kotlin companion object methods in backend implementation classes. A LunaLib update could rename, move, or remove them without notice.

**Why it happens:** LunaLib's public API (`LunaSettings.getString/getInt/getDouble`) is read-only. There is no public `LunaSettings.setString()` or `LunaSettings.setInt()`. To programmatically write settings values (e.g., syncing slider positions when a preset changes), the code must reach into LunaLib internals. The current code does this in three layers: (1) write to the JSON file via LazyLib, (2) reload LunaLib's in-memory cache, (3) update live UI elements.

**Consequences:**
- LunaLib update breaks the import, causing a `ClassNotFoundException` or `NoSuchMethodError` at runtime
- Since the listener is registered in `onApplicationLoad()`, this would crash the game on startup (not just when opening settings)
- Silently failing (catch Exception) means presets appear to change but sliders don't update, confusing users

**Prevention:**
- Wrap ALL internal API calls in individual try-catch blocks (already partially done, but should be more granular)
- Make the internal API usage a clearly isolated adapter class (e.g., `LunaLibInternals.java`) so breakage is contained to one file
- Document the exact LunaLib version these internals were tested against in a comment
- For the revamp: evaluate whether the new tab structure eliminates the need for programmatic value syncing entirely. If preset tabs show read-only values and only the "Custom" tab has editable sliders, you may not need to push values to UI elements at all -- just read from the correct source in `Settings.java`
- Test against each new LunaLib release before updating dependency

**Detection:**
- `ClassNotFoundException` or `NoSuchMethodError` in logs on game startup
- Preset changes don't visually update slider positions (user reports)
- Monitor LunaLib changelog for backend refactors

**Phase relevance:** Phase 2 (PresetListener rewrite). Strongly consider redesigning to eliminate internal API dependency.

**Confidence:** HIGH -- verified from source code that `lunalib.backend.ui.settings.*` imports are used, and LunaLib changelog shows backend refactors have occurred (v1.4.0 reworked settings saving, v1.5.6 changed CSV loading).

---

### Pitfall 4: Custom Values Lost When Switching Away From Custom Preset

**What goes wrong:** The PROJECT.md requires "Custom values persist when switching between presets (remembered for later)." Currently, when a user switches from Custom to a named preset, `PresetListener.settingsChanged()` overwrites the JSON file with preset values (lines 148-153 of PresetListener.java). The custom values are gone from the persistence file. If the user later switches back to Custom, they get the defaults, not their previous custom values.

**Why it happens:** LunaLib stores one value per field ID. There is no concept of "per-preset overrides" or "shadow values." When the preset listener writes preset values to the JSON, it replaces whatever was there.

**Consequences:**
- User spends time tuning 15+ custom values, switches to a preset to try it, switches back, all custom values are gone
- This is the #1 user experience complaint for preset systems in any settings UI

**Prevention:**
- Before overwriting with preset values, read and store the current custom values in a separate persistence location. Options:
  - (a) Use Starsector's `Global.getSector().getPersistentData()` with a key like `"smallersector_custom_values"` -- but this is per-save, not global
  - (b) Use LazyLib's `JSONUtils.loadCommonJSON()` with a dedicated file like `"LunaSettings/smallersector_custom_backup.json"` -- this persists across saves in `saves/common/`
  - (c) Add hidden LunaSettings fields (e.g., `custom_backup_cruiserToFrigate`) that mirror the editable fields but are never shown in UI
- When switching TO Custom preset, restore from the backup instead of using defaults
- When the user edits values in Custom mode, also update the backup
- Option (b) is recommended because it uses the same persistence mechanism LunaLib uses and survives across saves

**Detection:**
- Test: set Custom values, switch to Recommended, switch back to Custom, verify values match original custom values
- Log custom value backup/restore operations for debugging

**Phase relevance:** Phase 2 (preset switching logic). Must be designed into the architecture, not bolted on.

**Confidence:** HIGH -- verified from PresetListener source that preset values are written directly to the settings JSON, overwriting existing values.

---

### Pitfall 5: Settings Changes That Require Save/Reload Create Invisible Desync

**What goes wrong:** Operating cost changes (crew, supply, fuel multipliers) applied via the `SmallerSectorCostMod` hull mod only take effect after a save and reload. The hull mod's `applyEffectsBeforeShipCreation()` reads from `Settings` at ship stats calculation time, but Starsector caches ship stats. The current code displays a warning message ("Operating cost changes require SAVE and RELOAD"), but in a tabbed UI where users rapidly compare presets, the warning may be missed or the user may not understand which changes require reload vs. which are immediate.

**Why it happens:** Build cost multipliers (via `BaseValueModifier`) are applied immediately to hull specs in memory. Ship replacement probabilities take effect on next fleet spawn. But operating costs are recalculated by Starsector's stat system, which caches aggressively. The mod cannot force a stat recalculation.

**Consequences:**
- User changes cost multiplier from 1.5x to 3.0x, doesn't save/reload, wonders why costs haven't changed
- User switches presets rapidly comparing them, each switch triggers the warning message, cluttering the campaign UI
- If the tabbed UI shows "current effective values" alongside settings, the displayed values and actual game values disagree until reload

**Prevention:**
- In the tabbed UI, clearly label operating cost fields with "(requires save + reload)" in their tooltip or description
- Throttle the warning message: only show it once per settings session, not on every change
- Consider grouping settings by "immediate effect" vs "requires reload" in the UI layout
- Do NOT show "current effective values" for operating costs unless you can read them from the hull mod's actual applied stats (which you can, via the ship's `MutableShipStatsAPI`)
- In the new Settings class, add a method like `getEffectiveCruiserCrewMult()` that reads from the hull mod's applied stats rather than the settings, so UI can show the actual active value

**Detection:**
- Test: change operating cost multiplier, DON'T reload, check if ship tooltips in fleet screen show updated values (they won't)
- Compare `Settings.getCruiserCrewMult()` vs the actual stat modifier on a ship

**Phase relevance:** Phase 3 (UI polish). Not a blocker but significantly impacts user trust.

**Confidence:** HIGH -- verified from PresetListener source code (line 102-107) which explicitly warns about this.

## Moderate Pitfalls

### Pitfall 6: Faction Source Detection Via WithSourceMod Is Unreliable

**What goes wrong:** The `FactionManagerDialog.getFactionModSource()` method uses `ShipHullSpecAPI instanceof WithSourceMod` to determine which mod a faction's ships come from. This detection is based on hull spec provenance, not faction provenance. A faction can have ships from multiple mods (e.g., a mod faction that also uses vanilla ships). The current heuristic picks the mod with the most ships, which can misattribute.

**Why it happens:** Starsector's `WithSourceMod` interface is attached to individual specs (hulls, weapons), not to factions. A faction is defined by a `.faction` file, which can reference ships from any mod. There is no `FactionAPI.getSourceMod()` method. The current heuristic (`modCounts.entrySet().stream().max(...)`) returns "Unknown" when no ships implement `WithSourceMod`, and can return the wrong mod when a faction uses a mix of vanilla and modded ships.

**Consequences:**
- Faction tab shows "Starsector" (vanilla) as the source for a mod faction that borrows vanilla ships
- Shows "Unknown" for factions whose ships don't implement `WithSourceMod`
- User relies on source attribution to decide whether to blacklist, makes wrong decision

**Prevention:**
- Add a secondary detection method: check which mod's directory contains the faction's `.faction` file. This can be done by iterating `Global.getSettings().getModManager().getEnabledMods()` and checking if the faction ID appears in that mod's faction list
- Fall back to the hull-based heuristic only when file-based detection fails
- Display the source as "Mixed (Primary: X)" when ships come from multiple mods
- Accept "Unknown" as a valid state and display it clearly rather than guessing

**Detection:**
- Test with a modded faction that uses mostly vanilla ships (e.g., some Nexerelin configurations)
- Test with a faction whose ships all come from a single mod

**Phase relevance:** Phase 3 (faction tab implementation). Non-blocking but affects UI quality.

**Confidence:** MEDIUM -- based on code analysis of `FactionManagerDialog.getFactionModSource()` and understanding of Starsector's spec system. Have not verified all edge cases of `WithSourceMod` availability.

---

### Pitfall 7: Tab Column in CSV Creates Implicit "General" Tab

**What goes wrong:** In LunaLib's `LunaSettings.csv`, any row with an empty `tab` column is assigned to a "General" tab. The General tab is created automatically if ANY row has an empty tab value. If you intend all settings to be in named tabs but accidentally leave one row's tab column empty (including blank rows used as spacers), LunaLib creates an unwanted "General" tab containing that orphaned setting.

**Why it happens:** The current CSV has no tab values at all (the `tab` column exists but is empty for every row). When migrating to tabs, it's easy to miss a row -- especially the spacer rows (empty rows used for visual separation in the CSV) or the `Text`/`Header` type rows that don't have obvious field IDs.

**Consequences:**
- An orphaned "General" tab appears with one or two settings, looking broken
- Users see a confusing extra tab alongside the intended Replacement/Costs/Factions tabs
- If the preset Radio field ends up on "General" by accident, users can't find how to switch presets

**Prevention:**
- After writing the new CSV, validate that EVERY non-empty row has a tab value
- Remove or fill blank spacer rows (use `Header` type fields for visual separation instead of blank rows)
- Review the CSV in a spreadsheet editor where the tab column is visible for every row
- Test by opening the mod settings in-game and counting tabs -- should match exactly what you designed

**Detection:**
- Open settings in-game; if you see a "General" tab you didn't design, a row is missing its tab value
- Script/manual audit: check that column 9 (tab) is non-empty for every non-blank row in the CSV

**Phase relevance:** Phase 1 (CSV migration). Simple to prevent with a checklist.

**Confidence:** HIGH -- verified from LunaLib wiki: "Any Empty row will be assigned to the General Tab, which only gets created if there are empty rows."

---

### Pitfall 8: Percentage Sliders Can Sum Over 100% Without Validation

**What goes wrong:** Cruiser replacement has two probability sliders (`cruiserToFrigate` + `cruiserToDestroyer`) and capital has three (`capitalToFrigate` + `capitalToDestroyer` + `capitalToCruiser`). LunaLib provides no cross-field validation. A user in Custom mode can set all three capital sliders to 50%, totaling 150%. The code in `ShipReplacer.tryReplaceCapital()` handles this gracefully (the "stays" probability becomes negative, clamped to 0 by `Math.max(0, stays)`), meaning capitals are ALWAYS replaced and never kept, which may not be what the user intended.

**Why it happens:** Each slider is independent in LunaLib. There is no way to define cross-field constraints in the CSV. The existing `cruiserStays` and `capitalStays` display fields are "disabled" (read-only) and show the auto-calculated remainder, but they can show negative values or zero.

**Consequences:**
- User sees "Cruiser Stays: -30%" which is confusing
- Setting all sliders to 100% means every ship is replaced, which may crash if no valid replacements exist for every role
- No error message tells the user their percentages are invalid

**Prevention:**
- In `settingsChanged()`, validate percentage sums and display a warning message if they exceed 100%
- Update the auto-calculated "Stays" display fields to show `Math.max(0, 100 - sum)` (already done in Settings.java but not in UI display)
- Add `Text` type rows in the CSV below the sliders explaining "Total must not exceed 100%"
- Consider clamping: if sum > 100, proportionally reduce all values. But this changes user input which may be worse than a warning.
- In the new tabbed UI, the preset tabs show fixed valid values, so this only affects Custom mode

**Detection:**
- Set all replacement sliders to maximum in Custom mode and verify behavior
- Check that the "Stays %" display field shows 0, not a negative number

**Phase relevance:** Phase 2 (custom settings tab). Validation logic in listener.

**Confidence:** HIGH -- verified from source code that `getCruiserStays()` uses `Math.max(0, stays)` but the LunaSettings JSON display field has no such clamping.

---

### Pitfall 9: FactionManagerDialog Saves Preset to "Custom" on Every Blacklist Change

**What goes wrong:** `FactionManagerDialog.saveBlacklist()` (line 234) always sets `preset` to `"Custom"` when saving the blacklist. If the user is on "Sirix Recommended" and toggles one faction in the in-game dialog, their preset silently switches to Custom. The next time they open LunaLib settings, they see "Custom" selected but all the values are still at Recommended levels (because only the blacklist and preset fields were written). This creates a state where the preset says "Custom" but the values are "Recommended."

**Why it happens:** The in-game dialog was designed assuming that manually editing the blacklist means the user wants custom control. But with the new tabbed UI, there should be a way to have per-faction blacklist overrides WITHOUT switching the entire preset.

**Consequences:**
- User's preset silently changes from a named preset to Custom
- If Settings.java uses preset name to determine values (which it does for non-Custom presets), switching to Custom causes it to read the slider values instead of hardcoded values. The slider values may not match the named preset values if they were never explicitly set.
- Behavior diverges from what the user expects

**Prevention:**
- Separate blacklist from preset entirely. The blacklist should be its own independent setting that applies regardless of preset. The current code in `Settings.reloadBlacklist()` already merges user blacklist with preset defaults.
- In the new architecture, the faction tab should modify only the `factionBlacklist` field without touching `preset`
- If the in-game FactionManagerDialog is kept, remove the `data.put("preset", "Custom")` line
- Add a new field like `factionBlacklistCustom` for user overrides that are additive to preset defaults

**Detection:**
- Test: select Sirix Recommended, open in-game faction manager, toggle one faction, save. Open LunaLib settings and check which preset is selected.

**Phase relevance:** Phase 3 (faction tab). Must coordinate with in-game dialog behavior.

**Confidence:** HIGH -- verified directly from FactionManagerDialog.saveBlacklist() source code line 234.

## Minor Pitfalls

### Pitfall 10: Text/Header Fields Count Toward UI Density

**What goes wrong:** The current CSV has many `Text` type rows used for inline help (e.g., `cruiserToFrigatePresets`, `presetNote`, `factionBlacklistHelp`, etc.). When reorganized into tabs, these text blocks take up vertical space on each tab. If a tab has many settings plus help text, it may require scrolling or feel cluttered.

**Why it happens:** In a flat list, inline help text was necessary because settings lacked context. In a tabbed UI, tabs provide context through grouping, reducing the need for inline explanations.

**Prevention:**
- Audit all `Text` type rows and evaluate which are still needed in a tabbed layout
- Move explanatory text into `tooltip` fields (the `fieldDescription` column) instead of standalone `Text` rows
- Use `Header` type for section breaks within a tab, not `Text`
- Preset tabs that show read-only values need minimal help text since they're not editable

**Phase relevance:** Phase 1 (CSV design). Cosmetic but affects perceived UI quality.

**Confidence:** HIGH -- based on existing CSV structure and LunaLib documentation.

---

### Pitfall 11: LunaLib Listener Registration Must Survive Application Reload

**What goes wrong:** `PresetListener` is registered in `onApplicationLoad()` with a guard `if (!LunaSettings.hasSettingsListenerOfClass(PresetListener.class))`. If the mod is reloaded via devmode or a hot-reload scenario, the old listener instance may still be registered. LunaLib's `hasSettingsListenerOfClass` check prevents double-registration, but the old instance may hold stale references (e.g., `lastPreset` from a previous session).

**Why it happens:** Starsector's devmode allows reloading mods without full restart. The listener persists for the "game's entire runtime" per LunaLib docs. If the mod class is reloaded but LunaLib keeps the old listener, behavior is undefined.

**Prevention:**
- Add `LunaSettings.removeSettingsListenerOfClass(PresetListener.class)` before registering a new one (if such a method exists -- verify in LunaLib API)
- Make the listener stateless where possible -- read `lastPreset` from the settings JSON rather than storing it in an instance variable
- Test with Starsector's devmode F8 reload

**Phase relevance:** Phase 2 (listener rewrite). Low risk in production, higher risk during development.

**Confidence:** MEDIUM -- LunaLib listener lifecycle not fully documented; based on wiki statement that "the listener persists for the game's entire runtime."

---

### Pitfall 12: Double vs Float Type Mismatch in Preset Value Maps

**What goes wrong:** `PresetListener.java` stores preset values as `Double` in `HashMap<String, Object>` (e.g., `RECOMMENDED_VALUES.put("cruiserCrewMult", 1.5)`). Java autoboxes this as `Double`. But `LunaSettings.getDouble()` returns `Double`, and the CSV declares these as `Double` type. When writing values back, if the JSON serializer expects a specific numeric type and gets the wrong one, values may be truncated or fail to write.

**Why it happens:** Java's autoboxing of numeric literals: `1.5` becomes `Double`, `30` becomes `Integer`. LazyLib's `JSONObject.put()` accepts `Object`. The mismatch is usually harmless but can cause issues with strict JSON parsers.

**Prevention:**
- Explicitly type all values in the preset maps: use `1.5d` for doubles and `30` for ints
- Verify that `data.put(key, value)` correctly serializes both `Integer` and `Double` types
- Add type checking in the preset value population: assert that int fields get Integer values and double fields get Double values

**Phase relevance:** Phase 2 (preset listener rewrite). Easy to prevent.

**Confidence:** MEDIUM -- Java autoboxing behavior is well-known, but JSON serialization behavior depends on LazyLib's `CommonDataJSONObject` implementation which was not inspected.

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Phase 1: CSV Migration | Field ID rename destroys settings (#1) | Keep all existing IDs, use tab column only |
| Phase 1: CSV Migration | Orphan "General" tab (#7) | Validate every row has tab value |
| Phase 1: CSV Migration | UI clutter from help text (#10) | Audit Text rows, move to tooltips |
| Phase 2: Preset System | Radio-vs-checkbox architecture (#2) | Decide Radio vs Boolean approach first |
| Phase 2: Preset System | Custom values lost on switch (#4) | Design backup persistence before coding |
| Phase 2: Preset System | Internal API breakage (#3) | Isolate LunaLib internals in adapter class |
| Phase 2: Preset System | Percentage over-100% (#8) | Add validation in settingsChanged() |
| Phase 3: Faction Tab | Source detection unreliable (#6) | Add file-based detection fallback |
| Phase 3: Faction Tab | Blacklist forces Custom preset (#9) | Decouple blacklist from preset selection |
| Phase 3: UI Polish | Save/reload desync confusion (#5) | Label fields, throttle warnings |

## Sources

- [LunaLib Wiki: LunaSettings.CSV](https://github.com/Lukas22041/LunaLib/wiki/LunaSettings.CSV) -- field types, tab column behavior, persistence model
- [LunaLib Wiki: LunaSettingsListener](https://github.com/Lukas22041/LunaLib/wiki/LunaSettingsListener) -- callback timing, registration best practices
- [LunaLib Wiki: Integrating LunaSettings](https://github.com/Lukas22041/LunaLib/wiki/Integrating-LunaSettings) -- data storage location (`saves/common/LunaSettings/`), soft dependency pattern
- [LunaLib Changelog](https://github.com/Lukas22041/LunaLib/blob/main/changelog.txt) -- v1.4.0 tabs/radio added, v1.5.6 CSV loading change, v2.0.2 crash fix
- [LunaLib Forum Thread](https://fractalsoftworks.com/forum/index.php?topic=25658.0) -- community discussion of settings patterns
- Source code analysis: `Settings.java`, `PresetListener.java`, `FactionManagerDialog.java`, `LunaSettings.csv`, `LunaSettings.json` in the Smaller Sector codebase

---

*Pitfalls audit: 2026-02-21*
