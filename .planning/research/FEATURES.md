# Feature Landscape

**Domain:** Starsector Mod Settings UI (LunaLib Tabbed Interface)
**Researched:** 2026-02-21
**Overall Confidence:** MEDIUM-HIGH

## Table Stakes

Features users expect from a well-organized mod settings UI. Missing any of these makes the mod feel unfinished or confusing.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **Tab-based organization** | Users with 20+ mods each contributing settings need scannability. LunaLib's `tab` CSV column is the standard mechanism. Mods like VayraMerged already demonstrate multi-tab layouts (General, Popular Front, Colonial Factions, D-Mods). A flat wall of sliders is the #1 thing that makes settings feel amateurish. | Low | LunaLib natively supports this via the `tab` column in `LunaSettings.csv`. Just assign tab names to rows. Empty tab column = "General" tab. |
| **Preset selection** | Already exists as a Radio field. Users expect a quick way to pick a difficulty profile without touching individual sliders. Every "balance overhaul" mod in Starsector that offers configurability has some form of preset/difficulty tier. | Low | Already implemented via Radio field type. The existing `preset` Radio with `Custom,Vanilla,Sirix Recommended,Sirix Hardcore` is the correct pattern. |
| **Preset value visibility** | When a preset is selected, users need to SEE what values it applies. Currently the mod uses Text fields showing `[Recommended: 30% | Hardcore: 50%]` inline next to each slider. This works but is noisy. Users expect to preview what a preset does BEFORE committing. | Medium | Current approach clutters the UI with "Presets" Text rows between every slider. A better approach is showing preset values in the field descriptions or as tab-level summaries. |
| **Custom override capability** | Users expect to start from a preset and tweak individual values. The current system already supports "Custom" preset where sliders become authoritative. This is table stakes for any mod with presets. | Low | Already works: when preset is "Custom", `Settings.java` reads from LunaLib slider values. No new work needed on the logic side. |
| **Faction blacklist** | Users expect to exempt specific factions from mod effects. This is core to the mod's value proposition -- Remnant, Omega, and mod-added factions often have unique ship rosters that shouldn't be diluted. | Medium | Currently a comma-separated string field in LunaSettings + an in-game FactionManagerDialog via Intel. The string field is the weakest UX in the current settings. |
| **Setting descriptions/tooltips** | Every slider and field needs a tooltip explaining what it does and what reasonable values look like. LunaLib's `fieldDescription` column supports `[highlighted]` bracket syntax for emphasis. | Low | Already present. All fields have tooltip text. Ensure new tabbed layout preserves these. |
| **Settings persistence across preset switching** | When users switch from Custom to a preset and back, their custom values must be preserved. Losing custom values is a trust-breaking UX failure. | Medium | Currently handled by `PresetListener.java` which writes preset values to LunaLib's JSON. Custom values persist because they're only overwritten when a non-Custom preset is selected. This must be preserved in the new design. |
| **Save/reload warning for operating costs** | Operating cost changes via the hull mod require save+reload. Users MUST be told this or they'll think the mod is broken. | Low | Already implemented: `PresetListener.settingsChanged()` posts a campaign UI message. Keep this. |

## Differentiators

