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

## 2026-03-06 — Build System: Which Gradle plugin for a desktop-only Kotlin app?

**Decision:** Use `kotlin("jvm")` with `org.jetbrains.compose` plugin. Single-module project structure with `src/main/kotlin/`.
**Rationale:** Desktop-only scope. No need for multiplatform overhead. Simpler config, faster builds, easier to reason about. Can migrate to multiplatform later if needed.
**Alternatives considered:** `kotlin("multiplatform")` with `desktopMain` source sets — adds complexity with no benefit for a desktop-only app.
**Status:** ACTIVE
**ID:** D-1.1

## 2026-03-06 — Audio: Which FFT library?

**Decision:** Use JTransforms (`edu.emory.mathcs:JTransforms:2.4`) for FFT computation.
**Rationale:** Pure Java, multithreaded, significantly faster than Apache Commons Math. No power-of-2 requirement. Lightweight — no large dependency tree. Well-suited for real-time audio visualization.
**Alternatives considered:** Apache Commons Math (slower, power-of-2 only), custom FFT (maintenance burden), Kymatik (less mature).
**Status:** ACTIVE
**ID:** D-1.2

## 2026-03-06 — Audio: Capture method and format?

**Decision:** Use `javax.sound.sampled.TargetDataLine` for microphone capture. Format: 44.1 kHz, 16-bit PCM, mono, little-endian.
**Rationale:** Standard JVM API, no external dependencies. 44.1 kHz is universally supported. Mono simplifies processing. 16-bit PCM is the most compatible format across audio hardware.
**Alternatives considered:** Stereo capture (unnecessary complexity for visualization), 48 kHz (less universal hardware support).
**Status:** ACTIVE
**ID:** D-1.3

## 2026-03-06 — Audio: FFT buffer size?

**Decision:** Use 1024-sample FFT buffer with Hann windowing.
**Rationale:** 1024 at 44.1 kHz = 23.2 ms latency. Good balance between time resolution and frequency resolution. Hann window reduces spectral leakage — best general-purpose choice for music/audio signals.
**Alternatives considered:** 512 (too coarse frequency resolution), 2048 (46 ms latency — noticeable lag).
**Status:** ACTIVE
**ID:** D-1.4

## 2026-03-06 — Architecture: Coroutine threading model?

**Decision:** Three-dispatcher pipeline: `Dispatchers.IO` for TargetDataLine reads, `Dispatchers.Default` for FFT computation, Main dispatcher for Compose UI updates. Data flows via Kotlin `StateFlow`.
**Rationale:** IO dispatcher handles blocking audio reads without starving UI. Default dispatcher uses CPU cores efficiently for FFT. StateFlow provides thread-safe state sharing with built-in conflation (drops stale FFT frames).
**Alternatives considered:** Single background thread (less idiomatic Kotlin), Channel-based flow (more complex, no conflation benefit).
**Status:** ACTIVE
**ID:** D-1.5

## 2026-03-06 — Rendering: Visualization type for first phase?

**Decision:** Bar spectrum visualizer — frequency bars drawn with `drawRect()` on Compose Canvas. Frame loop via `LaunchedEffect` + `withFrameMillis`.
**Rationale:** Simplest to implement, validates the entire audio-to-visual pipeline. Clear visual feedback. ~256 bars maps directly to FFT output bins. `withFrameMillis` gives precise frame control without recomposition overhead.
**Alternatives considered:** Waveform (less visually striking for first demo), radial visualizer (requires trigonometry — better as phase 2).
**Status:** ACTIVE
**ID:** D-1.6

## 2026-03-06 — Rendering: Color scheme?

**Decision:** Frequency-to-hue mapping (HSL color space) with vertical gradient per bar. Low frequencies = warm (red/orange), high frequencies = cool (blue/purple). Amplitude controls brightness.
**Rationale:** Classic audio visualizer aesthetic. HSL hue rotation maps naturally to frequency bins. Vertical gradient adds depth without complexity. No glow effects in first phase — keep rendering simple.
**Alternatives considered:** Single-color gradient (less informative), glow/shadow effects (no native Compose Canvas blur — defer to later phase).
**Status:** ACTIVE
**ID:** D-1.7
