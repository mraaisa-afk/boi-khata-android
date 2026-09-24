# PHASE_PLAN.md — Boi-Khata Coder Navigation Aid

> **THIS FILE IS NOT A GATE FILE.**
> `PROGRESS.md` is the sole authoritative gate owner (per Decision D66).
> This file helps the **Boi-Khata Coder agent** navigate phases efficiently.
> Gate status, exit-gate checks, and phase progression all live in `PROGRESS.md`.

**Agent read-order before every session:**

1. `PROGRESS.md` — first unchecked item = your current task
2. This file -> find that phase row -> D-refs, files to read, open blockers
3. `DECISIONS.md` — all D-entries listed for that phase
4. `ERROR_LOG.md` — past mistakes in scope
5. `Boi-Khata-Master-Blueprint.md` — Constitution sections 0-14

---

## Phase Quick-Reference Table

| Phase | Name | Status | D-decisions | Design Spec? |
| --- | --- | --- | --- | --- |
| **P0** | স্কেলেটন ও গার্ডরেল | Complete | D1-D5 | No |
| **P1** | লোকাল-ফাউন্ডেশন | Exit-gate open | D6-D14, D25 | No |
| **P2** | ক্যাটালগ + POS + খাতা | Complete | D21, D24, D26, D2 | Partial (POS) |
| **P3** | হিসাব-কোর | Complete | D24-D38 | Partial (Reports) |
| **P4** | Firebase-ব্যাকবোন | Complete | D40-D50 | No |
| **P5** | সাপ্লায়ার + মেলা | Exit-gate open | D51-D58 | No |
| **P6** | রিপোর্ট + ট্রাস্ট + ভয়েস | Open | D37-D38 + TBD | Yes (Analytics Img 8) |
| **P7** | পাইলট-হার্ডেনিং | Open | TBD | No |
| **P8** | GA | Exit-gate open | TBD | No |
| **P10** | Design rebuild + D79/D81 | Items done; exit-gate open (device) | D71–D84 | Yes (Design v2) |
| **P11** | Crash-hardening + route-safety + device-gate automation | Open | D82–D84 (basis) | No |
| **P12** | Multi-line payment model (bill_payment_lines) | **In progress — opened by owner override 2026-09-24 (P11 exit-gate still open)** | D92 (draft re-issued this round) | No |
| **Post-GA** | Speculative | Not in PROGRESS.md yet | TBD | Yes (Design v2) |

---

## P0 — স্কেলেটন ও গার্ডরেল

**Agent note:** Fully complete. Do not modify P0 scaffolding without a D-decision.

**Built:** Gradle-KTS + version catalog, module shells, Hilt wiring + MainActivity + BoiKhataTheme, Noto Sans Bengali + NumberFormatter + strings (values-bn), CI gradlew build on every PR.

**D-decisions:** D1-D5

**Forbidden without D-decision:** New modules, version catalog changes, CI pipeline changes.

---

## P1 — লোকাল-ফাউন্ডেশন

**Status:** Logic complete. Exit-gate open — airplane-mode demo is device-only, not runnable in sandbox.

**Agent note:** If asked to work in P1 scope, flag that the exit-gate is unverified on device. Do not check P1 complete in `PROGRESS.md` until Sakira runs the demo.

**Built:** Room v1 schema, Tenant/User/Device + seed (t_1 + OWNER + GRACE license), PIN-login + role-switch (SessionManager) + 2-min auto-lock + biometric stub, খাতা-প্রথম Home (Room-backed), data meter, LicenseWriteGuard + AgingCalculator (FIFO). **44 tests passing.**

**D-decisions:** D6-D14, D25

**Open blocker:** Airplane-mode demo on a real device (Sakira runs manually).

---

## P2 — ক্যাটালগ + POS + খাতা

**Agent note:** Fully complete. **88 tests passing.**

**Built:** Catalog (Bengali fuzzy search via BengaliNormalizer + LIKE), POS (cart, discount PERCENT/FLAT, VAT split, partial -> auto-khata, bill number INV-YYYYMMDD-NNNN), WhatsApp receipt, Khata (name + area-key, installment, credit-limit warning, statement).

**D-decisions:** D2, D21, D24, D26

**Deferred to P5:** dispute-freeze, cohort tags.

---

## P3 — হিসাব-কোর

**Agent note:** Fully complete. **203 tests passing.**

**Built:** ExpenseCategory seed + 1-tap entry + PurchaseRouter, ঘরি sub-ledger (GoriBalanceCalculator), RecurringExpenseCalculator + BudgetAlertCalculator, Cashbook 3-account (নগদ/বিকাশ/ব্যাংক), OwnerDrawing, P&L monthly + BengaliFiscalCalendar + BalanceSheetCalculator-lite + COGS split, হিসাব-প্যাক builder, PeriodLockGuard, CashCloseCalculator.

