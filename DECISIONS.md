# DECISIONS.md — বই খাতা v5.0 Decision Log

**This file is append-only.** Never edit or delete a past entry — if a decision changes, add a new entry that references and supersedes the old one by number. This mirrors the app's own event-sourced ledger philosophy: the history is the source of truth, not the current state alone.

**When to add an entry:** any time you make a non-trivial choice that `ARCHITECTURE.md` doesn't already specify — a library choice between two reasonable options, a naming convention, a workaround for a platform limitation, an interpretation of an ambiguous requirement. If you're about to do something `ARCHITECTURE.md` doesn't cover, write the entry *before* you write the code, not after.

**Format:**
```
## D<number> — <short title>
**Date:** <session date>
**Phase:** <phase number from PROGRESS.md>
**Context:** what problem/ambiguity prompted this decision
**Decision:** what was chosen
**Alternatives considered:** what else was on the table and why it lost
**Supersedes:** (optional) D<number> if this replaces an earlier decision
```

Never resolve a merge conflict in this file by picking one side automatically — a conflict here means two sessions ran concurrently and need manual reconciliation.

---

## D1 — Seed entry: repo established per v5.0 Execution Guide
**Date:** _(fill in on first commit)_
**Phase:** 0
**Context:** Repository initialized following the "বই খাতা v5.0 — AI Agent Execution Guide." `ARCHITECTURE.md`, `PROGRESS.md`, and this file are the agent's persistent memory across sessions, per the guide's Master Strategy (§1.1). No code-level decisions have been made yet — this entry exists only to establish the log format and the numbering sequence starting at D2.
**Decision:** Use sequential `D<n>` numbering, oldest first, never renumbered even if an early decision is later superseded.
**Alternatives considered:** Date-only entries without sequence numbers — rejected because sequence numbers make "supersedes" references unambiguous even if two decisions land on the same date.
**Supersedes:** —

---

*(Next entry starts at D2. Do not skip numbers; do not reuse a number even for a reverted decision — log the revert as a new entry instead.)*

## D2 — Bangladesh Demographic UI/UX Optimization
**Date:** 2026-08-29
**Phase:** 0
**Context:** Need to optimize the UI/UX architecture to cater strictly to the target demographic: 45+ year-old BD shopkeepers in noisy environments using low-end devices. Prevailing Material 3 default configurations are too subtle, hard to tap, cause eye-strain under harsh lights, and rendering PNGs on 3GB RAM devices risks OutOfMemory (OOM) crashes.
**Decision:** 
1. **Receipts:** Abandon PNG rendering entirely. Use Unicode text or lightweight PDF for WhatsApp sharing.
2. **Colors & Theming:** Implement "Lal Khata" Theme (`#800000` primary, `#FDFAF6` ivory background to reduce eye strain).
3. **Accessibility:** Over-scale default Typography by 20% independent of OS settings.
4. **Touch & Feel:** Enforce 56dp–64dp touch targets, skip flat ghost buttons in favor of elevated skeuomorphic buttons, and mandate haptic feedback on saves.
5. **Layout:** Ban Hamburger menus (use Bottom Navigation) and eliminate dashboard charts (use Trident numbers: Cash, Supplier Dues, Customer Dues).
6. **Support UI:** Put a professional Vendor Card in Settings with big "Call" and "WhatsApp" buttons; no logos on login/dashboard.
**Alternatives considered:** Default Material 3 styling (rejected for poor accessibility), Chart-based dashboard (rejected for resource consumption and lack of utility to users).
**Supersedes:** —
