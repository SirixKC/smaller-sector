# Smaller Sector — Luna Settings Revamp

## What This Is

A total overhaul of the Smaller Sector mod's LunaLib settings UI. Currently settings are a flat list of sliders and a preset dropdown. The revamp introduces a tabbed interface with dedicated tabs for each preset (read-only previews with enable checkboxes), a custom settings tab with force-override capability, and a faction management tab showing every faction with its source mod and blacklist toggle.

## Core Value

Users can quickly understand and switch between presets, customize individual values when needed, and manage per-faction ship replacement — all from a clean, tabbed Luna settings UI.

## Requirements

### Validated

- ✓ Ship replacement pipeline (fleet, market, derelict) — existing
- ✓ Preset system (Vanilla/Sirix Recommended/Sirix Hardcore/Custom) — existing
- ✓ Operating cost and build cost multipliers — existing
- ✓ D-mod application to player-acquired large ships — existing
- ✓ Faction blacklist with in-game dialog — existing
- ✓ Station filtering across all pipelines — existing

### Active

- [ ] Tabbed Luna settings UI with multiple tabs
- [ ] Preset tabs (Vanilla, Sirix Recommended, Sirix Hardcore) showing read-only value previews
- [ ] Enable checkbox per preset tab — radio behavior (checking one unchecks others)
- [ ] Custom settings tab with force-enable checkbox that overrides active preset
- [ ] Custom tab shows editable sliders/fields only when force-enabled
- [ ] Custom values persist when switching between presets (remembered for later)
- [ ] Faction tab listing every faction with source identification (mod name or "Vanilla")
- [ ] Faction tab blacklist toggle per faction
- [ ] Faction tab coexists with existing in-game FactionManagerDialog
- [ ] Clean up old flat settings code after migration

### Out of Scope

- Removing the in-game FactionManagerDialog — users want both access points
- Adding new gameplay settings — this milestone is UI-only
- Changing ship replacement logic or probabilities — just reorganizing how they're configured

## Context

- **LunaLib** provides the settings UI framework (accessed via F3 in campaign). It supports tabs, various input types (sliders, checkboxes, text), and a listener system for changes.
- Current settings are defined in `Settings.java` as static methods wrapping LunaLib reads, with `PresetListener.java` syncing hardcoded preset values to LunaLib sliders.
- The faction blacklist is currently managed through `FactionManagerDialog` (in-game intel panel) and persisted via LunaLib settings. The new faction tab will be an additional entry point.
- Starsector modding uses Java 17, built with Ant. Dependencies: LunaLib, LazyLib, MagicLib.

## Constraints

- **LunaLib API**: All settings UI must work within LunaLib's capabilities — tabs, input types, and listener system
- **Compatibility**: Must not break existing save games — settings migration from flat to tabbed must preserve user's current values
- **No automated tests**: Testing is manual via Starsector game launcher
- **Starsector modding**: Java 17, Ant build, no dependency management beyond JARs

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Tabs over flat list | Better organization, clearer preset comparison | — Pending |
| Radio-style preset checkboxes | Only one preset active at a time, intuitive UX | — Pending |
| Custom overrides preset | Users want fine-grained control without losing preset as baseline | — Pending |
| Persist custom values | Switching presets shouldn't destroy custom work | — Pending |
| Keep in-game faction dialog | Two access points better than one, different contexts | — Pending |

---
*Last updated: 2026-02-21 after initialization*
