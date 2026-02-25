# Phase 3: Faction Management and Bugfixes - Research

**Researched:** 2026-02-24
**Domain:** Starsector FactionManagerDialog improvements, LunaLib Factions tab content, blacklist-preset decoupling
**Confidence:** HIGH

## Summary

Phase 3 addresses four requirements: FACT-01 (Factions tab guidance text), FACT-02 (comma-separated blacklist string), FACT-03 (improved in-game FactionManagerDialog), and CLNP-03 (decouple blacklist save from preset selection). Of these, FACT-01 and FACT-02 are already implemented in the current codebase -- the LunaSettings.csv already has the Factions tab with guidance text (line 51: `ss_faction_guide`), a help note (line 52: `ss_faction_help`), and the `factionBlacklist` String field (line 53). These were completed during Phase 1's CSV restructure and require no further work beyond verification.

The remaining work is concentrated in two areas: (1) improving the `FactionManagerDialog` to show better faction source identification and per-faction ship counts that distinguish between ships affected by replacement versus total ships (FACT-03), and (2) fixing the silent preset-switch bug in `FactionManagerDialog.saveBlacklist()` which currently forces `data.put("preset", "Custom")` every time the blacklist is saved from the in-game dialog (CLNP-03).

The CLNP-03 bug is clearly identified at line 235 of `FactionManagerDialog.java`. The fix is to remove the `data.put("preset", "Custom")` line from `saveBlacklist()`. The blacklist is already independent of presets -- `Settings.reloadBlacklist()` merges the user's blacklist string with the Sirix default blacklist based on the active preset. Phases 1 and 2 already excluded `factionBlacklist` from preset loading and backup, establishing the pattern that the blacklist is preset-independent.

For FACT-03, the current dialog already shows mod source and ship counts. The improvements are incremental: (a) better faction source identification that handles null source mod gracefully (vanilla ships) and removes the redundant `instanceof` check, and (b) displaying ship counts in a way that makes it clear which ships are affected by replacement (cruisers + capitals) versus the full roster, with station hulls filtered out for accurate counts.

**Primary recommendation:** Focus the plan on two tasks: (1) fix the CLNP-03 bug by removing the preset-override line in `saveBlacklist()` and adding LunaSettingsLoader reload for UI sync, and (2) improve the `FactionManagerDialog` display to show "Vanilla" for null-source ships, highlight cruiser/capital counts as "affected by replacement", and filter station hulls from counts. Verify FACT-01 and FACT-02 as already complete. End with a human verification checkpoint.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| LunaLib | 2.0.5 | Settings UI framework -- CSV Factions tab already hosts guidance text and blacklist field | Only settings framework in Starsector modding ecosystem |
| LazyLib | latest | `JSONUtils.loadCommonJSON` for blacklist persistence in `saveBlacklist()` | Standard persistence API; already used throughout the codebase |
| Starsector API | 0.98a-RC8 | `FactionAPI`, `ShipHullSpecAPI`, `ModSpecAPI`, `WithSourceMod`, `InteractionDialogPlugin`, `IntelInfoPlugin` | Runtime environment |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Log4j | 1.2.9 | Logging via `Global.getLogger()` | Error conditions in dialog save/display |

### Alternatives Considered
None. The existing libraries cover all needs for this phase.

**Installation:**
No new dependencies. All libraries are already in the build classpath via `build.xml`.

## Architecture Patterns

### Recommended File Structure
```
src/smallersector/
  FactionManagerDialog.java    # MODIFIED: Remove preset override in saveBlacklist(), improve display
  FactionManagerIntel.java     # NO CHANGES (already works correctly)
  Settings.java                # NO CHANGES (blacklist loading already preset-independent)
  PresetListener.java          # NO CHANGES (already excludes factionBlacklist from presets)
data/config/
  LunaSettings.csv             # NO CHANGES (Factions tab already complete from Phase 1)
```

### Pattern 1: Blacklist Save Without Preset Override (CLNP-03 Fix)
**What:** The `FactionManagerDialog.saveBlacklist()` method currently writes `data.put("preset", "Custom")` at line 235 when saving the blacklist. This silently switches the user's active preset to Custom, which is a bug. The fix is to remove this line entirely.

