# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 1 — Audio Visualizer Foundation (v0.1.x)

> Build a working Kotlin Compose Desktop app that captures microphone audio, performs real-time FFT analysis using coroutines, and renders a bar spectrum visualizer on Compose Canvas.

**Decisions:** D-1.1, D-1.2, D-1.3, D-1.4, D-1.5, D-1.6, D-1.7

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 1.1 | `0.1.1` | Project scaffold & Gradle config | in-session | pending |
| 1.2 | `0.1.2` | Audio capture engine | in-session | pending |
| 1.3 | `0.1.3` | FFT processor with Hann windowing | in-session | pending |
| 1.4 | `0.1.4` | Bar spectrum Canvas renderer | in-session | pending |
| 1.5 | `0.1.5` | Wire pipeline & main window | in-session | pending |

### Batch 1.1 — Project scaffold & Gradle config

**Delegation:** in-session
**Decisions:** D-1.1
**Files:** `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `src/main/kotlin/Main.kt`
**Work:**
- Create Gradle Kotlin DSL build with `kotlin("jvm")` and `org.jetbrains.compose` plugins
- Add dependencies: `compose.desktop.currentOs`, JTransforms, kotlinx-coroutines-core
- Configure `compose.desktop.application` block with `mainClass = "MainKt"`
- Create minimal `Main.kt` with `application { Window { Text("Audio Visuals") } }`
- Generate Gradle wrapper
**Test criteria:** `./gradlew run` launches a window with "Audio Visuals" text
**Acceptance:** Window opens, closes cleanly, Gradle build succeeds

### Batch 1.2 — Audio capture engine

**Delegation:** in-session (depends on 1.1)
**Decisions:** D-1.3, D-1.5
**Files:** `src/main/kotlin/audio/AudioCapture.kt`
**Work:**
- Create `AudioCapture` class that opens a `TargetDataLine` with format: 44.1 kHz, 16-bit PCM, mono, little-endian
- Implement `start()` that launches a coroutine on `Dispatchers.IO` reading audio into a 1024-sample byte buffer
- Convert byte buffer to `FloatArray` (normalize 16-bit PCM to -1.0..1.0)
- Expose samples via `StateFlow<FloatArray>`
- Implement `stop()` to close the line and cancel the coroutine
**Test criteria:** Run app, print `StateFlow` values to console — should see non-zero float arrays when speaking into mic
**Acceptance:** Audio data flows continuously as `FloatArray` values between -1.0 and 1.0

### Batch 1.3 — FFT processor with Hann windowing

**Delegation:** in-session (depends on 1.2)
**Decisions:** D-1.2, D-1.4, D-1.5
**Files:** `src/main/kotlin/audio/FFTProcessor.kt`
**Work:**
- Create `FFTProcessor` class that takes raw audio `FloatArray` input
- Apply Hann window function to 1024 samples
- Run real FFT via JTransforms `DoubleFFT_1D`
- Extract magnitude spectrum (first 512 bins) and convert to dB scale
- Expose frequency magnitudes via `StateFlow<FloatArray>`
- Collect from `AudioCapture.samples` on `Dispatchers.Default`, emit processed results
**Test criteria:** Print FFT output to console — should show varying magnitudes, higher values for spoken frequencies
**Acceptance:** FFT magnitudes array of 512 floats, values in reasonable dB range

### Batch 1.4 — Bar spectrum Canvas renderer

**Delegation:** in-session (depends on 1.3)
**Decisions:** D-1.6, D-1.7
**Files:** `src/main/kotlin/ui/SpectrumVisualizer.kt`
**Work:**
- Create `SpectrumVisualizer` composable that takes `FloatArray` magnitudes as parameter
- Use `Canvas` composable filling available space
- Draw frequency bars with `drawRect()` — one bar per FFT bin (or grouped bins)
- Color bars using HSL hue rotation: low freq → red, mid → green, high → blue
- Amplitude controls bar height and brightness
- Use `Brush.verticalGradient()` for per-bar depth
**Test criteria:** Pass static test data — bars render with correct colors and heights
**Acceptance:** Bars fill canvas width, heights proportional to magnitudes, color gradient visible

### Batch 1.5 — Wire pipeline & main window

**Delegation:** in-session (depends on 1.2, 1.3, 1.4)
**Decisions:** D-1.5, D-1.6
**Files:** `src/main/kotlin/Main.kt`
**Work:**
- Wire `AudioCapture` → `FFTProcessor` → `SpectrumVisualizer` in the main composable
- Use `LaunchedEffect` to start/stop audio capture with composable lifecycle
- Collect FFT `StateFlow` as Compose state via `collectAsState()`
- Add `withFrameMillis` animation loop to drive continuous Canvas redraws
- Set window size, title, and dark background
**Test criteria:** `./gradlew run` — speak into mic, see bars reacting in real-time
**Acceptance:** Real-time audio visualization with responsive bars, smooth rendering, clean shutdown

**Phase Acceptance Criteria:**
- [ ] `./gradlew run` builds and launches successfully
- [ ] Microphone audio is captured without errors
- [ ] FFT produces valid frequency magnitude data
- [ ] Bar spectrum renders with frequency-based coloring
- [ ] Visualization responds to audio input in real-time
- [ ] App shuts down cleanly (audio resources released)

**Completion triggers Phase 2 → version `0.2.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
