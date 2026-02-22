# Roadmap: Smaller Sector — Luna Settings Revamp

## Overview

This roadmap delivers a tabbed LunaLib settings UI for the Smaller Sector mod in three phases. Phase 1 creates the tab structure, preset radio selector, and value summaries — the highest-impact change that transforms the flat settings list into an organized interface. Phase 2 adds custom value persistence across preset switches and removes the old inline annotations they replace. Phase 3 completes the faction management tab, improves the in-game dialog, and fixes the blacklist-preset coupling bug.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Tab Structure and Preset Display** - Restructure settings into tabs with preset radio selector and value summaries
- [ ] **Phase 2: Preset Switching and Custom Persistence** - Custom value backup/restore across preset switches
- [ ] **Phase 3: Faction Management and Bugfixes** - Faction tab content, dialog improvements, blacklist-preset decoupling

## Phase Details

### Phase 1: Tab Structure and Preset Display
**Goal**: Users see a clean tabbed settings UI where they can select a preset via radio button and immediately see what each preset configures
**Depends on**: Nothing (first phase)
**Requirements**: TABS-01, TABS-02, PRES-01, PRES-02, CLNP-01
**Success Criteria** (what must be TRUE):
  1. Opening mod settings (F3) shows named tabs instead of a flat list of all settings
  2. User can select Vanilla, Sirix Recommended, Sirix Hardcore, or Custom via a radio field on the Presets tab
  3. Each preset's configured values are visible as read-only text summaries on the Presets tab, so the user can compare presets before committing
  4. Tabs have Header and Text separators that visually group related settings within each tab
  5. The duplicate `lunalib/LunaSettings.json` file is deleted and settings still load correctly from the CSV
**Plans:** 2 plans

Plans:
- [x] 01-01-PLAN.md — Restructure LunaSettings.csv into three-tab layout with preset summaries; delete LunaSettings.json
- [x] 01-02-PLAN.md — Add load-preset radio handler to PresetListener; human verification of complete UI

### Phase 2: Preset Switching and Custom Persistence
**Goal**: Users can freely switch between presets without losing their custom values, and old inline annotations are gone
**Depends on**: Phase 1
**Requirements**: PRES-03, CLNP-02
**Success Criteria** (what must be TRUE):
  1. User sets custom slider values, switches to a named preset (e.g., Sirix Recommended), then switches back to Custom — all custom values are restored exactly as they were
  2. Custom values survive across game restarts (persisted to disk, not just in-memory)
  3. Old inline preset annotation text (e.g., `[Recommended: 30%]`) no longer appears next to settings sliders
**Plans**: TBD

Plans:
- [ ] 02-01: TBD

### Phase 3: Faction Management and Bugfixes
**Goal**: Users can discover and manage per-faction ship replacement from both the Luna settings Factions tab and the improved in-game dialog
**Depends on**: Phase 1
**Requirements**: FACT-01, FACT-02, FACT-03, CLNP-03
**Success Criteria** (what must be TRUE):
  1. Factions tab in Luna settings contains guidance text directing the user to the in-game Faction Manager dialog
  2. Factions tab contains the comma-separated blacklist string as an advanced fallback for users who want to edit it directly
  3. In-game FactionManagerDialog shows each faction's source mod (or "Vanilla") and ship counts affected by replacement
  4. Toggling a faction in the in-game dialog no longer silently switches the user's active preset to Custom
**Plans**: TBD

Plans:
- [ ] 03-01: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Tab Structure and Preset Display | 2/2 | Complete | 2026-02-21 |
| 2. Preset Switching and Custom Persistence | 0/TBD | Not started | - |
| 3. Faction Management and Bugfixes | 0/TBD | Not started | - |