**D-decisions:** D24-D38

---

## P4 — Firebase-ব্যাকবোন

**Agent note:** Fully complete. **296 tests passing.** All Firestore round-trips CANNOT be verified without a real device.

**Built:** Phone OTP + ClaimsSession state machine + PendingActivationScreen, TenantRebindPlanner + one-time rebind Room transaction, LicenseSync + LicenseBanner, incremental backup + RestoreMapper, Subscription screen (PENDING-only), master catalog refresh (CatalogDeltaDetector), DailyBackupWorker.

**D-decisions:** D40-D50

**Files to read before any P4 modification:** `Firebase-Project-Context.md`, `DECISIONS.md` D40-D50, `CONVENTIONS.md` section 6.

---

## P5 — সাপ্লায়ার + মেলা

**Status:** Logic written. Exit-gate open — E2E test NOT RUN.

**Agent note:** The consignment settlement E2E test needs a real build. Flag to Sakira before marking P5 complete.

**Built:** দেনা-খাতা (SupplierEntryType, SupplierAgingCalculator FIFO, payable ledger), পাবলিশার-স্টেটমেন্ট, ReorderInsightCalculator, MelaStockCalculator, MelaRepository, `mela_sessions` table.

**D-decisions:** D51-D58

**Open blocker:** `./gradlew build` NOT run for P5.

---

## P6 — রিপোর্ট + ট্রাস্ট + ভয়েস

**Gate check:** P5 exit-gate must be complete in `PROGRESS.md` before starting P6.

**Remaining:** 12-month trend charts, top-10, comparison view, monthly data copy (CSV + share), Bengali TTS, Lite-UI mode toggle.

---

## P7 — পাইলট-হার্ডেনিং

**Gate check:** P6 exit-gate must be complete before starting P7.

**Remaining:** Trial-mode + anti-farm + number migration + device-group manager + demo-mode, offline chaos suite, 20-shop pilot APK.

---

## P8 — GA

**Status:** 4/5 items done. Exit-gate (first paying tenant) requires vendor activation.

**Done:** R8 + signed APK + version 0.8.0 / code 8, referral + co-sell kit + Founders Club, demo-mode, Lite device-group.

**Open:** First paying tenant live.

---

## P10 — Design rebuild + D79/D81

**Status:** Items complete through PR #61 (B-001/B-002 device-bug fixes). Exit-gate open — screenshot test at max font-scale is device-only (Sakira).

**Built:** D71 design tokens + BoiKhataTheme, BengaliFontFamily wiring, D79/D81 HomeScreen net-profit formula, D82 forced bn-BD locale (backfill), D83 khata reactive Flow, D84 NavHost routing rule.

**D-decisions:** D71–D84 (subset)

**Open blocker:** Max font-scale device test; U-001 + B-005/B-006 device re-test (stock changes after sale; khata shows বিক্রির ইতিহাস); dedicated Alerts screen needs D-ruling (D84 TODO); premium-badge re-ruling (ERR-008); rebind one-time-persist (D86 §3); home-header ivory remap + partial-payment UX need owner rulings (D88/D89 proposals, PROGRESS).

---

## P11 — Crash-hardening + route-safety + device-gate automation

**Gate note:** Opened by owner instruction 2026-09-17 while the P10 exit-gate remains device-pending; the device test is carried into P11. P9 unused (numbering skip).

