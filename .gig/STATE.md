# State

## Current State

| Field | Value |
|-------|-------|
| **Version** | `0.1.5` |
| **Phase** | 1 — Audio Visualizer Foundation |
| **Status** | `GOVERNED` |
| **Last Batch** | Wire pipeline & main window |
| **Last Updated** | 2026-03-06 |

---

## Batch History

<!-- Newest first. Type: PLANNED or UNPLANNED -->

| Version | Phase | Batch Title | Type | Status | Timestamp |
|---------|-------|-------------|------|--------|-----------|
| 0.1.5 | 1 | Wire pipeline & main window | PLANNED | done | 2026-03-06 |
| 0.1.4 | 1 | Bar spectrum Canvas renderer | PLANNED | done | 2026-03-06 |
| 0.1.3 | 1 | FFT processor with Hann windowing | PLANNED | done | 2026-03-06 |
| 0.1.2 | 1 | Audio capture engine | PLANNED | done | 2026-03-06 |
| 0.1.1 | 1 | Project scaffold & Gradle config | PLANNED | done | 2026-03-06 |
| 0.0.1 | 0 | Project discovery & scaffold | PLANNED | done | 2026-03-06 |

---

## Active Decisions

<!-- Decisions that affect current/upcoming work -->

_None — phase 1 decisions archived to `phases/v0.1-audio-visualizer-foundation/`._

---

## Open Flags

<!-- Items that need human attention -->

_None._

---

## Working Memory

<!-- Key context: file paths, patterns, naming conventions, gotchas.
     Updated during plan and apply. Keep under 100 lines. -->

- Package: root is `Main.kt` (no package), audio in `audio/`, ui in `ui/`
- Entry: `fun main() = application { Window { ... } }`
- Audio format: 44100 Hz, 16-bit PCM, mono, little-endian
- FFT: JTransforms `DoubleFFT_1D(1024)`, output 512 magnitude bins
- Gradle: `kotlin("jvm")` + `org.jetbrains.compose`, single module
- Data flow: AudioCapture(StateFlow) → FFTProcessor(StateFlow) → SpectrumVisualizer(composable)
- Threading: IO for audio read, Default for FFT, Main for UI

---

## Open Issues

<!-- Summary of deferred issues from ISSUES.md -->

_None._

---

## Session Recovery

1. Read this file — current state
2. Read `PLAN.md` — what's next
3. Read `DECISIONS.md` — what's been decided
4. Read `ISSUES.md` — open/deferred issues
5. Resume from next batch
