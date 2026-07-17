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

This prioritizes role and manufacturer identity—such as Low Tech or XIV designs—while retaining a same-faction fallback when no close match exists.

### Operating Cost Multipliers

Player-owned cruisers and capitals have configurable operating costs:

- Crew requirements
- Supply consumption
- Fuel consumption

### Build Cost Multipliers (In Development)

The settings remain reserved for this feature, but the unsafe experimental reflection path is disabled. Production-cost and production-time multipliers still need to be implemented and verified.

### D-Mod Penalties

Player-built or acquired cruisers/capitals receive automatic D-mods, representing the difficulty of maintaining such complex vessels. Ships delivered directly to storage receive this one-time penalty when first withdrawn.

### Planned

- Working production-cost and production-time multipliers
- Salvage and ship-recovery chance multipliers

Blueprint replacement is not planned.

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
| Build cost multiplier (planned) | 1.5x | 2.0x |
| D-mods on acquisition | 2 | 3 |

Build-cost values are reserved for the planned feature and are not currently applied.

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
| Build cost multiplier (planned) | 3.0x | 5.0x |
| D-mods on acquisition | 3 | 5 |

Build-cost values are reserved for the planned feature and are not currently applied.

### Custom

Use the individual sliders to define your own experience.

---

## Faction Blacklist

Blacklisted factions are **exempt from ship replacement** - their fleets and markets remain unchanged.

### Managing the Blacklist

**In-Game (Recommended):** Open the Intel screen → Missions tab → "Smaller Sector - Faction Manager". This shows all loaded factions and lets you toggle custom entries. Defaults supplied by the active Sirix preset are shown read-only.

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
| Random Assortment | Abyssals (enumerated variants) | `rat_abyssals` family |
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
| **Markets/Shops** | On stock generation/refresh | Each procedural sale ship is checked once, even if it survives a later refresh |
| **Derelicts** | Before visibility | New procedural wrecks are checked once |
| **Player Fleet** | On acquisition | D-mods and cost modifiers applied |

## What Is NOT Affected

- **Existing player ships** - Not replaced and receive no retroactive acquisition D-mods; operating-cost settings still apply
- **Player storage/free-transfer cargo** - Never considered market sale stock
- **Unique/named ships** - Story-critical ships are preserved
- **Blacklisted factions** - Configure via settings or in-game manager
- **Already-processed entities** - Ships won't re-roll on save/load

---

## Requirements

- Starsector 0.98a-RC8
- [LunaLib](https://fractalsoftworks.com/forum/index.php?topic=25658.0)
- [LazyLib](https://fractalsoftworks.com/forum/index.php?topic=5444.0)
- [MagicLib](https://fractalsoftworks.com/forum/index.php?topic=13718.0)

---

## Installation

1. Download and extract to your `mods` folder
2. Enable in the Starsector launcher
3. Configure via LunaLib (`F3` in campaign or main menu)
4. Start a **new game** for full effect (works on existing saves but won't affect already-spawned fleets)

---

## Compatibility

- **Ship packs**: Supported through faction role/design-type matching; unusual hull or variant definitions may remain unchanged
- **Fleet mods**: May conflict with mods that heavily modify fleet spawning
- **Vanilla files**: None modified - pure scripted mod

---

## Technical Notes

- Uses `FleetSpawnListener` for NPC fleets
- Uses market-open and economy listeners for natural stock refreshes
- Uses discovery events plus a bounded current-location scan for derelicts
- Replacement decisions are persistently marked or tracked to prevent re-rolling on save/load
- Active behavior settings are read from LunaLib at runtime
