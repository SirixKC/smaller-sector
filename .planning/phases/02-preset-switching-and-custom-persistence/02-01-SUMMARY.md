---
phase: 02-preset-switching-and-custom-persistence
plan: 01
subsystem: ui
tags: [lunalib, json-persistence, preset-system, backup-restore]

# Dependency graph
requires:
  - phase: 01-tab-structure-and-preset-display
    provides: PresetListener with handleLoadPreset radio handler, updateChangedSettings/updateUIElements pipeline
provides:
  - Custom value backup to saves/common/SmallerSectorCustomBackup.json.data via JSONUtils
  - Explicit "Restore Custom Values" radio option in ss_load_preset
  - Vanilla as default preset for new installs
  - Settings.java getPreset() fallback aligned with CSV default
affects: [02-02-PLAN]

# Tech tracking
tech-stack:
  added: []
  patterns: [JSONUtils.loadCommonJSON for cross-save persistence, per-key try-catch for forward-compatible backup loading]

key-files:
  created: []
  modified:
    - data/config/LunaSettings.csv
    - src/smallersector/Settings.java
    - src/smallersector/PresetListener.java

key-decisions:
  - "Vanilla is the default preset for new installs (not Sirix Recommended)"
  - "Backup only triggers when Custom preset is active on save (prevents overwriting custom backup with preset values)"
  - "Restore is an explicit user action via radio option, not automatic on switching to Custom"
  - "factionBlacklist excluded from backup (consistent with Phase 1 exclusion from preset loading)"
  - "Per-key error handling in loadBackupValues for forward compatibility with new settings"

patterns-established:
  - "Custom backup gate: only backup when Custom preset is active to avoid Pitfall 1 (overwriting with preset values)"
  - "Per-key try-catch in backup loading: new settings not in old backups are silently skipped"

# Metrics
duration: 2min
completed: 2026-02-22
---

# Phase 2 Plan 1: Custom Value Backup/Restore Summary

**Custom value backup/restore via JSONUtils with Restore radio option and Vanilla as default preset**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-22T01:47:50Z
- **Completed:** 2026-02-22T01:50:03Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments
- Added "Restore Custom Values" to the ss_load_preset radio on the Configuration tab, giving users an explicit way to recover backed-up custom slider values
- Implemented backupCustomValues() that saves all 15 numeric settings (7 Int, 8 Double) to saves/common/SmallerSectorCustomBackup.json.data, gated on Custom preset being active
- Implemented loadBackupValues() with per-key error handling so adding new settings in future updates does not break existing backups
- Changed default preset from "Sirix Recommended" to "Vanilla" in both CSV and Java fallback for a safer first-install experience

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Restore option to CSV radio and change default preset to Vanilla** - `0103591` (feat)
2. **Task 2: Add backup and restore logic to PresetListener** - `68d949f` (feat)

## Files Created/Modified
- `data/config/LunaSettings.csv` - Added "Restore Custom Values" to ss_load_preset radio options; changed preset defaultValue to Vanilla
- `src/smallersector/Settings.java` - Changed getPreset() null fallback from PRESET_RECOMMENDED to PRESET_VANILLA
- `src/smallersector/PresetListener.java` - Added backupCustomValues(), loadBackupValues(), hasBackup() methods; added Custom-gated backup call in settingsChanged(); added "Restore Custom Values" case in handleLoadPreset(); fixed null fallback to "Vanilla"

## Decisions Made
- Vanilla as default preset for new installs -- safer first experience, mod has no effect until user opts in
- Backup gated on Custom preset active -- prevents the pitfall of overwriting custom backup when a named preset is active
- Restore is explicit (radio action), not automatic -- switching to Custom just unlocks editing, user explicitly chooses to restore
- factionBlacklist excluded from backup arrays -- consistent with Phase 1 decision to exclude it from preset loading
- Per-key try-catch in loadBackupValues -- future settings additions won't break existing backups

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed settingsChanged null fallback inconsistency**
- **Found during:** Task 2 (PresetListener modifications)
- **Issue:** settingsChanged() had hardcoded `"Sirix Recommended"` fallback (line 112) which was inconsistent with the new Vanilla default
- **Fix:** Changed fallback to `"Vanilla"` to match CSV default and Settings.java getPreset() fallback
- **Files modified:** src/smallersector/PresetListener.java
- **Verification:** Build passes, grep confirms "Vanilla" fallback
- **Committed in:** 68d949f (Task 2 commit)

**2. [Rule 1 - Bug] Removed unused org.json.JSONObject import**
- **Found during:** Task 2 (PresetListener modifications)
- **Issue:** Initially added import for org.json.JSONObject but all JSON operations use CommonDataJSONObject
- **Fix:** Removed the unused import
- **Files modified:** src/smallersector/PresetListener.java
- **Verification:** Build passes with no warnings about unused imports
- **Committed in:** 68d949f (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (2 bugs)
**Impact on plan:** Both fixes necessary for consistency and code cleanliness. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Backup/restore pipeline is complete and ready for Phase 2 Plan 2 (if applicable)
- The hasBackup() method is available for any future UI gating (e.g., disabling Restore option when no backup exists)
- All 3 files modified in this plan are stable and build-passing

## Self-Check: PASSED

- FOUND: data/config/LunaSettings.csv
- FOUND: src/smallersector/Settings.java
- FOUND: src/smallersector/PresetListener.java
- FOUND: commit 0103591 (feat(02-01): add Restore Custom Values radio option)
- FOUND: commit 68d949f (feat(02-01): add custom value backup/restore pipeline)

---
*Phase: 02-preset-switching-and-custom-persistence*
*Completed: 2026-02-22*
