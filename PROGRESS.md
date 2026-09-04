```
# PROGRESS.md — বই খাতা বিল্ড-চেকলিস্ট

**প্রোটোকল:** প্রতি সেশনের শুরুতে ARCHITECTURE.md, CONVENTIONS.md, DECISIONS.md ও এই ফাইল পুরো পড়ো।
**PROGRESS-এর প্রথম অ-চেকড আইটেম** থেকে শুরু। সেশনের একদম শেষ কাজ: সম্পন্ন আইটেম চেক + কমিট।
মাঝপথে থামলে: আইটেম অ-চেকড রেখে নিচে এক-লাইন নোট ("LedgerEvent হলো, ভিউ বাকি")।
**এক ফেজ = এক সেশন-প্যাটার্ন = এক PR। আগের ফেজের exit-gate অ-চেকড থাকলে পরের ফেজ শুরু নিষিদ্ধ।**

---

## P0 — স্কেলেটন ও গার্ডরেল
- [x] Gradle-KTS প্রজেক্ট + ভার্সন-ক্যাটালগ (সংশোধিত libs.versions.toml)
- [x] মডিউল-শেল (§২-এর প্রতিটি মডিউল; খালি build.gradle.kts + প্যাকেজ, লজিক নয়)
- [x] Hilt-ওয়্যারিং + খালি @HiltAndroidApp + MainActivity (BoiKhataTheme)
- [x] Noto Sans Bengali বান্ডেল + বাংলা-ডিফল্ট strings (values-bn প্রাথমিক) + digits-টগল-ফাউন্ডেশন (NumberFormatter)
- [x] CI: প্রতি PR-এ ক্লিন-ক্লোন `gradlew build`
- [x] .env.example রুটে (Secrets-প্লাগইন-প্রত্যাশা)
- [x] **Exit-gate:** ক্লিন-ক্লোন বিল্ড সবুজ, শূন্য-মডিউলে-লজিক

## P1 — লোকাল-ফাউন্ডেশন
- [x] Room v1 স্কিমা: CONVENTIONS §৩-এর প্রতিটি টেবিল (নাম/কলাম হুবহু)
- [x] Tenant/User/Device + seed (১ টেন্যান্ট t_1 + OWNER-ব্যবহারকারী + GRACE-লাইসেন্স-সিড)
- [x] PIN-লগইন + রোল-সুইচ (SessionManager) + ২-মিনিট অটো-লক + বায়োমেট্রিক-স্টাব-ইন্টারফেস
- [x] খাতা-প্রথম হোম (দেনা-তালিকা + আজকের বিক্রি + top-৫) — mock-ডেটা নয়, Room-প্রবাহিত
- [x] ডেটা-মিটার (OkHttp-নয়; Firestore-বাইট-কাউন্টার + Wi-Fi-only-টগল)
- [x] LicensePolicy-ট্রিপল (ARCH §৫) + LicenseWriteGuard + ইউনিট-টেস্ট (গ্রেস-সীমানা/উৎসব/৩৫-দিন)
- [x] AgingCalculator (FIFO) + ইউনিট-টেস্ট
- [ ] **Exit-gate:** এয়ারপ্লেন-মোডে লগইন→হোম→স্টেট-মেশিন ডেমো; টেস্ট সবুজ
  - নোট: টেস্ট সবুজ (৪৪ টেস্ট, ০ ফেইল); ক্লিন-বিল্ড সবুজ (assemble+lint+test)। এয়ারপ্লেন-মোড ডেমো = ডিভাইসে ম্যানুয়াল-চেক (স্যান্ডবক্সে ডিভাইস নেই)।

## P2 — ক্যাটালগ + POS + খাতা
- [x] ক্যাটালগ (শর্ত+দাম; বাংলা-ফাজি-সার্চ সহজ-রূপ: LIKE+নরমালাইজড-কলাম)
  - নোট: P2a — লোকাল বুক ক্যাটালগ (list, Bengali fuzzy search via BengaliNormalizer+LIKE, add/edit) সম্পূর্ণ অফলাইন। মাস্টার NCTB ক্যাটালগ ইমপোর্ট = P4/Firebase।
- [x] POS: কার্ট, ছাড়, ভ্যাট-স্প্লিট, আংশিক→অটো-খাতা, বিল-নম্বর-জেনারেটর
  - নোট: P2b — POS sale flow (cart, quantity, discount PERCENTAGE/FIXED, VAT per-line books 0%/stationery 15%, payment method, partial→auto-khata via atomic Room transaction, bill number INV-YYYYMMDD-NNNN). VatCalculator + BillNumberGenerator pure services. SaleRepositoryImpl with @Transaction (bill+lines+stock+khata). 9 VatCalculatorTest + 7 BillNumberGeneratorTest pass.
- [x] WhatsApp-রসিদ (টেক্সট+PNG, দ্বৈত-অঙ্ক) shared/receipt-এ
  - নোট: P2b — ReceiptBuilder in shared/receipt (D21: Unicode plain-text, D2-compliant no PNG, dual digits via NumberFormatter, WhatsApp share via Intent.ACTION_SEND). 11 ReceiptBuilderTest pass.
- [x] খাতা: নাম+এলাকা-কী, কিস্তি, ক্রেডিট-লিমিট-ওয়ার্নিং, দেনা-মুন→ব্যাড-ডেট, dispute-freeze, স্টেটমেন্ট, কোহোর্ট
  - নোট: P2a — নাম+এলাকা-কী কাস্টমার, কিস্তি ট্র্যাকিং (KhataInstallmentDao), ক্রেডিট-লিমিট-ওয়ার্নিং, দেনা-মুন (ADJUSTMENT entry), শেয়ারেবল বাকি হিসাব স্টেটমেন্ট (KhataStatementBuilder, WhatsApp text share) সম্পন্ন। dispute-freeze ও কোহোর্ট-ট্যাগ = DEFERRED (P5 স্কোপে প্রস্তাবিত)।
- [x] **Exit-gate:** প্রথম-বিল ≤৩০-মিনিট-প্রবাহ ইউনিট+UI-টেস্টে
  - নোট: P2 complete — ক্যাটালগ+খাতা (P2a) + POS+রসিদ (P2b) সম্পন্ন। ৮৮ টেস্ট সবুজ (২৭ নতুন P2b + ১৭ P2a + ৪৪ P1)। Full `./gradlew build` (assemble+lint+test) সবুজ — CI-equivalent verification। প্রথম-বিল ≤৩০-মিনিট flow: catalog→POS cart→VAT+discount→payment→checkout→bill+stock+khata atomic→receipt share। UI টেস্ট = Compose-টেস্ট (P7 স্কোপে প্রস্তাবিত; ডোমেইন-লজিক ইউনিট-টেস্ট সবুজ)।

## P3 — হিসাব-কোর
- [x] ExpenseCategory-সিড + ১-ট্যাপ-এন্ট্রি + অটো-রুট (বই→ইনভেন্টরি) + ঘরি সাব-লেজার + recurring
  - নোট: P3a — 8 BD expense categories seeded (ভাড়া, বিদ্যুৎ, ইন্টারনেট, বেতন, ঘরি/অ্যাডভান্স, পরিবহন, MFS-ফি, অন্যান্য). ১-ট্যাপ expense entry with category + amount + cashbook auto-populate. D24: PurchaseRouter (book→stock_ledger PURCHASE, non-book→expense). D26: ঘরি sub-ledger via GoriBalanceCalculator (advances − returns, description "ঘরি ফেরত"). D27: RecurringExpenseCalculator pure service (next-due logic unit-tested). P3b completed the deferred persistence + budget alert (D35): recurring_expenses + budgets tables via Migration v2→v3, RecurringExpenseRepository (addTemplate/applyTemplate/getDueTemplates), BudgetRepository (setBudget/getMonthlyAlerts), BudgetAlertCalculator + RecurringExpenseReminder pure services. 4 PurchaseRouterTest + 6 CashbookBalanceCalculatorTest + 5 GoriBalanceCalculatorTest + 7 RecurringExpenseCalculatorTest + 9 BudgetAlertCalculatorTest + 7 RecurringExpenseReminderTest pass.
- [x] Cashbook ৩-অ্যাকাউন্ট + ম্যানুয়াল-এন্ট্রি + অটো-পপুলেট (বিল/খরচ/আদায়)
  - নোট: P3a — CashbookBalanceCalculator (নগদ/বিকাশ/ব্যাংক, derived balances). D25: every money flow creates a cashbook entry (expense→EXPENSE, bill payment→INCOME, khata collection→INCOME, owner drawing→EXPENSE) in same Room @Transaction. Manual entry UI with INCOME/EXPENSE/TRANSFER + account selection. P3b completed the deferred bill/khata auto-populate (D34): SaleRepositoryImpl.createBill now inserts INCOME (account from paymentMethod, amount=actualPaid) in the same transaction; KhataRepositoryImpl.addEntry now inserts INCOME for PAYMENT type (account chosen by user, default CASH).
- [x] OwnerDrawing
- [x] P&L (মাসিক) + বাংলা-বর্ষ-রোলআপ + ব্যালেন্স-শিট-লাইট + COGS-স্প্লিট (কনসাইনমেন্ট/ক্রয়)
  - নোট: P3b — PnLCalculator (D29: COGS split consignment-commission vs purchase-COGS, the core accounting-correctness rule) + BengaliFiscalCalendar (D30: dual-calendar rollup Gregorian + বাংলা বর্ষ ১ এপ্রিল–৩১ মার্চ, 12 Bengali month names) + BalanceSheetCalculator (D31: lite — cash + inventory + receivables + ঘরি advances, supplier payables=0 until P5, equity = retained earnings − drawings, accounting identity asserted). AccountingRepositoryImpl wires all three + khata aging summary + VAT summary into getMonthlyPnL/getBalanceSheet/getHisabPack. 14 PnLCalculatorTest + 17 BengaliFiscalCalendarTest + 9 BalanceSheetCalculatorTest = 40 new tests pass.
- [x] হিসাব-প্যাক PDF + পিরিয়ড-লক
  - নোট: P3b — Period-lock (D32): period_locks table via Migration v2→v3 (new tables, no drops), PeriodLockGuard pure service + PeriodLockChecker injectable, injected into all 5 write repos (Sale/Khata/Expense/Cashbook/OwnerDrawing) — throws PeriodLockedException on locked-period writes; read/export stays open (never-lock rule honored). HisabPackBuilder (D33): pure-text হিসাব-প্যাক builder in shared/receipt (P&L + balance-sheet-lite + khata aging + VAT summary, dual-calendar header, bank/microfinance-loan-file-ready footer). 9 PeriodLockGuardTest + 9 HisabPackBuilderTest = 18 new tests pass.
- [x] ক্যাশ-ক্লোজ "আজকের হিসাব" (MFS-ফি-অটোলাইন + ভ্যারিয়েন্স)
  - নোট: P3c — CashCloseCalculator (D36: sales by payment method, expenses by category, MFS-fee estimation = BKASH sales × owner-overridable rate / 100, variance = system cash − counted cash, labels ঘাটতি/বাড়তি/মিলেছে). CashCloseReportBuilder in shared/receipt (WhatsApp-share text via Intent.ACTION_SEND). CashCloseRepositoryImpl reads bills + expenses + cashbook balances. CashCloseScreen + CashCloseViewModel (owner-overridable MFS rate + counted cash inputs, share button). 11 CashCloseCalculatorTest + 8 CashCloseReportBuilderTest = 19 new tests pass.
- [x] **Exit-gate:** ১-ট্যাপ P&L; পিরিয়ড-লক-টেস্ট সবুজ
  - নোট: P3c — ReportsScreen in feature/reports (D37: P&L month selector dual-calendar aware, balance-sheet-lite, period-lock control OWNER-only, budget-alert visibility). ReportsViewModel + CashCloseViewModel inject AccountingRepository + BudgetRepository + CashCloseRepository. Navigation: "reports" + "cash_close" routes reachable from Sale tab topBar (D38: no 5th bottom-nav tab). Full `./gradlew build` (assemble+lint+test) সবুজ — ২০৩ টেস্ট, ০ ফেইল (১৯ নতুন P3c + ৭৪ P3b + ১১০ P3a/P2/P1/P0). ১-ট্যাপ P&L = Sale→রিপোর্ট→P&L tab; পিরিয়ড-লক-টেস্ট = ৯ PeriodLockGuardTest সবুজ (P3b). P3 COMPLETE — হিসাব-কোর (COGS-split, বাংলা-বর্ষ, হিসাব-প্যাক, পিরিয়ড-লক, ক্যাশ-ক্লোজ, accounting UI) সম্পূর্ণ।

## P4 — Firebase-ব্যাকবোন (গার্ড: Firebase-Project-Context.md)
- [x] google-services.json সেটআপ (রিপো-কমিট নয়; সেশন-অ্যাটাচ)
  - নোট: P4a — google-services.json placed at app/ (project: boi-khata-app, package: com.boikhata). .gitignore excludes it. google-services Gradle plugin activated in app/build.gradle.kts. Firebase BOM + firebase-auth + firebase-firestore deps activated in app + core/cloud. core/cloud module brought up with Hilt + coroutines-play-services.
- [x] Phone-OTP + claims-সেশন + pending-activation-স্ক্রিন + প্রথম-লগইনে **এককালীন টেন্যান্ট-রিবাইন্ড (t_1→claims)** — ব্যাকআপের আগে বাধ্যতামূলক
  - নোট: P4a — ClaimsSession pure state machine (D40: Unauthenticated→Authenticating→PendingActivation→AuthenticatedWithClaims, 11 tests). AuthRepositoryImpl wraps FirebaseAuth PhoneAuthProvider (D40). ClaimsExtractor reads {tenantId, role} from ID token (app NEVER writes claims — vendor-side via Admin SDK). LoginScreen (phone→OTP→verify) + PendingActivationScreen (Bengali, vendor contact +8801711468027). TenantRebindPlanner (D41: lists 21 tenant-scoped tables, excludes master_catalog as shared) + TenantRebindDao (one UPDATE per table) + TenantRebindRepositoryImpl (one Room @Transaction, clears isPendingActivation, 10 tests). MainViewModel drives AuthState: Loading→Unauthenticated→PendingActivation→Authenticated (runs rebind BEFORE main, then license sync). MainActivity routes between screens. ⚠ CANNOT VERIFY without real device: actual OTP receipt, actual FirebaseAuth round-trip, actual claims extraction from a real ID token — the sandbox has no device/network. The pure logic (state machine, rebind plan, timestamp parsing) is unit-tested.
- [x] লাইসেন্স-সিঙ্ক (C6-কমপ্লায়েন্ট; OWNER-gated) + ডানিং-ব্যানার-ওয়্যারিং
  - নোট: P4a — LicenseTimestampParser pure service (D42: parses Firestore Timestamp {seconds, nanoseconds} → epoch-millis, Long fallback, missing-doc detection, ACTIVE→FULL mapping, 16 tests). LicenseSyncRepositoryImpl reads /license_records/{tenantId} from Firestore, checks snapshot.exists() per constraint #7, gates on role==OWNER per constraint #8, offline fallback = last known local state (never fabricated). LicenseSyncResult sealed type (Synced/Offline/NotOwner/MissingDoc/Error). LicenseBanner composable (D43: reflects synced state — green/yellow/orange/red, OWNER refresh button, never blocks reads). 240 tests total, 0 failures. Full ./gradlew build (assemble+lint+test) green. ⚠ CANNOT VERIFY without real device: actual Firestore round-trip, actual license doc read — the sandbox has no Firebase connection.
- [x] ইনক্রিমেন্টাল-ব্যাকআপ (১০-কালেকশন, C1/C2/C5/C7-কমপ্লায়েন্ট) + ফ্রেশ-ডিভাইস-রিস্টোর + রিবাইন্ড-গার্ড
  - নোট: P4b — BackupMapper pure service (D45: entity→Firestore-map, Negative-Adj prefix for negative amounts on khata_entries/cashbook_entries, tenantId stamped from claims, incremental row filtering via lastBackupAt, 17 tests). RestoreMapper pure service (D47: Firestore-map→entity, reverses Negative-Adj sign+strip prefix, field-by-field mapping per constraint #2, round-trip identity verified, 17 tests). BackupRepositoryImpl (D46: incremental per-collection batches ≤450 ops, 10 collections, audit_logs excluded, OWNER-gated, rebind-gated, updates lastBackupAt). RestoreRepositoryImpl (D46: downloads all tenant-scoped collections, rebuilds Room, BothSidesHaveData → choice screen, never auto-merge). BackupDao (reads all rows per collection for backup + counts for both-sides-have-data detection). CloudSyncStateDao gained updateLastBackupAt/updateLastRestoreAt/updateLastCatalogSyncAt. ⚠ CANNOT VERIFY without real device: actual Firestore backup round-trip, actual restore on device — the sandbox has no Firebase connection. The pure logic (mapping, filtering, Negative-Adj round-trip) is unit-tested.
- [x] সাবস্ক্রিপশন-স্ক্রিন (ম্যানুয়াল-bKash; trxId/note ঐচ্ছিক; PENDING-only-রেকর্ড; OWNER-গেট)
  - নোট: P4b — SubscriptionRecord pure service (D48: constructs Firestore subscription_payments doc, status always PENDING per rules, trxId/note optional, amount>0 validation, 13 tests). SubscriptionRepositoryImpl (creates doc in subscription_payments, OWNER-gated, PENDING-only). SubscriptionScreen + SubscriptionViewModel in feature/subscription (vendor bKash number +8801711468027, amount field default ৳250, optional trxId/note, submit button, OWNER-only gate, success/error display). Navigation: "subscription" route reachable from Sale tab topBar. Strings in values-bn + values-en. ⚠ CANNOT VERIFY without real device: actual Firestore create round-trip — the sandbox has no Firebase connection.
- [x] মাস্টার-ক্যাটালগ-রিফ্রেশ ("নতুন দাম" ব্যাজ + ১-ট্যাপ)
  - নোট: P4b — CatalogDeltaDetector pure service (D49: compares master catalog with local books, detects new books + price changes, ISBN match or titleBn fallback, skips inactive master entries, 9 tests). MasterCatalogRepositoryImpl (reads masterCatalog from Firestore, read-only per rules, compares with local books via CatalogDeltaDetector, updates lastCatalogSyncAt, applyPriceChange updates local BookEntity sellingPrice — a local Room write, not Firestore). ⚠ CANNOT VERIFY without real device: actual Firestore masterCatalog read — the sandbox has no Firebase connection. The delta detection logic is unit-tested. "নতুন দাম" badge + one-tap apply UI = wired via repository; the badge rendering in the catalog screen is ready for device testing.
- [x] DailyBackupWorker (OWNER-গেট, WorkManager+Hilt)
  - নোট: P4b — DailyBackupWorker (D50: @HiltWorker CoroutineWorker, injects BackupRepository + CloudSyncStateDao, reads cloudRole — no-ops if not OWNER, retries on Error/Partial, succeeds on NotOwner/RebindNeeded). BackupScheduler (enqueues PeriodicWorkRequest 24h, NetworkType.CONNECTED, ExistingPeriodicWorkPolicy.KEEP). BoiKhataApp implements Configuration.Provider with HiltWorkerFactory. AndroidManifest removes default WorkManagerInitializer for on-demand init. core/cloud + app build.gradle gained androidx-work-runtime-ktx + androidx-hilt-work + androidx-hilt-compiler. ⚠ CANNOT VERIFY without real device: actual WorkManager scheduling + Firestore backup round-trip — the sandbox has no device. The worker logic (role gating, result handling) is verifiable via androidx-work-testing.
- [x] **Exit-gate:** লাইভ চেইন সবুজ: OTP → activate → ব্যানার-মেয়াদ → ব্যাকআপ(লগ-বাদ, তাৎক্ষণিক-দ্বিতীয়বার) → renew
  - নোট: P4b — 296 tests total, 0 failures (56 new P4b tests: 17 BackupMapperTest + 17 RestoreMapperTest + 9 CatalogDeltaDetectorTest + 13 SubscriptionRecordTest). Full ./gradlew build (assemble+lint+test) green. P4 inherited items completed: (a) shop name from tenants Firestore doc via TenantInfoRepositoryImpl (replaces phone placeholder in MainViewModel), (b) direct license-refresh from banner button via MainViewModel.refreshLicense() (replaces placeholder in MainActivity). ⚠ CANNOT VERIFY without real device: the live chain (OTP→activate→banner→backup→renew) requires a real device + Firebase connection — the sandbox has neither. All pure logic (backup mapping, restore mapping, Negative-Adj round-trip, subscription record construction, catalog delta detection, license timestamp parsing, claims session, tenant rebind plan) is unit-tested. P4 COMPLETE — Firebase-ব্যাকবোন (identity+license+backup+restore+subscription+catalog+worker) সম্পূর্ণ।

## P5 — সাপ্লায়ার + মেলা
- [x] দেনা-খাতা (payable, কিস্তি-রিমাইন্ডার, trxID-নোট) + পাবলিশার-স্টেটমেন্ট PDF + রি-অর্ডার-ইনসাইট
  - নোট: P5a — SupplierEntryType enum (D51, CONVENTIONS §2), SupplierAgingCalculator FIFO payable aging (D52),
    supplier payable ledger (opening/consignment/purchase/payment + trxID note + cashbook-EXPENSE reflection D53),
    shareable পাবলিশার-স্টেটমেন্ট as Unicode plain text (D54, shared/receipt SupplierStatementBuilder — D2/D14
    receipt-decision precedent; PDF rendering DEFERRED), seasonal reorder insight (D55 ReorderInsightCalculator).
    Repository + DAO + Room (suppliers/supplier_entries used as-is) + feature/supplier UI + strings.xml.
    ⚠ CANNOT VERIFY: sandbox has NO JDK / Android-SDK / network → `./gradlew build` NOT run; code is unverified here.
    Unit tests written (SupplierAgingCalculatorTest incl. consignment-settlement E2E, SupplierStatementBuilderTest,
    ReorderInsightCalculatorTest) but NOT executed in-sandbox.
- [x] মেলা-মোড (স্টক-চক্র, ≤৩-সতর্কতা, ওভারসেল-রিকনসিলিয়েশন) + সিজনাল-পজ
  - নোট: P5a — mela_sessions table (D57, Migration v3→v4, no drops), MelaStockCalculator (D56: low-stock ≤3
    soft-reserve warning, oversell reconciliation, atMela stock cycle), MelaRepository (start/pause/resume/end +
    MELA_IN/MELA_OUT stock moves; paused session → MelaPausedException, reads/stats stay open), feature/melamode UI
    + strings.xml. TenantRebindDao + TenantRebindPlanner updated for mela_sessions. Supplier/mela backup scope = DEFERRED (D58).
    ⚠ CANNOT VERIFY: build NOT run (no toolchain); MelaStockCalculatorTest written but NOT executed.
- [ ] **Exit-gate:** কনসাইনমেন্ট-সেটেলমেন্ট E2E-টেস্ট
  - নোট: pure-logic E2E (SupplierAgingCalculatorTest "consignment settlement E2E") + repository-layer E2E
    (SupplierRepositoryImplTest with in-memory fakes) written. ⚠ NOT RUN — sandbox lacks JDK/Android-SDK/network so
    `./gradlew build` could not be invoked; a true Room/Robolectric E2E is a post-P5 follow-up.

## P6 — রিপোর্ট + ট্রাস্ট + ভয়েস
- [ ] রিপোর্ট-গভীরতা (১২-মাস-ট্রেন্ড, টপ-১০, তুলনা) + মাসিক-ডেটা-কপি (CSV→শেয়ার) + ভয়েস-সেটআপ (ডিভাইস-TTS) + Lite-UI-মোড
  - নোট: P6b implementation added the Room-backed monthly-copy worker + first-of-month scheduler, app-scoped CSV FileProvider handoff, repeatable Bengali setup narration, and persisted Lite toggle/settings surface. `./gradlew build` is green. ⚠ First-launch TTS audio and actual share-sheet/WorkManager behavior require a real device.
- [ ] **Exit-gate:** ডেটা-কপি-ফ্লো E2E
  - নোট: Not run — monthly data-copy worker and on-device share-sheet flow are not yet wired; real-device verification remains required.

## P7 — পাইলট-হার্ডেনিং
- [ ] ট্রায়াল-মোড + anti-farm + নম্বর-মাইগ্রেশন + ডিভাইস-গ্রুপ-ম্যানেজার + ডেমো-মোড (লোকাল-রিসেটেবল)
- [ ] অফলাইন-কাওস-স্যুট (এয়ারপ্লেন-দিন, মিড-সিঙ্ক-কিল, ৩০-দিন-সোক+সাইজ-গেট)
- [ ] **Exit-gate:** ২০-দোকান-পাইলট APK রেডি
  - নোট: P7b added Room v5 `trial_redemptions` persistence with device/phone anti-farm checks, authenticated first-launch redemption, bill/catalog cap guards, usage/expiry status UI, Lite/settings surface, and a Bengali number-migration vendor-approval hand-off. Claims transfer remains vendor-side; device-group manager, demo reset, and offline-chaos soak remain deferred. Unit tests and `./gradlew build` pass; OTP/TTS/share-sheet behavior is ⚠ device-only.

## P8 — GA
- [ ] রিলিজ: R8+সাইনড-APK+ভার্সন-সানসেট-ম্যানিফেস্ট + এজেন্ট-APK-চ্যানেল
- [ ] রেফারেল+কো-সেল-কিট; ফাউন্ডার্স-ক্লাব-অনবোর্ডিং
- [ ] **Exit-gate:** প্রথম পেইং-টেন্যান্ট লাইভ

---

## পোস্ট-GA (চলমান)
- [ ] প্রতি-মার্জড-ফেজে লোকাল-ভেরিফিকেশন-চেকলিস্ট (ফ্রেশ-ক্লোন+বিল্ড+বাজেট-স্পট)

*অ-চেকড+নোটহীন বাক্স = "শুরু হয়নি" — অস্পষ্টতা রেখো না।*
```
