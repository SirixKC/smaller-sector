# Codebase Concerns

**Analysis Date:** 2026-02-21

## Tech Debt

**Reflection-based field discovery for base value modification:**
- Issue: `BaseValueModifier.java` relies on MagicLib's reflective field lookup to find and modify the obfuscated `baseValue` field on `ShipHullSpecAPI`. The search iterates through all float fields, comparing values with a tolerance threshold (`< 0.01f`). This is brittle and becomes harder to debug if Starsector is obfuscated differently in future versions.
- Files: `src/smallersector/BaseValueModifier.java` (lines 114-181)
- Impact: Build cost multipliers may silently fail if reflection breaks. The `searchFailed` flag prevents exceptions but causes silent degradation. Cached field name helps but still requires initial discovery to work.
- Fix approach: Add a fallback mechanism or explicit field name whitelist updated per Starsector version. Log more aggressively when discovery happens. Consider storing discovered field name in persistent data.

**Manual duplicate probability logic across interceptors:**
- Issue: Ship replacement probability calculations are duplicated in `ShipReplacer.tryReplaceCruiser()` / `tryReplaceCapital()` and again in `DerelictInterceptor.tryGetReplacement()` (lines 98-122). Same logic, written twice, increases maintenance burden.
- Files: `src/smallersector/ShipReplacer.java` (lines 44-82), `src/smallersector/DerelictInterceptor.java` (lines 97-122)
- Impact: Bug fixes or setting changes to probability logic must be applied in two places. Risk of divergence.
- Fix approach: Extract probability logic into a shared utility method in a new `ProbabilityUtil` class. Both `ShipReplacer` and `DerelictInterceptor` call this method, reducing duplication.

**State tracking via variant tags has implicit lifecycle:**
- Issue: Ships are marked as processed via variant tags (`smallersector_dmods_applied`, `smallersector_derelict_processed`). There's no explicit cleanup or expiration. If a ship's variant is reloaded or recreated, the tag persists in cached variant object but the tag-check logic assumes tags are stable across saves.
- Files: `src/smallersector/DmodApplicator.java` (line 36), `src/smallersector/DerelictInterceptor.java` (line 45)
- Impact: Low risk currently due to how Starsector handles variants, but fragile if variant loading behavior changes. Tags can accumulate without bounds in memory.
- Fix approach: Document the assumption that variant objects persist across save/load. Add cleanup method to clear applied tags if needed (optional).

**Market processing timestamp logic assumes monotonic clock:**
- Issue: `MarketInterceptor.shouldProcess()` (lines 62-85) assumes `Global.getSector().getClock().getTimestamp()` never decreases. If time is ever rewound (save/load, debug commands, etc.), the timestamp comparison breaks. Logic relies on `currentDay - lastProcessed > sinceLastUpdate` to detect stock refreshes.
- Files: `src/smallersector/MarketInterceptor.java` (lines 62-85)
- Impact: If clock rewinds, markets may be re-rolled unnecessarily or not at all. Unlikely in normal play but possible in modded scenarios.
- Fix approach: Use `Math.max(currentDay, lastProcessed)` or track refresh events directly instead of clock deltas. Alternatively, add a version field to invalidate old timestamp cache.

## Known Bugs

