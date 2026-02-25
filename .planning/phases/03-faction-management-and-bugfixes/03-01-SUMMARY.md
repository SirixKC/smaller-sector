---
phase: 03-faction-management-and-bugfixes
plan: 01
subsystem: ui
tags: [luna-settings, faction-dialog, bugfix, station-filter]

# Dependency graph
requires:
  - phase: 01-tab-structure-and-preset-display
    provides: "Tabbed LunaSettings.csv with Factions tab (FACT-01/FACT-02)"
  - phase: 02-preset-switching-and-custom-persistence
    provides: "PresetListener excludes factionBlacklist from preset load/backup"
provides:
  - "Bug-fixed saveBlacklist that does not override active preset"
  - "LunaSettingsLoader reload on blacklist save for Factions tab sync"
  - "Station-filtered ship counts in faction dialog"
  - "Vanilla source mod identification for vanilla-ship factions"
  - "Highlighted cruiser/capital counts as 'Affected' in dialog"
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "LunaSettingsLoader.INSTANCE.loadSettings reload after JSON save (same as PresetListener)"
    - "ShipTypeHints.STATION filter for accurate ship counts (same as RoleMatcher)"

key-files:
  created: []
  modified:
    - "src/smallersector/FactionManagerDialog.java"

key-decisions:
  - "Removed instanceof WithSourceMod — ShipHullSpecAPI already extends it, direct call is cleaner"
  - "Null source mod counted as 'Vanilla' instead of skipped — vanilla factions now show correct source"

patterns-established:
  - "Blacklist save always reloads LunaSettingsLoader to keep in-memory and on-disk in sync"

# Metrics
duration: 2min
completed: 2026-02-25
---

# Phase 3 Plan 1: Faction Dialog Bugfix and Display Improvements Summary

**Preset-override bugfix in saveBlacklist (CLNP-03), Vanilla source identification, station-filtered counts, and highlighted affected ship counts in FactionManagerDialog**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-25T00:23:05Z
- **Completed:** 2026-02-25T00:25:07Z
- **Tasks:** 2
- **Files modified:** 1

## Accomplishments
- Fixed silent preset-switch bug: saving blacklist from in-game dialog no longer overwrites active preset to "Custom"
- Added LunaSettingsLoader reload after blacklist save so LunaLib Factions tab stays in sync
- Faction dialog now shows "Vanilla" for factions whose ships have null source mod (instead of "Unknown")
- Ship counts in dialog now filter out station hulls for accurate cruiser/capital totals
- Cruiser and capital counts displayed with highlight color as "Affected" to indicate replacement scope
- Verified FACT-01 (guidance text) and FACT-02 (blacklist string field) already present in LunaSettings.csv

## Task Commits

Each task was committed atomically:

1. **Task 1: Fix CLNP-03 — Remove preset override from saveBlacklist and add LunaLib reload** - `a187d49` (fix)
2. **Task 2: Improve dialog display — faction source ID, station filtering, affected ship counts** - `724eb33` (feat)

## Files Created/Modified
- `src/smallersector/FactionManagerDialog.java` - Bug-fixed saveBlacklist, improved getFactionModSource (Vanilla fallback, no instanceof cast), station-filtered countShipsBySize, highlighted affected ship count display

## Decisions Made
- Removed `instanceof WithSourceMod` cast since `ShipHullSpecAPI` already extends `WithSourceMod` — direct `hull.getSourceMod()` call is cleaner
- Null source mod counted as "Vanilla" instead of being skipped — ensures vanilla-heavy factions (e.g., Hegemony) show correct source label

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Phase 3 Plan 1 complete. Remaining phase 3 plans (if any) can proceed.
- All Luna Settings Revamp requirements satisfied: tab structure (Phase 1), preset switching/persistence (Phase 2), faction dialog bugfix and improvements (Phase 3 Plan 1)

## Self-Check: PASSED

- FOUND: src/smallersector/FactionManagerDialog.java
- FOUND: commit a187d49 (Task 1)
- FOUND: commit 724eb33 (Task 2)
- FOUND: 03-01-SUMMARY.md

---
*Phase: 03-faction-management-and-bugfixes*
*Completed: 2026-02-25*
