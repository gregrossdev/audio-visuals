# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Iteration

### Iteration 14 — Screenshot Capture (v0.14.x)

> Add in-app screenshot capture via java.awt.Robot. Camera button in ControlBar + Cmd+S shortcut. Saves timestamped PNGs to ./screenshots/.

**Decisions:** D-14.1, D-14.2, D-14.3, D-14.4

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 14.1 | `0.14.1` | Screenshot capture feature | in-session | done |
| 14.2 | `0.14.2` | Gitignore update | in-session | done |

**Iteration Acceptance Criteria:**
- [ ] Camera button visible in ControlBar
- [ ] Clicking button saves PNG to `./screenshots/`
- [ ] Cmd+S keyboard shortcut triggers screenshot
- [ ] Status text shows save path briefly
- [ ] Screenshots directory is gitignored

**Completion triggers Iteration 15 → version `0.15.0`**