**Why the line existed originally:** Before the Phase 1/2 rework, changing any setting value would be inconsistent with a named preset. The original author assumed that any manual change (including blacklist changes) should switch to Custom for consistency. However, the blacklist is now explicitly independent of presets -- Phase 1 excluded it from preset loading (`PresetListener.handleLoadPreset()` filters it out), Phase 2 excluded it from backup (`BACKUP_SETTINGS_INT/DOUBLE` arrays do not include it), and `Settings.reloadBlacklist()` already merges the user blacklist with preset defaults.

**The fix:**
```java
// BEFORE (buggy):
private void saveBlacklist() {
    try {
        // ... build blacklistString ...
        JSONUtils.CommonDataJSONObject data = JSONUtils.loadCommonJSON(
                "LunaSettings/" + MOD_ID + ".json",
                "data/config/LunaSettingsDefault.default"
        );
        if (data != null) {
            data.put("factionBlacklist", blacklistString);
            data.put("preset", "Custom");  // <-- BUG: remove this line
            data.save();
        }
        Settings.reloadBlacklist();
        // ...
    }
}

// AFTER (fixed):
private void saveBlacklist() {
    try {
        // ... build blacklistString ...
        JSONUtils.CommonDataJSONObject data = JSONUtils.loadCommonJSON(
                "LunaSettings/" + MOD_ID + ".json",
                "data/config/LunaSettingsDefault.default"
        );
        if (data != null) {
            data.put("factionBlacklist", blacklistString);
            // No preset override -- blacklist is independent of presets
            data.save();
        }
        // Reload LunaLib in-memory settings so Factions tab shows updated blacklist
        LunaSettingsLoader.INSTANCE.loadSettings(MOD_ID, true);
        Settings.reloadBlacklist();
        // ...
    }
}
```

**Verified from:** `FactionManagerDialog.java` line 235, `Settings.reloadBlacklist()` line 279-295, `PresetListener.handleLoadPreset()` line 344 (`filteredValues.remove("factionBlacklist")`).

**Confidence:** HIGH -- the bug is clearly identified, the fix is a single line removal, and the blacklist independence pattern is well-established across Phase 1 and 2.

### Pattern 2: Faction Source Identification via Hull Spec Source Mod
**What:** The current `getFactionModSource()` method iterates a faction's known ships, casts each `ShipHullSpecAPI` to `WithSourceMod`, and picks the mod with the most ships. This approach is fundamentally sound but has two issues: (a) `ShipHullSpecAPI` already extends `WithSourceMod` (confirmed from API jar inspection via `javap`), so the `instanceof` check is redundant, and (b) when `getSourceMod()` returns `null` for vanilla hull specs, the ship is silently skipped -- it should count as "Vanilla".

**The Starsector API provides:**
- `ShipHullSpecAPI extends WithSourceMod` -- every hull spec has `getSourceMod()` directly, no cast needed
- `ModSpecAPI.getName()` returns the human-readable mod name (e.g., "Hazard Mining Incorporated", "LunaLib")
- `ModSpecAPI.getId()` returns the mod identifier (e.g., "hmi", "lunalib")
- `FactionAPI` does NOT implement `WithSourceMod` -- there is no `FactionAPI.getSourceMod()` method
- `FactionSpecAPI` does NOT have source mod information either

**Consequence:** There is no direct way to determine which mod defines a faction's `.faction` file. The hull-based heuristic (majority vote of ship sources) is the best available approach. The improvement is to handle null source mod as "Vanilla" instead of skipping:

```java
private String getFactionModSource(FactionAPI faction) {
    Map<String, Integer> modCounts = new HashMap<>();
    for (String hullId : faction.getKnownShips()) {
        ShipHullSpecAPI hull = Global.getSettings().getHullSpec(hullId);
        if (hull != null) {
            ModSpecAPI mod = hull.getSourceMod();
            if (mod != null) {
                String name = mod.getName();
                modCounts.put(name, modCounts.getOrDefault(name, 0) + 1);
            } else {
                // Null source mod = vanilla ship
                modCounts.put("Vanilla", modCounts.getOrDefault("Vanilla", 0) + 1);
            }
        }
    }
    return modCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Unknown");
}
```

