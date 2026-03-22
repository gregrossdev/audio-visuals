# Governance Report

> Iteration 17 — Recording & Export
> Date: 2026-03-22
> Version: v0.17.3

## Test Results
Automated: N/A — no test suite configured
Lint: N/A

## Acceptance Criteria
- [x] R key or record button starts/stops recording — PASS
- [x] Red pulsing dot + timer visible during recording — PASS
- [x] MP4 file saved to ./recordings/ with timestamp — PASS
- [x] Recording plays correctly in QuickTime — PASS
- [x] Record button disabled if ffmpeg not on PATH — PASS
- [x] recordings/ directory is gitignored — PASS

## Decision Audit

| Decision ID | Decision | Matches? | Notes |
|------------|----------|----------|-------|
| D-17.1 | MP4 via FFmpeg pipe | Yes | libx264 ultrafast, raw BGR pipe |
| D-17.2 | Robot at 30fps on IO | Yes | Coroutine loop, 33ms target |
| D-17.3 | Record button + R key | Yes | Circle/square toggle icon |
| D-17.4 | Red dot + timer overlay | Yes | Pulsing animation + REC M:SS |
| D-17.5 | ./recordings/ gitignored | Yes | Timestamps, gitignored |
| D-17.6 | Disable if no ffmpeg | Yes | isAvailable() gates UI |

## UAT Results
Summary: 6 passed, 0 failed, 0 skipped
Manual verification: User ran app, recorded video, confirmed MP4 plays in QuickTime.

## Issues Found

| ID | Title | Severity | Status |
|----|-------|----------|--------|
| — | — | — | — |

Blockers: 0
Majors: 0
Deferred (Minor/Cosmetic): 0

## Verdict
APPROVED
