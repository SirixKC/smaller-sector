# Testing Patterns

**Analysis Date:** 2025-02-21

## Test Framework

**Status:** Not Detected

**Automated Testing:**
- No unit test framework detected (no JUnit, TestNG, or equivalent)
- No test directory structure (`src/test/`, `tests/`, `*Test.java`, `*Spec.java`)
- No test configuration files (`junit.properties`, `testng.xml`, `pom.xml`)

**Build System:**
- Apache Ant (`build.xml`)
- No test targets in build configuration
- Build focuses on compilation and JAR generation only

**Testing Approach:**
- Manual testing via Starsector game launcher (documented in CLAUDE.md: "Testing is manual via the Starsector game launcher")
- Mod is loaded by game at runtime; behavior validated through in-game observation

## Manual Testing Patterns

**Key Entry Points to Validate:**

| Component | How to Test | Observable Behavior |
|-----------|-------------|-------------------|
| `SmallerSectorModPlugin.onApplicationLoad()` | Launch game with mod | Log messages show "Smaller Sector: Loading mod..." and "Mod loaded successfully" |
| `SmallerSectorModPlugin.onGameLoad()` | Start new game or load save | Listeners registered; intel item added to intel manager |
| `ShipReplacer.tryReplace()` | Observe NPC fleets | Large ships replaced according to probability settings |
| `RoleMatcher.findReplacement()` | Check fleet compositions | Replacement ships match role/design type when possible |
| `MarketInterceptor` | Open markets, trade | Market stock shows fewer cruisers/capitals |
| `DmodApplicator.applyDmodsIfNeeded()` | Acquire large ship to player fleet | Ship receives D-mods automatically |
| `CostModifier.applyToStats()` | Check ship stats in combat | Crew cost, supplies, fuel multiplied as configured |
| `BaseValueModifier.applyMultipliers()` | Check ship build cost in fleet builder | Cruiser/capital build costs increased |
| `Settings` preset system | Change preset in LunaLib UI | All multiplier values update; costs/supplies reflect new preset |
| `FactionManagerDialog` | Open Faction Manager intel | Toggle faction blacklist status; changes persist |

## Validation Through Logging

**Debug Information Available:**

All major operations log to the game log (`starsector-core/starsector.log`). Search for prefix "Smaller Sector: " to find:

1. **Initialization:**
   ```
   Smaller Sector: Loading mod...
   Smaller Sector: Registered preset listener.
   Smaller Sector: Mod loaded successfully.
   Smaller Sector: Initializing for game...
   Smaller Sector: Added Faction Manager intel item.
   ```

2. **Ship Replacement:**
   ```
   Smaller Sector: Looking for base value 5000.0 in destroyer_d_Hull, found 3 float fields
   Smaller Sector: Build cost [destroyer_d_Hull] (DESTROYER): 5000.0 * 1.5 = 7500.0
   Smaller Sector: Applied build cost multipliers to 250 ships.
   ```

3. **D-mod Application:**
   ```
   Smaller Sector: Applying 2 D-mods to shipname (Paragon)
   ```

4. **Cost Modifications:**
   ```
   Smaller Sector: [Paragon] CAPITAL - applied multipliers: crew=2.0, supply=2.0, fuel=2.0
   ```

5. **Market Processing:**
   ```
   Smaller Sector: Processing all markets on game load...
   Smaller Sector: Processed 42 markets.
   ```

**Checking Logs:**
- Open game log: `~/.local/share/Starsector/starsector-core/starsector.log` (or Windows equivalent)
- Filter by "Smaller Sector: " to isolate mod operations
- Check for error/warn messages to identify issues

## Testing Responsibilities

### Developer Checklist Before Committing

1. **Verify Compilation:**
   - Run `ant build` — must complete without errors
   - Check `jars/SmallerSector.jar` exists and is > 0 bytes

2. **Test Initialization:**
   - Start new game
   - Verify logs show "Mod loaded successfully" and "Game initialization complete"
   - Confirm intel item appears in intel panel

3. **Test Ship Replacement:**
   - Observe NPC fleets in multiple locations
   - Confirm cruisers/capitals are rare (replaced with smaller ships)
   - Check that fewer large ships appear in markets

4. **Test Settings:**
   - Change preset in LunaLib settings (Vanilla → Recommended → Hardcore)
   - Verify operating costs and build costs change accordingly
   - Confirm D-mod counts adjust per preset

5. **Test Faction Blacklist:**
   - Open Faction Manager dialog
   - Toggle a faction on/off
   - Verify that faction's ships are/aren't replaced accordingly