**Verified from:** `javap` inspection of `ShipHullSpecAPI` confirms `extends WithSourceMod`. `ModSpecAPI.getName()` confirmed via `javap`. The exact return value for vanilla hull specs (null vs "Starsector") needs runtime verification, but handling null covers the most likely case.

**Confidence:** HIGH for the mechanism. MEDIUM for the "null = Vanilla" assumption (needs runtime verification; the null-safe handling ensures correctness regardless).

### Pattern 3: Ship Count Display with Replacement Context
**What:** The current dialog shows ship counts in format `F: 12 | D: 8 | Cr: 4 | Cap: 2`. This is factually correct but does not help users understand that Smaller Sector only affects cruisers and capitals. The improvement is to visually distinguish which counts are affected by replacement.

**Approaches:**
1. **Separate line for affected ships:** Show cruiser/capital counts on a highlighted line like "Affected: 4 cruisers, 2 capitals" with frigate/destroyer counts below in gray as context.
2. **Bold formatting for affected counts:** Use LunaLib-style `[bracket]` highlighting... except the dialog uses `InteractionDialogAPI.getTextPanel()` which does not support bracket highlighting. It uses `addPara()` with explicit color parameters.
3. **Explicit labeling:** Use `addPara()` with highlight on the cruiser/capital numbers: `"Ships: F:12 D:8 | Affected by mod: Cr:[4] Cap:[2]"`. The `addPara()` method supports highlighting specific strings within the paragraph.

**Recommended:** Approach 3 -- label the cruiser and capital counts explicitly as "affected" and use `Misc.getHighlightColor()` to emphasize them. The `TextPanelAPI.addPara(String format, Color highlightColor, String... highlights)` overload supports highlighting specific strings.

**Verified from:** Existing code at `FactionManagerDialog.java` line 112 uses `addPara()` with highlight params. The `TextPanelAPI` pattern is well-established.

**Confidence:** HIGH

### Pattern 4: Station Filtering in Ship Counts (VERIFIED)
**What:** The current `countShipsBySize()` does not filter station hulls, potentially inflating capital ship counts. The `RoleMatcher.java` already implements station filtering at line 226 using the exact pattern:

```java
if (hull.getHints() != null && hull.getHints().contains(ShipTypeHints.STATION)) continue;
```

**The verified API:**
- `ShipHullSpecAPI.getHints()` returns `EnumSet<ShipTypeHints>` (confirmed from `RoleMatcher.java` line 32)
- `ShipHullSpecAPI.ShipTypeHints.STATION` is the enum constant to check (confirmed from import at `RoleMatcher.java` line 6)
- The null check on `getHints()` is a safety pattern used by `RoleMatcher`

**Confidence:** HIGH -- exact pattern verified from `RoleMatcher.java` lines 6, 32, and 226.

### Pattern 5: Factions Tab Content (FACT-01 and FACT-02 -- Already Complete)
**What:** The current `LunaSettings.csv` already contains:
- Line 51: `ss_faction_guide` -- Text field with guidance: `"[TIP:] Use the in-game [Faction Manager] to browse all loaded factions and toggle blacklist status. Open your [Intel Screen] (Missions tab) to find it."`
- Line 52: `ss_faction_help` -- Text field with help: `"Your custom blacklist entries below are ALWAYS applied regardless of preset. Sirix presets also include default AI/special factions."`
- Line 53: `factionBlacklist` -- String field for the comma-separated blacklist

**These satisfy FACT-01 and FACT-02.** The guidance text directs users to the in-game Faction Manager dialog, and the blacklist string is available as an advanced fallback. No changes needed.

**Verified from:** `LunaSettings.csv` lines 51-53.

**Confidence:** HIGH

