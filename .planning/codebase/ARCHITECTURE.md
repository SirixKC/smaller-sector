# Architecture

**Analysis Date:** 2025-02-21

## Pattern Overview

**Overall:** Event-driven listener system with probabilistic ship replacement pipeline

**Key Characteristics:**
- Event-driven: Core logic triggered by Starsector campaign listeners (fleet spawn, market economy ticks, entity discovery)
- Probabilistic: All replacement decisions are probability-based with configurable thresholds
- Role-preservation: Attempts to match hull design types and functional roles when selecting replacements
- Multi-phase initialization: Separation between application load (cost modifiers) and game load (listener registration)

## Layers

**Entry Point & Plugin Management:**
- Purpose: Orchestrate mod initialization, listener registration, and lifecycle hooks
- Location: `src/smallersector/SmallerSectorModPlugin.java`
- Contains: Main plugin class extending `BaseModPlugin`
- Depends on: All other components
- Used by: Starsector game engine

**Configuration & Settings:**
- Purpose: Centralize all user-configurable values with preset support
- Location: `src/smallersector/Settings.java`
- Contains: Static methods wrapping LunaLib settings reads; preset logic (Vanilla/Recommended/Hardcore/Custom)
- Depends on: LunaLib
- Used by: Ship replacement logic, cost modifier systems, UI components

**Ship Replacement Pipeline:**
- Purpose: Decide what ships to replace and find suitable replacements
- Location: `src/smallersector/ShipReplacer.java`, `src/smallersector/RoleMatcher.java`
- Contains: Probability-based decision logic (`ShipReplacer`); 9-priority matching algorithm (`RoleMatcher`)
- Depends on: Settings; Starsector API hull/fleet data
- Used by: All interceptor/listener classes

**Event Listeners:**
- Purpose: Hook into Starsector game events and trigger ship replacement
- Location:
  - `src/smallersector/FleetSpawnListener.java` - Fleet spawn events
  - `src/smallersector/MarketInterceptor.java` - Market/economy ticks and player interactions
  - `src/smallersector/DerelictInterceptor.java` - Derelict discovery
- Contains: Listener implementations, event-specific processing logic
- Depends on: Ship replacement pipeline
- Used by: Starsector listener manager

**Player Fleet Monitoring:**
- Purpose: Apply cost hull mod and D-mods to player-acquired cruisers/capitals
- Location: `src/smallersector/PlayerFleetMonitor.java`
- Contains: Transient script that checks player fleet every second
- Depends on: CostModifier, DmodApplicator
- Used by: Plugin on game load

**Cost & Value Systems:**
- Purpose: Apply operating cost and build cost multipliers
- Location: `src/smallersector/CostModifier.java`, `src/smallersector/BaseValueModifier.java`
- Contains: Multiplier application logic (operating costs via hull mod; build costs via reflection)
- Depends on: Settings; MagicLib (for reflection)
- Used by: Hull mod, plugin initialization, preset listener

**D-Mod Application:**
- Purpose: Apply cosmetic/defect mods to player-acquired large ships
- Location: `src/smallersector/DmodApplicator.java`
- Contains: Logic to add D-mods based on hull size and settings
- Depends on: Settings; Starsector DModManager
- Used by: PlayerFleetMonitor, plugin

**UI & Configuration:**
- Purpose: Player-facing settings and faction blacklist management
- Location:
  - `src/smallersector/FactionManagerIntel.java` - Intel panel entry
  - `src/smallersector/FactionManagerDialog.java` - Interactive faction blacklist dialog
  - `src/smallersector/PresetListener.java` - Preset change synchronization
- Contains: UI components, dialog logic, preset sync
- Depends on: Settings, LunaLib, LazyLib
- Used by: Player interaction in campaign