6. **Test Edge Cases:**
   - Load an existing save (not new game) — ships should still be processed
   - Save and reload — no duplicate processing/re-rolling
   - Check station fleets are untouched
   - Verify story/scripted fleets are skipped

## Test Structure Patterns (Implicit)

Although no formal test code exists, the source code is structured for testability:

**Separation of Concerns:**
- `Settings` — static configuration layer; easy to mock settings reads
- `ShipReplacer` — pure logic; easy to test with mock `FleetMemberAPI` objects
- `RoleMatcher` — stateless matching algorithm; no global state dependency
- `BaseValueModifier` — encapsulated reflection logic; testable with mock `ShipHullSpecAPI`

**State Isolation:**
- No shared mutable state between modules
- Persistent data isolated to `MarketInterceptor`
- D-mod tracking isolated to ship variant tags

**Null Safety:**
- Defensive null checks at module boundaries
- Safe to pass null objects without crashes (returns null or skips processing)

**Dependency Injection Patterns:**
- Fleet members passed as parameters, not looked up globally
- Market objects passed to processors, not looked up
- Settings accessed via static layer, not injected

## Code Sections Suitable for Unit Testing

If unit tests were added, these methods/functions would benefit most:

| Target | Why | Test Approach |
|--------|-----|---------------|
| `RoleMatcher.rolesMatch()` | Pure boolean logic | Mock ship specs with different roles |
| `RoleMatcher.designTypesMatch()` | Pure string comparison | Test case-insensitive matching, null handling |
| `ShipReplacer.tryReplaceCruiser()` | Probability-based | Mock Random to control roll values |
| `ShipReplacer.tryReplaceCapital()` | Probability-based | Mock Random and validate target size selection |
| `Settings.getCruiserToFrigate()` | Multi-branch preset logic | Mock LunaSettings; test each preset |
| `BaseValueModifier.setBaseValue()` | Reflection wrapper | Mock ReflectionUtils; verify field name caching |
| `MarketInterceptor.shouldProcess()` | Timestamp comparison | Mock clock; test refresh tracking |
| `DmodApplicator.applyDmodsIfNeeded()` | D-mod state management | Mock variant tags and DModManager |

## Missing Test Coverage

**Critical Unverified Paths:**

1. **Reflection Field Discovery (`BaseValueModifier`):**
   - Field name discovery logic depends on MagicLib API
   - No fallback if reflection fails gracefully
   - Only tested implicitly by checking logs for field name caching messages

2. **Fleet Event Listener (`FleetInterceptor`):**
   - `reportFleetDespawnedToListener()` and `reportBattleOccurred()` unimplemented (empty methods)
   - No error handling if fleet is null during concurrent modification

3. **Market Cargo Refresh Tracking (`MarketInterceptor`):**
   - Timestamp-based tracking may drift if game clock resets
   - No validation that `sinceLastCargoUpdate` behaves as expected
   - Dependency on `BaseSubmarketPlugin` cast may fail for custom submkt implementations

4. **D-mod Application (`DmodApplicator`):**
   - Uses `DModManager.addDMods()` from Starsector API — not tested for edge cases
   - No validation that D-mod list contains valid mods

5. **Player Fleet Monitoring (`PlayerFleetMonitor`):**
   - Memory leak prevention in `cleanupRemovedShips()` — no validation
   - Frame-skip logic (1.0s interval) may miss ships added mid-interval

6. **UI Dialog (`FactionManagerDialog`):**
   - Dialog rendering and user interaction untested
   - Blacklist toggle persistence untested

## Testing Notes for Future Test Framework

**If JUnit tests are added:**

1. **Avoid Global State:**
   - Mock `Global.getSector()`, `Global.getSettings()`, `Global.getLogger()`
   - Use dependency injection for logger instances instead of static `log` fields

2. **Mock Starsector APIs:**
   - Create test doubles for `CampaignFleetAPI`, `MarketAPI`, `FleetMemberAPI`, `ShipHullSpecAPI`
   - Mock LunaLib's `LunaSettings` with configured return values

3. **Parameterized Tests:**
   - Test all preset combinations (Vanilla, Recommended, Hardcore, Custom)
   - Test all hull sizes (FRIGATE, DESTROYER, CRUISER, CAPITAL_SHIP)
   - Test faction blacklist edge cases (empty, single, multiple)

4. **Integration Tests:**
   - Spin up mock game state; register listeners
   - Create fleet with known composition; verify replacements
   - Verify settings changes trigger listener updates

5. **Performance Tests:**
   - Market processing doesn't exceed budget (1000 markets, <100ms)
   - Fleet replacement doesn't block frame rate (EveryFrameScript check interval)

---

*Testing analysis: 2025-02-21*
