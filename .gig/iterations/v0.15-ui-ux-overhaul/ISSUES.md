# Issues

> Tracked during governance. Resolved issues are archived with their iteration.
> Deferred issues persist here and carry forward to future iterations.

<!-- Issue statuses:
  OPEN      — Discovered, not yet addressed
  FIXING    — Unplanned batch created, fix in progress
  RESOLVED  — Fixed and verified
  DEFERRED  — Severity allows deferral to a future iteration
-->

<!-- Entry format:
## ISS-{N}: {Title}

**Severity:** Blocker | Major | Minor | Cosmetic
**Source:** UAT-{N} | Decision Audit | Automated Tests | Lint
**Iteration:** {iteration number where discovered}
**Status:** OPEN | FIXING | RESOLVED | DEFERRED
**Description:** What's wrong.
**Evidence:** Error output, failing test, mismatched behavior.
**Batch:** — (assigned when fix starts)
-->

## ISS-1: No max-layer cap enforced in UI

**Severity:** Minor
**Source:** Decision Audit
**Phase:** 9
**Status:** RESOLVED
**Description:** D-9.1 specifies max 5 layers for performance, but the "Add Layer" button has no `enabled = layers.size < 5` guard. Users can add unlimited layers.
**Evidence:** SettingsPanel.kt Add Layer button has no size check.
**Batch:** —
