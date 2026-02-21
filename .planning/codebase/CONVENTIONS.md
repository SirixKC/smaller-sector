# Coding Conventions

**Analysis Date:** 2025-02-21

## Naming Patterns

**Files:**
- PascalCase for all public classes: `ShipReplacer.java`, `MarketInterceptor.java`, `SmallerSectorModPlugin.java`
- Descriptive suffixes indicate purpose: `*Modifier` (BaseValueModifier, CostModifier), `*Interceptor` (FleetInterceptor, MarketInterceptor), `*Applicator` (DmodApplicator), `*Listener` (FleetSpawnListener, PresetListener), `*Dialog` (FactionManagerDialog), `*Intel` (FactionManagerIntel)
- Hullmods placed in `src/smallersector/hullmods/` subdirectory: `SmallerSectorCostMod.java`

**Classes:**
- PascalCase: `ShipReplacer`, `RoleMatcher`, `Settings`, `DmodApplicator`
- Inner enums PascalCase: `Role` (in RoleMatcher)

**Methods:**
- camelCase: `tryReplace()`, `getFactionShips()`, `applyDmodsIfNeeded()`, `processPlayerFleet()`
- Getter prefix: `get*` — `getPreset()`, `getCruiserToFrigate()`, `getFactionBlacklist()`
- Setter prefix: `set*` — `setBaseValue()`
- Boolean prefix: `is*` or `has*` — `isStation()`, `isFactionBlacklisted()`, `hasBeenProcessed()`, `rolesMatch()`, `designTypesMatch()`
- Processor prefix: `process*` — `processFleet()`, `processMarket()`, `processPlayerFleet()`
- Checker prefix: `should*` — `shouldProcess()`
- Try-catch prefix: `try*` — `tryReplace()`, `tryReplaceCruiser()`, `tryReplaceCapital()`
- Static factory methods return instances: `createReplacement()`, `findReplacement()`, `pickRandom()`
- Private utility methods have underscores: none used (prefer descriptive names instead)

**Variables:**
- camelCase: `originalValue`, `targetSize`, `shipId`, `currentDay`, `existingDmods`
- Constants UPPER_SNAKE_CASE: `HULLMOD_ID`, `PERSISTENCE_KEY`, `DMOD_APPLIED_TAG`, `MOD_ID`, `CHECK_INTERVAL`
- Static field prefix for loggers: `log` (not capitalized per Apache Log4j convention)
- Short loop variables: `member`, `ship`, `fieldName` (descriptive even in loops, not single letters)

**Types:**
- Enum names PascalCase: `Role` with values UPPERCASE: `COMBAT`, `CARRIER`, `PHASE`, `CIVILIAN`, `UTILITY`

**Common Patterns:**

| Pattern | Example | Location |
|---------|---------|----------|
| Factory method | `createFleetMember()` | `ShipReplacer.createReplacement()` |
| Default values | `public static int getCruiserToFrigate() { return 30; }` | `Settings.java` |
| Null checks | `if (original == null) return null;` | Every public method |
| List operations | `List<...> toRemove = new ArrayList<>();` | `FleetInterceptor.processFleet()` |
| Enum operations | `EnumSet<ShipTypeHints> hints = hull.getHints();` | `RoleMatcher.getRoles()` |
| Cache patterns | `private static String baseValueFieldName = null;` | `BaseValueModifier.java` |
| Logging | `log.info("Smaller Sector: ...")` | All classes with prefix "Smaller Sector: " |

## Code Style

**Formatting:**
- No explicit formatter detected (no .editorconfig, .prettierrc, or formatter config)
- Tab indentation: 4 spaces per level (inferred from code)
- Line length: no hard limit observed, lines up to ~120 characters common
- Braces: Opening brace on same line (K&R style)
```java
if (original == null) {
    return null;
}
```
- Method declarations: single line if short, parameters on same line
```java
public static FleetMemberAPI tryReplace(FleetMemberAPI original, String factionId) {
```

**Linting:**
- No ESLint, Checkstyle, or PMD config detected
- Manual code quality practices observed

## Import Organization

**Order:**
1. Standard Java imports (`java.util.*`, `java.lang.*`)
2. External third-party imports (Starsector API, LunaLib, MagicLib, Log4j)
3. Internal mod imports (same package or `smallersector.*`)

**Pattern Examples:**

From `Settings.java`:
```java
import lunalib.lunaSettings.LunaSettings;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
```

From `RoleMatcher.java`:
```java
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import java.util.*;
```

**Wildcard Imports:**
- Used in some files: `import java.util.*;` in `RoleMatcher.java`
- Explicit imports preferred elsewhere for clarity

## Error Handling

**Patterns:**

1. **Null Checks (Defensive):**
   - Guard clause style, return early:
   ```java
   if (original == null || original.getHullSpec() == null) {
       return null;
   }
   ```
   - Used in all public entry points and sensitive paths

