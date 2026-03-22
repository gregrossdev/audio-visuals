# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-22 — Format: What format should recordings use?

**Decision:** MP4 via FFmpeg external process pipe — raw frames from Robot piped to ffmpeg stdin with libx264 / ultrafast preset
**Rationale:** Zero JVM dependencies, best quality output. FFmpeg already installed on macOS via Homebrew. Hardware acceleration available via VideoToolbox. Piping raw frames keeps JVM overhead minimal.
**Alternatives considered:** JCodec (pure Java but slow/lower quality), JavaCV (200MB+ native deps), GIF only (256 color limit ruins visualizer output)
**Status:** ACTIVE
**ID:** D-17.1

## 2026-03-22 — Capture: How are frames captured?

**Decision:** Extend existing Robot.createScreenCapture() in a coroutine loop at 30fps on Dispatchers.IO
**Rationale:** Already proven for screenshots. 30fps achievable at typical window sizes. Separate thread prevents UI lag. Reuses the exact pattern from takeScreenshot().
**Alternatives considered:** Skia render hook (no public API), Compose captureToImage (test context only), lower fps (choppy)
**Status:** ACTIVE
**ID:** D-17.2

## 2026-03-22 — Trigger: How does the user start/stop recording?

**Decision:** Record button (red circle icon) in ControlBar + R keyboard shortcut. Toggle start/stop.
**Rationale:** Consistent with screenshot button pattern. R is intuitive and not taken by existing shortcuts. Toggle is simpler than separate start/stop.
**Alternatives considered:** Menu item (no menu bar), settings panel only (too hidden), separate start/stop keys (more to remember)
**Status:** ACTIVE
**ID:** D-17.3

## 2026-03-22 — Feedback: How does the user know recording is active?

**Decision:** Red pulsing dot + elapsed time overlay (top-left) while recording. Status text on save like screenshots.
**Rationale:** User needs clear indication that recording is active and duration. Pulsing dot is universal recording indicator. Elapsed time prevents runaway recordings.
**Alternatives considered:** Red border (conflicts with visualizer), control bar indicator only (too subtle), no timer (user loses track)
**Status:** ACTIVE
**ID:** D-17.4

## 2026-03-22 — Storage: Where are recordings saved?

**Decision:** ./recordings/ directory, timestamped filenames recording-yyyy-MM-dd-HHmmss.mp4, add to .gitignore
**Rationale:** Matches screenshot pattern (./screenshots/). Keeps recordings with project. Timestamps avoid overwrites.
**Alternatives considered:** ~/Videos (away from project), file picker (interrupts workflow), same dir as screenshots (confusing)
**Status:** ACTIVE
**ID:** D-17.5

## 2026-03-22 — Fallback: What if FFmpeg is not installed?

**Decision:** Disable record button with tooltip "FFmpeg required". No JCodec fallback.
**Rationale:** Keeps implementation simple. FFmpeg is standard on macOS via Homebrew. Adding JCodec as fallback would bloat dependencies for an edge case.
**Alternatives considered:** JCodec fallback (adds deps, lower quality), GIF fallback (256 colors), error dialog (less discoverable)
**Status:** ACTIVE
**ID:** D-17.6