### Anti-Patterns to Avoid
- **Re-implementing blacklist persistence:** The existing `JSONUtils.loadCommonJSON()` + `.save()` pattern in `saveBlacklist()` is correct. Do not replace it with a different persistence mechanism.
- **Trying to detect faction source from `.faction` files:** There is no API to determine which mod provides a `.faction` file. The hull-based heuristic is the best available approach. Do not attempt file system scanning.
- **Adding LunaLib settings listener for blacklist changes:** The in-game dialog saves directly to the LunaSettings JSON file and calls `Settings.reloadBlacklist()`. This bypasses the `settingsChanged()` listener, which is correct -- the dialog is an alternative save path that does not need to trigger the full PresetListener pipeline.
- **Removing the `WithSourceMod` import:** Even though the `instanceof` check is technically unnecessary (since `ShipHullSpecAPI extends WithSourceMod`), the import can stay or be removed. The key change is removing the redundant cast, not the import.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Faction source detection | Custom file system scanning for `.faction` files | `ShipHullSpecAPI.getSourceMod()` majority-vote heuristic | The API provides `WithSourceMod` on hull specs but NOT on factions. File scanning is fragile and platform-dependent. |
| Blacklist persistence | Custom file I/O | `JSONUtils.loadCommonJSON()` (already used) | Standard pattern, handles file creation and error recovery |
| Dialog text formatting | Custom rendering code | `TextPanelAPI.addPara()` with highlight colors | The dialog API already supports rich text via color parameters |
| Station hull detection | Custom hull tag parsing | `hull.getHints().contains(ShipTypeHints.STATION)` | Exact pattern already used in `RoleMatcher.java` line 226 |

**Key insight:** Phase 3 is primarily bug-fixing and incremental improvement. The core architecture (dialog, intel item, settings tab, blacklist persistence) is already built. The work is surgical -- one file modified (`FactionManagerDialog.java`), no new files, no new dependencies.

## Common Pitfalls

### Pitfall 1: Blacklist Save Triggers PresetListener
**What goes wrong:** After removing the `data.put("preset", "Custom")` line, the `saveBlacklist()` method still writes to the LunaSettings JSON file. If this somehow triggers `PresetListener.settingsChanged()`, the listener might re-process settings.
**Why it might happen:** The `saveBlacklist()` method directly writes to `LunaSettings/smallersector.json` and calls `Settings.reloadBlacklist()`. It does NOT call `LunaSettings.reportSettingsChanged()`. The `settingsChanged()` callback is only triggered by LunaLib's save UI flow, not by direct JSON writes.
**How to avoid:** No action needed. The in-game dialog's save path is independent of the LunaLib settings UI save path. Verified by code inspection: `saveBlacklist()` calls `data.save()` and `Settings.reloadBlacklist()` but never invokes any LunaSettings listener method.
**Warning signs:** Unexpected log messages from PresetListener after saving blacklist from the in-game dialog.
**Confidence:** HIGH -- verified from code paths.

### Pitfall 2: Vanilla Faction Source Name Varies
**What goes wrong:** The assumption that vanilla ships return `null` from `getSourceMod()` may be incorrect. They might return a `ModSpecAPI` with name "Starsector" or something else entirely.
**Why it happens:** `ModSpecAPI.getName()` returns the `name` field from `mod_info.json`. The vanilla game core may or may not register as a "mod" in the source mod system.
**How to avoid:** Handle both `null` and known vanilla names. The recommended code treats `null` as "Vanilla". Additionally, after implementing, check via runtime logging what vanilla hull specs actually return. If a non-null name appears (like "Starsector"), add a mapping.
**Warning signs:** Vanilla factions showing a mod name like "Starsector" instead of "Vanilla".
**Confidence:** MEDIUM -- the exact return value needs runtime verification. The null-safe code path ensures no crashes regardless.

### Pitfall 3: Faction With No Known Ships
**What goes wrong:** Some factions (especially generated/placeholder factions) may have zero ships in `getKnownShips()`. The current `getFactionModSource()` returns "Unknown" for these, and `countShipsBySize()` returns all zeros. The dialog shows an empty-looking entry.
**Why it happens:** The dialog already filters out `player` and `neutral` factions (lines 52-53), but other placeholder factions may exist depending on installed mods.
**How to avoid:** Consider filtering factions with zero known ships entirely, or showing them in a distinct "No ships" section. The current code does not filter these out.
**Warning signs:** Faction entries in the dialog with all-zero ship counts and "Unknown" mod source.
**Confidence:** HIGH -- visible from code analysis.