**No automated tests for replacement logic:**
- Symptoms: Changes to `RoleMatcher` or `ShipReplacer` replacement priorities are only caught by manual game testing. Edge cases (factions with no ships of a size, variants that don't exist, etc.) are discovered late.
- Files: No test files exist; all testing is manual
- Trigger: Adding new ship packs, changing priority matching, modifying fallback behavior
- Workaround: Manual playtesting with `bd ready` or `bd show` for tracking. Document expected behavior in tests (not yet implemented).

**Role detection for utility ships is heuristic-based:**
- Symptoms: Ships with high cargo/fuel (>100 units) are classified as `Role.UTILITY` even if they're combat-focused. This can cause a combat freighter to be replaced with a pure civilian hauler, losing combat capability.
- Files: `src/smallersector/RoleMatcher.java` (lines 62-65)
- Trigger: Any ship with `cargo > 100 || fuel > 100`; affects some mod pack vessels
- Workaround: Add such factions to blacklist or use Custom preset to adjust thresholds. Currently no way to tune cargo/fuel thresholds per-faction.

**Variant lookup for derelicts uses hardcoded patterns:**
- Symptoms: If a hull spec exists but no variant matches the hardcoded patterns (`_Hull`, `_standard`, `_Standard`, or bare hull ID), derelict replacement fails silently. Ship stays original instead of warning the player.
- Files: `src/smallersector/DerelictInterceptor.java` (lines 140-158)
- Trigger: Modded ships with non-standard variant naming (e.g., `myhull_custom`, `myhull_v2`)
- Workaround: Derelict replacement simply returns `null` and logs a warning. Ship is not replaced. No harm done but player sees no feedback.

**PlayerFleetMonitor memory tracking never caps processedShips set:**
- Symptoms: If player acquires and scraps hundreds of ships across a long campaign, `processedShips` Set grows unbounded. Memory impact is low but the set is never trimmed except on ship removal.
- Files: `src/smallersector/PlayerFleetMonitor.java` (lines 19, 95-106)
- Trigger: Playing for hundreds of hours, scrapping many ships
- Workaround: `cleanupRemovedShips()` (line 105) uses `retainAll()` to remove IDs not in the fleet, which keeps the set bounded by current fleet size.

## Security Considerations

**MagicLib reflection grants arbitrary field mutation:**
- Risk: Using `ReflectionUtils.INSTANCE.set()` to modify private fields bypasses encapsulation. If Starsector adds validation or internal consistency checks to `baseValue`, reflection bypasses them.
- Files: `src/smallersector/BaseValueModifier.java` (lines 118, 153, 173)
- Current mitigation: MagicLib is trusted mod library; reflection only targets known fields; changes are cached by field name to minimize calls.
- Recommendations: Add assertions after field mutation (e.g., verify `spec.getBaseValue()` returns expected value). Log all reflection failures aggressively.

**Persistent data map keys could collide:**
- Risk: `MarketInterceptor.getSubmarketKey()` (line 50) uses `market.getId() + "_" + submarket.getSpecId()`. If IDs contain `_`, collisions are possible (e.g., `"a_b" + "_c"` vs `"a" + "_b_c"`).
- Files: `src/smallersector/MarketInterceptor.java` (line 50)
- Current mitigation: In practice, market/submarket IDs follow strict naming conventions without underscores.
- Recommendations: Use a delimiter that cannot appear in IDs (e.g., `"|"` or hash the composite key).

## Performance Bottlenecks

**Market processing iterates all markets on every economy tick:**
- Problem: `MarketInterceptor.reportEconomyTick()` (lines 123-130) calls `processMarket()` for every market in the sector on each tick. In large modded games with 100+ markets, this is called frequently.
- Files: `src/smallersector/MarketInterceptor.java` (lines 123-130)
- Cause: Timestamp-based tracking is O(n) per tick. The `shouldProcess()` check is fast but iteration is not.
- Improvement path: Batch process on specific tick intervals (e.g., every 10th tick) or use a work queue to spread processing across frames. Track which markets were checked this tick to avoid re-checking.

**RoleMatcher creates new Random instance per-class:**
- Problem: `RoleMatcher` and `ShipReplacer` each create independent `Random` objects. Each object has its own seed state, potentially consuming more entropy than needed.
- Files: `src/smallersector/RoleMatcher.java` (line 11), `src/smallersector/ShipReplacer.java` (line 12)
- Cause: No shared random seed state; instances are static and long-lived.
- Improvement path: Use a single shared `Random` instance or seed from `Global.getRandom()` to use Starsector's PRNG state.

**getFactionShips() iterates all known ships for every replacement search:**
- Problem: `RoleMatcher.getFactionShips()` (lines 209-231) iterates all known ships in a faction, checks hull size, D-hull status, and station hints. This is called per ship replaced and can be slow with 100+ ships per faction.
- Files: `src/smallersector/RoleMatcher.java` (lines 209-231)
- Cause: No caching; rebuilds the faction ship list on every call.
- Improvement path: Cache faction ship lists by size on application load or first use. Invalidate cache on preset changes.

## Fragile Areas

**DerelictInterceptor variant variant retrieval:**
- Files: `src/smallersector/DerelictInterceptor.java` (lines 52-95)
- Why fragile: Multiple points of failure: `data.ship` may be null, `getVariant()` may return null, variant hints may not have STATION check, hull spec may be null. Each null check is necessary but the chain is brittle. If Starsector changes derelict data structure, this breaks silently.
- Safe modification: Add a try-catch wrapper around the entire `processDerelictShip()` method to catch and log any exceptions. Add explicit null checks with descriptive log messages at each step.
- Test coverage: No unit tests for derelict processing. Manual testing only. Should add test fixtures for derelict ship data structures.

**BaseValueModifier field discovery:**
- Files: `src/smallersector/BaseValueModifier.java` (lines 114-191)
- Why fragile: Reflection-based discovery is order-dependent and tolerance-based (0.01f threshold). If a second field matches the tolerance, the wrong field is cached. If MagicLib's API changes or reflection fails mid-discovery, `searchFailed` flag is set permanently.
- Safe modification: Once `baseValueFieldName` is found and cached, validate it by writing and reading a test value before committing to cache. Add explicit assertions that the field value matches expected ranges.
- Test coverage: No test coverage. Should mock `ShipHullSpecAPI` with known field values.

**Settings preset system lacks validation:**
- Files: `src/smallersector/Settings.java` (lines 23-48, 156-174)
- Why fragile: Preset names are compared as strings. If a preset name is misspelled or LunaLib returns unexpected values (null, whitespace), the Custom preset is silently selected with defaults. No validation that returned percentages sum to 100 or that multipliers are positive.
- Safe modification: Add assertions that preset names match known constants. Validate that percentage sums are reasonable (warn if not 100). Add null-safety: treat null returns as missing settings, not invalid presets.
- Test coverage: No tests. Should test preset switching and fallback behavior.

## Scaling Limits

**Sector-wide fleet iteration on game load:**
- Current capacity: Tested with ~100 NPC fleets in mod-heavy saves
- Limit: Processing all fleets via `processAllExistingFleets()` is O(n) where n = all fleets in all locations. In a very large modded sector (1000+ fleets), this could take several seconds at game load.
- Scaling path: Use an async job queue or spread processing across multiple game frames. Skip already-processed fleets (add a tag check before processing).

**Blacklist lookup is O(n) per ship:**
- Current capacity: Up to ~50 factions in blacklist
- Limit: `isFactionBlacklisted()` does a set lookup which is O(1) but `getFactionBlacklist()` rebuilds the set on demand if cache is null.
- Scaling path: Ensure cache is always populated and only cleared on preset change (already done). Consider using a thread-local cache if multi-threaded access occurs.

## Dependencies at Risk

**MagicLib reflection API is not documented:**
- Risk: `ReflectionUtils.INSTANCE.getFieldsOfType()` and `.set()` are not part of Starsector's official API. If MagicLib is abandoned or API changes, code breaks.
- Impact: Build cost modifier feature becomes non-functional; mod still works but costs are not increased.
- Migration plan: Extract reflection calls into an adapter class that can be swapped. If MagicLib changes, only the adapter needs updating. Alternatively, use a Java agent or tool to pre-discover field names at build time.

**LunaLib settings are read at runtime, no static analysis:**
- Risk: Settings keys (`"cruiserToFrigate"`, `"preset"`) are strings. If LunaLib renames keys, code reads null silently and uses defaults. No compile-time safety.
- Impact: Settings changes in LunaLib go unnoticed; defaults mask the breakage.
- Migration plan: Create a settings schema document. Add debug logging when LunaLib returns null. Test against LunaLib version upgrade before deploying.

## Missing Critical Features

**No way to disable mod without uninstalling:**
- Problem: "Vanilla" preset disables replacements but still applies build cost multipliers. User must set all multipliers to 1.0x separately. No single "disable all" toggle.
- Blocks: Testing compatibility; fast iteration during development.
- Fix: Add an "Enabled" boolean setting. When disabled, skip all hooks and restore original base values.

**No logging of what ships were replaced and why:**
- Problem: Player can't tell why they didn't see a cruiser (was it replaced? Was the faction blacklisted? Was there no replacement found?). Diagnostics require reading logs.
- Blocks: Player troubleshooting; balance feedback.
- Fix: Add a UI panel that shows recent replacements with reason (e.g., "Hegemony Dominator → Enforcer (matched role + design type)").

**Faction blacklist manager is in-game only:**
- Problem: Setting faction blacklist via LunaLib string field is error-prone. The in-game manager (`FactionManagerDialog`) is convenient but requires running the game.
- Blocks: Batch configuration; mod config-as-code workflows.
- Fix: Support reading faction blacklist from a JSON file in the mod directory, with UI override.

## Test Coverage Gaps

**No unit tests for ship replacement logic:**
- What's not tested: `ShipReplacer.tryReplace()`, role matching priority chain, design type matching, fallback to alternate sizes
- Files: `src/smallersector/ShipReplacer.java`, `src/smallersector/RoleMatcher.java`
- Risk: Regression when changing replacement priorities; edge cases (no replacement available) not validated
- Priority: High — replacement logic is the mod's core feature

**No integration tests for market/fleet processing:**
- What's not tested: `MarketInterceptor` timestamp tracking across save/load, `FleetInterceptor` processing of existing fleets, D-mod application to player fleet
- Files: `src/smallersector/MarketInterceptor.java`, `src/smallersector/FleetInterceptor.java`, `src/smallersector/DmodApplicator.java`
- Risk: Bugs in state tracking (re-rolling ships, missed replacements) caught only in manual game testing
- Priority: High — affects player experience

**No tests for settings and preset switching:**
- What's not tested: Preset switching updates all settings correctly, custom preset overrides presets, blacklist reloading
- Files: `src/smallersector/Settings.java`, `src/smallersector/PresetListener.java`
- Risk: Settings not applied correctly after preset change; defaults masked by caching
- Priority: Medium — settings bugs are caught in UI testing

---

*Concerns audit: 2026-02-21*
