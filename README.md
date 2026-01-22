# Smaller Sector

A Starsector mod that creates a frigate/destroyer-focused experience by making cruisers and capital ships rarer and harder to maintain.

## Features

### Ship Replacement
Cruisers and capitals are randomly replaced with smaller ships based on configurable percentages:

- **Cruisers** → Can become frigates or destroyers
- **Capitals** → Can become frigates, destroyers, or cruisers

### Smart Replacement Matching

When replacing a ship, the mod uses a priority-based matching system to find the most appropriate replacement:

| Priority | Criteria | Example |
|----------|----------|---------|
| 1 | Same faction + size + role + design type | XIV Onslaught → Enforcer (XIV) |
| 2 | Same faction + size + role | XIV Onslaught → Low Tech Enforcer |
| 3 | Same faction + size + design type | XIV Onslaught → XIV Brawler |
| 4 | Same faction + size | XIV Onslaught → Any Hegemony destroyer |
| 5-8 | Alternate size (frigate↔destroyer) | Same progression as above |
| 9 | No replacement found | Ship stays as original |

**Examples:**

| Original Ship | Best Replacement | Why |
|---------------|------------------|-----|
| Hegemony Dominator (Low Tech) | Enforcer (Low Tech) | Matches faction, role (combat), and design type |
| Hegemony Onslaught (XIV) | Enforcer (XIV) | XIV stays XIV - preserves rarity |
| Tri-Tachyon Paragon (High Tech) | Medusa (High Tech) | High Tech carrier → High Tech combat if no carriers |
| Persean Conquest (Midline) | Hammerhead (Midline) | Midline carrier → Midline combat |
| Pirate Atlas Mk.II | Any Pirate destroyer | Pirates lack design type consistency |

This ensures Low Tech fleets stay Low Tech, XIV Battlegroup ships remain rare, and faction identity is preserved.

### Operating Cost Multipliers
Cruisers and capitals that survive replacement have increased operating costs:
- Crew requirements
- Supply consumption
- Fuel consumption

### Build Cost Multipliers
Purchase prices and colony production costs are multiplied for larger ships.

### D-Mod Penalties
Player-built or acquired cruisers/capitals receive automatic D-mods, representing the difficulty of maintaining such complex vessels.

---

## Presets

All settings are configurable via **LunaLib** (press `F3` in campaign). Choose a preset or use Custom for full control.

### Vanilla (Disabled)
No changes - the mod effectively does nothing. Useful for temporarily disabling without removing.

| Setting | Value |
|---------|-------|
| All replacement chances | 0% |
| All cost multipliers | 1.0x |
| All D-mods | 0 |
| Faction blacklist | None |

### Sirix Recommended
Balanced experience - cruisers and capitals are notably rarer but still achievable.

| Setting | Cruiser | Capital |
|---------|---------|---------|
| → Frigate | 30% | 20% |
| → Destroyer | 50% | 40% |
| → Cruiser | — | 25% |
| **Stays original** | **20%** | **15%** |
| Crew multiplier | 1.5x | 2.0x |
| Supply multiplier | 1.5x | 2.0x |
| Fuel multiplier | 1.5x | 2.0x |
| Build cost multiplier | 1.5x | 2.0x |
| D-mods on acquisition | 2 | 3 |

### Sirix Hardcore
Punishing experience - cruisers are rare, capitals are exceptional finds.

| Setting | Cruiser | Capital |
|---------|---------|---------|
| → Frigate | 50% | 40% |
| → Destroyer | 45% | 40% |
| → Cruiser | — | 18% |
| **Stays original** | **5%** | **2%** |
| Crew multiplier | 2.0x | 3.0x |
| Supply multiplier | 3.0x | 4.0x |
| Fuel multiplier | 3.0x | 4.0x |
| Build cost multiplier | 3.0x | 5.0x |
| D-mods on acquisition | 3 | 5 |

### Custom
Use the individual sliders to define your own experience.

---

## Faction Blacklist

Blacklisted factions are **exempt from ship replacement** - their fleets and markets remain unchanged.

### Managing the Blacklist

**In-Game (Recommended):** Open the Intel screen → Missions tab → "Smaller Sector - Faction Manager". This shows all loaded factions and lets you toggle them with one click.

**Via LunaLib:** Enter comma-separated faction IDs in the settings.

### Default Blacklist (Sirix Presets)

The Sirix presets automatically blacklist AI and special factions:

| Mod | Faction | ID |
|-----|---------|-----|
| Vanilla | Remnant | `remnant` |
| Vanilla | Omega | `omega` |
| Vanilla | Derelict | `derelict` |
| Hazard Mining Inc. | Nightmarish Horror | `hmi_nightmare` |
| Hazard Mining Inc. | Mess | `mess` |
| Jaydeena's Piracy | ERROR: DATA INVALID | `jdp_deadcomm` |
| Emergent Threats | Threat | `threat` |
| Random Assortment | Abyssals (all variants) | `rat_abyssals*` |
| Secrets of the Frontier | Dreaming Gestalt | `sotf_dreaminggestalt` |
| Elysian Fields | Elysians | `zea_elysians` |
| Elysian Fields | Duskborne | `zea_dusk` |
| Elysian Fields | Dawntide | `zea_dawn` |
| Phase-Warped Omega | Phase-Warped Omega | `khewarpedomega` |
| MagicLib | Bounty Target | `ml_bounty` |
| Nexerelin | Domain Exploration | `nex_derelict` |

---

## What Gets Affected

| System | When | Notes |
|--------|------|-------|
| **NPC Fleets** | On spawn | Checked once, permanent |
| **Markets/Shops** | On game load + economy ticks | Ships for sale are replaced |
| **Derelicts** | On discovery | Replaced when player approaches |
| **Player Fleet** | On acquisition | D-mods and cost modifiers applied |

## What Is NOT Affected

- **Existing fleet members** - Ships already in your fleet stay as-is
- **Unique/named ships** - Story-critical ships are preserved
- **Blacklisted factions** - Configure via settings or in-game manager
- **Already-processed entities** - Ships won't re-roll on save/load

---

## Requirements

- Starsector 0.98a+
- [LunaLib](https://fractalsoftworks.com/forum/index.php?topic=25658.0)
- [LazyLib](https://fractalsoftworks.com/forum/index.php?topic=5444.0)

---

## Installation

1. Download and extract to your `mods` folder
2. Enable in the Starsector launcher
3. Configure via LunaLib (`F3` in campaign or main menu)
4. Start a **new game** for full effect (works on existing saves but won't affect already-spawned fleets)

---

## Compatibility

- **Ship packs**: Fully compatible - uses faction doctrine and role/design type matching
- **Fleet mods**: May conflict with mods that heavily modify fleet spawning
- **Vanilla files**: None modified - pure scripted mod

---

## Technical Notes

- Uses `FleetSpawnListener` for NPC fleets
- Uses `EconomyTickListener` for markets
- Uses `DiscoverEntityListener` for derelicts
- Replacement decisions are tagged to prevent re-rolling on save/load
- All settings read from LunaLib at runtime
