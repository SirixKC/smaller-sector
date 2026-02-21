# Project Research Summary

**Project:** Smaller Sector — LunaLib Tabbed Settings UI Revamp
**Domain:** Starsector mod settings UI (LunaLib CSV-driven interface)
**Researched:** 2026-02-21
**Confidence:** HIGH

## Executive Summary

This project is a settings UI overhaul for an existing Starsector mod. The goal is to restructure a flat LunaLib settings panel into a tabbed interface with preset preview tabs, editable custom settings, and faction management discoverability. Research confirms LunaLib's native tab system — driven entirely by a `tab` column in `LunaSettings.csv` — covers roughly 70% of the required UI with zero Java code changes. The remaining 30% (read-only preset value display and per-faction toggles) requires creative use of `Text` field types and an intentional decision to keep faction management in the existing in-game `FactionManagerIntel`/`FactionManagerDialog` system, which is already capable of what LunaLib cannot do.

The recommended approach is a hybrid: LunaLib CSV tabs for presets, replacement settings, costs, and a faction navigation entry point; the existing in-game intel dialog for dynamic faction management. The highest-value change is tab organization, which is a pure CSV edit with no Java impact. The most significant architectural decision is whether preset tabs use a single `Radio` dropdown (native, simple, proven) or per-tab `Boolean` checkboxes with listener-enforced radio behavior (fragile, complex, but visually richer). Research recommends keeping the `Radio` approach unless there is a strong UX reason to deviate.

The top risks are field ID preservation during migration (renaming IDs destroys existing user settings), LunaLib internal API fragility in `PresetListener` (backend classes are not public API), and a subtle preset-switch bug where custom values are silently overwritten when switching to a named preset. All three have documented mitigations. Save compatibility is largely free if field IDs are preserved, making this a lower-risk refactor than it might appear.

## Key Findings

### Recommended Stack

LunaLib's CSV-based settings system is the right tool for this task and is already in use. The `tab` column (ninth column in `LunaSettings.csv`) is the sole mechanism needed for tab creation — no Java API calls required. LunaLib natively supports `Text`, `Header`, `Int`, `Double`, `Boolean`, and `Radio` field types, all of which are needed. The existing `LunaSettingsListener` pattern (already implemented as `PresetListener`) handles reactive updates when settings change.

