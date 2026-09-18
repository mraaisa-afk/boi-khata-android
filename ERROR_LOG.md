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

## ERR-012 — 2026-09-18 — P11 — Owner device test after PR #65: stock display never moved after sale, khata history blind to cash sales, discount parser ASCII-only, pink checkout FAB

**Type:** Logic error (3×) + styling regression (1×)
**Phase:** P11
**Date:** 2026-09-18
**Task:** Owner device-testing of the PR #65 build found 4 defects: (1) opening stock মিসির আলী=40 / হিমু=30 unchanged after a 3+3-unit sale; (2) the customer's khata page still showed «কোনো লেনদেন নেই» after a sale to করিম; (3) the ছাড় discount field appeared cosmetic; (4) the «বিক্রি সম্পন্ন» checkout control rendered as a small pink pill unlike every standard button.

**Error / Root cause (per issue, verified line-level):**
1. **Stock (B-005) — read-side display gap, NOT a missing decrement.** `SaleRepositoryImpl.createBill` (core/database/.../SaleRepositoryImpl.kt, D22 transaction step 3) correctly appends `stock_ledger` rows with `changeQuantity = -qty`. But `CatalogScreen.kt:159` displayed `book.initialStock` — the static opening column — and the domain `Book` model had no current-stock field at all. `BookRepositoryImpl.kt` D79 note explicitly deferred the "stock-ledger join" to a "PR E" that never landed. The decrement was recorded; nothing read it. Secondary inconsistency: `AccountingRepositoryImpl.getBalanceSheet` valued inventory at bare `SUM(ledger)` WITHOUT initialStock — a book with opening stock 40 and no movement was valued at zero.
2. **Khata history (B-006) — write is conditional, display never looked at bills.** `SaleRepositoryImpl.createBill` step 4 writes a khata entry ONLY when `dueAmount > 0.01 && customerId != null`. A fully-paid নগদ sale → due = 0 → no khata entry → `KhataViewModel.loadDetail` (which reads only `khataRepository.getEntries`) shows «কোনো লেনদেন নেই». The bill IS written with `customerId` (explains home «১টি বিক্রি, আয় ৳১,৯৫০») but `BillDao` had no `getByCustomer` query anywhere.
3. **Discount (B-007) — parser, not wiring.** The field IS wired (`PosScreen` → `setDiscount` → `recalculateTotals` computes discount and total correctly for ASCII input — proven by new `SaleViewModelTest`). But parsing used `toDoubleOrNull()`, which accepts ASCII [0-9] only; the app's output digits are Bangla (DigitStyle.BANGLA) and Bangla keyboards emit ০-৯, so a Bengali digit silently parses to null → 0 discount → "the total never moved". Glyph forensics on the screenshot were inconclusive (৪ renders nearly identical to ASCII 8 in Noto Sans Bengali), so BOTH robustness paths were fixed: Bengali-digit-tolerant parsing (`BengaliNormalizer.toAsciiDigits`) on discount + paid-amount inputs, and digit-only input filters on both fields.
4. **Checkout button (B-008) — unmapped M3 token.** The checkout control was a default `FloatingActionButton`, whose M3 default container is `primaryContainer = Color(0xFFFFD7D7)` (BoiKhataTheme.kt:22) — the pink pill. The D71/D75 design-enforcement passes never touched the POS screen's FAB.

