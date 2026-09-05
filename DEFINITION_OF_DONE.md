# DEFINITION_OF_DONE.md — Boi-Khata Phase Completion Checklist

> A task or phase is **Done** only when ALL applicable gates below pass.
> "It works on my machine" is not done. Partial is not done.
> The agent self-checks before every delivery. Builder/Sakira verify before merging.

---

## Gate 1 — Build

- [ ] `./gradlew build` passes clean (assemble + lint + test) on a fresh clone
- [ ] Zero lint errors in changed files (pre-existing warnings acceptable)
- [ ] No `@Suppress` added without an explanatory comment
- [ ] No `TODO` left without a linked D-decision or issue reference

---

## Gate 2 — Tests

- [ ] Every new logic unit has a matching unit test in the same commit
- [ ] Tests are written against **JUnit 4** (`import org.junit.Test`) — see D68
- [ ] Total test count is greater than or equal to the previous count
- [ ] All existing tests still pass (zero new failures)
- [ ] Pure-service classes (Calculator, Builder, Parser): at least 85% branch coverage
- [ ] Repository classes: happy path plus at least one error path
- [ ] No `@Ignore` without a comment and a linked follow-up

---

## Gate 3 — Room & Data

- [ ] If the Room schema changed, the matching `Migration<N>To<N+1>` class exists in the same commit (current head is `Migration4To5`, so the next one is `Migration5To6`)
- [ ] `ROOM_MIGRATION_LEDGER.md` updated if `TenantRebindPlanner.ALL_TENANT_TABLES` changed
- [ ] No `fallbackToDestructiveMigration()` anywhere in the codebase
- [ ] Migration test exists, or the existing suite still passes

---

## Gate 4 — Bengali & Strings

- [ ] Zero hardcoded strings in Kotlin or Compose
- [ ] Every new string has both a `values-bn/strings.xml` and `values-en/strings.xml` entry
- [ ] No new Bengali UI term without owner approval
- [ ] `NumberFormatter` used for all displayed numeric values

---

## Gate 5 — Architecture & Conventions

- [ ] No new Gradle dependency without a D-decision number in the commit
- [ ] No new Gradle module without a D-decision
- [ ] `LicenseWriteGuard` present in every new write repository method
- [ ] `PeriodLockGuard` checked in all write repos (Sale/Khata/Expense/Cashbook/OwnerDrawing)
- [ ] Firestore claims never written from app code
- [ ] No line chart or pie chart (bar charts only)
- [ ] Bottom nav tabs still at most 4

---

## Gate 6 — Git & PR

- [ ] Branch name: `agent/phase-N-slug`
- [ ] Commit message: `feat(phaseN): description [D-X, D-Y]`
- [ ] No direct commit to `main`
- [ ] No secrets, API keys, or `google-services.json` in the diff
- [ ] PR description fills out `.github/pull_request_template.md` fully
- [ ] CI passes (GitHub Actions `./gradlew build` green)

---

## Gate 7 — Design Compliance (UI tasks only)

- [ ] Component name matches the Locked Design Spec registry (no invented names)
- [ ] Locked spacing tokens used: `screenPadding=24dp`, `statusBarPadding=56dp`, `bottomNavHeight=64dp`, `heroCardHeight=156dp`, `posButtonHeight=104dp`, `minTouchTarget=56dp`, `primaryTouchTarget=64dp`
- [ ] Dark mode handled, or noted as a pending owner ruling
- [ ] Bengali-first text verified
- [ ] Screenshot diff: **N/A until P7 screenshot infra is set up**

---

## Gate 8 — Owner Rulings

- [ ] No Pending Owner Ruling silently skipped
- [ ] Any ruling encountered is listed in the PR description
- [ ] Any newly discovered ruling is added to the Locked Design Spec

---

## Gate 9 — Phase Exit (phase-completing PRs only)

- [ ] All `PROGRESS.md` items for this phase are ticked
- [ ] Phase exit-gate ticked in `PROGRESS.md`
- [ ] Sakira has confirmed the phase is complete
- [ ] `PROGRESS.md` completion note updated by Sakira/Builder, never the agent

---

## Agent Quick Self-Check (before every PR)

```text
[ ] gradlew build green?
[ ] Tests: count up, zero new failures?
[ ] JUnit 4 imports only (org.junit.Test)?
[ ] No hardcoded strings?
[ ] No new dependency without a D-number?
[ ] Branch name correct?
[ ] Commit message format correct?
[ ] PR template filled?
[ ] Any Pending Owner Ruling encountered? (list if yes)
```

---

*Last updated: 2026-09-05 · Maintained by: Builder + Sakira Suva*
*Referenced by: AGENT_PLAYBOOK.md Step 5, .github/pull_request_template.md*
