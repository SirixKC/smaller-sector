# Requirements: Smaller Sector — Luna Settings Revamp

**Defined:** 2026-02-21
**Core Value:** Users can quickly understand and switch between presets, customize individual values when needed, and manage per-faction ship replacement — all from a clean, tabbed Luna settings UI.

## v1 Requirements

### Tab Organization

- [ ] **TABS-01**: Settings restructured into 4 named tabs: Presets, Replacement, Costs, Factions
- [ ] **TABS-02**: Each tab has Header and Text separators for visual clarity

### Preset Management

- [ ] **PRES-01**: Radio field on Presets tab for selecting Vanilla/Sirix Recommended/Sirix Hardcore/Custom
- [ ] **PRES-02**: Preset value summaries shown as Text rows on Presets tab (what each preset configures)
- [ ] **PRES-03**: Custom values backed up before preset switch and restored when switching back to Custom

### Faction Management

- [ ] **FACT-01**: Factions tab with guidance Text directing users to in-game Faction Manager dialog
- [ ] **FACT-02**: Comma-separated blacklist string retained as advanced fallback on Factions tab
- [ ] **FACT-03**: In-game FactionManagerDialog shows improved faction source identification and ship counts

### Cleanup

- [ ] **CLNP-01**: Delete duplicate `lunalib/LunaSettings.json` — CSV is canonical
- [ ] **CLNP-02**: Remove inline preset annotation Text rows (e.g., `[Recommended: 30%]`) from settings sliders
- [ ] **CLNP-03**: Decouple blacklist save from preset selection — fix silent preset-switch bug

## v2 Requirements

### Visual Polish

- **POLSH-01**: Custom mod icon via `LunaSettingsConfig.json`
- **POLSH-02**: Auto-calculated "stays" percentage display (live-updating)
- **POLSH-03**: Percentage validation warning when replacement sliders sum > 100%

### Advanced Presets

- **ADVP-01**: Per-preset read-only preview tabs with disabled sliders
- **ADVP-02**: Cross-tab enable checkbox with radio behavior

## Out of Scope

| Feature | Reason |
|---------|--------|
| Dynamic faction rows in LunaSettings.csv | LunaLib CSV is static at build time; cannot generate rows for runtime-loaded factions |
| Custom UI panels injected into LunaLib | Unsupported by API, breaks on LunaLib updates |
| Live slider sync across tabs | Requires LunaLib internals, fragile |
| Removing in-game FactionManagerDialog | Both access points are valuable (pre-game config vs in-game context) |
| Per-setting override within a preset | Creates confusing hybrid state; binary Custom vs preset is cleaner |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| TABS-01 | — | Pending |
| TABS-02 | — | Pending |
| PRES-01 | — | Pending |
| PRES-02 | — | Pending |
| PRES-03 | — | Pending |
| FACT-01 | — | Pending |
| FACT-02 | — | Pending |
| FACT-03 | — | Pending |
| CLNP-01 | — | Pending |
| CLNP-02 | — | Pending |
| CLNP-03 | — | Pending |

**Coverage:**
- v1 requirements: 11 total
- Mapped to phases: 0
- Unmapped: 11

---
*Requirements defined: 2026-02-21*
*Last updated: 2026-02-21 after initial definition*