The existing `FactionManagerDialog` (using Starsector's `InteractionDialogPlugin` + `CustomPanelAPI`) is the correct tool for dynamic faction management and must not be replaced. It is the only way to enumerate loaded factions at runtime. The mod's `MagicLib`, `LazyLib`, and `LunaLib` dependencies are all appropriate and should not change.

**Core technologies:**
- LunaLib CSV tabs: `tab` column in `LunaSettings.csv` — zero-code tab organization
- LunaLib `Text`/`Header` field types: read-only preset value display — only supported approach
- LunaLib `LunaSettingsListener`: reactive preset switching — existing `PresetListener` pattern
- Starsector `InteractionDialogPlugin`: dynamic faction toggles — already implemented as `FactionManagerDialog`
- LazyLib `JSONUtils`: custom values persistence backup — prevents data loss on preset switch

### Expected Features

The table stakes for a quality Starsector mod settings UI are already partially met. The major missing piece is tab organization — the current flat list of 20+ settings is the primary usability problem. Every other must-have feature already works in code; the UI just needs structural reorganization.

**Must have (table stakes):**
- Tab-based organization — current flat list is the top UX complaint in modded Starsector
- Preset selection via `Radio` field — already implemented, preserve it
- Preset value visibility — show values for each preset so users can compare before committing
- Custom override capability — already works; `Settings.java` reads sliders when preset is "Custom"
- Faction blacklist — already implemented; surface it better in the new tab layout
- Save/reload warning for operating costs — already implemented; preserve and improve

**Should have (differentiators):**
- Per-preset read-only tabs with `Text` field value display — cleaner than inline annotations
- Custom settings mod icon via `LunaSettingsConfig.json` — quick visual polish win
- Auto-calculated "stays" percentage display — low effort, improves Custom tab comprehension
- Percentage sum validation warning (> 100%) — prevents user confusion in Custom mode
- Grouped cost sliders by ship class with `Header` separators — already partially done

**Defer (v2+):**
- Per-tab `Boolean` checkboxes with cross-tab radio enforcement — high complexity, fragile against LunaLib updates
- Live-updating "stays" fields that mirror preset values in real time — requires LunaLib internal API access
- Dynamic faction rows in LunaSettings.csv — architecturally impossible; LunaLib CSV is static

### Architecture Approach

The architecture is a refactor of existing components, not a rebuild. `Settings.java` remains the single read point for all consumers. `PresetListener.java` handles reactive updates. The new `PresetValues.java` class extracts preset constant maps out of the listener (separation of concerns). The CSV restructure is the largest surface area change but has no Java runtime impact.

**Major components:**
1. `data/config/LunaSettings.csv` — complete tab column assignment; add `Text`/`Header` rows for preset display
2. `PresetValues.java` (new) — extracted preset constants; single source of truth for all preset values
3. `Settings.java` (refactored) — `getActivePreset()` method reads from `Radio` field (or Boolean fields); all getters switch on active preset
4. `PresetListener.java` (refactored) — isolated into `LunaLibInternals` adapter; focused on change detection and downstream triggers
5. `FactionManagerDialog.java` / `FactionManagerIntel.java` — unchanged; referenced from a "Factions" info tab in LunaLib
6. Delete `lunalib/LunaSettings.json` — duplicate settings definition causing confusion; CSV is canonical since LunaLib v1.5.6

### Critical Pitfalls

1. **Field ID rename destroys saved user settings** — LunaLib persists to `saves/common/LunaSettings/smallersector.json.data` keyed by `fieldID`. Renaming any existing ID silently reverts that setting to CSV default for all users. Fix: never change existing field IDs; use the `tab` column to reorganize without renaming.

2. **Custom values overwritten when switching presets** — `PresetListener` currently writes preset values directly to the LunaLib JSON, destroying whatever the user had in Custom mode. Fix: before overwriting, back up custom values to a separate file (e.g., `LunaSettings/smallersector_custom_backup.json` via LazyLib) and restore them when switching back to Custom.

3. **LunaLib internal API fragility** — `PresetListener` uses `lunalib.backend.ui.settings.*` classes that are not part of the public API. A LunaLib update could break the import at startup, crashing the game. Fix: isolate all internal calls into a `LunaLibInternals` adapter class with try-catch wrappers; evaluate whether the new tab design reduces the need for programmatic slider syncing.

4. **FactionManagerDialog silently switches preset to "Custom"** — `saveBlacklist()` unconditionally sets `preset` to `"Custom"` when saving the blacklist. Users on a named preset who toggle one faction end up with an inconsistent state. Fix: decouple blacklist from preset; remove the `data.put("preset", "Custom")` call from `saveBlacklist()`.

5. **Orphaned "General" tab from empty CSV rows** — Any row with an empty `tab` column creates an automatic "General" tab. Easy to miss during migration. Fix: audit every non-blank CSV row to confirm it has a tab value; remove blank spacer rows.

## Implications for Roadmap

Based on research, the work has a natural 4-phase dependency chain. Each phase validates independently before the next begins.

### Phase 1: PresetValues Extraction and Settings Refactor

**Rationale:** Everything else depends on `Settings.java` knowing the new preset mechanism. This is a pure Java refactor with no visible UI change — it validates the core value resolution logic before touching the CSV. It is also the phase where the critical architecture decision (Radio vs. per-tab Boolean) must be made.

**Delivers:** `PresetValues.java` with extracted preset constants; `Settings.java` refactored with `getActivePreset()` method; confirmed the value resolution works correctly before any UI changes.

**Addresses:** Preset value visibility (groundwork); custom override capability (preserved).

**Avoids:** Pitfall 3 (internal API fragility) — this phase should introduce `LunaLibInternals` adapter class.

**Research flag:** No additional research needed. This is a code refactor of known logic.

### Phase 2: CSV Tab Structure Migration

**Rationale:** Tab organization is the highest-impact, lowest-effort change. It is a CSV-only edit (zero Java code) but it depends on Phase 1 having finalized the new field IDs. This phase also resolves the duplicate `LunaSettings.json` anti-pattern.

**Delivers:** Fully tabbed LunaLib settings UI with Vanilla/Recommended/Hardcore preset tabs (read-only `Text` fields), Custom tab (editable sliders), and Factions tab (info text directing to in-game dialog). Delete `lunalib/LunaSettings.json`.

**Addresses:** Tab-based organization (primary deliverable); preset value visibility; faction blacklist discoverability; custom settings mod icon.

**Avoids:** Pitfall 1 (field ID rename) — critical checklist item: diff old/new CSV and confirm zero changes to the `fieldID` column. Pitfall 7 (orphaned General tab) — validate every row has a tab value before testing.

**Research flag:** No additional research needed. Tab column behavior is well-documented and verified.

### Phase 3: PresetListener Rewrite and Preset Switching Logic

**Rationale:** Needs both the refactored `Settings.java` (Phase 1) and the new CSV field IDs (Phase 2) to exist. This is the highest-complexity phase due to LunaLib internal API usage and the custom values persistence problem.

**Delivers:** `PresetListener` rewritten with: isolated `LunaLibInternals` adapter, custom value backup/restore via LazyLib, percentage sum validation, decoupled blacklist from preset. Downstream effects (BaseValueModifier, blacklist reload) continue to fire correctly.

**Addresses:** Settings persistence across preset switching; percentage validation; save/reload warning throttling.

**Avoids:** Pitfall 2 (custom values lost on switch) — design backup persistence before coding; Pitfall 4 (blacklist forces Custom preset) — remove `data.put("preset", "Custom")` from `FactionManagerDialog`; Pitfall 8 (percentage over-100%) — add validation in `settingsChanged()`.

**Research flag:** May need deeper research into LazyLib's `JSONUtils.loadCommonJSON()` persistence behavior for the custom value backup mechanism. The `settingsChanged()` ordering behavior when multiple fields change in one save needs empirical testing.

### Phase 4: Cleanup and Polish

**Rationale:** Final validation and polish pass after all functional work is complete.

**Delivers:** Remove old `preset` Radio field handling from `Settings.java`; remove old flat-list `Text` annotation rows from CSV; verify `FactionManagerDialog` still works; test save/load compatibility; add `LunaSettingsConfig.json` icon; audit UI density on each tab.

**Addresses:** Custom settings mod icon; grouped cost slider improvements; text clutter reduction.

**Avoids:** Pitfall 10 (text/header UI density) — audit inline help text and move to tooltips.

**Research flag:** No additional research needed. Standard cleanup and validation.

### Phase Ordering Rationale

- Phase 1 before Phase 2: The CSV cannot be written correctly until `Settings.java` has been refactored to use the new preset mechanism. Field ID decisions made in Phase 1 define what can appear in Phase 2's CSV.
- Phase 2 before Phase 3: `PresetListener` cannot be rewritten until the new field IDs exist in the CSV. The listener must reference `enableVanilla`, `enableRecommended`, etc. by name.
- Phase 3 before Phase 4: Cleanup removes the old `preset` Radio field and old inline text rows; those removals are safe only after the new listener is confirmed working.
- Faction management stays in `FactionManagerDialog` throughout: the research is conclusive that LunaLib CSV cannot support dynamic faction lists. No phase should attempt to move faction toggles into LunaLib settings.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 3:** LazyLib `JSONUtils` API for the custom value backup mechanism — the exact method signatures and file path behavior for `saves/common/` storage need verification. Also: empirical testing of `settingsChanged()` callback timing when multiple Boolean fields change in a single save action (relevant to the Radio vs. Boolean decision).

Phases with standard patterns (skip research-phase):
- **Phase 1:** Pure Java refactor of existing patterns — `Settings.java` and `PresetValues` extraction are standard OOP, no new APIs.
- **Phase 2:** Tab column is documented and verified — this is a CSV editing task, not a research question.
- **Phase 4:** Cleanup and polish — no new technology, all known patterns.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | LunaLib docs are authoritative and well-maintained; existing codebase confirms integration patterns work |
| Features | HIGH | Feature set is clearly bounded by LunaLib's documented capabilities; constraints are well-understood |
| Architecture | HIGH | Component boundaries are clear; the one MEDIUM area (cross-tab Boolean radio behavior) is explicitly flagged as the alternative to avoid |
| Pitfalls | HIGH | All critical pitfalls verified from source code analysis and LunaLib wiki; not inferred |

**Overall confidence:** HIGH

### Gaps to Address

- **LazyLib JSONUtils persistence API for custom value backup:** The recommended approach for Pitfall 4 (custom values lost on preset switch) uses `LazyLib.loadCommonJSON()` to persist custom values outside LunaLib's JSON. The exact API signature and behavior for writing to `saves/common/` has not been verified from documentation — needs confirmation before Phase 3 coding begins.

- **`settingsChanged()` callback semantics:** LunaLib fires `settingsChanged()` once per save action, not once per changed field. If a user changes multiple fields in one save, the listener receives a single callback and must diff against prior state to determine what changed. The exact interaction when multiple Boolean preset fields change simultaneously (e.g., user saves after manually editing the JSON) needs empirical testing. Low risk for the `Radio` approach; higher risk if per-tab Boolean checkboxes are used.

- **`LunaSettings.json` vs CSV coexistence behavior:** The mod currently has both `lunalib/LunaSettings.json` and `data/config/LunaSettings.csv`. The architecture recommendation is to delete the JSON file (it is a legacy format superseded in LunaLib v1.5.6). Before deleting, verify empirically that removing it does not affect existing saved settings — the JSON file may have been the primary source for some saved values.

- **Architecture decision: Radio vs. per-tab Boolean:** The research recommends `Radio` (simpler, native, proven) over per-tab `Boolean` checkboxes (fragile, complex). This decision should be finalized before Phase 1 coding begins. The architecture docs describe both approaches; the `Radio` approach is documented as the recommended choice.

## Sources

### Primary (HIGH confidence)
- [LunaLib Wiki — LunaSettings.CSV](https://github.com/Lukas22041/LunaLib/wiki/LunaSettings.CSV) — tab column spec, all field types, bracket highlighting, persistence model
- [LunaLib Wiki — LunaSettingsListener](https://github.com/Lukas22041/LunaLib/wiki/LunaSettingsListener) — callback registration, timing, best practices
- [LunaLib Wiki — Integrating LunaSettings](https://github.com/Lukas22041/LunaLib/wiki/Integrating-LunaSettings) — setup patterns, `saves/common/LunaSettings/` storage location
- [LunaLib Wiki — LunaSettingsConfig.json](https://github.com/Lukas22041/LunaLib/wiki/LunaSettingsConfig.json) — custom icon support
- [LunaLib GitHub — Changelog](https://github.com/Lukas22041/LunaLib/blob/main/changelog.txt) — tab support since v1.4.0; CSV canonical since v1.5.6
- Existing codebase: `Settings.java`, `PresetListener.java`, `FactionManagerDialog.java`, `LunaSettings.csv`, `LunaSettings.json` — authoritative source for current implementation behavior

### Secondary (MEDIUM confidence)
- [LunaLib Forum Thread](https://fractalsoftworks.com/forum/index.php?topic=25658.0) — community usage patterns, VayraMerged as multi-tab reference example
- [Starsector Custom UI Guide](https://fractalsoftworks.com/forum/index.php?topic=28425.0) — `InteractionDialogPlugin` and `CustomPanelAPI` patterns for `FactionManagerDialog`
- [Custom UI Panels and Handling Input](https://fractalsoftworks.com/forum/index.php?topic=25284.0) — `TooltipMakerAPI` and `CustomPanelAPI` layout patterns

### Tertiary (LOW confidence)
- LunaLib backend package source code (`lunalib.backend.ui.settings.*`) — used to understand `PresetListener` internals; not part of public API, subject to change without notice

---
*Research completed: 2026-02-21*
*Ready for roadmap: yes*
