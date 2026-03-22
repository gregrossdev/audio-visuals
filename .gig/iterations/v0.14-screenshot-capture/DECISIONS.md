# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-22 — Capture Method: How to take screenshots?

**Decision:** `java.awt.Robot.createScreenCapture()` using window bounds from Compose `LocalWindow`
**Rationale:** Zero dependencies, already in JDK. Captures exact window content.
**Alternatives considered:** `screencapture` CLI (external tool dependency), Compose `captureToImage` (test context only)
**Status:** ACTIVE
**ID:** D-14.1

## 2026-03-22 — Trigger: How does the user take a screenshot?

**Decision:** Camera button in ControlBar + Cmd+S keyboard shortcut
**Rationale:** Button is discoverable, keyboard shortcut is fast for repeated captures.
**Alternatives considered:** Menu item (no menu bar in app), settings panel only (too hidden)
**Status:** ACTIVE
**ID:** D-14.2

## 2026-03-22 — Save Location: Where are screenshots stored?

**Decision:** `./screenshots/` directory in project root, timestamped PNG filenames (`screenshot-2026-03-22-143052.png`). Add `screenshots/` to `.gitignore`.
**Rationale:** Keeps screenshots with the project for easy sharing/reference. Timestamps avoid overwrites. PNG for lossless quality.
**Alternatives considered:** ~/Pictures (away from project), file picker dialog (interrupts workflow)
**Status:** ACTIVE
**ID:** D-14.3

## 2026-03-22 — Feedback: How does the user know it worked?

**Decision:** Brief flash overlay + status text in ControlBar showing save path for ~3 seconds
**Rationale:** User needs confirmation the screenshot was taken and where it was saved.
**Alternatives considered:** System notification (heavyweight), no feedback (confusing), sound effect (app is playing audio)
**Status:** ACTIVE
**ID:** D-14.4