2. **Exception Handling:**
   - Try-catch for reflection errors (MagicLib calls):
   ```java
   try {
       List<String> floatFieldNames = ReflectionUtils.INSTANCE.getFieldsOfType(spec, float.class);
       // ...
   } catch (Exception e) {
       log.error("Smaller Sector: MagicLib reflection failed: " + e.getMessage(), e);
       searchFailed = true;
       return false;
   }
   ```
   - Suppress warnings for unchecked casts:
   ```java
   @SuppressWarnings("unchecked")
   private static Map<String, Float> getProcessedMap() {
       Map<String, Object> persistent = Global.getSector().getPersistentData();
       Map<String, Float> map = (Map<String, Float>) persistent.get(PERSISTENCE_KEY);
   ```

3. **Validation:**
   - Check for null dependencies before use: `if (Global.getSector() == null) return;`
   - Verify API objects before calling methods: `if (fleet.getFaction() == null) return;`

4. **Failure Modes:**
   - Return null to indicate "not found": `public static ShipHullSpecAPI findReplacement(...) { return null; }`
   - Return false to indicate "operation failed": `return false;` in `setBaseValue()`
   - Track failure state with flags: `private static boolean searchFailed = false;`

## Logging

**Framework:** Apache Log4j 1.2.9 (via Starsector API)

**Pattern:**
```java
private static final Logger log = Global.getLogger(ClassName.class);
```

**Conventions:**
- Log level: `info` for normal operations, `warn` for degradation, `error` for failures
- Prefix all messages with "Smaller Sector: " for easy identification:
  ```java
  log.info("Smaller Sector: Loading mod...");
  log.warn("Smaller Sector: No original values stored. Call storeOriginalValues() first.");
  log.error("Smaller Sector: MagicLib reflection failed: " + e.getMessage(), e);
  ```
- Include context in messages: ship names, counts, values
  ```java
  log.info("Smaller Sector: Build cost [" + hullId + "] (" + size + "): " +
           originalValue + " * " + multiplier + " = " + newValue);
  ```
- First-time debug info: log detection details once
  ```java
  if (modified < 3) {
      log.info("Smaller Sector: Build cost [" + hullId + "] (" + size + "): ...");
  }
  ```

## Comments

**When to Comment:**
- Javadoc for public methods and classes (required)
- Inline comments for complex logic or non-obvious decisions
- Section comments to divide logical blocks (see `MarketInterceptor.java` with `// ========== Persistence ==========`)

**Javadoc/Documentation:**
```java
/**
 * Modifies ship base values (production/purchase cost) for cruisers and capitals.
 * Uses MagicLib's ReflectionUtils to bypass security restrictions.
 */
public class BaseValueModifier {
```

```java
/**
 * Attempts to replace a fleet member with a smaller ship.
 * Returns null if ship should not be replaced.
 */
public static FleetMemberAPI tryReplace(FleetMemberAPI original, String factionId) {
```

- Comments explain WHY, not WHAT:
  ```java
  // Skip stations entirely — they should never be replaced
  if (original.isStation()) {
      return null;
  }
  ```

**Algorithm Documentation:**
- Priority chains documented with numbered steps:
  ```java
  /**
   * Priority chain:
   * 1. faction + size + role + designType
   * 2. faction + size + role (any design type)
   * 3. faction + size + designType
   * 4. faction + size (any design type)
   * 5. faction + altSize + role + designType
   * 6. faction + altSize + role (any design type)
   * 7. faction + altSize + designType
   * 8. faction + altSize (any design type)
   * 9. No valid replacement
   */
  ```

## Function Design

**Size:** Methods stay focused on single responsibility. Range from 5–50 lines typically.
- Short utility: `isApplicableToShip()` in hull mod (5 lines)
- Medium processing: `processFleet()` (25 lines)
- Complex matching: `findReplacement()` (65 lines)

**Parameters:**
- Minimal parameters (1–3 typical): `tryReplace(original, factionId)`, `processMarket(market)`
- API objects passed directly; no redundant lookups
- Return early if validation fails

**Return Values:**
- Nullable return types used for "not found" cases: `FleetMemberAPI tryReplace()` returns null if no replacement
- Primitive returns with defaults: `int getCruiserToFrigate()` returns int with fallback defaults
- Boolean flags for state: `boolean isDone()` for EveryFrameScript

**Side Effects:**
- Methods either read state OR modify state, rarely both
- State-modifying methods are explicit: `applyDmodsIfNeeded()`, `markProcessed()`, `storeOriginalValues()`
- Logging is acceptable side effect in all methods

## Module Design

**Exports:**
- Static utility classes expose only public static methods: `Settings`, `BaseValueModifier`, `ShipReplacer`, `RoleMatcher`
- No package-private exports; package is single-access
- Event listeners are instantiated in plugin: `FleetSpawnListener`, `MarketInterceptor`, `DerelictInterceptor`, `PlayerFleetMonitor`
- UI handlers instantiated and registered: `FactionManagerIntel`, `FactionManagerDialog`

**Barrel Files:**
- No barrel/index files used (single package `smallersector`)
- Subpackage for hullmods: `smallersector.hullmods.*`

**Visibility:**
- `public static` for utility methods
- `private static` for internal helpers
- No protected fields (no inheritance)
- No package-private members

---

*Convention analysis: 2025-02-21*