Features that set the settings UI apart from typical Starsector mods. Not expected by users, but create a notably better experience.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Per-preset read-only tabs** | Instead of showing preset values as inline text annotations, dedicate a tab to each preset showing all values as disabled/read-only fields. Users can click between tabs to compare presets visually. This is a step above what any Starsector mod currently does for preset comparison. | Medium | LunaLib supports disabled fields (the existing `cruiserStays` and `capitalStays` fields already use `"disabled": true` in the JSON). Create tabs like "Vanilla Preview", "Recommended Preview", "Hardcore Preview" with all values as disabled sliders at their preset positions. Problem: LunaLib fields are defined statically in CSV -- you can't dynamically show/hide them based on which preset is active. All tabs exist simultaneously. |
| **Enable-preset checkbox per tab** | Each preset tab gets a Boolean checkbox at the top: "Enable this preset". Checking one should ideally uncheck others (radio behavior across tabs). This replaces the Radio field with a more spatial, visual preset selector. | High | LunaLib does NOT natively support radio behavior across tabs. A Boolean on each tab is independent. The `PresetListener.settingsChanged()` callback would need to detect which checkbox changed, uncheck the others, and sync. This requires careful listener logic and direct UI element manipulation (already done for slider updates). |
| **Per-faction Boolean toggles** | Replace the comma-separated string blacklist with individual Boolean checkboxes per faction. Each faction gets its own row with a checkbox and its display name. Far more intuitive than typing faction IDs. | High | Problem: The faction list is dynamic and depends on which mods are loaded. LunaSettings.csv is static -- defined at mod packaging time. You CANNOT dynamically generate CSV rows per loaded faction. This feature is only achievable via the in-game FactionManagerDialog, NOT via LunaLib settings. The existing dialog already does this. |
| **Faction source mod identification** | The faction tab/dialog shows which mod each faction comes from (e.g., "Hegemony - Vanilla", "Nightmare - HMI"). Helps users understand what they're blacklisting. | Low | Already implemented in `FactionManagerDialog.getFactionModSource()` using `WithSourceMod` interface and `ModSpecAPI`. Keep and possibly improve (show in LunaLib text if doing a faction section). |
| **Ship count per faction** | Show how many ships of each size class a faction has. Users can make informed blacklist decisions -- a faction with zero cruisers/capitals doesn't need blacklisting. | Low | Already implemented in `FactionManagerDialog.countShipsBySize()`. Display format: `F: 12 | D: 8 | Cr: 4 | Cap: 2`. |
| **Auto-calculated "stays" display** | Show the computed remaining percentage (100% minus replacement percentages) as a live, disabled field. Users immediately see the probability of a ship keeping its size class. | Low | Already implemented as disabled fields `cruiserStays` and `capitalStays` in `LunaSettings.json`. Currently they show static default values. To make them truly live-updating, the PresetListener would need to recalculate and push new values when replacement percentages change. |
| **Percentage validation (sum <= 100%)** | Prevent users from setting replacement percentages that sum to more than 100%. Either cap values, show a warning, or auto-adjust the "stays" percentage to indicate over-allocation. | Medium | Not currently implemented. The existing `cruiserStays` and `capitalStays` fields show defaults, not live calculations. LunaLib doesn't support cross-field validation natively. Best approach: calculate in the listener callback and post a warning message if sum > 100%. |
| **Custom settings mod icon** | Use `LunaSettingsConfig.json` to set a custom 40x40 icon for the mod in LunaLib's settings menu. Makes Smaller Sector visually distinct in a list of 20+ mods. | Low | LunaLib supports this via `data/config/LunaSettingsConfig.json`. Icon must be preloaded via `settings.json` graphics entries. A ship silhouette or size-comparison icon would be thematic. |
| **Grouped cost sliders by ship class** | Instead of listing all 6 cost multipliers flat, group them: "Cruiser Costs" section with crew/supply/fuel, then "Capital Costs" section. Headers already exist but can be made more visually distinct with tab separation. | Low | Already partially done with Header fields. Moving cruiser costs and capital costs into sub-sections within the same tab (or separate tabs) improves scannability. |

## Anti-Features

