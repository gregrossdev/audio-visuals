# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Phase

### Phase 2 — Audio Source Selection & Controls (v0.2.x)

> Add audio input device selection, gain control, and pause/resume to the visualizer. Refactor AudioCapture for device switching, build a themed control bar UI.

**Decisions:** D-2.1, D-2.2, D-2.3, D-2.4, D-2.5, D-2.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 2.1 | `0.2.1` | Refactor AudioCapture for device selection & gain | in-session | pending |
| 2.2 | `0.2.2` | Dark theme & control bar layout | in-session | pending |
| 2.3 | `0.2.3` | Device dropdown, gain slider & pause toggle | in-session | pending |
| 2.4 | `0.2.4` | Wire controls to audio pipeline | in-session | pending |

### Batch 2.1 — Refactor AudioCapture for device selection & gain

**Delegation:** in-session
**Decisions:** D-2.1, D-2.6
**Files:** `src/main/kotlin/audio/AudioCapture.kt`
**Work:**
- Add `companion object` with `fun availableDevices(): List<Mixer.Info>` that enumerates mixers with TargetDataLine support
- Refactor `start()` to accept `mixerInfo: Mixer.Info?` parameter (null = system default)
- Open TargetDataLine from specific mixer: `AudioSystem.getMixer(mixerInfo).getLine(info)`
- Add `gain: Float` property (default 1.0f) applied as multiplier in `bytesToFloats()`
- Add `pause()` and `resume()` methods that stop/start the TargetDataLine without closing it
**Test criteria:** `./gradlew compileKotlin` succeeds, `availableDevices()` returns non-empty list
**Acceptance:** AudioCapture supports device selection, gain control, and pause/resume

### Batch 2.2 — Dark theme & control bar layout

**Delegation:** in-session (depends on 2.1)
**Decisions:** D-2.3, D-2.5
**Files:** `src/main/kotlin/ui/Theme.kt`, `src/main/kotlin/Main.kt`
**Work:**
- Create `Theme.kt` with `AudioVisualsTheme` composable wrapping `MaterialTheme` with `darkColorScheme()`
- Custom colors: primary = blue-purple (#7B61FF), surface = near-black (#1A1A1A), background = #0D0D0D
- Refactor `Main.kt` App composable: wrap in `AudioVisualsTheme`, use `Column` layout with visualizer (`weight(1f)`) above a bottom control bar `Row` (~56dp height)
**Test criteria:** `./gradlew run` shows visualizer with empty dark control bar at bottom
**Acceptance:** Theme applied, layout correct, visualizer fills space above bar

### Batch 2.3 — Device dropdown, gain slider & pause toggle

**Delegation:** in-session (depends on 2.2)
**Decisions:** D-2.4, D-2.5
**Files:** `src/main/kotlin/ui/ControlBar.kt`
**Work:**
- Create `ControlBar` composable with three controls in a `Row`:
  1. Device dropdown: `DropdownMenu` triggered by a button showing current device name. Lists available devices from `AudioCapture.availableDevices()`. On selection, emits callback with `Mixer.Info`.
  2. Gain slider: `Slider` with `valueRange = 0f..5f`, default 1.0f. Label shows current multiplier value.
  3. Pause/Resume: `IconButton` with play/pause icon toggle.
- All state hoisted — composable takes values + callbacks as parameters
**Test criteria:** `./gradlew compileKotlin` succeeds, controls render in the bar
**Acceptance:** All three controls visible, interactive, properly themed

### Batch 2.4 — Wire controls to audio pipeline

**Delegation:** in-session (depends on 2.1, 2.2, 2.3)
**Decisions:** D-2.4, D-2.6
**Files:** `src/main/kotlin/Main.kt`
**Work:**
- Add state variables: `selectedDevice`, `gain`, `isPaused` in App composable
- Wire device dropdown: on selection, call `audioCapture.stop()` then `audioCapture.start(scope, newDevice)`; restart FFT processor
- Wire gain slider: update `audioCapture.gain` on change
- Wire pause toggle: call `audioCapture.pause()` / `audioCapture.resume()`
- Initialize device list on launch, pre-select system default
**Test criteria:** `./gradlew run` — switch devices, adjust gain, pause/resume all work
**Acceptance:** Device switching restarts capture cleanly, gain affects bar heights, pause freezes visualization

**Phase Acceptance Criteria:**
- [ ] Available audio devices listed in dropdown
- [ ] Selecting a device switches audio input
- [ ] Gain slider amplifies/reduces visualization sensitivity
- [ ] Pause/resume toggles audio capture
- [ ] Controls themed consistently with dark visualizer
- [ ] App builds and runs without errors

**Completion triggers Phase 3 → version `0.3.0`**

---

## Plan Amendments

<!-- Log any changes to the plan after creation -->

| Date | Version | Amendment | Reason |
|------|---------|-----------|--------|
| — | — | — | — |
