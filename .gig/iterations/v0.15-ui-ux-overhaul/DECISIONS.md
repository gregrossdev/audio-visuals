# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-22 — Controls: How should the control bar behave?

**Decision:** Auto-hide control bar — fade out after 3s of mouse inactivity, reappear on mouse hover near bottom 80px
**Rationale:** MilkDrop/iTunes pattern — visualization is the primary experience, controls should disappear when not needed. Recovers 56dp of visualization space.
**Alternatives considered:** Always visible (current — wastes space), collapse to thin strip (still visible), keyboard-only controls (not discoverable)
**Status:** ACTIVE
**ID:** D-15.1

## 2026-03-22 — Layout: How should controls be organized?

**Decision:** Group controls into 3 visual sections with subtle dividers: Source (mode+device/file), Audio (gain+log), Display (theme+layers+screenshot+settings+pause)
**Rationale:** Current bar is a flat row of 8+ controls with no hierarchy. Grouping reduces cognitive load and makes the bar scannable.
**Alternatives considered:** Two rows of controls (increases bar height), tabbed sections (hides controls), floating toolbar (unfamiliar in visualizers)
**Status:** ACTIVE
**ID:** D-15.2

## 2026-03-22 — Icons: How should action buttons be rendered?

**Decision:** Replace Unicode emoji icons (📷 ⚙ ▶ ⏸) with Canvas-drawn geometric icons
**Rationale:** Unicode emoji render at inconsistent sizes and weights across platforms. Canvas-drawn icons provide pixel-perfect consistency, proper alignment, and match the app's visual language.
**Alternatives considered:** Material Icons library (adds dependency), SVG icons (complex for simple shapes), keep emoji (inconsistent rendering)
**Status:** ACTIVE
**ID:** D-15.3

## 2026-03-22 — Settings Panel: How should it look and animate?

**Decision:** Semi-transparent background (alpha 0.75) with blur effect, slide-in animation via AnimatedVisibility
**Rationale:** Current opaque panel blocks visualization. Frosted-glass overlays are standard in VJ tools (Resolume, VDMX). AnimatedVisibility provides smooth slide transitions instead of instant show/hide.
**Alternatives considered:** Keep opaque (blocks viz), separate window (loses focus context), bottom sheet (conflicts with control bar)
**Status:** ACTIVE
**ID:** D-15.4

## 2026-03-22 — Interaction: What keyboard shortcuts should exist?

**Decision:** Space=pause, Tab=settings toggle, F/F11=fullscreen, T=cycle theme, L=log toggle, 1-0=quick preset load
**Rationale:** Only Cmd+S exists today. Every professional visualizer (MilkDrop, projectM, ProVisHD) is keyboard-driven. These shortcuts match common conventions.
**Alternatives considered:** Vim-style keys (unfamiliar), modifier-heavy shortcuts (slower), no new shortcuts (forces mouse use)
**Status:** ACTIVE
**ID:** D-15.5

## 2026-03-22 — Display: How should fullscreen work?

**Decision:** F or F11 toggles borderless fullscreen with zero UI. Mouse movement near edges reveals controls temporarily (same auto-hide behavior).
**Rationale:** Expected feature in every visualizer. MilkDrop, iTunes, Plane9 all use single-key fullscreen toggle. Currently missing entirely.
**Alternatives considered:** Menu-based fullscreen (no menu bar), always show controls in fullscreen (defeats purpose), Cmd+F (conflicts with find in some contexts)
**Status:** ACTIVE
**ID:** D-15.6

## 2026-03-22 — Layers: Should there be a layer cap?

**Decision:** Enforce max 5 layers with `enabled = layers.size < 5` guard on Add Layer button
**Rationale:** Resolves ISS-1 deferred from iteration 9. D-9.1 specified max 5 layers for performance but was never enforced. Simple one-line fix.
**Alternatives considered:** No cap (current — performance risk), configurable cap (over-engineered), warning instead of hard cap (users ignore warnings)
**Status:** ACTIVE
**ID:** D-15.7
