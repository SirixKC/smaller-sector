# External Integrations

**Analysis Date:** 2026-02-21

## Mod Frameworks

**LunaLib:**
- Package: `lunalib.*`
- Purpose: Settings UI and persistent configuration system
- Version: Specified in `mod_info.json` as required dependency
- Usage:
  - `LunaSettings` class in `Settings.java` - All configuration reads via `LunaSettings.getString()`, `LunaSettings.getInt()`, `LunaSettings.getDouble()`
  - Settings listener registration: `LunaSettings.addSettingsListener(new PresetListener())`
  - Configuration stored in game save via LunaLib backend
  - In-game access: F3 menu to modify settings
- Configuration file: `data/config/LunaSettings.csv` - Defines all UI fields, sliders, radio buttons, and text descriptions
- Classes using LunaLib:
  - `Settings.java` - Reads preset selection and all numerical parameters
  - `PresetListener.java` - Listens for preset changes and syncs UI

**MagicLib:**
- Package: `org.magiclib.*`
- Purpose: Reflection utilities for accessing obfuscated Starsector fields
- Version: Declared in `mod_info.json` as required dependency
- Usage: `ReflectionUtils` in `BaseValueModifier.java`
  - Bypasses Java security restrictions to modify private `baseValue` field on `ShipHullSpecAPI`
  - Caches obfuscated field name after first discovery to avoid repeated reflection
  - Allows build cost multiplier application without source-level access to game code
- Classes using MagicLib:
  - `BaseValueModifier.java` - Applies cost multipliers via reflection

**LazyLib:**
- Package: `org.lazywizard.lazylib.*`
- Purpose: Utility library for JSON and data operations
- Version: Optional dependency (not enforced in `mod_info.json`)
- Usage:
  - `JSONUtils` in code for potential JSON parsing
  - Never directly instantiated in provided source (imports present but unused in visible code)

## Game State & APIs

**Starsector Campaign System:**
- Fleet system: Accesses and modifies `CampaignFleetAPI` instances
- Market/economy system: Monitors `MarketAPI` and `SubmarketAPI` for inventory changes
- Fleet member API: Reads/modifies `FleetMemberAPI` (individual ships)
- Derelict system: Integrates with `DerelictShipEntityPlugin` to intercept derelict discovery

**Ship Data System:**
- Hull specifications: Reads `ShipHullSpecAPI` for ship size, roles, and costs
- Variants: Modifies `ShipVariantAPI` to add hull mods and D-mods
- D-mod system: Uses `DModManager` to apply temporary/permanent modifications
- Hull mods: Adds `SmallerSectorCostMod` via variant API

## Event Systems

**Listener Infrastructure:**
- Fleet event listener system: `FleetEventListener` - Detects when ships are acquired by player
- Economy tick listener: `EconomyTickListener` - Fires on regular economy updates to refresh market inventories
- Derelict discovery: `DiscoverEntityListener` - Fires when derelict is discovered
- Transient listeners: `addTransientListener()` for fleet spawn events
- Transient scripts: `addTransientScript()` for per-frame updates

**Classes Implementing Listeners:**
- `FleetSpawnListener.java` - Implements campaign event listener for fleet spawns
- `MarketInterceptor.java` - Implements `EconomyTickListener` and `ColonyInteractionListener`
- `DerelictInterceptor.java` - Implements `DiscoverEntityListener`
- `PlayerFleetMonitor.java` - Implements `EveryFrameScript` and `FleetEventListener`

## Data Storage

**Game Save State:**
- No external database - All state persists in Starsector game save files
- LunaLib handles settings serialization/deserialization
- Variant tags used for tracking processed ships: `smallersector_dmods_applied`, `smallersector_derelict_processed`
- Fleet memory flags prevent re-processing of scripted/story fleets
- Timestamp-based tracking in persistent data maps for submarket processing

**Configuration Files (In-Game):**
- Settings CSV: `data/config/LunaSettings.csv` - LunaLib reads this to build UI and define settings schema
- Hull mods CSV: `data/hullmods/hull_mods.csv` - Defines `SmallerSectorCostMod` hull mod metadata

## Campaign Data Integration

**Faction System:**
- Reads faction IDs from Starsector's faction registry
- Maintains hardcoded blacklist of ~25 mod-incompatible factions (AI, special, derelict factions)
- User-configurable faction blacklist via LunaLib settings UI
- Checks `FactionAPI.getId()` and compares against blacklist before replacing ships

**Fleet Member Queries:**
- Checks `FleetMemberAPI.isStation()` to exclude stations from replacement
- Reads `FleetMemberAPI.getHullSpec()` for ship class and role info
- Accesses `FleetMemberAPI.getVariant()` to add hull mods and D-mods
- Uses `FleetMemberAPI.getFleetData()` for crew requirements

**Market/Submarket System:**
- Iterates `MarketAPI.getSubmarketsCopy()` to find ship inventories
- Accesses `SubmarketAPI.getCargo()` to inspect and replace inventory items
- Listens to economy ticks via `MarketAPI` to refresh periodically
- Tracks market processing timestamps to avoid re-rolling on save/load

## UI Integration

**Intel System:**
- Extends `BaseIntelPlugin` in `FactionManagerIntel.java`
- Registered via `IntelManager.addIntel()` to appear in Missions tab
- Provides entry point to `FactionManagerDialog`

**Dialog System:**
- Extends `InteractionDialogPlugin` in `FactionManagerDialog.java`
- Uses `InteractionDialogAPI` for UI layout and button/text field creation
- Implements `ColonyInteractionListener` for colony market interaction hooks
- Provides UI for toggling per-faction blacklist status

**UI Components (LunaLib):**
- LunaLib provides styled text fields, sliders, radio buttons, headers
- Used in `FactionManagerDialog` for interactive blacklist editing
- Components: `LunaUITextFieldWithSlider`, `LunaUITextField`, `LunaUIBaseElement`

## Logging & Monitoring

**Log Output:**
- All major operations logged via Log4j to Starsector console
- Entry points: `onApplicationLoad()`, `onGameLoad()`
- Processing steps: Market processing, fleet processing, derelict processing
- Listener registration confirmation
- Error conditions: Missing LunaLib, null sector/listener manager

**Error Handling:**
- Explicit LunaLib availability check in `onApplicationLoad()`
- Null checks on Global API objects before operations
- No external error tracking - errors logged locally only

## External Service Integrations

**None detected:**
- No remote API calls
- No cloud storage integration
- No analytics or telemetry
- No social features
- No payment/in-app purchase systems

## Webhooks & Callbacks

**None:**
- Mod is entirely event-driven via Starsector's listener system
- No outbound webhooks
- No inbound webhook endpoints

## Build & Deployment

**Artifact Output:**
- `jars/SmallerSector.jar` - Built JAR file
- Deployed to Starsector mods directory: `~/Games/starsector/mods/smallersector/`
- No external repository or artifact server integration

**Mod Loading:**
- Starsector mod loader reads `mod_info.json` for plugin class
- Plugin class instantiated and lifecycle methods invoked by Starsector engine
- No custom classloading or package management

---

*Integration audit: 2026-02-21*