**Scope:** Device-bug fixes (B-003 add-book "+" crash per D85, PR #63, device-confirmed; B-004 khata customer only-after-restart per D86, PR #64, device-confirmed; U-001 POS pickers blank-on-open per D87, PR #65, merged; B-005 stock display never moved after sale (read-side ledger derivation), B-006 khata history blind to cash sales (getBillsByCustomer + বিক্রির ইতিহাস), B-007 discount Bangla-digit parsing, B-008 checkout button pink FAB → standard primary button — ERR-012, PR #66 merged; B-009 আরও tab rendered stale child after cross-tab navigation (restoresState=false for the hub tab + TabNavigationTest) — ERR-013, PR #67 merged; B-010/B-011/B-012 expense-screen dead category picker + ASCII-only parses + «ক্যাশবক» spelling + U-002 customer opening-due atomic write path (addCustomerWithOpeningDue) — ERR-014, PR #68 merged + device round 2026-09-19 (ক্যাশবুক/তোলা/পূর্বের বাকি/FAB colors ✅); B-013 expense categories empty on EVERY install (seeder only reachable from demo reset → session-bootstrap seeding at MainViewModel auth resolution) and B-014 add-book gate = invisible required fields + latent raw onClick parse — ERR-015), D88 HomeAppBar ivory remap (owner ruling implemented, merged, device-confirmed; D88+D90 CONFIRMED pasted to DECISIONS.md by owner 2026-09-19), NavHost route constants (dead-route fail-fast), Roborazzi max-font-scale screenshot tests, Alerts screen (pending D-ruling), premium-badge re-ruling (ERR-008), open-PR dispositions (#58/#59/#50), rebind-per-launch follow-up (D86 §3 — needs owner ruling), D89 partial-payment UX — DEFERRED by owner, untouched; Part D systemic cleanup round — 4 raw-parse sites + CatalogViewModel D86 fail-fast IMPLEMENTED 2026-09-20 (owner go-ahead "fix all 4"; C-1 supplier + C-3 cash-close + C-4 subscription RED-proven real bugs, C-2 mela disclosed audit correction — toIntOrNull is Unicode-aware, no live bug, kept as uniformity hardening, ERR-016; 6 VM t_1 sentinels REMAIN inventoried, no go-ahead yet); Part A Bengali terminology audit list DELIVERED to owner 2026-09-20 (batch ruling pending, no strings changed); Part B multi-line payment-model redesign INVESTIGATION + PROPOSAL delivered 2026-09-20 (D-ruling pending; recommended as own phase P12 opener; additive Migration6To7 design; 3-way paid+paid+বাকি scope question open).

**D-decisions:** D82–D84 (basis); D85 (B-003 optional-arg navigation ruling); D86 (write-path tenant threading, fixes B-004); D87 (search-as-filter pickers + U-register, fixes U-001); **D88 — RULED by owner, implemented + merged via PR #67 (D71 §1 superseded); owner pasted D88+D90 to DECISIONS.md 2026-09-19; D89 — DEFERRED by owner (partial-payment UX, untouched); D90 — RULED + pasted by owner (U-002 opening-due write path); FAB «নতুন বিক্রি» restore-state — OPTION 1 CONFIRMED by owner 2026-09-19 (tracked accepted limitation, no code change); B-013 seeding policy — DRAFT D-entry text in PR #69 description (session-bootstrap defaults, idempotent, claims-tenant-scoped), pending owner DECISIONS.md commit; Part B payment-model redesign — DRAFT D-entry text delivered in chat 2026-09-20 (multi-line bill_payment_lines, additive Migration6To7, CONVENTIONS §2/§3 amendment required), pending owner ruling; Part A terminology batch — owner ruling pending (no strings touched)**

**2026-09-24 status update:** PR #70 MERGED at `f1bce2d`. The Part A implementation commits (`767e190`/`743b86a`) were **never pushed** (no token that session) and died with the sandbox reset — the owner's device regression report (2026-09-24) proved main never had them (ERR-018); the batch is RE-LANDED on `agent/part-a-terminology-reland` (`bba3208`, RED→GREEN re-proven). Part A rulings A1–A5 all received 2026-09-20 and are now implemented (A5b হাটি ক্রেতা + A5c MFS-ফি explicitly NOT renamed per ruling). Part B (P12) implementation **authorized by owner 2026-09-24 by explicit override of the open P11 exit-gate** — see the P12 section. ERR-017 (lost) re-created; ERR-018 written.

---

## P12 — Multi-line payment model (bill_payment_lines)

**Gate note — OWNER OVERRIDE, LOGGED 2026-09-24:** P12 implementation began **before the P11 exit-gate formally closed** (open items: B-005/B-006 + normal-sale + B-013/B-014 + C-round device re-tests, Roborazzi screenshot tests, Part A device sweep, D91/D92 DECISIONS.md pastes). The owner explicitly authorized starting P12 now and instructed that this be logged so it is never mistaken for a properly closed gate. The P11 exit-gate items remain open and mandatory.

**Owner requirements (2026-09-24 restatement, per D92 draft):**
1. Payment method categories at checkout: নগদ (cash) / ব্যাংক (bank or bank transfer) / মোবাইল ব্যাংকিং (mobile banking — shows a provider selector: বিকাশ, নগদ (Nagad), রকেট, উপায়, অন্যান্য) — বাকি (credit) remains its own option.
2. MULTIPLE payment lines combinable within a SINGLE sale (e.g. ৳1000 = ৳600 নগদ + ৳400 mobile banking → zero remaining due).
3. 3-way combination explicitly IN SCOPE (ruled in D92): paid + paid + remaining বাকি in the same sale (e.g. ৳600 cash + ৳300 mobile + ৳100 বাকি posted to the customer's খাতা).
4. Build per the full D92 specification: additive-only Migration6To7 for `bill_payment_lines` (zero data transformation; legacy bills keep the single-method columns), D22 atomic transaction extended to write all payment lines + per-line cashbook mirror rows in ONE transaction, explicit **MOBILE** cashbook bucket (ruling: do NOT reuse BKASH), and every downstream consumer updated (CashCloseCalculator, CashCloseReportBuilder, CashCloseScreen, CashCloseRepositoryImpl, ReceiptBuilder, PosScreen, cloud Backup/Restore mappers, BillHistoryScreen).
5. RED→GREEN test discipline; MigrationTestHelper test for 6→7; backup/restore round-trip tests for old-format AND new-format bills.
6. CONVENTIONS §2 (PaymentMethod enum) + §3 (bills schema) amendment is a P12-implementation-time task — done in this phase (the owner ruled MOBILE bucket in advance).

**Status (2026-09-24, post-implementation):** IMPLEMENTED — schema v7 + D22 `createBillWithPaymentLines` + per-line cashbook + MOBILE bucket + POS multi-line editor + all consumers; 14-module testDebugUnitTest green, 9-module lintDebug green, assembleDebug green. **Delivery incident:** PR #72 was merged into its stacked base branch (`agent/part-a-terminology-reland` @ `801a4bc`) instead of main — P12 was briefly NOT on main while the owner believed it was; no CI ran for that merge. Logged as **ERR-019**; re-landed as **PR #73 (base main)** with identical content. Lesson recorded: retarget a stacked PR's base immediately after its base PR merges, or open non-stacked PRs.

**Provider-list mechanism (decision recorded):** hardcoded starter set as a single Kotlin source of truth (like DefaultExpenseCategories but compile-time): বিকাশ / নগদ (Nagad) / রকেট / উপায় / অন্যান্য. Rationale: the MFS provider market is small and stable; a DB-managed list (expense-categories style) would add a table + CRUD/settings UI that D92 v1 did not budget; the Standing Instruction means this can be revised any time via a D-entry — the upgrade path (additive table + settings screen) stays open.

**Status:** IMPLEMENTED 2026-09-24 on `agent/p12-multi-line-payments` (schema v7 + D22 extension + per-line cashbook MOBILE bucket + POS multi-line editor + all consumers + backup/restore + MigrationTestHelper/Robolectric tests). Pending: push + PR + CI, owner device test (migration on live pilot data; 2-way/3-way splits), D92 paste.

**D-decisions:** D92 (drafted 2026-09-20; re-issued with this round's report for owner paste).

**P12 device-round follow-up (2026-09-24, branch `agent/p13-device-round-fixes`):** owner device testing of the merged P12 build surfaced 5 issues — fixed per mandatory protocol:
1. **D93 (new ruling, implemented):** overpayment at checkout — named customer → excess posts to their খাতা as a জমা entry INSIDE the D22 transaction; walk-in → rejected with an explanatory message. CashClose gains a `khataAdvance` bucket (মোট বিক্রি stays = sales). D93 draft text re-issued for DECISIONS.md.
2. **D94 (new ruling, implemented):** revenue = ALL bill totals (credit included) — Home + P&L; নিট লাভ = (revenue − COGS) − expenses via a new `getCogsByDateRange` JOIN (books.purchasePrice; deleted books contribute 0 — disclosed, same basis as the D29 P&L); আজকের হিসাব labels → «নগদে X / [provider] হতে X / বাকিতে X»; গোনা নগদ field placeholder → «৳ পরিমাণ লিখুন». D94 draft text re-issued for DECISIONS.md.
3. **Issues 3/4 (ইলিয়াস +400 missing, রহিম's empty history):** NOT live code bugs (both write/read paths proven correct at file/line level, both generations); consistent with an out-of-sync cloud restore (bills ↔ khata_entries collections restore independently). Shipped: restore-time khata integrity repair (KhataRepairPlanner + RestoreRepositoryImpl.repairMissingKhataCredits, idempotent). রহিম's bills — if absent from the backup — are unrecoverable from code; owner confirmation pending.

---

## Next-Phase Eligibility Check

Before starting any phase:

- [ ] Open `PROGRESS.md`, find the previous phase exit-gate checkbox. Not ticked? **STOP. Alert Sakira + Builder.**
- [ ] Any Pending Owner Ruling touching this phase scope? If yes -> **STOP. List them. Wait.**
- [ ] New dependency needed? If yes -> **STOP. Propose a D-decision. Wait for the D-number.**
- [ ] All D-decisions for this phase read? If no -> read `DECISIONS.md` first.

---

*Last updated: 2026-09-24 · Maintained by: Builder + Sakira Suva*
*Gate authority: PROGRESS.md (D66) · Phase numbering: P0 to P12 (P9 unused)*