Features to explicitly NOT build. These seem appealing but create maintenance burden, break LunaLib conventions, or harm UX.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **Dynamic faction rows in LunaSettings.csv** | LunaSettings.csv is static, defined at mod build time. You cannot generate rows dynamically based on which mods the user has installed. Attempting to generate/rewrite the CSV at runtime would be fragile, break LunaLib's loading assumptions, and conflict with how LunaLib caches settings. | Keep the in-game FactionManagerDialog as the per-faction toggle UI. Use a LunaLib Text field on the Factions tab to say "Open the in-game Faction Manager (Intel > Missions) for per-faction toggles." The CSV faction blacklist string is a fallback for advanced users who know faction IDs. |
| **Custom UI panels in LunaLib settings** | LunaLib's settings UI is declarative (CSV-driven). Injecting custom Swing/LWJGL panels into it would break on LunaLib updates, is unsupported by the API, and goes against the grain of the framework. | Use LunaLib's native field types (Boolean, Radio, Int, Double, Text, Header) which are stable and well-tested. For complex UI needs (faction toggles with search), use the separate InteractionDialogPlugin system which is designed for custom interfaces. |
| **Live slider sync across tabs** | Making a slider on the "Custom" tab update a disabled slider on a "Preview" tab in real-time requires deep manipulation of LunaLib's UI element list. It's fragile, depends on LunaLib internals (`LunaSettingsUISettingsPanel.Companion.getAddedElements()`), and breaks when LunaLib updates its UI rendering. | Show preset values as static Text annotations or field descriptions. Accept that preview tabs show preset values and the Custom tab shows editable values -- they don't need to mirror each other in real-time. |
| **Removing the in-game FactionManagerDialog** | The in-game dialog serves a different context -- users configure factions while playing, seeing their fleet and faction relationships. The LunaLib settings serve a pre-game/configuration context. Both access points are valuable. PROJECT.md explicitly marks this as out of scope. | Keep both. The LunaLib Factions tab provides a text prompt directing users to the in-game manager. The in-game dialog remains the primary faction configuration tool. |
| **Auto-unchecking preset checkboxes across tabs** | Implementing radio-button behavior across LunaLib tabs (check "Enable Recommended" auto-unchecks "Enable Hardcore") requires intercepting settingsChanged, iterating all UI elements, and pushing checkbox state changes. The current PresetListener already does something similar with sliders and it's 100+ lines of fragile code accessing LunaLib internals. Replicating this for cross-tab checkboxes adds more fragility. | Use a single Radio field on a "General" or "Preset" tab. Radio fields natively enforce single-selection within LunaLib. The Radio field is the correct tool for "pick one of N" -- that's literally what it was designed for. Put the Radio on a dedicated tab and show preview values below it. |
| **Per-setting override granularity** | Allowing users to override individual settings within a preset (e.g., "Use Recommended but change capital crew to 3.0x") creates a hybrid state that's confusing to display and hard to reason about. Is the user on "Recommended" or "Custom"? What resets when they switch presets? | Binary choice: either a preset is active (all its values apply) or Custom is active (all values are user-controlled). When users want to tweak one value from a preset, they select Custom and manually set all values. The "Custom values persist" requirement helps -- they can start from preset values and adjust. |
| **Undo/reset per field** | LunaLib has a global "Reset Settings" button that restores CSV defaults. Adding per-field reset (right-click to restore default) would require custom UI injection. | Rely on LunaLib's built-in "Reset Settings" button plus presets. Users who want to start over can switch to a preset, then switch to Custom. |

## Feature Dependencies

```
Tab Organization (csv tab column)
  |
  +-> Preset Tab with Radio selector
  |     |
  |     +-> Preset value preview (Text fields showing values for selected preset)
  |     |
  |     +-> PresetListener sync (updates slider values when preset changes)
  |           |
  |           +-> Custom values persistence (Custom values not overwritten by preset switch)
  |
  +-> Replacement Settings Tab
  |     |
  |     +-> Cruiser replacement sliders
  |     |     |
  |     |     +-> Auto-calculated "stays" percentage
  |     |
  |     +-> Capital replacement sliders
  |           |
  |           +-> Auto-calculated "stays" percentage
  |           |
  |           +-> Percentage validation (sum <= 100% warning)
  |
  +-> Cost Settings Tab
  |     |
  |     +-> Operating cost multiplier sliders (crew, supply, fuel)
  |     |     |
  |     |     +-> Save/reload warning message
  |     |
  |     +-> Build cost multiplier sliders
  |     |
  |     +-> D-mod count sliders
  |
  +-> Factions Tab
        |
        +-> Text prompt directing to in-game Faction Manager
        |
        +-> Comma-separated blacklist string (advanced fallback)
        |
        +-> Default blacklist info text (shows Sirix preset defaults)

In-Game FactionManagerDialog (separate from LunaLib tabs)
  |
  +-> Per-faction Boolean toggles (dynamic, based on loaded mods)
  |
  +-> Faction source mod identification
  |
  +-> Ship count per faction display
  |
  +-> Pagination for large faction lists
```

## MVP Recommendation

Prioritize:

1. **Tab organization** -- Restructure the existing LunaSettings.csv into logical tabs. This is the single highest-impact change with the lowest effort. Use tabs: "Presets", "Replacement", "Costs", "Factions".

2. **Clean preset presentation** -- On the "Presets" tab, keep the Radio selector at top, followed by a summary showing what the selected preset does. Remove the inline `[Recommended: 30% | Hardcore: 50%]` Text rows from other tabs -- they clutter every settings page. Move that information into the Presets tab as a consolidated reference.

