# ERROR_LOG.md — Boi-Khata Coder Mistake & Blocker Log

> **Rules:**
> - Append-only. Never edit or delete past entries.
> - The agent writes a new entry after every non-trivial mistake, build failure, or blocker.
> - Sakira/Builder may add entries after a review catch.
> - This file is mandatory pre-session reading (last 10 entries minimum).
> - The entry format must be followed exactly. No free-form entries.

---

## Entry Format

```text
## ERR-<NNN> — <YYYY-MM-DD> — P<N> — <brief title>

**Type:** Build failure | Test failure | Logic error | Guardrail violation | Blocker | Misunderstanding
**Phase:** P<N>
**Date:** YYYY-MM-DD
**Task:** what was being worked on
**Error:** what went wrong, verbatim error message if applicable
**Root cause:** why it happened
**Fix applied:** what was done to fix it
**Lesson:** one sentence to remember next time
```

**Numbering:** ERR-001, ERR-002, ERR-003 and so on. Sequential. Never reuse a number.

---

## When to Write an Entry

| Situation | Write entry? |
| --- | --- |
| `./gradlew build` fails | Yes |
| A test fails unexpectedly | Yes |
| The agent violated a guardrail (G1-G35) | Yes |
| The agent misunderstood a task | Yes |
| The agent hit a blocker (pending ruling, missing file) | Yes |
| Trivial typo fix with no learning | No |
| Successful task, no issues | No |

---

<!-- New entries go ABOVE this line. Most recent entry first. -->
<!-- DO NOT edit entries below. ONLY append above. -->

---

## ERR-001 — 2026-09-05 — Pre-Launch — Seed entry

**Type:** Blocker
**Phase:** Pre-launch
**Date:** 2026-09-05
**Task:** Agent bootstrap document creation
**Error:** None. This is the seed entry that initialises the log format.
**Root cause:** N/A
**Fix applied:** N/A
**Lesson:** Read the last 10 entries of this file before every session. Past mistakes are the best predictor of future mistakes.

---

*Last updated: 2026-09-05 · Maintained by: Agent (append) + Builder/Sakira (review)*
*Read by: the agent every session, last 10 entries mandatory*
