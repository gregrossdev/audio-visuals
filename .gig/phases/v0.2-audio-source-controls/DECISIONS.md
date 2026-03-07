# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

<!-- Decision statuses:
  PROPOSED  — Claude's recommendation, awaiting user approval
  ACTIVE    — Approved and in effect
  AMENDED   — Overridden by user (original preserved, new entry appended)
  REVISED   — Claude revised based on new information (original preserved)
-->

<!-- Entry format:
## YYYY-MM-DD — Domain: Question

**Decision:** What was decided.
**Rationale:** Why this choice was made.
**Alternatives considered:** What else was evaluated.
**Status:** PROPOSED | ACTIVE | AMENDED | REVISED
**ID:** D-{batch}.{num}
-->

## 2026-03-06 — Audio: How to enumerate and select input devices?

**Decision:** Use `AudioSystem.getMixerInfo()` to list all mixers, filter for those supporting `TargetDataLine`, present device names via `MixerInfo.getName()`. Open lines from specific mixers via `mixer.getLine(info)` instead of `AudioSystem.getLine(info)`.
**Rationale:** Standard JVM API, no additional dependencies. `MixerInfo.getName()` returns user-friendly names on macOS. Filtering for TargetDataLine support ensures only input-capable devices appear.
**Alternatives considered:** Platform-specific native APIs (CoreAudio) — unnecessary complexity for device selection.
**Status:** ACTIVE
**ID:** D-2.1

## 2026-03-06 — Audio: Should we support audio file input?

**Decision:** Defer file input to a future phase. Phase 2 focuses on live mic device selection and controls only.
**Rationale:** File input requires format conversion, playback timing, transport controls (play/pause/seek), and codec handling — a full phase of work on its own. Device selection + gain controls are a coherent scope.
**Alternatives considered:** Include file input now — too much scope for one phase; WAV-only would be limited.
**Status:** ACTIVE
**ID:** D-2.2

## 2026-03-06 — UI: How to structure the control panel?

**Decision:** Bottom control bar using a `Row` inside a `Column` layout. Visualizer gets `Modifier.weight(1f)` to fill remaining space. Control bar has fixed height (~56dp) with dark surface background.
**Rationale:** Bottom bar keeps the visualizer prominent. Fixed-height bar avoids layout jitter. Weight modifier ensures visualizer fills all available space above the bar.
**Alternatives considered:** Top bar (covers visualizer area), overlay/floating controls (harder to click, occludes visualization).
**Status:** ACTIVE
**ID:** D-2.3

## 2026-03-06 — UI: What controls to include?

**Decision:** Three controls: (1) Device dropdown selector, (2) Gain slider (0.0–5.0x multiplier, default 1.0), (3) Pause/Resume toggle button. All in a single bottom bar row.
**Rationale:** Device selection is the primary goal. Gain slider addresses sensitivity differences between mics. Pause/resume is essential UX for a live audio app. Three controls fit cleanly in one row.
**Alternatives considered:** FFT size selector (too technical for UI), sensitivity auto-detect (unreliable across devices).
**Status:** ACTIVE
**ID:** D-2.4

## 2026-03-06 — UI: How to theme the controls?

**Decision:** Use `MaterialTheme` with `darkColorScheme()` (Material 3). Custom primary color derived from the visualizer palette (blue-purple). Surface colors near-black to match the `0xFF0D0D0D` background.
**Rationale:** Material 3 dark theme integrates naturally with existing dark background. Components auto-adapt to dark colors. Custom primary adds visual cohesion with the spectrum visualizer.
**Alternatives considered:** Custom-styled components without Material — more work, less consistent.
**Status:** ACTIVE
**ID:** D-2.5

## 2026-03-06 — Architecture: How to handle device switching at runtime?

**Decision:** Refactor `AudioCapture` to accept a `MixerInfo?` parameter in `start()`. Switching devices: stop current capture, start with new mixer. Expose available devices as a function returning `List<MixerInfo>`. Apply gain multiplier in `AudioCapture.bytesToFloats()` before emitting to StateFlow.
**Rationale:** Keeps device management inside `AudioCapture` where it belongs. Stop/start is simpler and more reliable than hot-swapping a running TargetDataLine. Gain at the capture level avoids modifying FFT or renderer.
**Alternatives considered:** Gain in FFTProcessor (wrong layer — gain is an input concern), gain in renderer (would need dB recalculation).
**Status:** ACTIVE
**ID:** D-2.6
