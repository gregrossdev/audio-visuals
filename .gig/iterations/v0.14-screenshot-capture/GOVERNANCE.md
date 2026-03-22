# Governance Report

> Iteration 14 — Screenshot Capture
> Date: 2026-03-22
> Version: v0.14.2

## Test Results
Automated: N/A — no test suite configured
Lint: N/A

## Acceptance Criteria
- [x] Camera button visible in ControlBar — PASS
- [x] Clicking button saves PNG to `./screenshots/` — PASS
- [x] Cmd+S keyboard shortcut triggers screenshot — PASS
- [x] Status text shows save path briefly — PASS
- [x] Screenshots directory is gitignored — PASS

## Decision Audit

| Decision ID | Decision | Matches? | Notes |
|------------|----------|----------|-------|
| D-14.1 | java.awt.Robot via window bounds | Yes | Robot + window.bounds in Main.kt |
| D-14.2 | Camera button + Cmd+S | Yes | IconButton in ControlBar, onKeyEvent in Main.kt |
| D-14.3 | ./screenshots/ + timestamps + gitignore | Yes | Timestamped PNGs, gitignored |
| D-14.4 | Flash overlay + status text 3s | Yes | LaunchedEffect delay + overlay composable |

## UAT Results
Summary: 5 passed, 0 failed, 0 skipped
Manual verification: User confirmed screenshot capture works after granting iTerm2 screen recording permission.

## Issues Found

| ID | Title | Severity | Status |
|----|-------|----------|--------|
| — | — | — | — |

Blockers: 0
Majors: 0
Deferred (Minor/Cosmetic): 0

Note: ISS-1 (no max-layer cap) remains DEFERRED from iteration 9.

## Verdict
APPROVED
