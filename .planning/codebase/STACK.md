# Technology Stack

**Analysis Date:** 2026-02-21

## Languages

**Primary:**
- Java 17 - All mod source code compiled with Java 17 target (build.xml: source="17" target="17")
- CSV - Configuration files for settings and hull mods
- JSON - Mod metadata, settings configuration

## Runtime

**Environment:**
- Starsector 0.98a-RC8 - Game engine that hosts the mod

**Java Distribution:**
- OpenJDK 25.0.2 - Development environment (system has modern JDK installed, though Starsector bundles its own runtime)

## Build System

**Build Tool:**
- Apache Ant - Used for compilation, jarring, and artifact creation
- `build.xml` - Defines three primary targets: `compile`, `jar`, and full `build` (clean + compile + jar)

**Build Process:**
- Source directory: `src/`
- Build output: `build/` (intermediate class files)
- JAR output: `jars/SmallerSector.jar`
- Clean command: `ant clean` (removes build/)
- Full build command: `ant build` (clean + compile + jar)

## Core Starsector API

**Starfarer API:**
- Package: `com.fs.starfarer.api.*`
- Scope: Base mod plugin system, campaign API, combat API, UI API
- Key interfaces used:
  - `BaseModPlugin` - Main entry point for all mods (extended by `SmallerSectorModPlugin`)
  - `BaseCampaignEventListener` - Base class for campaign listeners
  - `EveryFrameScript` - Per-frame update hook (used by `PlayerFleetMonitor`)
  - `IntelInfoPlugin` / `BaseIntelPlugin` - Intel panel system (used by `FactionManagerIntel`)
  - `InteractionDialogPlugin` - Dialog system (used by `FactionManagerDialog`)

**Core Classes:**
- `Global` - Singleton access to game state (sector, settings, logger)
- `CampaignFleetAPI` - Fleet data and manipulation
- `MarketAPI` - Market/trading system
- `FleetMemberAPI` - Individual ship in fleet
- `ShipHullSpecAPI` - Ship hull specifications
- `ShipVariantAPI` - Ship variant with equipped modules
- `HullSize` enum - Ship size categories (FRIGATE, DESTROYER, CRUISER, CAPITAL_SHIP)

**Listener System:**
- `ListenerManagerAPI` - Registers campaign event listeners
- `EconomyTickListener` - Fires on economy ticks (used by `MarketInterceptor`)
- `DiscoverEntityListener` - Fires when derelicts discovered (used by `DerelictInterceptor`)
- `FleetEventListener` - Fires on fleet events (used by `PlayerFleetMonitor`)
- `ColonyInteractionListener` - Colony/market interaction (used by `FactionManagerDialog`)

## Logging

**Log4j:**
- Version: 1.2.9
- Used throughout codebase via `org.apache.log4j.Logger`
- Logger instance obtained from Starsector API: `Global.getLogger(ClassName.class)`
- All major lifecycle events logged to Starsector console

## Classpath Dependencies (Via build.xml)

**Starsector Core JARs** (located at `~/Games/starsector/`):
- `starfarer.api.jar` - Core Starsector API
- `lwjgl.jar` - OpenGL graphics library (LWJGL)
- `lwjgl_util.jar` - LWJGL utilities
- `log4j-1.2.9.jar` - Logging framework
- `json.jar` - JSON parsing/manipulation

**External Mods (dependency JARs):**
- Loaded from `${starsector.dir}/mods/` directory structure

## Configuration

**Environment:**
- Starsector installation path: Hardcoded to `${user.home}/Games/starsector/`
- Mods directory: `${starsector.dir}/mods/`
- Mod-specific directories in build:
  - `${mods.dir}/LunaLib/jars/` - LunaLib library JARs
  - `${mods.dir}/LazyLib/jars/` - LazyLib library JARs
  - `${mods.dir}/MagicLib/jars/` - MagicLib library JARs

**Build Configuration:**
- `build.xml` - Single build file with properties for all paths
- No `pom.xml` or Gradle files - pure Ant-based build
- Compilation includes debug symbols: `debug="true"`
- Classpath resolved at compile-time via `<path id="compile.classpath">`

**Runtime Configuration:**
- No system environment variables required
- All configuration through in-game LunaLib UI (F3 in campaign)
- Settings stored in game save state via LunaLib backend

## Platform Requirements

**Development:**
- Java 17 or later (for compilation)
- Apache Ant (for building)
- Starsector installation at `~/Games/starsector/`
- Mods: LunaLib, LazyLib, MagicLib

**Production (In-Game):**
- Starsector 0.98a-RC8 or compatible
- LunaLib mod installed (required dependency, verified in `onApplicationLoad()`)
- MagicLib mod installed (required dependency, declared in `mod_info.json`)
- LazyLib mod optional (used for JSON utilities)

## Mod Metadata

**mod_info.json:**
- Mod ID: `smallersector`
- Version: `1.0.0`
- Main plugin class: `smallersector.SmallerSectorModPlugin`
- JAR location: `jars/SmallerSector.jar`
- Game version: `0.98a-RC8`

## Testing

**Test Framework:**
- None - No automated test infrastructure
- Manual testing via Starsector game launcher
- No test directories or testing JARs in classpath

---

*Stack analysis: 2026-02-21*
