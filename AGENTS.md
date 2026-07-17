# Smaller Sector Agent Guide

## Purpose

This repository is a Starsector campaign mod. Follow explicit user instructions first, preserve user-owned changes, and use this file for repository-specific development rules.

## Sources of Truth

Use this order when requirements disagree:

1. The current user request and this file.
2. Runtime code and data in `src/`, `data/`, and `mod_info.json`.
3. `README.md` as the public behavior contract.
4. `docs/plans/` as historical or aspirational context only.

Treat a code/README disagreement as a bug or product question; do not silently choose whichever is easier.

`data/config/LunaSettings.csv` is the canonical LunaLib settings schema. `data/config/settings.json` appears legacy and globally merged; do not remove or expand it without confirming its compatibility purpose.

## Project Snapshot

- Mod ID: `smallersector`
- Target: Starsector `0.98a-RC8`
- Language/bytecode target: Java 17
- Build: Apache Ant
- Entry point: `smallersector.SmallerSectorModPlugin`
- Runtime artifact: `jars/SmallerSector.jar`
- Libraries: LunaLib, LazyLib, and MagicLib
- Automated tests: none currently
- Gameplay verification: manual through the Starsector launcher

The repository is installed locally through `~/Games/starsector/mods/SmallerSector`, so rebuilding updates the live development copy.

## Product Scope and Roadmap

Implemented or partially implemented systems:

- Cruiser/capital replacement in procedural NPC fleets.
- Market sale-stock replacement.
- Procedural derelict replacement.
- Same-faction role/manufacturer replacement matching.
- Cruiser/capital operating-cost modifiers.
- One-time D-mod penalties on newly acquired player ships.
- LunaLib presets, Custom settings, and faction blacklist management.
- In-campaign faction-manager Intel/dialog UI.

Authoritative roadmap decisions:

- Blueprint replacement is abandoned. Do not implement or advertise it.
- Salvage and recovery-chance multipliers still need implementation.
- Production-cost and production-time multipliers still need implementation.
- The existing reflection-based base-value modifier is not trusted as a working production-cost implementation.
- Do not describe roadmap features as supported until they work and have been tested in game.

## Repository Map

- `mod_info.json`: metadata, dependencies, JAR, and plugin entry point.
- `build.xml`: Java compilation and JAR packaging.
- `src/smallersector/SmallerSectorModPlugin.java`: lifecycle and registration.
- `src/smallersector/Settings.java`: Luna reads, presets, blacklist cache.
- `src/smallersector/PresetListener.java`: preset persistence and Luna UI synchronization.
- `src/smallersector/ShipReplacer.java`: replacement eligibility, probability, and member creation.
- `src/smallersector/RoleMatcher.java`: same-faction candidate selection.
- `src/smallersector/FleetSpawnListener.java`: procedural NPC fleet hook.
- `src/smallersector/FleetInterceptor.java`: one-time fleet processing.
- `src/smallersector/MarketInterceptor.java`: sale stock and refresh tracking.
- `src/smallersector/DerelictInterceptor.java`: pre-visibility derelict processing.
- `src/smallersector/PlayerFleetMonitor.java`: player acquisition and recovery monitoring.
- `src/smallersector/DmodApplicator.java`: acquisition penalties and durable marker.
- `src/smallersector/CostModifier.java`: operating-cost stat changes.
- `src/smallersector/VariantUtils.java`: safe member-owned variant cloning.
- `src/smallersector/BaseValueModifier.java`: disabled experimental base-value mutation.
- `src/smallersector/hullmods/SmallerSectorCostMod.java`: hidden operating-cost hullmod.
- `src/smallersector/FactionManagerIntel.java`: Intel entry point.
- `src/smallersector/FactionManagerDialog.java`: blacklist UI.
- `data/config/LunaSettings.csv`: settings UI schema.
- `data/hullmods/hull_mods.csv`: hidden hullmod registration.
- `README.md`: user-facing behavior and compatibility.
- `docs/plans/`: historical designs, not active requirements.

## Starsector API Rules

The exact installed API is the primary technical reference:

- `~/Games/starsector/starfarer.api.jar`
- `~/Games/starsector/starfarer.api.zip`
- `~/Games/starsector/starfarer_obf.jar` when implementation bytecode is required

Before guessing an API signature, unit, lifecycle, or serialization behavior:

1. Inspect `starfarer.api.zip` or use `javap` on the shipped JARs.
2. Search vanilla source and comparable installed mods.
3. Consult current library documentation.
4. Clearly record anything that remains an inference.

Do not use Java syntax or standard-library APIs newer than Java 17, even when the host JDK is newer. Prefer public APIs. Treat reflection and `lunalib.backend.*` calls as high-risk compatibility surfaces.

Never edit or bundle Starsector core or dependency JARs. Preserve exact ID/path case, use `/` in game resource paths, and namespace new IDs, tags, memory flags, modifier IDs, and persistent keys with `smallersector`.

