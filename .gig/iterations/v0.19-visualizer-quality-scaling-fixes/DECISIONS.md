# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-22 — Scaling: How to fix hardcoded values?

**Decision:** Replace all hardcoded pixel values with window-relative calculations (minDim * fraction) across Mandala, Kaleidoscope, Particles, Terrain, Trails
**Rationale:** Audit found 15+ hardcoded values causing rendering issues at different window sizes. Proportional sizing ensures correct appearance at any resolution.
**Alternatives considered:** CSS-like responsive breakpoints (over-engineered), fixed minimum window size (avoids problem, doesn't solve it)
**Status:** ACTIVE
**ID:** D-19.1

## 2026-03-22 — Mandala: How to make it visually compelling?

**Decision:** Replace basic shape-ring mandala with sacred geometry: Flower of Life base (overlapping circles at 60° intervals), recursive fractal arms, string-art web connections between elements
**Rationale:** Current mandala draws identical shapes in rings — visually boring. Sacred geometry is mathematically beautiful, well-documented in creative coding, and maps naturally to audio parameters.
**Alternatives considered:** More shape types (still basic), Penrose tiling (too complex), keep current + polish (lipstick on a pig)
**Status:** ACTIVE
**ID:** D-19.2

## 2026-03-22 — Mandala Audio: What drives each visual parameter?

**Decision:** Bass→breathing/scale pulse, mids→rotation speed, highs→detail count (recursive levels), beat→direction flip, centroid→hue shift
**Rationale:** Each frequency band drives a visually distinct parameter. Bass for macro movement, mids for flow, highs for detail. Beat direction flip adds rhythmic punch.
**Alternatives considered:** Single amplitude mapping (flat), random per-frame (chaotic), user-configurable mapping (too complex for this iteration)
**Status:** ACTIVE
**ID:** D-19.3

## 2026-03-22 — Polish: What's the highest-impact visual improvement?

**Decision:** Add trailFade float (0=off, 0.02-0.15) to ReactivityConfig. When >0, draw semi-transparent black rect at frame start instead of clearing canvas.
**Rationale:** Trail/fade is the single biggest visual polish technique in creative coding. Makes every visualizer look dramatically better by creating motion blur and persistence. Optional toggle preserves clean rendering for users who prefer it.
**Alternatives considered:** Per-visualizer trails (already exists in some), bloom/glow (complex without shaders), chromatic aberration (gimmicky)
**Status:** ACTIVE
**ID:** D-19.4

## 2026-03-22 — Kaleidoscope: How to improve visual depth?

**Decision:** Add layered rendering: inner ring of detailed shapes + outer ring of simpler shapes + connecting arc lines. Scale all radii proportionally to minDim.
**Rationale:** Current kaleidoscope is flat — single layer of shapes. Layered depth with varying detail creates visual richness. Proportional scaling fixes the hardcoded radius issues.
**Alternatives considered:** 3D projection (heavy), more segments (just busier, not better), shader effects (not available)
**Status:** ACTIVE
**ID:** D-19.5
