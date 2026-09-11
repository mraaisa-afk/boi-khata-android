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

## ERR-002 — 2026-09-11 — P10 — PR B CI Run 1: 4 compile errors in HomeScreen

**Type:** Build failure
**Phase:** P10
**Date:** 2026-09-11
**Task:** PR B — HomeScreen v2 (D79 §2.1–§2.3): AppBar + Hero card + Quick-action grid
**Error:**
```
e: HomeScreen.kt:89 Unresolved reference 'HomeData'
e: HomeScreen.kt:161 Unresolved reference 'Icons'
e: HomeScreen.kt:170 Unresolved reference 'app_name'
e: HomeScreen.kt:203 Unresolved reference 'contentDescription'
(+ cascading errors from the above 4 roots)
```
**Root cause:**
1. `import com.boikhata.core.domain.model.HomeData` was missing — new screen, no copy-paste from old HomeScreen which also imported it.
2. `material-icons-extended` not in `feature/home/build.gradle.kts` — new icons (Notifications, ShoppingCart, TrendingUp, TrendingDown, Inventory, Visibility, VisibilityOff) are in the extended library, not the core icons that were previously sufficient for this module.
3. `R.string.app_name` belongs to the `:app` module, not `:feature:home`. The feature module cannot access app-module resources.
4. `contentDescription` is a Compose semantics property extension (`androidx.compose.ui.semantics.contentDescription`) that must be explicitly imported — it is NOT included by `import androidx.compose.ui.semantics.semantics`.
**Fix applied:** Commit `7f5e35a` on branch `agent/phase-10-homescreen-appbar-hero`:
- Added `import com.boikhata.core.domain.model.HomeData`
- Added `import androidx.compose.ui.semantics.contentDescription`
- Added individual icon imports (explicit, not wildcard, to avoid future ambiguity)
- Added `implementation(libs.androidx.compose.material.icons.extended)` to `feature/home/build.gradle.kts`
- Replaced `R.string.app_name` with `R.string.home_wordmark`; added `home_wordmark` to all 3 locale strings.xml
- Fixed `todaySalesTotal` type: `Double` (taka), not `Long` (paise) — renamed helper to `formatBengaliTaka(Double)`
**Lesson:** Before pushing a new Composable screen to a feature module, verify: (a) all domain model imports, (b) all icon library dependencies in that module's build.gradle.kts, (c) all string keys exist in the *feature* module's strings.xml (never assume app-module strings are accessible), (d) explicit import for every semantics property extension.

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

*Last updated: 2026-09-11 · Maintained by: Agent (append) + Builder/Sakira (review)*
*Read by: the agent every session, last 10 entries mandatory*
