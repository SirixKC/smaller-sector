# Codebase Structure

**Analysis Date:** 2025-02-21

## Directory Layout

```
smaller-sector/
├── src/                           # Java source code (Apache Ant compiled to build/)
│   └── smallersector/
│       ├── SmallerSectorModPlugin.java    # Main mod entry point
│       ├── Settings.java                  # Configuration wrapper (LunaLib)
│       ├── ShipReplacer.java              # Probability-based replacement logic
│       ├── RoleMatcher.java               # 9-priority ship matching algorithm
│       ├── FleetSpawnListener.java        # Fleet spawn events
│       ├── FleetInterceptor.java          # Fleet processing logic
│       ├── MarketInterceptor.java         # Market/economy listener
│       ├── DerelictInterceptor.java       # Derelict discovery listener
│       ├── PlayerFleetMonitor.java        # Player fleet monitor script
│       ├── CostModifier.java              # Operating cost multiplier logic
│       ├── BaseValueModifier.java         # Build cost multiplier (reflection)
│       ├── DmodApplicator.java            # D-mod application logic
│       ├── FactionManagerIntel.java       # Settings UI panel
│       ├── FactionManagerDialog.java      # Faction blacklist dialog
│       ├── PresetListener.java            # Preset sync listener
│       └── hullmods/
│           └── SmallerSectorCostMod.java  # Hull mod for operating costs
├── data/                          # Game data files
│   ├── config/
│   │   ├── LunaSettings.csv               # LunaLib settings configuration
│   │   └── settings.json                  # (if exists) Legacy settings
│   └── hullmods/
│       └── hull_mods.csv                  # Hull mod definitions
├── build.xml                      # Apache Ant build configuration
├── mod_info.json                  # Starsector mod metadata
├── README.md                      # User documentation
├── CLAUDE.md                      # This project's Claude Code guidelines
├── AGENTS.md                      # Issue tracking workflow
├── jars/                          # Output JAR (created by ant build)
├── build/                         # Compiled classes (created by ant compile)
├── lunalib/                       # LunaLib local copy (if present)
└── .git/                          # Version control

```

## Directory Purposes

**src/smallersector/:**
- Purpose: All Java source code
- Contains: Plugin entry point, listeners, interceptors, configuration, UI, utility classes
- Key files: `SmallerSectorModPlugin.java` (entry point), `Settings.java` (config), `RoleMatcher.java` (core algorithm)

**data/config/:**
- Purpose: Game data configuration files
- Contains: LunaLib settings definitions (CSV) and settings JSON
- Key files: `LunaSettings.csv` defines all UI sliders and their properties

**data/hullmods/:**
- Purpose: Hull modification definitions
- Contains: CSV with hull mod metadata
- Key files: `hull_mods.csv` registers the `smallersector_cost_modifier` hull mod

**jars/:**
- Purpose: Compiled mod JAR output
- Generated: `SmallerSector.jar` produced by `ant build`
- Committed: No (in .gitignore)

**build/:**
- Purpose: Compiled .class files
- Generated: By `ant compile`
- Committed: No (in .gitignore)

## Key File Locations

**Entry Points:**
- `src/smallersector/SmallerSectorModPlugin.java`: Main plugin class extending `BaseModPlugin`; orchestrates initialization and listener registration

**Configuration:**
- `src/smallersector/Settings.java`: Wrapper around LunaLib settings; provides preset system and individual setting getters
- `data/config/LunaSettings.csv`: LunaLib UI configuration defining all sliders and text fields

**Core Logic:**
- `src/smallersector/ShipReplacer.java`: Probability-based decision to replace a ship and target hull size
- `src/smallersector/RoleMatcher.java`: Complex matching algorithm to find suitable replacement from faction ship pool

**Event Listeners:**
- `src/smallersector/FleetSpawnListener.java`: Hooks fleet spawn events
- `src/smallersector/FleetInterceptor.java`: Fleet processing logic (not a listener itself, called by FleetSpawnListener)
- `src/smallersector/MarketInterceptor.java`: Implements both EconomyTickListener and ColonyInteractionListener
- `src/smallersector/DerelictInterceptor.java`: Hooks derelict discovery events

**Player Fleet Management:**
- `src/smallersector/PlayerFleetMonitor.java`: Transient script that checks player fleet every second
- `src/smallersector/DmodApplicator.java`: Applies D-mods to large ships
- `src/smallersector/CostModifier.java`: Applies operating cost multipliers
- `src/smallersector/hullmods/SmallerSectorCostMod.java`: Hull mod that applies operating costs at combat time

**Cost Systems:**
- `src/smallersector/BaseValueModifier.java`: Modifies hull spec base values using MagicLib reflection (build costs)
- `src/smallersector/CostModifier.java`: Applies multipliers to ship stats (operating costs)

**UI & Configuration:**
- `src/smallersector/Settings.java`: Central settings provider
- `src/smallersector/FactionManagerIntel.java`: Intel panel entry for accessing faction manager
- `src/smallersector/FactionManagerDialog.java`: Interactive dialog for toggling faction blacklist
- `src/smallersector/PresetListener.java`: Watches for preset changes and syncs all individual settings

**Build:**
- `build.xml`: Apache Ant build configuration with classpath references to Starsector jars and mod dependencies

**Metadata:**
- `mod_info.json`: Starsector mod metadata (version, dependencies, main plugin class)

## Naming Conventions

