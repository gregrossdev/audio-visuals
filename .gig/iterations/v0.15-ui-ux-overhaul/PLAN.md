# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Iteration

### Iteration 15 — UI/UX Overhaul (v0.15.x)

> Redesign the control bar with grouped sections and Canvas-drawn icons, add glassmorphism settings panel with slide-in animation, auto-hide controls after inactivity, add comprehensive keyboard shortcuts, fullscreen performance mode, and enforce the max 5 layer cap.

**Decisions:** D-15.1, D-15.2, D-15.3, D-15.4, D-15.5, D-15.6, D-15.7

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 15.1 | `0.15.1` | Control bar redesign | team | done |
| 15.2 | `0.15.2` | Settings panel + layer cap | team | done |
| 15.3 | `0.15.3` | Auto-hide control bar | in-session | done |
| 15.4 | `0.15.4` | Keyboard shortcuts + fullscreen | in-session | done |

### Batch 15.1 — Control bar redesign (D-15.2, D-15.3)

**Delegation:** team
**Files:** `src/main/kotlin/ui/ControlBar.kt`
**Work:**
- Replace emoji icons (📷 ⚙ ▶ ⏸) with Canvas-drawn geometric shapes
- Reorganize flat Row into 3 grouped sections with VerticalDivider:
  - Source: SourceModeToggle + DeviceSelector/FileSelector
  - Audio: GainSlider + LogScale toggle
  - Display: ThemeSelector + LayerSummary + Screenshot + Settings gear + Pause
**Test criteria:** Icons render crisp, 3 groups visible with dividers, all controls functional
**Acceptance:** ControlBar has clear visual grouping, no emoji icons remain

### Batch 15.2 — Settings panel + layer cap (D-15.4, D-15.7)

**Delegation:** team
**Files:** `src/main/kotlin/ui/SettingsPanel.kt`, `src/main/kotlin/Main.kt` (call site)
**Work:**
- Add `enabled = layers.size < 5` guard on Add Layer button
- Add `isVisible: Boolean` parameter to SettingsPanel
- Wrap content in AnimatedVisibility with slide-in/out + fade
- Replace opaque background with semi-transparent (alpha 0.75)
- Update Main.kt call site to always-render with isVisible
**Test criteria:** Panel slides in/out smoothly. Add Layer disabled at 5 layers. Semi-transparent background.
**Acceptance:** Settings panel animates, layer cap enforced

### Batch 15.3 — Auto-hide control bar (D-15.1)

**Delegation:** in-session
**Depends on:** Batch 15.1, Batch 15.2
**Files:** `src/main/kotlin/Main.kt`
**Work:**
- Replace Column layout with Box(fillMaxSize) stacking
- Mouse activity tracking via pointerInput
- 3-second auto-hide timer with LaunchedEffect
- Bottom-edge hover detection (80px)
- AnimatedVisibility wrapper for ControlBar
- Semi-transparent control bar background
**Test criteria:** Bar hides after 3s. Reappears on mouse near bottom. Smooth animation.
**Acceptance:** Full-height visualization when bar hidden, smooth reveal on hover

### Batch 15.4 — Keyboard shortcuts + fullscreen (D-15.5, D-15.6)

**Delegation:** in-session
**Depends on:** Batch 15.3
**Files:** `src/main/kotlin/Main.kt`
**Work:**
- Expand onKeyEvent: Space, Tab, F/F11, T, L, 1-0, Escape
- Elevate WindowState to main(), pass to App()
- Toggle fullscreen via WindowPlacement
- Escape exits fullscreen
**Test criteria:** Each shortcut triggers correct action. Fullscreen toggles. Auto-hide works in fullscreen.
**Acceptance:** All 10+ keyboard shortcuts working, fullscreen mode functional

**Iteration Acceptance Criteria:**
- [ ] Control bar has 3 grouped sections with Canvas icons
- [ ] Settings panel slides in/out with semi-transparent background
- [ ] Add Layer disabled at 5 layers
- [ ] Control bar auto-hides after 3s, reappears on hover
- [ ] Keyboard shortcuts work (Space, Tab, F, T, L, 1-0)
- [ ] Fullscreen mode toggles with F/F11, exits with Escape
- [ ] Visualizations fill full window when controls hidden

**Completion triggers Iteration 16 → version `0.16.0`**
