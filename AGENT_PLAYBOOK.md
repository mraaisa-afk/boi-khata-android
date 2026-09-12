# AGENT_PLAYBOOK.md — Boi-Khata Coder Session Playbook

> Operational workflow for Boi-Khata coding agents.
> Follow every step, every session, no exceptions.

---

## Mandatory Pre-Session Read

Before writing code, read via GitHub:

1. `PROGRESS.md` — identify active workstream and first unchecked item
2. `PHASE_PLAN.md` — phase row, D-decisions, blockers
3. `DECISIONS.md` — every D-entry in scope
4. `ERROR_LOG.md` — last 10 entries minimum
5. `Boi-Khata-Master-Blueprint.md` — relevant phase/screen constraints
6. If UI work: locked design spec and pending owner rulings
7. `GIT_WORKFLOW.md` — especially Exact Stacked-PR Workflow

If any required file is unavailable, STOP and report the missing file. Do not guess.

---

## Exact Branch / PR Rule

Before creating a branch or PR, decide whether the work belongs to an existing active workstream.

- Same workstream, follow-up fix, CI fix, or next batch → push to the **same branch / same PR**.
- Independent workstream or different owner-approved scope → create a separate branch / PR.
- If unsure, STOP and ask the owner.

Required pattern:

```text
same branch / same PR
  batch 1 commit → push → CI
  batch 2 commit → push → CI
  CI fix commit → push → CI
  final batch → push → CI green
owner merges once
```

Do not create separate branches just because a new batch begins.

---

## 6-Step Session Workflow

### Step 1 — READ

Output a one-line confirmation:

> "Read: PROGRESS.md, PHASE_PLAN.md, DECISIONS.md D-X/D-Y, ERROR_LOG.md, GIT_WORKFLOW.md. Current task: [task]. Active branch/PR: [branch/PR]."

### Step 2 — UNDERSTAND

Identify affected modules, conventions, existing classes, and pending owner rulings.
If a pending ruling blocks scope, STOP.

### Step 3 — PLAN

Write 3–7 bullets:

- Files/classes to create or modify
- Room tables/DAOs involved
- Tests to write or why no test is needed
- Existing branch/PR to reuse, or reason for a new one
- Commit message

Wait for owner confirmation unless the task is tiny and clearly scoped.

### Step 4 — CONFIRM

- [ ] Same workstream? Reuse same branch/PR.
- [ ] New external library? STOP. Propose D-decision.
- [ ] Gate file modification? STOP unless owner explicitly requested it.
- [ ] Touching `main` directly? STOP. Use `agent/*`.
- [ ] New Bengali UI string? Add to required string resources, never hardcode.
- [ ] Room schema/migration? Read `ROOM_MIGRATION_LEDGER.md` first.

### Step 5 — CODE

Follow `CONVENTIONS.md` and `CODING_STANDARDS.md` strictly.

- Every new logic unit gets a matching unit test unless clearly justified.
- Never use `String` for money amounts.
- Never add Gradle dependencies without owner-approved D-decision.
- Never modify Room schema without migration and ledger update.
- Run the relevant build/test when possible.

### Step 6 — DELIVER

Reply with:

1. What was built
2. Files created/modified
3. Test count and build status, or clear reason not run
4. Branch and PR reused/created
5. CI status if known
6. Assumptions and pending owner rulings

---

## CI Failure Protocol

1. Read the full CI log.
2. State root cause, not symptom.
3. Fix on the same branch/PR.
4. Push and let CI rerun.
5. Report new commit hash and verification.
6. Update `ERROR_LOG.md` if non-trivial.

---

## Stop Conditions

| Condition | Action |
| --- | --- |
| Same workstream but a new branch would be created | STOP and reuse existing PR or ask owner |
| New library needed | Propose D-decision, wait |
| D-decision conflict | Flag conflict, wait |
| Observed code contradicts docs | Flag discrepancy, wait |
| Pending owner ruling blocks scope | List ruling, wait |
| Push to `main` requested | Refuse and explain |
| Room migration unclear | Read ledger, then ask |
| Unknown test failure | Share full stack trace, wait |

---

*Updated: 2026-09-12 · Exact stacked-PR workflow enforced.*
