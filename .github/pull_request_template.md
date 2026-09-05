## Phase & Task

**Phase:** P<!-- N -->
**Branch:** `agent/phase-<!-- N -->-<!-- slug -->`

**Task description:**

> <!-- One sentence: what this PR implements -->

**D-decisions applied:**

- [ ] D-<!-- number -->: <!-- brief description of the decision -->

---

## Pre-Session Read

- [ ] Read `PROGRESS.md` — first unchecked item matched this task
- [ ] Read `PHASE_PLAN.md` — phase row D-decisions noted
- [ ] Read `DECISIONS.md` — all applicable D-entries reviewed
- [ ] Read `ERROR_LOG.md` — last 10 entries reviewed
- [ ] Read `Boi-Khata-Master-Blueprint.md` — relevant sections
- [ ] If a UI task: read the Locked Design Spec — component: <!-- name -->

---

## Definition of Done Self-Check

### Gate 1 — Build

- [ ] `./gradlew build` passes clean (assemble + lint + test)
- [ ] Zero lint errors in changed files
- [ ] No `@Suppress` without an explanatory comment
- [ ] No `TODO` without a linked D-decision or issue

### Gate 2 — Tests

- [ ] Every new logic unit has a unit test in this commit
- [ ] Test count: <!-- X --> new / <!-- Y --> total passing
- [ ] All existing tests still pass
- [ ] No `@Ignore` without a comment and a linked follow-up

### Gate 3 — Room & Data

- [ ] N/A — no schema change
- [ ] Migration class written (if the schema changed)
- [ ] `ROOM_MIGRATION_LEDGER.md` updated (if `ALL_TENANT_TABLES` changed)
- [ ] No `fallbackToDestructiveMigration()` added

### Gate 4 — Bengali & Strings

- [ ] Zero hardcoded strings
- [ ] New strings in both `values-bn` and `values-en`
- [ ] `NumberFormatter` used for all displayed numeric values

### Gate 5 — Architecture

- [ ] No new Gradle dependency without a D-decision
- [ ] `LicenseWriteGuard` in every new write repo method
- [ ] `PeriodLockGuard` checked where applicable
- [ ] No line or pie charts added
- [ ] Bottom nav tabs still at most 4

### Gate 6 — Git & PR

- [ ] Branch: `agent/phase-<N>-<slug>`
- [ ] Commit format: `feat(phaseN): desc [D-X]`
- [ ] No direct commit to `main`
- [ ] No secrets in the diff
- [ ] CI green

### Gate 7 — Design (UI tasks only)

- [ ] N/A — not a UI task
- [ ] Component name matches the Locked Design Spec
- [ ] Locked spacing tokens used
- [ ] Bengali-first text verified
- [ ] fontScale 1.30 manual check passed

---

## Pending Owner Rulings Encountered

<!-- List any Pending Owner Rulings that touch this PR. If none, write None. -->

- None

---

## Test Summary

| | Count |
| --- | --- |
| New tests in this PR | <!-- X --> |
| Total passing after this PR | <!-- Y --> |
| Tests removed (with reason) | <!-- Z or 0 --> |

---

## Screenshots / Output (UI tasks only)

<!-- Attach screenshots or paste receipt output here if applicable. -->
<!-- Bengali numerals visible? fontScale 1.30 tested? -->

*N/A or attach below.*

---

## Notes for Reviewer (Sakira / Builder)

<!-- Anything unusual, assumptions made, or follow-up needed. -->
<!-- "I assumed X because Y. Correct me if wrong." -->

---

> **Merge authority:** Sakira Suva only. The agent does not merge.
> **After merge:** delete the branch.