Perform campaign mutations only from appropriate lifecycle/listener callbacks. Treat Starsector game state as single-threaded; do not touch its APIs from worker threads.

Use defensive guards around `Global`, sector state, fleets, markets, cargo, members, variants, and hull specs. Check stations through the relevant member, fleet-mode, and hull-hint APIs.

## Processing Invariants

Process new procedural content at the earliest reliable hook, before the player can see or interact with it:

- Newly spawned procedural NPC fleets.
- Newly refreshed sale stock in eligible market submarkets.
- Newly created procedural derelicts, before their ship identity is visible.
- Newly acquired eligible player ships, for acquisition penalties only.

Every decision is one-time and idempotent:

- Persist a namespaced processed marker.
- Mark the object even when the roll keeps the original ship.
- Never use save/load, revisit, or settings changes as another roll opportunity.
- A capital replaced by a cruiser receives one roll only.

Existing unmarked content found during installation or state migration is grandfathered: mark it without retroactive replacement or penalty. Existing player ships must not gain D-mods merely because the mod was installed, updated, enabled, or loaded.

Player-owned storage and other free-transfer cargo are never market-replacement candidates. Taking an already-owned ship out of storage is not a new acquisition. Never mutate a stored member, variant, loadout, name, S-mods, condition, or identity.

Ships delivered directly into storage by production are tracked as pending acquisitions by durable member ID. Do not mutate them in storage; apply their one-time acquisition D-mod decision when they first enter the active player fleet. Baseline all ships already stored on game load so installation, updates, and re-enabling cannot penalize old property.

Story, mission, scripted, boss, named-unique, station, and otherwise unique content is never eligible. Use Starsector's canonical tags and memory constants as well as compatibility guards; do not rely only on ad hoc string flags.

Faction-blacklisted content is never replaced. A one-time decision made while a faction is blacklisted remains final.

Replacement candidates remain same-faction. If none is valid, preserve the original. Never assume every modded hull has a usable `hullId + "_Hull"` variant without validation. NPC replacements must be outfitted and combat-capable, and required captain/flagship/script semantics must survive.

Never mutate a shared stock/goal variant in place. Clone it, assign the clone to the member, and only then add tags, permanent hullmods, S-mods, or D-mods.

The fleet-spawn callback occurs inside `LocationAPI.addEntity()`, before the caller may attach defender, story, or mission flags. Keep an unprocessed fleet hidden until the callback stack has returned, then process it and restore its original hidden state. Flush that queue before saving.

Vanilla `DerelictShipEntityPlugin` entities are not discoverable by default, so `DiscoverEntityListener` is insufficient on its own. The bounded current-location scan is an intentional exception to the no-global-scan preference; keep its persistent tag guard and do not expand it to all locations per frame.

Replacing a derelict's member and refreshing its plugin visuals does not recalculate the original entity's BASIC salvage drop, discovery/salvage XP, or interaction duration. Treat those values as a known compatibility gap, not as an implemented salvage modifier, until the derelict pipeline deliberately rebuilds them and is tested in game.

## Lifecycle and Save Compatibility

- `onApplicationLoad()`: application-global setup after data loading; do not assume a campaign sector.
- `onGameLoad()`: campaign setup, migrations, and transient listener/script registration.
- `onNewGame*()`: genuinely new-game generation only.
- Prefer transient listeners/scripts unless an object intentionally belongs in the save.
- Avoid duplicate listener, script, and Intel registration.
- Clear campaign-specific static caches on game load.

Anything attached to sector persistent data, fleets, entities, variants, Intel, or non-transient listeners is part of the save contract. Do not rename serialized classes/keys or change stored value types without migration.

Stable identifiers currently include:

- `$smallersector_fleet_processed`
- `smallersector_market_processed`
- `smallersector_market_ship_processed`
- `smallersector_derelict_processed`
- `smallersector_dmods_applied`
- `smallersector_cost_modifier`
- `smallersector_fleet_processing_initialized_v1`
- `smallersector_market_processing_initialized_v2`
- `smallersector_derelict_processing_initialized_v1`
- `smallersector_storage_known_ship_ids`
- `smallersector_storage_pending_acquisition_ids`

Use XStream-safe values such as strings, boxed primitives, and ordinary collections. Persistent markers—not transient Java sets—must establish save/load idempotence.

Campaign timestamps are millisecond-scale `long` values. Use `CampaignClockAPI.getElapsedDaysSince(long)` or `convertToDays()` rather than hand-converting timestamps. Stock-refresh counters such as `BaseSubmarketPlugin.getSinceSWUpdate()` are campaign days. Avoid absolute timestamps in `float` values.

When changing old persistent data, handle both the legacy and new representation without unchecked casts that can break save loading.

## Settings Coupling

Adding or changing a setting can require coordinated edits to:

