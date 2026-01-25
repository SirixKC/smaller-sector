# Production Time Multipliers Design

## Overview

Add configurable production time multipliers to make cruisers and capitals take longer to build, limiting their acquisition rate across all factions.

## Features

### Global Production Multiplier
- Affects ALL ship production (frigates through capitals)
- Uses `Global.getSettings().setFloat("productionCapacityPerSWUnit", original / globalMult)`
- Fully mod-compatible (standard game settings API)

### Per-Size Production Multipliers
- Additional multipliers for cruisers and capitals specifically
- Applied via `ItemInProductionAPI.setBuildDelay()` when ships are queued
- Stacks with global multiplier: `finalTime = baseTime * globalMult * sizeMult`

## Settings

### LunaSettings Entries

| Field ID | Name | Type | Default | Min | Max |
|----------|------|------|---------|-----|-----|
| globalProductionMult | Global Production Time Multiplier | Double | 1.0 | 1.0 | 10.0 |
| cruiserProductionMult | Cruiser Production Time Multiplier | Double | 2.0 | 1.0 | 10.0 |
| capitalProductionMult | Capital Production Time Multiplier | Double | 3.0 | 1.0 | 10.0 |

### Preset Values

| Preset | Global | Cruiser | Capital |
|--------|--------|---------|---------|
| Vanilla | 1.0x | 1.0x | 1.0x |
| Sirix Recommended | 1.0x | 2.0x | 3.0x |
| Sirix Hardcore | 2.0x | 4.0x | 6.0x |

## Implementation

### New Class: ProductionTimeModifier.java

An `EveryFrameScript` that:
1. Runs every frame to catch new production items immediately
2. Iterates all factions' `FactionProductionAPI` queues
3. For each ship in production:
   - Check if already modified (track via Set or item memory)
   - Get hull size from `ItemInProductionAPI.getShipSpec()`
   - If cruiser/capital, calculate new delay: `baseBuildDelay * globalMult * sizeMult`
   - Call `setBuildDelay(newDelay)`
   - Mark as modified

### Integration Points

- `SmallerSectorModPlugin.onGameLoad()`: Register the EveryFrameScript
- `Settings.java`: Add getter methods for new multipliers
- `PresetListener.java`: Add preset values for new settings

## Player Experience

- **Production picker dialog**: Shows base build time (limitation of game UI)
- **Production queue**: Shows correct multiplied time immediately after queuing
- **All factions affected**: Both player colonies and NPC faction production

## Mod Compatibility

- Global multiplier: Uses standard `Global.getSettings()` API
- Per-size multiplier: Non-invasive monitoring of production queues
- Does not replace or extend any industries
- Works with mods that add custom industries or modify production

## Known Limitations

1. Production picker shows base time, not multiplied time
2. Multiplier applied after queuing, not before (unavoidable without deep UI hooks)