**Files:**
- `SmallerSectorModPlugin.java`: Main plugin (pattern: `[ModName]ModPlugin`)
- `FleetSpawnListener.java`: Listener implementations (pattern: `[EventType]Listener`)
- `MarketInterceptor.java`: Event interceptors (pattern: `[Domain]Interceptor`)
- `PlayerFleetMonitor.java`: Scripts (pattern: `[Domain]Monitor`)
- `CostModifier.java`: Modifier utilities (pattern: `[Domain]Modifier`)
- `SmallerSectorCostMod.java`: Hull mods (pattern: `SmallerSector[Feature]Mod`)

**Packages:**
- `smallersector`: Single main package
- `smallersector.hullmods`: Sub-package for hull modifications

**Classes:**
- PascalCase: `ShipReplacer`, `RoleMatcher`, `DmodApplicator`
- Listener implementations: Suffix `Listener` or `Interceptor`
- Utility classes: Suffix `Modifier`, `Applicator`, `Manager`

**Methods:**
- camelCase: `tryReplace()`, `processFleet()`, `findReplacement()`
- Static utility methods: `tryReplace()`, `processMarket()`, `applyMultipliers()`
- Listener callbacks: `reportFleetSpawned()`, `reportEntityDiscovered()`, `settingsChanged()`

**Constants:**
- UPPER_SNAKE_CASE: `HULLMOD_ID`, `PROCESSED_TAG`, `CHECK_INTERVAL`
- Setting keys: lowercase with underscores: `cruiserToFrigate`, `factionBlacklist`

## Where to Add New Code

**New Ship Replacement Logic:**
- Core decision logic: `src/smallersector/ShipReplacer.java` - Add new probability rolls here
- Matching algorithm: `src/smallersector/RoleMatcher.java` - Modify 9-priority matching system here
- Integration: Update relevant listener/interceptor if new event type needed

**New Event Handler (Fleet, Market, Derelict):**
- Create new listener class in `src/smallersector/[Domain]Listener.java` or `[Domain]Interceptor.java`
- Implement appropriate Starsector listener interface
- Register in `SmallerSectorModPlugin.onGameLoad()`
- Add transient listener to sector or listener manager

**New Operating Cost Type:**
- Add getter to `src/smallersector/Settings.java` (wraps LunaLib read)
- Add setting to `data/config/LunaSettings.csv` with slider definition
- Add multiplier logic to `src/smallersector/CostModifier.java`
- Add preset values to `src/smallersector/PresetListener.java` static maps

**New Build Cost Feature:**
- Add getter to `src/smallersector/Settings.java`
- Add setting to `data/config/LunaSettings.csv`
- Modify `src/smallersector/BaseValueModifier.applyMultipliers()` to use new setting
- Update `src/smallersector/PresetListener.java` with preset values

**New UI Component:**
- Create in `src/smallersector/[Feature]Dialog.java` or `[Feature]Intel.java`
- Implement appropriate Starsector UI interface
- Register in `SmallerSectorModPlugin.onGameLoad()` if intel panel, or call from existing UI

**New Utility/Helper:**
- Add to `src/smallersector/` directly if general purpose
- Add to `src/smallersector/hullmods/` if hull mod-related

**Tests:**
- Note: No test infrastructure exists. Testing is manual via Starsector game launcher
- Place any future test classes in `test/smallersector/` directory

## Special Directories

**jars/:**
- Purpose: Compiled mod JAR output directory
- Generated: Yes (by `ant build` target)
- Committed: No - in .gitignore
- Contents: `SmallerSector.jar` after successful build

**build/:**
- Purpose: Intermediate compiled .class files
- Generated: Yes (by `ant compile` target)
- Committed: No - in .gitignore
- Contents: Compiled Java classes with original package structure

**lunalib/:**
- Purpose: LunaLib dependency
- Generated: No
- Committed: May vary; typically mods reference system LunaLib installation
- Contents: LunaLib JAR and resources if local copy present

**data/:**
- Purpose: Starsector game data files loaded by the mod
- Generated: No
- Committed: Yes - part of mod distribution
- Contents: LunaSettings CSV (UI definitions) and hull mods CSV

**.git/:**
- Purpose: Version control
- Generated: Yes
- Committed: Yes (standard git)

## Build System

**Tool:** Apache Ant

**Key Targets:**
- `ant clean` - Remove build/ directory
- `ant compile` - Compile Java source to build/
- `ant jar` - Package build/ into jars/SmallerSector.jar
- `ant build` - Clean + compile + jar (default)

**Classpath:**
- Starsector core JARs: `$HOME/Games/starsector/`
- Mod dependencies: `$HOME/Games/starsector/mods/LunaLib/jars/`, LazyLib, MagicLib
- Properties in `build.xml` define these paths

**Compilation:**
- Java 17 source and target
- Includes debug information
- Compiles all classes in `src/` to `build/`

## Configuration Loading

**Settings Source Hierarchy:**
1. LunaLib JSON: `LunaSettings/smallersector.json` (player saves)
2. Default fallback: Preset system hardcoded in `Settings.java`
3. Preset selection: `data/config/LunaSettings.csv` defines `preset` dropdown

**Hull Mod Registration:**
- File: `data/hullmods/hull_mods.csv`
- Entry: `smallersector_cost_modifier` pointing to `smallersector.hullmods.SmallerSectorCostMod`
- Hidden: Yes - not shown in manual hull mod picker

**Dependency Verification:**
- LunaLib check in `SmallerSectorModPlugin.onApplicationLoad()`
- Throws exception if LunaLib not found
- LazyLib and MagicLib soft dependencies (referenced but not hard-required)