3. **Tab-separated settings groups** -- Put replacement percentages (cruiser + capital) on the "Replacement" tab, all cost multipliers + D-mods on the "Costs" tab, and faction blacklist on the "Factions" tab. Each tab becomes a focused concern.

4. **Faction tab with guidance** -- On the "Factions" tab, include Text fields explaining how to use the in-game Faction Manager, list the default AI faction blacklist for reference, and keep the comma-separated string as an advanced input.

5. **Custom settings icon** -- Add `LunaSettingsConfig.json` with a custom icon. Quick win for visual polish.

Defer:

- **Per-preset read-only tabs**: High effort, moderate value. The Radio + summary text approach is sufficient for preset comparison. If users really want to compare, they can click between Radio options. Read-only preview tabs are a nice-to-have for a future iteration.

- **Cross-tab enable checkbox radio behavior**: HIGH complexity and fragility for marginal UX gain over a native Radio field. Do not build this -- use the Radio field which is purpose-built for single-selection.

- **Live-updating "stays" fields**: Medium effort, low urgency. The stays percentage is trivially computed by the user (100 minus the two/three sliders). If implemented, do it in the PresetListener callback as a quality-of-life polish after the core tab restructure ships.

- **Percentage sum validation**: Nice-to-have. The current code uses `Math.max(0, stays)` which gracefully handles over-allocation. A warning is better than blocking, but not required for launch.

## Implementation Notes

### LunaLib Tab Column Mechanics

The `tab` column in `LunaSettings.csv` is the ninth column. Assigning the same string to multiple rows groups them under that tab. Empty tab values go to a "General" tab (created only if empty tab values exist). This is the entirety of the tab system -- there is no programmatic tab creation API.

**Current CSV has no tab assignments.** Every field currently falls into the implicit default. Adding tab names is a CSV-only change with zero Java code impact.

### Critical Constraint: Static vs Dynamic Content

LunaSettings.csv is read once at mod load. It cannot be dynamically modified based on game state (loaded mods, factions, etc.). This is the fundamental reason per-faction toggles CANNOT live in LunaLib settings. The FactionManagerDialog (InteractionDialogPlugin) exists precisely because it can query `Global.getSector().getAllFactions()` at runtime.

### Existing Code Touchpoints

The tab restructure primarily affects:
- `data/config/LunaSettings.csv` -- Add tab column values, reorganize rows
- `lunalib/LunaSettings.json` -- May need parallel updates (this file appears to be an alternative/duplicate format)
- `PresetListener.java` -- May need updates if field IDs change or if preset Text rows are removed
- `Settings.java` -- No changes needed if field IDs are preserved

The key architectural constraint: **field IDs must remain stable** for existing save compatibility. Renaming `cruiserToFrigate` to something else would break saved custom values.

## Sources

- [LunaSettings.CSV Documentation](https://github.com/Lukas22041/LunaLib/wiki/LunaSettings.CSV) -- Authoritative source for CSV format, tab column behavior, field types. HIGH confidence.
- [LunaSettingsListener Documentation](https://github.com/Lukas22041/LunaLib/wiki/LunaSettingsListener) -- Listener API for reacting to settings changes. HIGH confidence.
- [LunaSettingsConfig.json Documentation](https://github.com/Lukas22041/LunaLib/wiki/LunaSettingsConfig.json) -- Custom icon support. HIGH confidence.
- [Integrating LunaSettings Guide](https://github.com/Lukas22041/LunaLib/wiki/Integrating-LunaSettings) -- Integration patterns and soft dependency approach. HIGH confidence.
- [LunaLib GitHub Repository](https://github.com/Lukas22041/LunaLib) -- Source code reference for internal API usage. HIGH confidence.
- [LunaLib Forum Thread](https://fractalsoftworks.com/forum/index.php?topic=25658.0) -- Community discussion and usage patterns. MEDIUM confidence (forum, may contain outdated info).
- [Starsector Custom UI Guide](https://fractalsoftworks.com/forum/index.php?topic=28425.0) -- Custom UI panel patterns for InteractionDialogPlugin. MEDIUM confidence.
- Existing codebase analysis: `Settings.java`, `PresetListener.java`, `FactionManagerDialog.java`, `LunaSettings.csv`, `LunaSettings.json`. HIGH confidence (primary source).

---

*Feature landscape analysis: 2026-02-21*