**Hull Modification:**
- Purpose: Apply operating cost multipliers at combat time
- Location: `src/smallersector/hullmods/SmallerSectorCostMod.java`
- Contains: Hull mod implementation that calls CostModifier
- Depends on: CostModifier, Starsector hull mod API
- Used by: Starsector combat/ship creation system

## Data Flow

**Ship Replacement (Fleet Spawn):**

1. `FleetSpawnListener.reportFleetSpawned()` - Starsector calls on fleet spawn
2. Check for scripted/story flags (memory), skip if present
3. Call `FleetInterceptor.processFleet()`
4. For each fleet member, call `ShipReplacer.tryReplace()`
5. `ShipReplacer` decides if replacement happens based on:
   - Station check: Skip if `isStation()`
   - Faction blacklist check: Skip if blacklisted
   - Probability roll against Settings thresholds (cruiserToFrigate, cruiserToDestroyer, etc.)
6. If replacing, determine target hull size and call `RoleMatcher.findReplacement()`
7. `RoleMatcher` executes 9-priority matching algorithm:
   - Priority 1-4: Target size with role/design type matching
   - Priority 5-8: Alternate size (frigate↔destroyer swap)
   - Priority 9: No replacement found
8. Create new `FleetMemberAPI` with replacement hull
9. Remove original from fleet, add replacement

**Ship Replacement (Market):**

1. `MarketInterceptor` listens on `EconomyTickListener` and `ColonyInteractionListener`
2. On economy tick or player opens market: `processMarket()`
3. For each submarket, check if should process using timestamp tracking:
   - If never processed before → process
   - If stock has refreshed since last processing → process
4. Same replacement logic as fleet spawn applied to `submarket.getCargo().getMothballedShips()`
5. Mark submarket as processed with current timestamp

**Ship Replacement (Derelict):**

1. `DerelictInterceptor.reportEntityDiscovered()` - Starsector calls when player discovers derelict
2. Extract `DerelictShipData` from entity plugin
3. Check if already processed (skip if tag present)
4. Determine faction and check blacklist
5. Same probability roll as fleet spawn
6. Use `RoleMatcher` to find replacement
7. Modify derelict `variantId` and clear cached variant
8. Tag entity as processed

**Operating Cost Application:**

1. When cruiser/capital added to player fleet:
   - `PlayerFleetMonitor` detects new ship each second
   - Adds `smallersector_cost_modifier` hull mod if not present
2. At combat time:
   - `SmallerSectorCostMod.applyEffectsBeforeShipCreation()` called
   - `CostModifier.applyToStats()` applies multipliers to crew, supplies, fuel based on hull size

**Build Cost Application:**

1. `SmallerSectorModPlugin.onApplicationLoad()`:
   - `BaseValueModifier.storeOriginalValues()` - Cache all cruiser/capital base values
   - `BaseValueModifier.applyMultipliers()` - Use MagicLib reflection to modify `baseValue` field

**State Management:**

- **Variant Tags** (`smallersector_dmods_applied`): Track ships that have had D-mods applied, prevent re-application
- **Entity Tags** (`smallersector_derelict_processed`): Track derelicts that have been processed, prevent re-rolling on reload
- **Persistent Data Map** (sector): Track submarket processing timestamps to avoid re-rolling on save/load
- **Settings Cache** (LunaLib): User configuration stored in JSON, wrapped by Settings class

## Key Abstractions

**ShipReplacer:**
- Purpose: Pure probability-based decision whether to replace a ship and what target size to use
- Files: `src/smallersector/ShipReplacer.java`
- Pattern: Static methods with no mutable state; takes original `FleetMemberAPI` and faction, returns replacement or null
- Single responsibility: Probability calculation and hull size selection

**RoleMatcher:**
- Purpose: 9-priority matching algorithm to find best replacement from faction ship pool
- Files: `src/smallersector/RoleMatcher.java`
- Pattern: Complex cascading filters; reduces constraints progressively if no matches found
- Design type matching: Uses hull manufacturer field to identify "Low Tech", "High Tech", "Midline", etc.
- Role detection: Analyzes fighter bays, phase hints, hull tags, cargo capacity to categorize ships

