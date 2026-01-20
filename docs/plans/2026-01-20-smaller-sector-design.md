# Smaller Sector - Mod Design Document

**Author:** Sirix
**Date:** 2026-01-20
**Status:** Design Complete - Ready for Implementation

---

## Overview

**Smaller Sector** is a Starsector mod that makes cruisers and capital ships significantly rarer and more impactful. When procedurally generated ships spawn (fleets, markets, salvage, blueprints), cruisers and capitals have a configurable chance to be replaced with smaller ship classes. Additionally, cruisers and capitals have increased operational costs and reduced salvage chances.

**Design Goals:**
- Cruisers and capitals feel rare and precious
- Strategic trade-off: powerful ships vs. logistical burden
- Swarm tactics with destroyers become genuinely competitive
- Story/unique spawns remain untouched - the challenge stays real when it's meant to be

---

## Core Mechanics

### 1. Ship Replacement System

When the game procedurally generates a ship, the mod intercepts and potentially replaces it:

**Cruiser Replacement (default: Sirix's Recommended)**
- → Frigate: 30%
- → Destroyer: 50%
- → Stays Cruiser: 20%

**Capital Replacement (default: Sirix's Recommended)**
- → Frigate: 20%
- → Destroyer: 40%
- → Cruiser: 25%
- → Stays Capital: 15%

**Important:** Capitals that become cruisers are NOT re-checked against cruiser replacement. One roll only.

### 2. Replacement Scope

**Affected (procedurally generated only):**
- Random fleet spawns (patrols, traders, pirates, bounties)
- Market inventory (military, open market, black market)
- Salvage and derelict ships
- Blueprint drops (exploration, salvage, research stations)

**NOT Affected:**
- Story missions and scripted events
- Unique/boss spawns
- Custom fleet spawners from other mods
- Player's existing ships

### 3. Role Matching Algorithm

Replacement ships match the original's role when possible:

**Priority Chain:**
1. Same faction + target size + matching role tags
2. Same faction + target size + any role
3. Same faction + alternate small size + matching role tags
4. Same faction + alternate small size + any role
5. No frigates OR destroyers in faction → keep original

**Role Categories:**
- Combat (line ships, brawlers, fire support)
- Carrier (dedicated carriers, light carriers)
- Hybrid (combat ships with flight decks)
- Utility (freighters, tankers, transports)
- Civilian (non-combat, low-profile)
- Phase (phase cloak ships)

**Never:** Cross-faction replacement. A Hegemony ship always becomes a Hegemony ship.

### 4. Blueprint Replacement

Same logic applies to blueprint drops:
- Cruiser blueprint → chance to become frigate/destroyer blueprint
- Capital blueprint → chance to become frigate/destroyer/cruiser blueprint

**Settings Option:** "Sync with ship settings" checkbox (default: enabled)
- When enabled: Uses same percentages as ship replacement
- When disabled: Separate sliders for blueprint-specific percentages

**Implementation Note:** Sync checkbox writes values to blueprint config - replacement code always reads from blueprint-specific config (no conditional branching).

### 5. Faction Blacklist

Certain factions can be exempted from replacement entirely:
- Ships from blacklisted factions are never replaced
- Cost multipliers and salvage multipliers still apply
- User-configurable via comma-separated faction IDs

**Default:** Empty (user adds as needed)

**Example entries:**
```
abyss, remnants, derelict
```

---

## Cost Multiplier System

Makes cruisers and capitals expensive to operate:

**Affected Stats:**
- Crew requirement
- Supply cost (maintenance + recovery)
- Fuel cost per light-year

**Settings Range:** 0.1x - 10.0x for each

**Defaults (Sirix's Recommended):**
| Stat | Cruiser | Capital |
|------|---------|---------|
| Crew | 1.5x | 2.0x |
| Supply | 1.5x | 2.0x |
| Fuel | 1.5x | 2.0x |

**Applies to:** ALL cruisers and capitals, including blacklisted factions and captured ships.

---

## Salvage Chance Multipliers

Controls ship recovery probability after battle:

**Affects BOTH:**
- Normal recovery chance
- Story point recovery chance

**Settings Range:** 0.1x - 10.0x per hull size

**Defaults (Sirix's Recommended):**
| Hull Size | Multiplier |
|-----------|------------|
| Frigate | 1.0x |
| Destroyer | 1.0x |
| Cruiser | 0.75x |
| Capital | 0.5x |

**UI Accuracy Requirement:** The displayed recovery chance MUST reflect the actual modified chance. If the UI shows 40%, a roll of 39 or below recovers the ship.

**Example with 0.5x Capital multiplier:**
- Base normal chance: 40% → displays and calculates as 20%
- Base story point chance: 80% → displays and calculates as 40%

---

## LunaLib Settings Menu

```
┌─────────────────────────────────────────────────┐
│  SMALLER SECTOR - SETTINGS                      │
├─────────────────────────────────────────────────┤
│  Preset: [Sirix's Recommended ▼]                │
│                                                 │
│  ── CRUISER REPLACEMENT ──                      │
│  → Frigate:    [====30%====]                    │
│  → Destroyer:  [=====50%=====]                  │
│  → Stays:      20% (auto-calculated)            │
│                                                 │
│  ── CAPITAL REPLACEMENT ──                      │
│  → Frigate:    [===20%===]                      │
│  → Destroyer:  [====40%====]                    │
│  → Cruiser:    [===25%===]                      │
│  → Stays:      15% (auto-calculated)            │
│                                                 │
│  ── BLUEPRINTS ──                               │
│  ☑ Sync with ship settings                      │
│  (sliders hidden when synced)                   │
│                                                 │
│  ── COST MULTIPLIERS ── (0.1x - 10x)            │
│  Cruiser Crew:    [==1.5x==]                    │
│  Cruiser Supply:  [==1.5x==]                    │
│  Cruiser Fuel:    [==1.5x==]                    │
│  Capital Crew:    [===2.0x===]                  │
│  Capital Supply:  [===2.0x===]                  │
│  Capital Fuel:    [===2.0x===]                  │
│                                                 │
│  ── SALVAGE CHANCE MULTIPLIERS ── (0.1x - 10x)  │
│  Frigate:     [===1.0x===]                      │
│  Destroyer:   [===1.0x===]                      │
│  Cruiser:     [==0.75x==]                       │
│  Capital:     [==0.5x===]                       │
│                                                 │
│  ── FACTION BLACKLIST ──                        │
│  [                                    ]         │
│  (comma-separated faction IDs)                  │
└─────────────────────────────────────────────────┘
```

**Presets:**
- **Sirix's Recommended** (default) - Values shown above
- **Vanilla** - All replacement at 0%, all multipliers at 1.0x

---

## Technical Architecture

### File Structure

```
SmallerSector/
├── mod_info.json
├── data/
│   └── config/
│       └── settings.json              # Default config values
├── src/
│   └── smallersector/
│       ├── SmallerSectorModPlugin.java    # Main entry, registers listeners
│       ├── FleetInterceptor.java          # Hooks fleet generation
│       ├── MarketInterceptor.java         # Hooks market stock refresh
│       ├── SalvageInterceptor.java        # Hooks salvage/derelict spawns
│       ├── BlueprintInterceptor.java      # Hooks blueprint drops
│       ├── ShipReplacer.java              # Core replacement logic
│       ├── RoleMatcher.java               # Matches ships by role/faction
│       ├── CostModifier.java              # Applies stat multipliers
│       ├── SalvageModifier.java           # Modifies salvage chances + UI
│       └── LunaSettingsManager.java       # LunaLib integration
└── lunalib/
    └── LunaSettings.json              # In-game settings menu definition
```

### Interception Points

| System | Hook Method | Catches |
|--------|-------------|---------|
| Fleet Generation | `FleetInflater` / `FleetFactory` events | Patrols, traders, pirates, bounties |
| Market Inventory | `EconomyTickListener` | Military, open, black market |
| Salvage/Derelicts | Salvage generation managers | Derelict ships, battle salvage |
| Blueprints | `DropGroupManager` / loot callbacks | Research stations, exploration |
| Salvage Chance | `ShipRecoverySpecial` or similar | Post-battle recovery + UI display |

### Processing Timing

All replacements happen at **generation time**, not on access:
- Fleet spawns → processed immediately
- Market restock → processed on economy tick
- Salvage → processed when generated
- No re-rolling on revisit

---

## Save Game Compatibility

| Change Type | When Applied |
|-------------|--------------|
| New fleet spawns | Immediately on next spawn |
| Market restocking | Next economy tick (~1 month) |
| New salvage/derelicts | When generated during exploration |
| Cost multipliers | Immediately |
| Salvage multipliers | Immediately |
| Existing ships | Never retroactively changed |

**Mid-playthrough safe:**
- Adding mod: Only affects new content
- Removing mod: Ships remain as-is, multipliers revert

**No save dependency:** Mod doesn't store custom data in save files.

---

## Mod Compatibility

**Automatic compatibility:**
- Faction mods (new factions auto-detected)
- Ship pack mods (new ships indexed by size/role)
- Economy mods (standard API hooks)

**Potential edge cases:**
- Mods with custom fleet spawners bypassing FleetFactory → those fleets unaffected (intentional)
- Ship stat overhaul mods → cost multipliers stack multiplicatively

**Dependencies:**
- **LunaLib** - Hard requirement for settings menu

**Load order:** No special requirements.

---

## Implementation Notes

### Key Principles

1. **One roll only** - Capitals becoming cruisers skip cruiser check
2. **Faction loyalty** - Never cross-faction replacement
3. **Graceful fallback** - If no valid replacement, keep original
4. **UI honesty** - Displayed chances match actual calculation
5. **Clean config** - Sync checkbox writes values, code doesn't branch

### Edge Cases

- Faction has no destroyers → try frigates
- Faction has no frigates → try destroyers
- Faction has neither → keep original
- Blueprint package → check each blueprint individually
- Replacement sliders exceed 100% → "stays" becomes 0%

---

## Presets Reference

### Sirix's Recommended (Default)

```
Cruiser → Frigate: 30%
Cruiser → Destroyer: 50%
Cruiser → Stays: 20%

Capital → Frigate: 20%
Capital → Destroyer: 40%
Capital → Cruiser: 25%
Capital → Stays: 15%

Blueprint Sync: Enabled

Cruiser Crew/Supply/Fuel: 1.5x
Capital Crew/Supply/Fuel: 2.0x

Salvage Frigate: 1.0x
Salvage Destroyer: 1.0x
Salvage Cruiser: 0.75x
Salvage Capital: 0.5x

Faction Blacklist: (empty)
```

### Vanilla

```
All replacement: 0%
All multipliers: 1.0x
Faction Blacklist: (empty)
```
