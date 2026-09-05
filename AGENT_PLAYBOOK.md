# AGENT_PLAYBOOK.md — Boi-Khata Coder Session Playbook

> This is the operational workflow for the Boi-Khata Coder agent.
> Follow every step, every session, no exceptions.
> The same content is mirrored in the agent's Notion instructions page.

---

## Mandatory Pre-Session Read (every session, in this order)

Before writing a single line of code, read via the GitHub tool:

1. `PROGRESS.md` — the first unchecked item is your task
2. `PHASE_PLAN.md` — find that phase row, note D-decisions and open blockers
3. `DECISIONS.md` — every D-entry listed for the current phase
4. `ERROR_LOG.md` — last 10 entries minimum
5. `Boi-Khata-Master-Blueprint.md` — constraints plus the current phase section
6. If a UI task: the Locked Design Spec in Notion, including pending owner rulings

**Non-negotiable:** if any file is unavailable via the GitHub tool, STOP and tell Sakira which file is missing. Do not guess or proceed from memory.

---

## 6-Step Session Workflow

### Step 1 — READ

Read all mandatory pre-session files. Output a one-line confirmation:

> "Read: PROGRESS.md, PHASE_PLAN.md, DECISIONS.md D-X/D-Y, ERROR_LOG.md. Current task: [task]. D-decisions in scope: D-X, D-Y."

### Step 2 — UNDERSTAND

Identify which modules are affected, which conventions apply, and which existing classes to reuse.
Check whether the task touches any pending owner ruling. If yes, **STOP**, list the ruling, and wait.

### Step 3 — PLAN

Write a brief plan of 3 to 7 bullets:

- Which files or classes will be created or modified
- Which Room tables or DAOs are involved
- Which tests will be written
- What the commit message will be

Wait for Sakira or Builder to confirm before proceeding.
Exception: a task of 10 lines or fewer that is clearly scoped — proceed directly and note that you did.

### Step 4 — CONFIRM (gate check before coding)

- [ ] New external library needed? **STOP. Propose a D-decision. Wait.**
- [ ] Task modifies `PROGRESS.md`, `DECISIONS.md`, or any gate file? **STOP. Gate files are owner-only.**
- [ ] Task touches `main` directly? **STOP. Use `agent/phase-<N>-<slug>`.**
- [ ] New Bengali UI string? It goes in `strings.xml` only. Never hardcoded.
- [ ] Task touches `TenantRebindPlanner.ALL_TENANT_TABLES`? Read `ROOM_MIGRATION_LEDGER.md` first.

### Step 5 — CODE

Follow `CONVENTIONS.md` and `CODING_STANDARDS.md` strictly:

- Every new logic unit gets a matching unit test in the same commit
- Never use `String` for money amounts
- Never add a Gradle dependency without a D-decision number in the commit message
- Never modify the Room schema without a migration class and a `ROOM_MIGRATION_LEDGER.md` update
- After coding, run through the `DEFINITION_OF_DONE.md` checklist

### Step 6 — DELIVER

Commit format: `feat(phase<N>): <description> [D-X, D-Y]`

Reply to Sakira with:

1. What was built — Bengali for UI context, English for technical names
2. Files created or modified
3. Test count: "X new tests, Y total passing"
4. Build status, or a clear flag explaining why it could not be run
5. Any assumptions made, stated explicitly
6. Any pending owner ruling encountered

---

## STOP Conditions

| Condition | Action |
| --- | --- |
| New library needed | Propose a D-decision, wait |
| Task contradicts a D-decision | Flag the conflict, wait |
| Task contradicts observed code | Flag the discrepancy, wait |
| Pending owner ruling blocks scope | List the ruling, wait |
| Gate file modification requested | Refuse and explain |
| Push to `main` requested | Refuse and explain |
| Room migration unclear | Read `ROOM_MIGRATION_LEDGER.md`, then ask |
| Test fails and the cause is unknown | Share the full stack trace, wait |

---

## Communication Rules

- **Language:** Bengali for UI and UX context, English for technical class and method names
- **Never paraphrase** instructions — quote the exact wording
- **Assumptions** are always explicit: "I assumed X because Y. Correct me if wrong."
- **Errors:** share the full stack trace, not a summary
- **No "maybe" code** — if correctness is uncertain, say so before committing

---

## Error Recovery Protocol

1. Read the full error. Do not skim.
2. Identify the root cause, not the symptom.
3. Propose the fix: "Root cause: X. Fix: Y. Files affected: Z."
4. Do not wait for Builder to diagnose — find the root cause independently.
5. After the fix, restate the test count and build status.
6. Append an entry to `ERROR_LOG.md`.

---

*Last updated: 2026-09-05 · Maintained by: Builder + Sakira Suva*
*Mirrored in: the Boi-Khata Coder Notion instructions page*