**Fix applied:** B-005 — `StockLedgerDao.getDeltasByTenant` (one grouped query), `Book.currentStock` derived as `initialStock + delta` in every `BookRepositoryImpl` read path, Catalog tab + low-stock alerts + balance-sheet inventory all switched to live stock. B-006 — `BillDao.getByCustomer` + `BillRepository.getBillsByCustomer` + «বিক্রির ইতিহাস» section in `KhataCustomerDetailScreen` (display-only; money ledger untouched). B-007 — Bengali-digit-tolerant parsing + input filters. B-008 — full-width standard primary `Button` (theme primary #800000 / onPrimary white). Regression tests: `SaleRepositoryImplTest` (table-level before/after evidence), `BookRepositoryImplTest` (40 → 37), `SaleViewModelTest` (discount/partial/tenant math), `BengaliNormalizerTest` additions.

**Lesson:** An append-only ledger without a read-side derivation is a silent no-op for the user; and any numeric input field in a Bangla-locale app must parse ০-৯ or it will randomly do nothing. UI default tokens (FAB primaryContainer) are design decisions by omission — enforce the palette on every new screen, not just the ones an audit covers.


## ERR-011 — 2026-09-18 — P11 — B-004: khata customer appears only after app restart — add-customer writes under the seed tenant "t_1"

**Type:** Logic error
**Phase:** P11
**Date:** 2026-09-18
**Task:** Sakira device report after PR #63 install: «Newly added customers do not appear instantly on the Khata screen without restarting the app. The UI fails to automatically observe or collect the latest database state changes in real-time.»
**Error:** No crash. Symptom: insert a customer via the Khata "+" FAB → save → list still shows «কোনো কাস্টমার নেই» (Success-empty state, not Error, not Loading) → force-stop → relaunch → customer appears.
**Root cause:** The B-002 Flow chain (ERR-007 fix) is correct end-to-end — `KhataCustomerDao.getActiveByTenantFlow` → `KhataRepositoryImpl.getCustomersFlow` → `KhataViewModel.loadCustomers` collector was live the whole time. The break was on the WRITE side: `khata_add_customer` is a separate NavHost destination, so it scopes its OWN `KhataViewModel` instance (`hiltViewModel()` is per-back-stack-entry), and that instance never runs `loadCustomers`/`loadDetail`. `addCustomer` therefore wrote with the hardcoded VM fallback `currentTenantId = "t_1"` while the list Flow observes the claims tenant (`MainActivity → BoiKhataMainScreen(tenantId)`). Room's invalidation tracker DID fire on the insert, but the re-run query `WHERE tenantId = <claims>` correctly returned no new row. The «appears after restart» half is the D41 rebind masking the corruption: `MainViewModel.resolveAuthState` passes the hardcoded literal `"t_1"` into `ClaimsSession.needsRebind`, so `rebind("t_1" → claimsTenant)` executes on EVERY launch and migrated the stray row before the fresh Flow's first emission. Two distinct defects: (1) tenantless write path with seed-tenant fallback (this fix, B-004), (2) needsRebind hardcoded-true → rebind-per-launch safety net that hides mis-tenant writes instead of surfacing them (flagged as P11 follow-up, needs owner ruling).
**Fix applied:** Branch `agent/fix-b4-khata-tenant-write` — (1) `KhataViewModel.addCustomer` takes `tenantId: String` as an explicit parameter and refuses blank tenants with an Error state (fail-fast, no guessed writes); (2) `KhataAddCustomerScreen` receives `tenantId` as a composable parameter and threads it into the call; (3) `BoiKhataNavigation` passes `tenantId = tenantId` into the `khata_add_customer` destination; (4) VM fallback default changed `"t_1"` → `""` so any future tenantless write fails loudly instead of corrupting silently. Audited the sibling `"t_1"` defaults (`SaleViewModel`, `ExpenseViewModel`, `SupplierViewModel`, `MelaViewModel`, `CashCloseViewModel`, `ReportsViewModel`): all are set by their screens' mandatory `load*(tenantId)` before any write, and detail-path khata writes (`addCredit`/`addPayment`/`forgiveDebt`/`addInstallment`) are guarded by `KhataDetailUiState.Success` which only exists post-`loadDetail` — `addCustomer` was the only reachable tenantless write. Verified: `:app:assembleDebug` BUILD SUCCESSFUL locally (JDK 17.0.20.1 toolchain).
**Lesson:** A reactive read chain cannot compensate for a wrong write — when a destination scopes its own ViewModel instance, EVERY write argument derived from navigation context (tenantId especially) must be threaded through the destination's parameters, not read from uninitialized VM state. And a per-launch rebind is not a migration, it is a mask: data recovered by it signals a write-path defect that must be found, not a bug that healed itself.

## ERR-010 — 2026-09-18 — P11 — B-003: add-book "+" crash — navigation 2.8.x deserializes literal "null" path segment into actual null

**Type:** Logic error
**Phase:** P11
**Date:** 2026-09-18
**Task:** Fix the real-device crash on "+" (add book) from the stock screen — corrected logcat supplied by Sakira after the ERR-009 stale-log confusion
**Error:**
```
FATAL EXCEPTION: main (Process: com.boikhata)
java.lang.IllegalArgumentException: Wrong argument type for 'bookId' in argument bundle. string expected.
    at androidx.navigation.NavDestination.addInDefaultArgs(NavDestination.kt:619)
    at androidx.navigation.NavController.navigate(NavController.kt:2437)
    at androidx.navigation.NavController.navigate$default(NavController.kt:2418)
    at com.boikhata.BoiKhataNavigationKt.BoiKhataMainScreen$lambda$2$0$0$1$0$0(BoiKhataNavigation.kt:155)
```
The stack lines match main exactly (155 = `onAddBook = { navController.navigate("book_add_edit/null") }`, navigate(route) overload = 2418, addInDefaultArgs call = 2437, with navigation-compose 2.8.5 sources) — this log IS the current APK, closing the ERR-009 fresh-capture item.
**Root cause:** Navigation 2.8.0 changed `NavType.StringType.parseValue` to deserialize the literal string `"null"` into an actual null ("reversion of Kotlin standard library serializing null receivers of kotlin.toString into 'null'"). `navigate("book_add_edit/null")` therefore stored a null value in the argument bundle for `{bookId}`, and `NavArgument.verify` (NavArgument.kt:85) rejects a null value for the declared NON-nullable argument. The `"null"`-string sniffing at the destination (`if (bookIdArg == "null") null else bookIdArg`) was written for pre-2.8 semantics and can never work on 2.8.5 (`libs.versions.toml`: navigationCompose = "2.8.5").
**Fix applied:** Branch `agent/fix-b3-bookid-nav-null-crash` — route changed to `book_add_edit?bookId={bookId}` with `nullable = true; defaultValue = null` (canonical androidx optional-argument pattern); `onAddBook` → `navigate("book_add_edit")`; `onEditBook` and the B-001 low-stock alert tap (HomeScreen.kt) → `book_add_edit?bookId=$id`; "null"-string sniffing removed. Audited the other path-arg routes (`khata_detail/{customerId}`, `bill_detail/{billId}`): all callback parameters are non-null `String` from Room entity ids — no crash-capable site. Verified: `:app:assembleDebug` BUILD SUCCESSFUL locally (Temurin 21 toolchain).
**Lesson:** On navigation 2.8.x a literal "null" path segment is NOT a string value — it deserializes to actual null and fails argument verification for non-nullable args; optional route args must use the `route?arg={arg}` + nullable + null-default pattern (D85). Inverse of the ERR-009 lesson also holds: an exact stack-line match against main proves the APK is current.

## ERR-009 — 2026-09-18 — P11 — Stale device log: pre-#60 APK formatBengaliTaka crash misread as the current add-book crash

**Type:** Misunderstanding
**Phase:** P11
**Date:** 2026-09-18
**Task:** Diagnose device-reported crash on "+" (add book) from stock screen after the PR #62 install
**Error:** Logcat pasted for the current repro instead showed the OLD crash: `java.lang.StringIndexOutOfBoundsException: length=10; index=2486` at `formatBengaliTaka(HomeScreen.kt:448)` via `HeroCard`, timestamped 09-15 06:46.
**Root cause (of the LOGGED crash, now fully identified):** `String.format("%,d", rounded)` uses the DEFAULT locale — which D82 forces to bn-BD — so the formatted string ALREADY contains Bengali digits (value 0 → "০"). The legacy `if (c.isDigit()) map[c - '0']` re-converted them because `Char.isDigit()` is true for all Unicode Nd digits → `map[0x09E6 - 48] = map[2486]` on the 10-char digit map. index=2486 = '০' − '0' exactly; HomeScreen.kt:448 matches the pre-PR-#60 source line-for-line.
**Root cause (of the confusion):** the pasted logcat was captured from the OLD APK, not the current build. Current main's formatters are range-guarded (`in '0'..'9'`) and try/catch-wrapped; a full static audit of the add-book path (nav route args → form parsing → CatalogViewModel → BookRepositoryImpl → core NumberFormatter → all format-resource specifiers %1$s/%1$d) found no crash-capable site. The current "+" crash therefore has no known stack yet — it must be captured fresh before any code change.
**Fix applied:** No speculative code change. ERR-009 logged; PR #58 confirmed superseded (same hardening, older base — main already guarded via PR #60) — recommend close. Fresh-capture procedure handed to Sakira: `adb logcat -c` → reproduce → `adb logcat AndroidRuntime:E *:S -d`; verify APK freshness first via `adb shell dumpsys package com.boikhata | findstr -i "lastUpdateTime versionName"`.
**Lesson:** A stack trace whose line numbers do not exist in main is evidence about an OLD APK, not the current build — cross-check line numbers before debugging. Durable rule: with D82 forcing bn-BD, `String.format` emits Bengali digits — NEVER pair locale-formatted output with `isDigit()`-based re-conversion; keep the `in '0'..'9'` range guard everywhere (`NumberFormatter.applyDigitStyle` depends on the same invariant).

## ERR-008 — 2026-09-17 — P10 — branch commits replaced whole files with base64 blobs (DECISIONS/ERROR_LOG/code)

**Type:** Guardrail violation
**Phase:** P10
**Date:** 2026-09-17
**Task:** PR #61 — doc appends (D82–D84, ERR-006/007) + B-001/B-002 fixes
**Error:** CI run 35193682336: `:core:domain:compileDebugKotlin` syntax errors at `1:1`; `DECISIONS.md` shrank ~848 → 1 line, `ERROR_LOG.md` ~148 → 1 line.
**Root cause:** Several branch commits (incl. `ca89d0e`, `5d246ad`) wrote base64-encoded blobs REPLACING entire file contents instead of appending; code files (`Repositories.kt`, `CatalogViewModel.kt`, `KhataAddCustomerScreen.kt`, …) were corrupted the same way by earlier commits.
**Fix applied:** `b4c4e64` restored code files from main and applied the real fixes; `06d122f` restored both governance files to main state. Side effect: the intended D82–D84 + ERR-006/007 appends were lost — re-landed via `agent/log-reland-p11-start` (D82 re-designated to the locale ruling its shipped code reference declares; B-001 routing ruling recorded as D84; premium-badge ruling needs owner re-ruling).
**Lesson:** Never rewrite a governance file wholesale — append with targeted edits and verify line-count after every write; CI "syntax error at 1:1" on a previously-green module is the whole-file-corruption signature — check file size first.

## ERR-007 — 2026-09-17 — P10 — B-002: khata list stale after add — one-shot DAO + hiltViewModel isolation

**Type:** Logic error
**Phase:** P10
**Date:** 2026-09-17
**Task:** Fix real-device Bug B (B-002) — newly added customer not reflected in the list without restart
**Error:** Customer saved successfully from `KhataAddCustomerScreen`, but the khata customer list did not update until manual reload/app restart.
**Root cause:** `KhataCustomerDao.getActiveByTenant` was a suspend one-shot snapshot, and `KhataAddCustomerScreen` runs on its own `hiltViewModel()` instance — the list screen's ViewModel never re-queried after the insert.
**Fix applied:** PR #61 (merged `a1d6316`): non-suspend `KhataCustomerDao.getActiveByTenantFlow` (Room invalidation-tracked) → `KhataRepository.getCustomersFlow` → collected by `KhataViewModel` with `CancellationException` rethrow. Ruled in D83.
**Lesson:** Room-backed list screens must be Flow-driven; one-shot suspend reads are only for snapshots that are explicitly re-fetched.

## ERR-006 — 2026-09-17 — P10 — B-001: Home crash after add-book — navigate() to unregistered NavHost routes

**Type:** Logic error
**Phase:** P10
**Date:** 2026-09-17
**Task:** Fix real-device Bug A (B-001) — hard crash right after adding a new book
**Error:**
```
Unhandled exception: java.lang.IllegalArgumentException
Navigation destination that matches request ... cannot be found from the current destination
```
(from `NavController.navigate` on tap; reported on device — invisible to unit tests)
**Root cause:** `HomeScreen` called `navigate("catalog/order/{bookId}")` and `navigate("alerts")`, but neither route exists in `BoiKhataNavigation`'s NavHost. Every newly added book (initialStock 0 ≤ lowStockThreshold 5) immediately surfaces as a low-stock alert, so the crash fired inside the add-book flow. The earlier catch(Throwable) theory was disproven: `LicenseBlockedException : Exception` and `CapExceededException : IllegalStateException` are both already caught by `catch(Exception)` in `CatalogViewModel`.
**Fix applied:** PR #61 (merged `a1d6316`): alert-card tap → `book_add_edit/{bookId}`; "See all" → `catalog` tab. Routing rule recorded in D84; dedicated Alerts screen left as `TODO(P10)` for a future owner ruling.
**Lesson:** Check every `navigate()` string against the NavHost destination set before shipping; centralize route constants so dead routes fail fast (P11 item).

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

*Last updated: 2026-09-18 · Maintained by: Agent (append) + Builder/Sakira (review)*
*Read by: the agent every session, last 10 entries mandatory*