- `data/config/LunaSettings.csv`
- `Settings.java`
- Preset maps, backup arrays, and description ID sets in `PresetListener.java`
- Preset comparison text
- `data/config/settings.json`, only if deliberately retained
- `README.md`
- Existing Luna-setting migration/default behavior

Preset names and field IDs are exact string contracts. Display-only Luna fields use the `ss_` prefix.

Custom values must survive preset switching and restart. Loading preset values into Custom must not overwrite faction settings. The user faction blacklist is preset-independent; named presets may contribute defaults by union but must not overwrite user entries. Refresh the runtime blacklist cache after any relevant save.

Prefer LunaLib's public API. Changes involving its backend UI classes require compilation against the supported LunaLib version plus main-menu and campaign UI smoke tests.

## Logging and Performance

Use Log4j through `Global.getLogger()` and prefix useful messages with `Smaller Sector:`.

Use INFO for concise lifecycle summaries and aggregated outcomes. Do not INFO-log from settings getters, hullmod callbacks, per-frame/per-second checks, per-hull loops, or candidate searches. Put bounded diagnostics at DEBUG or behind an explicit debug flag.

Prefer event hooks and interval/indexed work over per-frame global scans. Iterate copy-returning API collections before mutation.

## Code Style

- Four spaces and K&R braces.
- PascalCase classes, camelCase methods/fields, UPPER_SNAKE_CASE constants.
- Guard clauses for invalid API state.
- Javadocs on public lifecycle and core behavior.
- Comments explain lifecycle, persistence, compatibility, units, or non-obvious guards.
- Keep random decisions separate from mutation where practical.
- Share eligibility and probability logic instead of duplicating it across pipelines.

## Build and Generated Files

Normal commands from the repository root:

```bash
ant clean
ant compile
ant build
```

Use `ant build` as the normal gate because it removes stale classes before packaging.

`build/` and `jars/` are generated and uncommitted. Do not manually edit or stage them. The untracked local `smaller-sector` symlink points back to this repository; never commit, follow recursively, or package it.

Close Starsector before replacing a loaded development JAR. A distributed mod archive must include a freshly built `jars/SmallerSector.jar` together with `mod_info.json`, `data/`, and user documentation. Do not include `build/`, source-control metadata, the recursive symlink, or dependency/core JARs.

## Quality Gates

For every code change:

```bash
git diff --check
ant build
jar tf jars/SmallerSector.jar
```

Confirm the expected `smallersector/` classes exist in the JAR. Compilation is not gameplay verification.

Manually test relevant behavior through the launcher:

- Cold launch with required libraries and this mod.
- New-game initialization and existing-save migration.
- Two consecutive save/load cycles with no rerolls or duplicate registrations.
- Procedural fleets, including a roll that retains the original.
- Story/mission/unique/boss/station exclusions.
- Natural market refresh and open/black/military/modded shops.
- Player storage and all free-transfer submarkets remain unchanged.
- Derelict discovery and revisit stability.
- Derelict visual identity plus salvage value, XP, and interaction duration after replacement.
- Existing player fleet remains unpenalized.
- New eligible acquisition receives D-mods once.
- Storage withdrawal and fleet re-entry do not retrigger acquisition.
- Direct-to-storage production receives its acquisition decision on first withdrawal.
- Vanilla, Recommended, Hardcore, and Custom settings.
- Custom backup/restore and blacklist persistence through both UIs.
- Modded factions with unusual hull/variant naming.
- Operating-cost values and relevant log output.

Inspect `~/Games/starsector/starsector.log`, starting from the first relevant ERROR on the failing thread. Search for `Smaller Sector`, `ERROR`, and `WARN`. If in-game testing is unavailable, report exactly what was compiled and what remains unverified.

## Documentation and Metadata

Update `README.md` for player-visible behavior, requirements, defaults, compatibility, or installation changes. Keep `mod_info.json` synchronized with version, game target, dependencies, plugin, JAR path, and description.

Do not advertise blueprint, salvage/recovery, production-cost, or production-time behavior contrary to the roadmap status above. Keep historical plans clearly subordinate to current code and documentation.

## Git Handoff

Start with `git status --short --branch` and `git diff`. Assume unrelated edits/deletions belong to the user. Never restore, delete, stage, stash, or rewrite them without explicit permission.

Make focused commits for implementation work unless the user requests otherwise. Use established prefixes such as `fix:`, `feat:`, and `docs:`. Stage exact task files only; exclude generated output, the local symlink, and unrelated changes.

Before handoff:

1. Review the exact diff.
2. Run applicable gates.
3. Commit the intended files.
4. Synchronize without discarding user work.
5. Push the commit and verify the branch is up to date.
6. Report manual tests still required and any follow-up roadmap work.

If unrelated dirty state makes a safe rebase impossible, fetch and verify whether the remote advanced before pushing. Never use automatic stashing or destructive cleanup on user-owned work. If authentication, networking, or conflicts prevent a safe push, report the exact blocker and local commit hash.
