# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Smaller Sector is a **Starsector mod** (Java 17) that makes cruisers and capital ships rarer and costlier to maintain. It replaces large ships in NPC fleets, markets, and derelicts with smaller alternatives based on configurable probabilities.

**Dependencies:** LunaLib (settings UI), LazyLib (utilities), MagicLib (reflection for field modification)

## Build

Uses Apache Ant. Starsector and mod jars must be at `~/Documents/Games/starsector/`.

```bash
ant build      # clean + compile + jar → jars/SmallerSector.jar
ant compile    # compile only
ant clean      # remove build/
```

No automated tests exist. Testing is manual via the Starsector game launcher.

## Architecture

Event-driven listener system registered by the main plugin entry point:

**Entry point:** `SmallerSectorModPlugin` — registers all listeners in two phases:
- `onApplicationLoad()`: stores original hull base values, applies build cost multipliers, registers PresetListener
- `onGameLoad()`: registers fleet/market/derelict listeners, processes existing game state, adds intel UI

**Core processing pipeline:**
- `FleetSpawnListener` → `FleetInterceptor.processFleet()` — replaces cruisers/capitals in NPC fleets at spawn
- `MarketInterceptor` — replaces large ships in submarket inventories (on economy ticks and player market interaction); uses timestamp-based tracking in persistent data to avoid re-rolling on save/load
- `DerelictInterceptor` — replaces derelict ships on discovery
- `PlayerFleetMonitor` (EveryFrameScript) — applies cost hull mod and D-mods to player-acquired large ships

**Ship replacement logic:**
- `ShipReplacer` — probability-based decision to replace a ship and what size to replace it with; primary gate with `isStation()` check
- `RoleMatcher` — 9-priority matching system that tries to preserve design type (Low Tech/High Tech/Midline/XIV) and role (carrier/phase/combat/civilian), progressively relaxing constraints; filters station hulls from candidates

**Cost system:**
- `CostModifier` — operating cost multipliers applied via the `SmallerSectorCostMod` hull mod (crew, supplies, fuel)
- `BaseValueModifier` — build cost multipliers applied via MagicLib reflection on obfuscated `baseValue` field; caches field name after discovery
- `DmodApplicator` — applies D-mods to player-acquired cruisers/capitals based on settings

**UI:**
- `FactionManagerIntel` — intel panel entry for accessing the faction blacklist manager
- `FactionManagerDialog` — in-game dialog for toggling per-faction ship replacement

**Configuration:**
- `Settings` — static methods wrapping LunaLib settings reads; supports presets (Vanilla/Sirix Recommended/Sirix Hardcore/Custom)
- `PresetListener` — syncs hardcoded preset values with LunaLib UI sliders on change
- Faction blacklist configured via LunaLib; defaults exclude AI/special factions

**Station filtering:**
Stations (orbital defense platforms, pirate/Luddic Path colonies) are excluded from all replacement pipelines. Stations can have `HullSize.CAPITAL_SHIP` (the `STATION` hull size only exists in combat), so filtering uses `FleetMemberAPI.isStation()` where fleet members are available, `CampaignFleetAPI.isStationMode()` to skip station fleets entirely, and `ShipTypeHints.STATION` for hull spec-level checks.

**State tracking patterns:**
- Variant tags (`smallersector_dmods_applied`, `smallersector_derelict_processed`) prevent re-processing ships
- Memory flags skip story/scripted fleets
- Persistent data map tracks submarket processing timestamps

## Issue Tracking

This project uses `bd` (beads) for issue tracking. See AGENTS.md for workflow.
