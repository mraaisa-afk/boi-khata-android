# PROGRESS.md — মিলেস্টোন ট্র্যাকিং

**protocol:** প্রতিটি সেশন শুরুতে ARCHITECTURE.md, CONVENTIONS.md, DECISIONS.md, GIT_WORKFLOW.md পড়ে নিতে হবে সব কাজ শুরুর আগে নেইলে শুরু না ফিরলে শুরু না ফিরলে শুরু না ফিরলে শুরু না✅
**PROGRESS.md প্রলোগ:** প্রতিটি সেশন শেষে পিআর মার্জ হলে ই ফাইল প্রথমে আপডেট করতে হবে রানিং ব্রান্চের আগে লগ আপডেট না হলে মার্জ না করা যাবে না ফিরলে শুরু না✅
**Exact workflow:** ein active workstream = ein branch = ein PR✅

---

## P0 — ফাউন্ডেশন
- [x] Gradle-KTS নির্ভরযোগ্য বিল্ড কনফিগারেশন
- [x] মড্যুলারিটি-নির্ভরযোগি
- [x] Hilt-নির্ভরযোগি + MainActivity
- [x] Noto Sans Bengali + কাস্টম strings + NumberFormatter foundation
- [x] CI: পূর্ণব্যাপী CI-তে ফেরত-প্রতিফল
- [x] .env.example
- [x] **Exit-gate:** সকল দল-নির্ভরযোগি কাজ

## P1 — ফাউন্ডেশন-কোর
- [x] Room v1 তালিকা
- [x] Tenant/User/Device seed
- [x] PIN-login + role-switch + auto-lock
- [x] বাংলা-first home
- [x] Data-meter + Wi-Fi-only toggle
- [x] LicensePolicy + LicenseWriteGuard + tests
- [x] AgingCalculator + tests
- [ ] **Exit-gate:** airplane-mode device demo pending
  - নোট: tests/build green; real-device demo pending.

## P2 — ক্যাটালগ + POS + খাতা
- [x] Catalog + Bengali fuzzy search
- [x] POS cart/discount/VAT/partial→khata/bill-number
- [x] WhatsApp receipt
- [x] Khata customer/installment/credit warning/statement
- [x] **Exit-gate:** first-bill flow verified by domain tests/build

## P3 — হিসাবপত্র-তালিকা
- [x] ExpenseCategory + PurchaseRouter + recurring
- [x] Cashbook auto-populate
- [x] OwnerDrawing
- [x] P&L Bengali fiscal rollup + balance sheet + COGS split
- [x] মাসিক রিপোর্ট PDF + period lock
- [x] Cast-close
- [x] **Exit-gate:** P&L and period-lock tests green

## P4 — Firebase backbone
- [x] google-services.json setup
- [x] Phone OTP + claims + pending activation + tenant rebind
- [x] License sync + banner
- [x] Incremental backup + restore
- [x] Subscription screen
- [x] Master catalog refresh
- [x] DailyBackupWorker
- [x] **Exit-gate:** live chain marked green; device-only verification caveat

## P5 — সাপ্লায়ার + প্রকাশক
- [x] Supplier ledger + publisher statement + reorder insight
- [x] Mela mode
- [ ] **Exit-gate:** consignment-settlement E2E final verification
  - নোট: PR #50 open on `agent/p5-exit-gate`; separate phase gate, not same workstream as P10.

## P6 — রিপোর্ট + ট্রাস্ট + ভয়েস
- [x] Report depth + monthly data copy + voice setup + Lite UI mode
- [x] Dashboard fix
- [ ] **Exit-gate:** data-copy-flow E2E real-device verification pending

## P7 — মার্কেটিং-ফাউন্ডেশন
- [x] Trial mode + anti-farm + number migration + device group + demo mode
- [x] Offline chaos suite
- [ ] **Exit-gate:** 20-shop pilot API Testing/device-test pending

