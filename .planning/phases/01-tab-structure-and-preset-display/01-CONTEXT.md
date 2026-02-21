# Phase 1: Tab Structure and Preset Display - Context

**Gathered:** 2026-02-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Restructure the flat LunaLib settings list into a tabbed UI with three tabs, a preset radio selector, and read-only value summaries showing what each preset configures. Users can select a preset and immediately see its configured values. Individual sliders are organized with visual grouping. The duplicate LunaSettings.json file is removed.

</domain>

<decisions>
## Implementation Decisions

### Tab organization
- Three tabs: **Presets**, **Configuration**, **Factions**
- Presets tab opens by default when user opens mod settings (F3)
- Presets tab contains: radio selector + grouped value comparison summaries for all presets
- Configuration tab contains: all individual sliders/fields, grouped with headers and sub-headers
- Factions tab contains: blacklist string field + guidance text pointing to the in-game Faction Manager dialog
- Preset radio selector lives only on the Presets tab (not duplicated elsewhere)

### Preset comparison display
- Summaries grouped by category (Replacement, Costs, D-mods) with all preset values inline — comparison-table style in text form
- Full detail: show every value for every preset, not abbreviated
- Custom preset shows its own summary block reflecting current slider values (same treatment as named presets)
- Currently active preset's summary is visually highlighted (marker, arrow, or prefix) to stand out from others

### Preset selection behavior
- Sliders on Configuration tab stay at their Custom values — they do NOT update when a named preset is selected
- Sliders are disabled/grayed out when a named preset (Vanilla/Recommended/Hardcore) is active; only enabled when Custom is selected
- When sliders are disabled, a text element at the top of Configuration tab explains why (e.g., "Select Custom preset to edit values")
- Configuration tab includes a dropdown/selector to load a preset's values into the sliders as a starting point — only visible/available when Custom is active
- No auto-detection: if Custom values happen to match a named preset, nothing happens — stays on Custom
- Named presets override slider values internally via Settings.java (existing behavior preserved)

### Settings grouping within tabs
- Configuration tab has 2 main groups with Header separators: **Ship Replacement** and **Costs & Penalties**
- Ship Replacement group has sub-headers (Text separators): "Cruiser" and "Capital", with the stays fields alongside their respective sliders
- Costs & Penalties group has sub-headers for "Cruiser" and "Capital" (covering operating costs, build costs per class). D-mods flow at the end without their own sub-header
- No intro text on the Presets tab — radio selector and summaries are self-explanatory

### Claude's Discretion
- Exact text formatting for comparison summaries (line breaks, alignment, symbols)
- How to visually highlight the active preset (arrow prefix, brackets, etc.) within LunaLib text constraints
- LunaSettings.json deletion verification approach
- Exact wording of the "sliders disabled" explanation text
- Dropdown vs radio for the "load preset values" control (based on LunaLib field type support)

</decisions>

<specifics>
## Specific Ideas

- User wants the Configuration tab slider-loading control to be a single dropdown/selector (not one button per preset) — compact and clean
- "Stays as cruiser/capital" auto-calculated disabled fields should be kept alongside replacement sliders
- Factions tab guidance text should direct users to the in-game Faction Manager dialog even though dialog improvements are Phase 3

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 01-tab-structure-and-preset-display*
*Context gathered: 2026-02-21*