### Pitfall 4: Stale Blacklist After In-Game Dialog Save
**What goes wrong:** The user saves blacklist changes in the in-game dialog, but LunaLib's in-memory settings still hold the old blacklist string. If the user then opens LunaLib settings (F3), the `factionBlacklist` field shows the old value.
**Why it happens:** `saveBlacklist()` writes to the JSON file and calls `Settings.reloadBlacklist()`, which updates the mod's internal cache. But LunaLib's own in-memory settings (read via `LunaSettings.getString()`) are not reloaded until LunaLib re-reads the JSON file.
**How to avoid:** Add `LunaSettingsLoader.INSTANCE.loadSettings(MOD_ID, true)` to `saveBlacklist()` after `data.save()`. This forces LunaLib to re-read the JSON, so the Factions tab in LunaLib settings shows the updated blacklist string.
**Warning signs:** Factions tab blacklist string does not match what was just saved in the in-game dialog.
**Confidence:** HIGH -- verified from code analysis. The existing PresetListener uses this reload pattern (lines 189, 262, 328, 363, 398).

### Pitfall 5: Ship Count Includes Station Hulls
**What goes wrong:** The `countShipsBySize()` method counts ALL known ships in a faction, including station hulls. Stations with `HullSize.CAPITAL_SHIP` would inflate the capital count, making the "affected by replacement" count misleading since the mod explicitly skips stations.
**Why it happens:** `faction.getKnownShips()` returns all hull IDs the faction can use. Station hulls have `HullSize.CAPITAL_SHIP` in the API (the `STATION` hull size only exists in combat contexts). The current code at line 154 checks `!hull.isDefaultDHull()` but does not filter stations.
**How to avoid:** Add station filtering in `countShipsBySize()`. Use `hull.getHints().contains(ShipTypeHints.STATION)` -- the exact pattern from `RoleMatcher.java` line 226.
**Warning signs:** Capital ship counts higher than expected for factions with orbital stations.
**Confidence:** HIGH -- verified from `RoleMatcher.java` codebase pattern.

### Pitfall 6: addPara Highlight String Matching
**What goes wrong:** The `TextPanelAPI.addPara(String format, Color highlightColor, String... highlights)` method highlights the FIRST occurrence of each highlight string in the format text. If the same number appears in multiple places (e.g., "4" appears in both the total and the affected count), the wrong instance may be highlighted.
**Why it happens:** The highlight matching is positional based on first occurrence, not templated.
**How to avoid:** Use `%s` format placeholders with the `addPara(String format, Color color, Color highlightColor, String... highlights)` overload which replaces `%s` tokens sequentially. This is the pattern already used at line 108 and 112 of the existing dialog code.
**Warning signs:** Wrong numbers highlighted in the ship count display.
**Confidence:** HIGH -- verified from existing dialog code patterns.

## Code Examples

### CLNP-03 Fix: Remove Preset Override from saveBlacklist()
```java
// Source: FactionManagerDialog.java saveBlacklist() method
// Remove line 235: data.put("preset", "Custom");
// Add LunaSettingsLoader reload for UI sync
// Add import: lunalib.backend.ui.settings.LunaSettingsLoader

private void saveBlacklist() {
    try {
        StringBuilder sb = new StringBuilder();
        for (String factionId : currentBlacklist) {
            if (sb.length() > 0) sb.append(",");
            sb.append(factionId);
        }
        String blacklistString = sb.toString();

        JSONUtils.CommonDataJSONObject data = JSONUtils.loadCommonJSON(
                "LunaSettings/" + MOD_ID + ".json",
                "data/config/LunaSettingsDefault.default"
        );

        if (data != null) {
            data.put("factionBlacklist", blacklistString);
            // REMOVED: data.put("preset", "Custom"); -- blacklist is preset-independent
            data.save();
        }

        // Reload LunaLib in-memory settings so Factions tab shows updated blacklist
        LunaSettingsLoader.INSTANCE.loadSettings(MOD_ID, true);

        Settings.reloadBlacklist();

        Global.getSector().getCampaignUI().addMessage(
                "Faction blacklist saved. " + currentBlacklist.size() + " factions protected.",
                Misc.getPositiveHighlightColor());

    } catch (Exception e) {
        Global.getLogger(this.getClass()).error("Failed to save blacklist", e);
        Global.getSector().getCampaignUI().addMessage(
                "Error saving blacklist: " + e.getMessage(),
                Misc.getNegativeHighlightColor());
    }
}
```

