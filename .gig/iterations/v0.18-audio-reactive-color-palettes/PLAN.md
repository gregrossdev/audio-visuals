# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Iteration

### Iteration 18 — Audio-Reactive Color Palettes (v0.18.x)

> Add REACTIVE ThemePreset with dynamic hues from spectral centroid and energy modulation. Add OCEAN and FIRE static presets.

**Decisions:** D-18.1, D-18.2, D-18.3, D-18.4, D-18.5

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 18.1 | `0.18.1` | DynamicColorTheme + REACTIVE preset | in-session | done |
| 18.2 | `0.18.2` | OCEAN and FIRE static presets | in-session | done |

**Iteration Acceptance Criteria:**
- [ ] REACTIVE preset shifts hue based on spectral centroid
- [ ] Energy modulates saturation and lightness
- [ ] Beat triggers brightness flash
- [ ] OCEAN preset shows blue/teal palette
- [ ] FIRE preset shows red/orange/yellow palette
- [ ] T key cycles through 7 themes
- [ ] Static themes remain unchanged

**Completion triggers Iteration 19 → version `0.19.0`**