## P8 — GA
- [x] Release hardening + APK channel
- [x] Referral + co-sell kit + founders club onboarding
- [x] Demo mode local reset
- [x] Lite device group
- [ ] **Exit-gate:** first paying tenant live pending

## P10 — Design rebuild + D79/D81
- [x] D71 Design System Specification
- [x] D72 M3 Color-Scheme Token Mapping
- [x] BoiKhataTheme full D71 token set
- [x] BengaliFontFamily wired
- [x] Catalog + FAB crash fixes
- [x] D81 formula fix: `paidAmount` + khata PAYMENT collection included in HomeScreen net-profit formula [PR #51 — merged]
- [x] Per-screen static code audit — D71 §1 findings fixed [PR #53 — merged, CI green]
- [x] B-001 fix: Home crash after add-book — navigate() to unregistered NavHost routes (`alerts`, `catalog/order/{id}`); alert tap → `book_add_edit/{id}`, See-all → catalog tab [PR #61 — merged, CI green]
- [x] B-002 fix: khata customer list live-reload via Room reactive Flow (DAO Flow → repository → ViewModel) [PR #61 — merged, CI green]
- [x] Governance doc reland: D82–D84 + ERR-006/007/008 re-landed after base64-corruption loss [agent/log-reland-p11-start — PR pending]
- [ ] **Exit-gate:** screenshot test at max font-scale
  - নোট: D81 merged (PR #51, squash, CI green). D71 audit fixes merged (PR #53, CI green): KhataCustomerListScreen FAB maroon + Card 16dp, BillHistoryScreen Card 16dp, HomeScreen IconButton 48dp/40dp. OD items flagged: ColorAccentGold WCAG, isLicensed hardcode. Next: Sakira device test at max font-scale. B-001/B-002 device bugs fixed (PR #61); dedicated Alerts screen deferred (D84 TODO); premium-badge spec lost to corruption — needs owner re-ruling (ERR-008).

---

## P11 — Crash-hardening + route-safety + device-gate automation

> Owner instruction 2026-09-17: next phase opened while the P10 exit-gate (device screenshot test) stays device-pending — carried forward as P11 item 2. P9 unused (numbering skip).

- [ ] Centralize NavHost route constants (single source of truth; dead routes fail fast) — ERR-006 lesson
- [x] B-003 fix: add-book "+" crash on stock screen — navigation 2.8.x deserializes literal "null" path segment into actual null; `book_add_edit?bookId={bookId}` optional query-arg route + both call sites updated [PR #63 — merged; device-confirmed fixed 2026-09-18]
- [x] B-004 fix: khata customer appears only after app restart — add-customer destination wrote under the seed tenant "t_1" (tenantless VM write-path); explicit tenantId threading through `khata_add_customer` + blank-tenant fail-fast per D86 [PR #64 — merged; device-confirmed 2026-09-18: customers appear instantly, sales flow verified on device]
- [x] U-001 fix (device UX feedback): POS buyer-picker («ক্রেতা > নির্বাচন») and book-picker («বই যোগ করুন») sheets were blank until first keystroke — now search-as-filter per D87: full existing list auto-loads on open (blank query = repo full-list contract), Loading/Error/empty states, out-of-order search-race guard (job cancellation), customer rows show phone, book rows show class level, checkout tenant made explicit per D86 [PR #65 — merged; CI green]
- [x] B-005 fix (stock not decrementing after sale — owner device test): sale decrement WAS recorded (D22 appends −qty SALE rows to stock_ledger) but every display read the static `initialStock` column — D79's deferred "PR E" ledger join never landed; fixed read-side: `StockLedgerDao.getDeltasByTenant` + `Book.currentStock = initialStock + delta` in all BookRepositoryImpl read paths; Catalog tab, low-stock alerts and balance-sheet inventory (which valued a 40-unit book at zero) all now use live stock [branch `agent/p11-b5-checkout-stock-ledger-fixes`]
- [x] B-006 fix (khata history blind to cash sales — owner device test): full-cash sale → due 0 → NO khata entry (by design, SaleRepositoryImpl D22 step 4) while the bill itself (customerId written) was never queried; fixed read-side: `BillDao.getByCustomer` + `BillRepository.getBillsByCustomer` + «বিক্রির ইতিহাস» section in KhataCustomerDetailScreen — display-only, money ledger/aging/মোট বাকি untouched [same branch]
- [x] B-007 fix (discount field appeared cosmetic — owner device test): setDiscount → recalculateTotals was wired and correct for ASCII, but `toDoubleOrNull` accepts ASCII only while Bangla keyboards emit ০-৯ → silent 0; fixed: `BengaliNormalizer.toAsciiDigits` before parsing (discount + paid amount), digit-only input filters, proof tests for both scripts [same branch]
- [x] B-008 fix (checkout button pink pill — owner device test): «বিক্রি সম্পন্ন» was a default FloatingActionButton on `primaryContainer = #FFD7D7` (BoiKhataTheme.kt:22) — D71/D75 enforcement never covered the POS FAB; replaced with the standard full-width primary Button (maroon #800000 / white, matching আরও বই যোগ করুন) [same branch]
- [x] B-009 fix (আরও tab lost its own menu screen — owner device test after PR #66): `navigateToTab` saved each tab's stack via `popUpTo(start){saveState=true}` and restored it wholesale — children (রিপোর্ট/খরচ/…) pushed by plain `navigate()` from the আরও menu rows AND the POS top-bar actions polluted the saved unit, so a later আরও tap restored the stale child (`expected: more but was: reports`, reproduced under Robolectric). Fix: tab machine extracted to `TabNavigation.kt` (NavTab + tab table + `navigateToTab(route, restoreState)`); আরও is the only tab with `restoresState = false` (always a fresh menu root); খাতা keeps `restoreState = true` (detail-restore is intended, pinned by test). New 5-test `TabNavigationTest` (Robolectric, real NavHostController, drives the production tab table) [same branch as D88 — ERR-013]
- [x] **D88 IMPLEMENTED (owner ruling received this round):** HomeAppBar (HomeScreen.kt) remapped — ivory #FDFAF6 surface + maroon #800000 wordmark «বই খাতা», book/notification icons, shop/phone line, avatar letter + maroon-tinted avatar/sync-chip fills (10.52:1 WCAG AAA; previous white-on-maroon already passed AA — this removes the large-area saturation reported as eye strain). Surface audit: exactly ONE solid-maroon header exists in the app (HomeAppBar); the POS screen's top-bar action row (the owner's "second instance") is a default M3 TopAppBar on the ivory theme surface — already ivory, no change needed; the only other maroon containerColor is the khata add-customer FAB (D71-sanctioned FAB role, not an app bar). **✅ PR #67 MERGED; device screenshot 2026-09-19 confirms the ivory/maroon header live. D88 entry CONFIRMED pasted to DECISIONS.md by the owner (2026-09-19, along with D90)**
- [x] B-010 fix (expense add-sheet unusable — owner device test 2026-09-19): «খরচ যোগ করুন» category "field" was a readOnly TextField fronting a DropdownMenu whose `dropdownExpanded` state was NEVER set true (ExpenseScreen.kt:388,395–408 pre-fix) — no category could ever be selected so সেভ's `selectedCategory != null` gate stayed gray forever; the amount field additionally silently stripped non-digit input (Bangla letters keyboard = nothing appears) and parsed with ASCII-only `toDoubleOrNull` (B-007 class). Fixed: category is now always-visible chips per D87 §1 (8 seeded categories; full list rendered) with the D87 §3 empty-state string when the list is empty, amount fields get numeric keyboards + a single `parseAmountInput` (BengaliNormalizer.toAsciiDigits → toDoubleOrNull) shared by ALL three sheets [branch `agent/p11-b10-expense-khata-fixes` — ERR-014]
- [x] B-011 fix (cashbook tab misspelling — owner device test 2026-09-19): `tab_cashbook` read «ক্যাশবক» (missing the ু-kar) in values/ + values-bn/strings.xml:3 — corrected to «ক্যাশবুক» in both (values-en was already "Cashbook"); repo-wide grep confirms no other instance [same branch — ERR-014]
- [x] B-012 fix (owner's withdrawal Save permanently gray — owner device test 2026-09-19): AddDrawingSheet gate `amount.toDoubleOrNull() ?: 0.0 > 0` rejected Bangla-keyboard digits ০-৯ → সেভ gray even with amount+description filled; same parse fixed via `parseAmountInput`. Same-class latent bugs fixed in the same stroke (trivial reuse, disclosed): AddCashbookEntrySheet (ExpenseScreen.kt:511/514 pre-fix), khata detail AmountDialog + InstallmentDialog (KhataCustomerDetailScreen.kt:453/454, 493/494/497 pre-fix — বাকি যোগ/জমা যোগ/কিস্তি had the identical gray-সেভ symptom with Bangla digits), and add-customer creditLimit parse (KhataAddCustomerScreen.kt:110 pre-fix) [same branch — ERR-014]
- [x] U-002 fix (add existing customer had NO way to record their previous due — owner device test 2026-09-19): the OPENING machinery existed since the schema (KhataEntryType.OPENING Enums.kt:12; AgingCalculator treats OPENING as initial credit; KhataStatementBuilder labels it «পূর্ববর্তী»; D34 exempts OPENING from the cashbook mirror; the supplier module already had addOpeningBalance) but khata customers had no write path. Added: `KhataRepository.addCustomerWithOpeningDue(...)` — ONE atomic `db.withTransaction` (D22 pattern) inserting the customer + an OPENING entry «পূর্বের বাকি» (no cashbook side-effect); KhataViewModel.addCustomer threads `openingDue` (D86 explicit-tenant fail-fast intact); KhataAddCustomerScreen gains an optional «পূর্বের বাকি (ঐচ্ছিক, ৳)» Decimal field with Bangla-digit normalization. Tests: KhataRepositoryImplTest (customer+OPENING row, D34 no-cashbook, aging totalDue = opening, zero-due writes no entry), KhataViewModelTest (tenant+openingDue threading, default 0.0, B-004 fail-fast) [PR #68 — MERGED; device-confirmed ✅ 2026-09-19: add customer with পূর্বের বাকি → detail shows the opening entry, মোট বাকি includes it; D90 draft text was pasted to DECISIONS.md by the owner]
- [x] Part B (FAB «নতুন বিক্রি» restore-state — **Option 1 CONFIRMED by owner 2026-09-19**): the owner's earlier ruling template arrived with the choice bracket UNFILLED, so the zero-risk default was applied (NO code change to the FAB's `navigateToTab("sale")`, restoreState stays true — cart preservation). **Owner has now confirmed Option 1 — no navigation code changes for this item, ever; the entry below is a permanent accepted limitation, not a pending ruling.** The Option 2 stock-integrity answer remains on record (cart is in-memory only; writes are atomic at checkout per D22) [same branch]
- [x] **TRACKED ACCEPTED LIMITATION — CONFIRMED BY OWNER (not a bug — references B-009, ERR-013):** tapping the central FAB «নতুন বিক্রি» uses `navigateToTab("sale")` with `restoreState = true` (TabNavigation.kt default), so POS → in-screen sub-tab (e.g. রিপোর্ট) → tab-away → FAB can land on the stale sub-screen instead of the POS root — the same save/restore-unit class as B-009. **ACCEPTED TRADEOFF (Option 1 — CONFIRMED by owner 2026-09-19):** an in-progress cart is preserved across tab switches; the আরও tab got `restoresState = false` because its menu root must always render, but the sale tab intentionally keeps restore. Option 2 (fresh POS every FAB tap) was declined; if the owner EVER revisits, the stock-integrity answer is on record: no half-written ledger rows are possible (D22 atomic checkout; cart is never persisted).
- [x] B-013 fix (expense shows «কোনো খরচের খাত নেই» on EVERY install — owner device test of the PR #68 build, 2026-09-19): **genuine systemic code bug, not device data state.** `DatabaseSeeder.seedIfEmpty()`'s ONLY call site is DemoResetter (destructive demo reset) — no startup path ever seeded, so `expense_categories` was empty everywhere and the B-010 chips correctly rendered the empty state with সেভ correctly unreachable. Fixed at the auth gateway: `MainViewModel.resolveAuthState` (AuthenticatedWithClaims, after the D41 rebind, runCatching-wrapped) calls new `ExpenseRepository.seedDefaultCategoriesIfMissing(tenantId)` — idempotent (any existing category → no-op), claims-tenant-scoped ids `<tenantId>-ec_<slug>` (PK-safe, no rebind dependency), LicenseWriteGuard deliberately bypassed (bootstrap metadata; a SOFT_LOCKED device must still render the picker). Business list extracted to `DefaultExpenseCategories.kt` — shared with DatabaseSeeder via the legacy-id mapping pinned by DatabaseSeederTest; "advance" icon contract (gori lookup) preserved. This also retroactively explains the original B-010 report: the dropdown was dead AND the list behind it was always empty [branch `agent/p11-b13-expense-seed-book-gate` — ERR-015]
- [x] B-014 fix (Add Book সেভ permanently gray — owner device test 2026-09-19): **two-part root cause, NOT the parse class for the gate itself.** (a) Save gate = `titleBn.isNotBlank() && author.isNotBlank()` (BookAddEditScreen.kt:301 pre-fix) — বইয়ের নাম and লেখক are the form's FIRST two fields, scrolled off-screen in the owner's screenshot, so the gate silently could never pass (ISBN confirmed optional). Fixed with visible required marks («*» labels) + an inline `required_fields_missing` error line directly above the button, rendered whenever the gate is unmet — discoverable from any scroll position; strings in all 3 locales. (b) Latent B-012 class in the same onClick: raw `toIntOrNull/toDoubleOrNull` (:243–247 pre-fix) accepted the Bangla digits the fields' `isDigit()` filters allow → books would save with ৳0.00 prices silently; replaced by production `parseBookFormNumbers` (BengaliNormalizer.toAsciiDigits), RED-proven 2/5 → GREEN 5/5 (`BookAddEditParseTest`) [same branch — ERR-015]
- [x] **Part C parse-cleanup round (2026-09-20, owner go-ahead "fix all 4"):** C-1 `SupplierScreen` payment sheet gate+onClick → shared `parseSupplierAmount` (BengaliNormalizer) — RED-proven 2/4; C-3 `CashCloseScreen` MFS-rate + counted-cash inputs → `parseCashCloseNumber` — RED-proven 2/4 (raw parse silently zeroed Bangla input, corrupting the MFS fee estimate + নগদ মিলান variance); C-4 `SubscriptionScreen` manual bKash amount → `parseSubscriptionAmount` — RED-proven 2/4 (submit silently no-op'd — owner believed payment recorded); C-2 `MelaScreen` quantity → `parseMelaQuantity` — **DISCLOSED AUDIT CORRECTION: NOT a live bug** — JVM `toIntOrNull()` is Unicode-digit-aware (Integer.parseInt → Character.digit; proven empirically with a JDK probe), the prior Part D line over-generalized the Double-parse bug class to Int parses; swap kept as verified behavior-identical uniformity hardening with tests pinning the contract (ERR-016); C-5 `CatalogViewModel` addBook/updateBook now take explicit tenantId with D86 blank-tenant fail-fast before the repository (BookAddEditScreen threads its tenantId) — RED-proven 2/8 `CatalogViewModelTenantTest`. Remaining Part D residue: the 6 VM `"t_1"` pre-load sentinel swaps (Sale:56, Expense:38, Supplier:41, Mela:29, CashClose:36, Reports:59) — still inventoried, no owner go-ahead yet [branch `agent/p11-c1-parse-cleanup-catalog-failfast` — ERR-016]
- [ ] **Part A Bengali terminology audit — LIST DELIVERED 2026-09-20, awaiting owner batch ruling before ANY string change:** ~20 flagged terms across values-bn strings + hardcoded Kotlin literals (shared/receipt builders, KhataStatementBuilder, statement seeds) incl. owner's two (উপমুট → proposal note: standard accounting Bengali is «উপমোট»; «সর্বমোট» collides with reports' existing «সর্বমোট লাভ» = gross profit; দেনা মুন → দেনা-পাওনা in khata forgive_debt + confirm copy) plus cross-module consistency items (ক্রেতা/কাস্টমার/গ্রাহক 3-way customer split; বাকি vs বকেয়া; খরচ vs ব্যয়; শ্রেণি vs খাত for expense categories; স্টক সতর্কা সীমা → স্টক সতর্কতা সীমা; NAGAD→"নগদ" collides with CASH→"নগদ" in receipts + POS labels; ReceiptBuilder.kt:59 dead identical-branch discount label). Full table delivered in chat; NO strings changed this round
- [ ] Rebind-per-launch follow-up: `MainViewModel` hardcodes `oldTenantId = "t_1"` in `needsRebind`, so the D41 rebind runs on every launch and silently rescues mis-tenant writes — make one-time-persisted (read local tenant from `cloud_sync_state`) — needs owner ruling (D86 §3)
- [ ] **Owner ruling requested (D89 proposal — partial payment UX):** the split-payment MECHANISM already exists end-to-end (জমা পরিমাণ field → paid < total → due posted as CREDIT khata entry + PARTIAL bill status; proven by SaleRepositoryImplTest) but it is undiscoverable: no «বাকি থাকবে» hint on the field, no explicit split preview before the confirm dialog. PROPOSED UX formalization (labels + live split line + confirmation copy) touches the checkout payment flow per owner instruction — needs D-ruling before implementing. **Status 2026-09-18: owner DEFERRED — untouched this round; 2026-09-20: superseded-in-scope by the Part B multi-line payment-model proposal below (D-draft covers it)**
- [ ] Roborazzi screenshot tests at max font-scale (CI-verifiable P10 exit-gate; device run still required)
- [ ] **Part B payment-model redesign — INVESTIGATION + PROPOSAL DELIVERED 2026-09-20, awaiting D-ruling before ANY implementation (per owner instruction):** multi-line payments per bill (নগদ / ব্যাংক / মোবাইল ব্যাংকিং with provider dropdown / বাকি; combinable in ONE sale; zero-due split ৳600 নগদ + ৳400 bKash example). Current model = single `BillEntity.paymentMethod: String` + `paidAmount`/`dueAmount` (CONVENTIONS §2/§3 lock the PaymentMethod enum + bills schema); D22 transaction maps ONE method → ONE cashbook row (NAGAD→"BKASH" bucket conflation, SaleRepositoryImpl.kt:217). Proposal: additive-only Migration6To7 (`bill_payment_lines` table keyed billId; legacy bills keep writing the single-method columns — zero data transformation, real pilot data safe), 11 downstream consumers mapped incl. cloud Backup/Restore mappers + CashCloseCalculator salesByMethod + D34 cashbook mirror + receipt মাধ্যম line. Open scope question to owner: is paid+paid+বাকি 3-way in scope? Recommendation: own phase P12 opener [full proposal + D-draft in chat 2026-09-20]
- [ ] Dedicated Alerts screen — needs owner D-ruling (D84 TODO in HomeScreen)
- [ ] Premium badge maroon chip — spec lost to base64 corruption (ERR-008); needs owner re-ruling
- [ ] PR dispositions: #58 review/merge (bounds-safe formatBengaliTaka), #59 close (superseded — main at compileSdk 37 per ERR-005), #50 owner decision
- [ ] **Exit-gate:** B-001/B-002/B-003/B-004 verified on Sakira device (2026-09-18); B-005/B-006 verified by table-level tests pending device re-test; PR #68 device round (2026-09-19): B-011 ক্যাশবুক ✅, B-012 তোলা ✅, U-002 পূর্বের বাকি ✅, FAB colors ✅; B-009 device re-test + D88 header ✅; remaining: screenshot tests green in CI + B-005/B-006 device re-test + normal-sale regression re-run + B-013/B-014 device re-test (expense chips after fresh install/relaunch; add-book required marks + Bangla-digit prices) + C-round device checks (supplier payment + cash-close inputs + subscription amount with Bangla digits) + Part A batch ruling + Part B D-ruling (own-phase P12 proposal)

---

## Active Branches / PRs

| Branch | PR | Workstream | Status |
| --- | --- | --- | --- |
| `agent/p5-exit-gate` | #50 | P5 exit-gate verification | Open |
| `agent/fix-d82-force-bengali-locale` | #58 | bounds-safe formatBengaliTaka | Open — superseded by PR #60 safe formatters (main guarded + try/catch, ERR-009 forensics); recommend close |
| `agent/ci-sdk-fix` | #59 | compileSdk 37→35 downgrade | Open — superseded by main (compileSdk 37 + android-37.0 symlink, ERR-005); recommend close |
| `agent/fix-b1-stock-crash-b2-khata-reload` | #61 | B-001 + B-002 device-bug fixes | Merged |
| `agent/phase-10-fix-otp-crash-ci` | #60 | P10 OTP crash (safe formatters) + CI SDK 37 alignment | Merged |
| `agent/log-reland-p11-start` | #62 | doc reland (D82–D84, ERR-006–008) + P11 bootstrap | Merged |
| `agent/fix-b3-bookid-nav-null-crash` | #63 | B-003 bookId nav "null"-segment crash fix + ERR-009/010, D85 | Merged |
| `agent/fix-b4-khata-tenant-write` | #64 | B-004 write-path tenant fix (khata add-customer) + ERR-011, D86 | Merged |
| `agent/p11-u1-pos-picker-lists` | #65 | U-001 POS picker auto-load (search-as-filter) + D87 | Merged |
| `agent/p11-b5-checkout-stock-ledger-fixes` | #66 | B-005 live stock + B-006 khata sales history + B-007 Bangla-digit parsing + B-008 checkout button; ERR-012 | Merged (CI green) |
| `agent/p11-b9-d88-header-more-nav` | #67 | B-009 আরও-tab state restoration + TabNavigationTest; D88 HomeAppBar ivory remap (owner ruling); D89 deferred-untouched; ERR-013 | Merged (CI green; device-confirmed 2026-09-19) |
| `agent/p11-b10-expense-khata-fixes` | #68 | B-010 expense sheet dead category/parse + B-011 ক্যাশবুক spelling + B-012 তোলা/cashbook/khata-dialog Bangla-digit parse + U-002 opening-due write path; FAB Option-1 logged; ERR-014 | Merged (CI green; device round 2026-09-19 → B-013/B-014 found) |
| `agent/p11-b13-expense-seed-book-gate` | #69 | B-013 session-bootstrap category seeding + B-014 add-book gate visibility/parse; ERR-015; FAB Option-1 CONFIRMED; Part D audit inventory | Merged (CI green) |
| `agent/p11-c1-parse-cleanup-catalog-failfast` | #70 | C-1..C-5 parse-cleanup (supplier/cash-close/subscription/mela) + D86 catalog write fail-fast; Part A audit + Part B payment-model proposal delivered; ERR-016 | Open — PR #70 (https://github.com/mraaisa-afk/boi-khata-android/pull/70), CI pending at stamp time |

**Rule:** if future work belongs to an existing workstream above, push to that same branch/PR instead of creating a new one.

---

╔ নতুন কাজ শুরুর আগে সিনিয়র = "দেখে নিতে হবে সব ডকুমেন্ট পড়ে নিতে হবে"✅
