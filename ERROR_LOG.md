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

<!-- New entries go ABOVE this line. Most recent entry first. -->
<!-- DO NOT edit entries below. ONLY append above. -->

## ERR-005 — 2026-09-15 — P10 — CI: platforms;android-37 not found on runner (android-37.0 only) after SDK bump

**Type:** Build failure
**Phase:** P10
**Date:** 2026-09-15
**Task:** Fix AAR metadata / compileSdk 37 for Compose BOM + make CI pass on GitHub runner
**Error:**
(Repeated from ERR-004 + new run after bump commit 6eca5c7)
Task :app:checkDebugAarMetadata FAILED (when at 35)
... and after bump: build still failing quickly on install or metadata (check run conclusion failure)
**Root cause:** 
- composeBom 2026.08.00 (1.12.0) AARs hard-require compileSdk 37 (ERR-004).
- GitHub ubuntu-latest runner image installs Android API 37 *only* as `platforms/android-37.0` (not `android-37`). 
- sdkmanager "platforms;android-37" either fails to find or the dir isn't created as expected. Gradle/AGP then fails to locate the platform.
- Previous downgrade to 35 (be94bf9e) was a temporary workaround that broke the Compose requirement.
- Repeated get_file_contents calls on the same paths happened while debugging state/SHA.
**Fix applied:** 
- libs.versions.toml: compileSdk/targetSdk = "37" (already done in 6eca5c7)
- .github/workflows/ci.yml: 
  - sdkmanager "platforms;android-37.0" + "build-tools;37.0.0"
  - Added ln -sf symlink: android-37.0 → android-37 (workaround for runner + AGP lookup)
- Appended this ERR-005
- Updated PR #60 body with accurate history and verification steps
**Lesson:** When bumping to high API levels (37+), always cross-check actions/runner-images issues for .0 suffix on the platform dir. Add explicit symlink after sdkmanager. Avoid repeated file reads for the same content — fetch SHA only when about to edit.

## ERR-004 — 2026-09-15 — P10 — PR #60 CI: AAR metadata requires compileSdk 37

**Type:** Build failure
**Phase:** P10
**Date:** 2026-09-15
**Task:** Fix OTP crash CI (safe formatBengaliTaka/toBanglaDigits) + SDK alignment
**Error:**
```
Task :app:checkDebugAarMetadata FAILED
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_3-1789479172645.json
FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:checkDebugAarMetadata'.
> A failure occurred while executing com.android.build.gradle.internal.tasks.CheckAarMetadataWorkAction
   > 9 issues were found when checking AAR metadata:
     1.  Dependency 'androidx.compose.animation:animation-core-android:1.12.0' requires compileSdk version 37 or higher.
         This dependency is consuming the API version 37.
     ... (and 8 identical for: animation, foundation, foundation-layout, material3, material-icons-core, runtime, ui, ui-graphics, ui-text)
```
**Root cause:** composeBom = "2026.08.00" (maps to Compose 1.12.0) + 9 compose-* AARs hard-require compileSdk 37. The branch was still at compileSdk="35" + CI android-35 (from earlier failed downgrade attempt be94bf9e + b62c033e). OTP safe-formatters (ecddc2c2) were pushed without the required SDK bump.
**Fix applied:** 
- Updated gradle/libs.versions.toml: compileSdk/targetSdk = "37"
- Updated .github/workflows/ci.yml: sdkmanager "platforms;android-37" + "build-tools;37.0.0"
- Appended this ERR-004 (with verbatim error)
**Lesson:** When choosing or upgrading a Compose BOM, immediately cross-check its required compileSdk (via BOM notes or first AAR error) and update BOTH versions.toml AND the CI sdkmanager line in the same batch. Do not rely on "stable" comments.

## ERR-003 — 2026-09-12 — P10 — PR D CI Run 1: BookRepositoryImpl missing getLowStockBookSummaries

**Type:** Build failure
**Phase:** P10
**Date:** 2026-09-12
**Task:** PR D — HomeScreen «আজকের করণীয়» alerts section (D79 §2.4)
**Error:**
```
e: BookRepositoryImpl.kt:19:1 Class 'BookRepositoryImpl' is not abstract and does not implement abstract member:
suspend fun getLowStockBookSummaries(tenantId: String): List<LowStockBookSummary>
Task :core:database:compileDebugKotlin FAILED
```
**Root cause:** `getLowStockBookSummaries` was added to the `BookRepository` interface in `Repositories.kt` but its implementation was never written in `BookRepositoryImpl`. The agent wrote the interface contract and the ViewModel/UI callers but failed to read `BookRepositoryImpl.kt` before pushing — violating the rule of always reading existing impl files before adding interface methods.
**Fix applied:** Commit `533d0ffc` on branch `agent/phase-10-homescreen-alerts`:
- Added `override suspend fun getLowStockBookSummaries(tenantId: String): List<LowStockBookSummary>` to `BookRepositoryImpl`
- Implementation filters `bookDao.getActiveByTenant(tenantId)` client-side: `initialStock ≤ lowStockThreshold`, sorted ascending
- Uses `initialStock` as a proxy for current stock; stock-ledger join deferred to PR E (noted in KDoc comment)
- Added `import com.boikhata.core.domain.model.LowStockBookSummary` to impl file
**Lesson:** Before adding any method to a repository interface, always fetch and read the corresponding `*RepositoryImpl.kt` file first — then write the interface method AND its implementation in the same PR commit.

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

*Last updated: 2026-09-15 · Maintained by: Agent (append) + Builder/Sakira (review)*
*Read by: the agent every session, last 10 entries mandatory*