**Station Filtering:**
- Purpose: Ensure stations (orbital platforms) are never replaced
- Pattern: Multi-level filtering
  - `FleetMemberAPI.isStation()` in fleet members (primary gate)
  - `CampaignFleetAPI.isStationMode()` to skip station fleets entirely
  - `ShipTypeHints.STATION` in hull specs for spec-level checks
  - `isDefaultDHull()` filter to exclude default derelict hulls

**Persistent Tracking:**
- Purpose: Prevent re-rolling ships on save/load cycles
- Pattern: Variant tags for D-mods and derelict processing; persistent data map for markets with timestamp-based refresh detection

## Entry Points

**SmallerSectorModPlugin (Main):**
- Location: `src/smallersector/SmallerSectorModPlugin.java`
- Triggers:
  - `onApplicationLoad()` - Called once when mod JAR is loaded (before game starts)
  - `onGameLoad(boolean newGame)` - Called when game world is loaded
- Responsibilities:
  - Store original hull base values and apply build cost multipliers
  - Register all listeners (fleet spawn, market, derelict)
  - Register UI intel item
  - Process existing game state on load

**FleetSpawnListener:**
- Location: `src/smallersector/FleetSpawnListener.java`
- Triggers: `reportFleetSpawned(CampaignFleetAPI)` - Called by Starsector when NPC fleet spawns
- Responsibilities: Process newly spawned NPC fleets, skip scripted/mission fleets

**MarketInterceptor:**
- Location: `src/smallersector/MarketInterceptor.java`
- Triggers:
  - `reportEconomyTick()` - Called periodically during campaign
  - `reportPlayerOpenedMarketAndCargoUpdated()` - Called when player opens market
- Responsibilities: Replace large ships in submarket inventories with timestamp-based refresh detection

**DerelictInterceptor:**
- Location: `src/smallersector/DerelictInterceptor.java`
- Triggers: `reportEntityDiscovered()` - Called when player discovers derelict entity
- Responsibilities: Replace derelict cruisers/capitals before player sees them

**PlayerFleetMonitor:**
- Location: `src/smallersector/PlayerFleetMonitor.java`
- Triggers: `advance(float)` - Called every frame (checks every 1 second)
- Responsibilities: Apply cost hull mod and D-mods to newly acquired player ships

## Error Handling

**Strategy:** Defensive null checks and try-catch blocks; fail gracefully, log warnings

**Patterns:**
- Null safety on all entity/fleet/variant access
- Try-catch around MagicLib reflection with `searchFailed` flag to prevent repeated attempts
- Reflection field name caching to avoid repeated searches
- Graceful degradation when optional components (derelict plugin, submarket plugin) unavailable
- Log levels: INFO for normal flow, WARN for missing fields, ERROR for exceptions

## Cross-Cutting Concerns

**Logging:**
- Framework: Apache Log4j 1.2.9 (Starsector standard)
- Location: Each class has static logger `private static final Logger log = Global.getLogger(ClassName.class);`
- Pattern: INFO on major operations (listener registration, fleet processing), DEBUG for field lookups, WARN/ERROR on failures
- File: Captured by Starsector's log system

**Validation:**
- Null checks on all external inputs (fleet, market, entity, hull spec)
- Hull size validation using `HullSize` enum
- Faction ID blacklist check before any processing
- Station type check using multiple methods (isStation(), ShipTypeHints, hints)

**Configuration:**
- Settings read via LunaLib wrapper in `Settings.java`
- Preset system: Hardcoded values for Vanilla/Recommended/Hardcore; Custom uses individual sliders
- Dynamic: Changes to presets sync via `PresetListener` watching LunaLib's `settingsChanged()` event
- Faction blacklist: Managed via `FactionManagerDialog`, persisted in LunaSettings JSON