### Improved getFactionModSource()
```java
// Source: FactionManagerDialog.java
// ShipHullSpecAPI extends WithSourceMod (confirmed via javap)
// No instanceof check needed; handle null source mod as "Vanilla"

private String getFactionModSource(FactionAPI faction) {
    Map<String, Integer> modCounts = new HashMap<>();
    for (String hullId : faction.getKnownShips()) {
        ShipHullSpecAPI hull = Global.getSettings().getHullSpec(hullId);
        if (hull != null) {
            ModSpecAPI mod = hull.getSourceMod();
            if (mod != null) {
                String name = mod.getName();
                modCounts.put(name, modCounts.getOrDefault(name, 0) + 1);
            } else {
                // Null source mod = vanilla ship
                modCounts.put("Vanilla", modCounts.getOrDefault("Vanilla", 0) + 1);
            }
        }
    }
    return modCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("Unknown");
}
```

### Ship Count Display with Replacement Context
```java
// Source: FactionManagerDialog.java showFactionList()
// Show ship counts with explicit "affected" labeling
// Uses %s placeholder pattern (same as existing line 108/112)

Map<HullSize, Integer> counts = countShipsBySize(faction);
int totalCount = counts.get(HullSize.FRIGATE) + counts.get(HullSize.DESTROYER)
        + counts.get(HullSize.CRUISER) + counts.get(HullSize.CAPITAL_SHIP);

String countsLine = String.format("  Ships: %d total | Affected: %s Cr, %s Cap",
        totalCount,
        counts.get(HullSize.CRUISER),
        counts.get(HullSize.CAPITAL_SHIP));
dialog.getTextPanel().addPara(countsLine, g, h,
        String.valueOf(counts.get(HullSize.CRUISER)),
        String.valueOf(counts.get(HullSize.CAPITAL_SHIP)));
```

