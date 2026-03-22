# Plan

> Living document — reflects what was done, not just what was intended.
> This file tracks the ACTIVE phase only. Completed phases are archived to `phases/`.

---

## Active Iteration

### Iteration 16 — Layer UX Improvements (v0.16.x)

> Click layer name to pick visualization mode directly, removing redundant Mode dropdown from config section.

**Decisions:** D-16.1, D-16.2, D-16.3, D-16.4

| Batch | Version | Title | Delegation | Status |
|-------|---------|-------|------------|--------|
| 16.1 | `0.16.1` | Layer name mode picker | in-session | done |

### Batch 16.1 — Layer name mode picker (D-16.1, D-16.2, D-16.3, D-16.4)

**Delegation:** in-session
**Files:** `src/main/kotlin/ui/SettingsPanel.kt`
**Work:**
- Add onModeChanged callback to LayerRow with inline DropdownMenu
- Add ▾ dropdown indicator to layer name text
- Remove separate Mode dropdown from config section
- Change section header to "Selected Layer"
**Test criteria:** Click layer name → mode dropdown appears. Mode changes on selection. No redundant Mode dropdown. Header reads "Selected Layer".
**Acceptance:** Layer mode switching is 2 clicks instead of 4.

**Iteration Acceptance Criteria:**
- [ ] Clicking layer name opens visualization mode dropdown
- [ ] Dropdown indicator ▾ visible next to layer name
- [ ] Mode change resets config to defaults
- [ ] No separate Mode dropdown in config section
- [ ] Section header reads "Selected Layer"

**Completion triggers Iteration 17 → version `0.17.0`**
