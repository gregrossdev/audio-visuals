# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Iteration

### Iteration 17 — Recording & Export (v0.17.x)

> Add video recording via FFmpeg pipe. Robot captures frames at 30fps, pipes to ffmpeg for MP4 encoding. Record button + R shortcut. Red pulsing dot overlay during recording.

**Decisions:** D-17.1, D-17.2, D-17.3, D-17.4, D-17.5, D-17.6

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 17.1 | `0.17.1` | ScreenRecorder utility class | in-session | done |
| 17.2 | `0.17.2` | Record button + R shortcut | in-session | done |
| 17.3 | `0.17.3` | Recording indicator overlay + gitignore | in-session | done |

**Iteration Acceptance Criteria:**
- [ ] R key or record button starts/stops recording
- [ ] Red pulsing dot + timer visible during recording
- [ ] MP4 file saved to ./recordings/ with timestamp
- [ ] Recording plays correctly in QuickTime
- [ ] Record button disabled if ffmpeg not on PATH
- [ ] recordings/ directory is gitignored

**Completion triggers Iteration 18 → version `0.18.0`**
