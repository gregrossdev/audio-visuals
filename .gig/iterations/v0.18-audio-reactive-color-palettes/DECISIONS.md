# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-22 — Mechanism: How should reactive colors work?

**Decision:** Add a new REACTIVE ThemePreset that dynamically computes hueStart/hueEnd from spectralCentroid each frame, rather than modifying existing presets
**Rationale:** Preserves existing static themes unchanged. Users who want predictable colors keep them. New preset opts into dynamic behavior explicitly.
**Alternatives considered:** Modify all presets (breaks predictability), separate toggle (adds UI complexity), per-layer reactivity (over-engineered)
**Status:** ACTIVE
**ID:** D-18.1

## 2026-03-22 — Hue: How should frequency map to color?

**Decision:** Spectral centroid (0→1) maps to hue: low=warm (reds/oranges 0-60°), mid=greens/cyans (90-200°), high=blues/purples (220-300°). Smoothed to prevent jitter.
**Rationale:** Follows natural synesthesia — low sounds feel warm, high sounds feel cool. Smoothing prevents distracting flickering on fast transients.
**Alternatives considered:** Random hue (chaotic), fixed cycle (ignores audio), per-band mapping (too complex)
**Status:** ACTIVE
**ID:** D-18.2

## 2026-03-22 — Energy: How should loudness affect color?

**Decision:** Overall energy (0→1) boosts saturation (0.7→1.0) and lightness scale (0.3→0.5). Beat detection triggers brief brightness flash.
**Rationale:** Makes quiet passages muted and loud passages vivid. Beat flash adds rhythmic punch that matches the music.
**Alternatives considered:** No energy modulation (flat feel), only saturation (subtle), opacity modulation (conflicts with layer opacity)
**Status:** ACTIVE
**ID:** D-18.3

## 2026-03-22 — Architecture: How is this implemented?

**Decision:** Create DynamicColorTheme that accepts AudioFeatures and produces a frame-specific ColorTheme. Called in Main.kt where activeTheme is computed.
**Rationale:** Clean separation — visualizers don't change at all. They still receive a standard ColorTheme. Dynamic computation happens upstream in Main.kt.
**Alternatives considered:** Modify ColorTheme directly (breaks static themes), pass AudioFeatures to all visualizers for color (already done, redundant)
**Status:** ACTIVE
**ID:** D-18.4

## 2026-03-22 — Variety: Should we add more static presets?

**Decision:** Add OCEAN (blues/teals) and FIRE (reds/yellows/oranges) static presets
**Rationale:** 4 presets feels thin. 6 static + 1 reactive = 7 options gives good variety. T key cycling becomes more interesting.
**Alternatives considered:** Keep 4 (limited), add 4+ (too many to cycle through), user-configurable (complex UI)
**Status:** ACTIVE
**ID:** D-18.5