### Station Filtering in countShipsBySize() (Verified Pattern)
```java
// Source: FactionManagerDialog.java
// Station filtering pattern from RoleMatcher.java line 226
// Import: com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints

private Map<HullSize, Integer> countShipsBySize(FactionAPI faction) {
    Map<HullSize, Integer> counts = new LinkedHashMap<>();
    for (HullSize size : new HullSize[]{HullSize.FRIGATE, HullSize.DESTROYER,
            HullSize.CRUISER, HullSize.CAPITAL_SHIP}) {
        counts.put(size, 0);
    }
    for (String hullId : faction.getKnownShips()) {
        ShipHullSpecAPI hull = Global.getSettings().getHullSpec(hullId);
        if (hull != null && !hull.isDefaultDHull()) {
            // Skip station hulls -- they are never replaced by the mod
            // Pattern from RoleMatcher.java line 226
            if (hull.getHints() != null && hull.getHints().contains(ShipTypeHints.STATION)) continue;

            HullSize size = hull.getHullSize();
            if (counts.containsKey(size)) {
                counts.put(size, counts.get(size) + 1);
            }
        }
    }
    return counts;
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Blacklist save forces preset to Custom | Blacklist save is preset-independent | This phase (CLNP-03) | Users retain their active preset when modifying factions |
| `instanceof WithSourceMod` cast + skip null | Direct `getSourceMod()` + null = "Vanilla" mapping | This phase (FACT-03) | Cleaner source identification, "Vanilla" instead of "Unknown" for core ships |
| Flat ship count display (F/D/Cr/Cap) | Replacement-context display (total + affected) | This phase (FACT-03) | Users understand which ships the mod actually affects |
| Station hulls counted in ship totals | Station hulls filtered from counts | This phase (FACT-03) | Accurate capital ship counts |

**Deprecated/outdated:**
- The `data.put("preset", "Custom")` line in `saveBlacklist()` is the bug being fixed. It was a holdover from before the blacklist was decoupled from presets.
- The `instanceof WithSourceMod` check in `getFactionModSource()` is unnecessary since `ShipHullSpecAPI extends WithSourceMod` directly.
- The `WithSourceMod` import can be removed from `FactionManagerDialog.java` after the refactor (but leaving it is harmless).

## Open Questions

1. **Vanilla Ship Source Mod Name**
   - What we know: `ShipHullSpecAPI.getSourceMod()` returns a `ModSpecAPI` for modded ships. For vanilla ships, it likely returns `null` (since vanilla is the game core, not a mod).
   - What's unclear: Whether vanilla hull specs return `null` or a `ModSpecAPI` with name "Starsector". The `instanceof WithSourceMod` check in the original code may suggest some hull specs don't implement the interface (though `javap` shows `ShipHullSpecAPI` extends it).
   - Recommendation: Handle null as "Vanilla". If runtime testing reveals a non-null return with name "Starsector" or similar, add a mapping. The null-safe code ensures correctness either way.
   - Confidence: MEDIUM -- needs runtime verification during human testing.

## Sources

### Primary (HIGH confidence)
- Starsector API jar at `~/Games/starsector/starfarer.api.jar` -- Direct `javap` inspection of:
  - `ShipHullSpecAPI` -- confirms `extends WithSourceMod`
  - `FactionAPI` -- confirms NO `getSourceMod()` method; has `getKnownShips()`
  - `FactionSpecAPI` -- confirms NO source mod information
  - `ModSpecAPI` -- confirms `getName()`, `getId()`, `getPath()`, `getDirName()` methods
  - `WithSourceMod` -- confirms single method: `getSourceMod()`
  - `ModManagerAPI` -- confirms `getEnabledModsCopy()`, `getModSpec()`
  - `InteractionDialogPlugin` -- confirms `init()`, `optionSelected()` interface
- Existing mod source at `/home/sirix/Games/Modding/smaller-sector/src/smallersector/`:
  - `FactionManagerDialog.java` -- Bug at line 235, current `getFactionModSource()` and `countShipsBySize()` implementations
  - `FactionManagerIntel.java` -- Intel item for dialog access (no changes needed)
  - `Settings.java` -- `reloadBlacklist()` merges user blacklist with preset defaults (lines 279-295)
  - `PresetListener.java` -- Excludes `factionBlacklist` from preset loading (line 344-346) and backup (line 504)
  - `RoleMatcher.java` -- Station filtering pattern at line 226: `hull.getHints().contains(ShipTypeHints.STATION)` with import at line 6
- `LunaSettings.csv` lines 51-53 -- Factions tab already contains guidance text, help note, and blacklist field
- Phase 1 and 2 research and plan documents -- Established patterns for LunaLib internal API usage, JSON persistence, factionBlacklist independence

### Secondary (MEDIUM confidence)
- [.faction File Overview | StarSector Wiki](https://starsector.fandom.com/wiki/.faction_File_Overview) -- Faction file structure reference
- [FactionAPI Javadoc](https://fractalsoftworks.com/starfarer.api/com/fs/starfarer/api/campaign/FactionAPI.html) -- Official API documentation
- `.planning/research/PITFALLS.md` Pitfall 6 -- Prior analysis of `WithSourceMod` reliability

### Tertiary (LOW confidence)
None -- all findings verified against source code or API inspection.

## Metadata

**Confidence breakdown:**
- CLNP-03 bug fix: HIGH -- bug clearly identified at line 235, fix is single-line removal plus LunaSettingsLoader reload, blacklist independence pattern well-established
- FACT-01/FACT-02: HIGH -- already complete in current CSV, verification only
- FACT-03 dialog improvements: HIGH for mechanism (existing code already works, improvements are incremental, station filtering pattern verified from RoleMatcher); MEDIUM for vanilla source mod name mapping (needs runtime verification)
- Pitfalls: HIGH -- verified from code analysis and API inspection

**Research date:** 2026-02-24
**Valid until:** 2026-04-24 (stable codebase, no external dependency changes expected)
