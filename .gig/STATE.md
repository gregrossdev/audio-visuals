# State

## Current State

| Field | Value |
|-------|-------|
| **Version** | `0.1.0` |
| **Phase** | 1 — Audio Visualizer Foundation |
| **Status** | `GATHERED` |
| **Last Batch** | — |
| **Last Updated** | 2026-03-06 |

---

## Batch History

<!-- Newest first. Type: PLANNED or UNPLANNED -->

| Version | Phase | Batch Title | Type | Status | Timestamp |
|---------|-------|-------------|------|--------|-----------|
| 0.0.1 | 0 | Project discovery & scaffold | PLANNED | done | 2026-03-06 |

---

## Active Decisions

<!-- Decisions that affect current/upcoming work -->

- D-1.1: kotlin("jvm") + Compose plugin, single-module
- D-1.2: JTransforms for FFT
- D-1.3: TargetDataLine, 44.1kHz, 16-bit PCM, mono
- D-1.4: 1024-sample FFT + Hann window
- D-1.5: IO → Default → Main via StateFlow
- D-1.6: Bar spectrum with drawRect + withFrameMillis
- D-1.7: Frequency→hue HSL color mapping

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
