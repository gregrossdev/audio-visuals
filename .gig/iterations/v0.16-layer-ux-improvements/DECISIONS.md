# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-22 — Layer Name: What happens when you click a layer name?

**Decision:** Clicking the layer name opens an inline dropdown to pick visualization mode directly
**Rationale:** Reduces mode switching from 4 steps (select layer → scroll → open Mode dropdown → pick) to 2 (click name → pick). Layer name already displays the mode, making it the natural edit point.
**Alternatives considered:** Keep separate Mode dropdown (too many steps), double-click to edit (not discoverable), right-click context menu (uncommon in Compose Desktop)
**Status:** ACTIVE
**ID:** D-16.1

## 2026-03-22 — Config Section: Should the Mode dropdown remain?

**Decision:** Remove the separate "Mode" dropdown from the selected layer config section
**Rationale:** Redundant once D-16.1 is implemented. Saves ~40dp of vertical space in the settings panel.
**Alternatives considered:** Keep both (redundant, confusing which to use), disable the config one (weird UX)
**Status:** ACTIVE
**ID:** D-16.2

## 2026-03-22 — Section Header: What should the layer config header show?

**Decision:** Change "Layer: {Mode}" header to "Selected Layer" — mode is already visible in the layer name
**Rationale:** Mode information is redundant in the header since D-16.1 makes the layer name the mode indicator. Simplifies the UI hierarchy.
**Alternatives considered:** Remove header entirely (loses section separation), keep mode in header (redundant)
**Status:** ACTIVE
**ID:** D-16.3

## 2026-03-22 — Affordance: How does the user know the layer name is clickable?

**Decision:** Show a small dropdown indicator (▾) next to the layer name text
**Rationale:** Without a visual cue, users won't discover the click-to-change behavior. A small triangle is a standard dropdown affordance used across desktop UIs.
**Alternatives considered:** Underline text (looks like a link), hover tooltip (too slow to discover), no indicator (relies on exploration)
**Status:** ACTIVE
**ID:** D-16.4
