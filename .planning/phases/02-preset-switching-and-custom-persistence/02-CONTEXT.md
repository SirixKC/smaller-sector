# Phase 2: Preset Switching and Custom Persistence - Context

**Gathered:** 2026-02-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Enable users to switch between presets without losing their custom setting values. Custom values are backed up to disk and can be explicitly restored. Old inline preset annotations (`[Recommended: 30%]`) are replaced with dynamic active-preset hints. Creating new presets, editing preset definitions, or adding new settings are out of scope.

</domain>

<decisions>
## Implementation Decisions

### Backup timing & scope
- Custom values are saved when the user clicks "Save Settings" in LunaLib (ties into existing save flow, not on every slider change)
- All settings are backed up as a full snapshot (not just diff from preset defaults)
- factionBlacklist is excluded from backup — consistent with Phase 1's exclusion from preset loading
- Single backup file, overwritten each save — no history

### Switching experience
- No confirmation dialog when switching from Custom to a named preset — immediate switch
- Switching TO Custom does NOT auto-restore backed-up values — it just unlocks editing with current preset values as starting point
- Separate "Restore Custom Values" action to explicitly restore backed-up snapshot — Claude's discretion on UI placement (button vs radio option, within LunaLib capabilities)
- Restore action is disabled/hidden when no backup exists

### First-time & edge cases
- Default preset on first install: Vanilla
- If mod update adds new settings not in backup: those settings keep their active preset default values during restore (only backed-up settings are overwritten)
- Backup persistence: best effort — store where LunaLib/LazyLib normally stores data; don't go out of the way to guarantee survival across mod reinstall

### Annotation cleanup
- Remove old inline `[Recommended: 30%]` annotations from setting descriptions
- Replace with dynamic active-preset value hints, e.g., `[Preset: 30%]`
- Hints show the active preset's value for that setting (changes when user switches presets)
- Hints prepended to the setting description, e.g., `[Preset: 30%] Controls fleet replacement chance.`
- Hints update dynamically in real-time when the user switches presets (not just on settings open)

### Claude's Discretion
- Persistence mechanism (LazyLib JSONUtils or alternative)
- Restore action UI placement (button on Presets tab, radio option, or other)
- Backup file location and format
- How to handle dynamic description rewriting within LunaLib's API constraints

</decisions>

<specifics>
## Specific Ideas

- Backup should feel seamless — tied to existing "Save Settings" flow, no separate save step
- Switching to Custom is about "edit from here" — the restore action is a deliberate choice, not automatic
- Annotations should be dynamic, not static — when user switches to Sirix Hardcore, the hints should immediately reflect Hardcore values

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 02-preset-switching-and-custom-persistence*
*Context gathered: 2026-02-21*
