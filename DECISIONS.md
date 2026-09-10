# DECISIONS.md — বই খাতা Decision Log

**This file is append-only.** Never edit or delete a past entry — if a decision changes, add a new entry that references and supersedes the old one by number. This mirrors the app’s own event-sourced ledger philosophy: the history is the source of truth, not the current state alone.

**When to add an entry:** any time you make a non-trivial choice that `ARCHITECTURE.md` doesn’t already specify — a library choice between two reasonable options, a naming convention, a workaround for a platform limitation, an interpretation of an ambiguous requirement. If you’re about to do something `ARCHITECTURE.md` doesn’t cover, write the entry *before* you write the code, not after.

**Format:**
```
## D<number> — <short title>
**Date:** <session date>
**Phase:** <phase number from PROGRESS.md>
**Context:** what problem/ambiguity prompted this decision
**Decision:** what was chosen
**Alternatives considered:** what else was on the table and why it lost
**Supersedes:** (optional) D<n> if this replaces an earlier decision
```

Never resolve a merge conflict in this file by picking one side automatically — a conflict here means two sessions ran concurrently and need manual reconciliation.

---

## D1 — Seed entry: repo established
**Date:** 2026-08-29
**Phase:** 0
**Context:** Repository initialized with the agent constitution (Blueprint v1.0, ARCHITECTURE, CONVENTIONS, BUILD, PROGRESS, Firebase-Project-Context, .gitignore, catalog). No code-level decisions made yet — this entry establishes the log format and numbering from D2.
**Decision:** Sequential `D<n>` numbering, oldest first, never renumbered even if an early decision is later superseded.
**Alternatives considered:** Date-only entries without sequence numbers — rejected because sequence numbers keep “supersedes” references unambiguous even for same-day decisions.
**Supersedes:** —

---

## D2 — Bangladesh Demographic UI/UX Optimization (“Lal Khata” theme)
**Date:** 2026-08-29
**Phase:** 0
**Context:** Need to optimize the UI/UX architecture to cater strictly to the target demographic: 45+ year-old BD shopkeepers in noisy environments using low-end devices. Prevailing Material 3 default configurations are too subtle, hard to tap, cause eye-strain under harsh lights, and rendering PNGs on 3GB RAM devices risks OutOfMemory (OOM) crashes.
**Decision:**
1. **Receipts:** Abandon PNG rendering entirely. Use Unicode text or lightweight PDF for WhatsApp sharing.
2. **Colors & Theming:** Implement “Lal Khata” Theme (`#800000` primary, `#FDFAF6` ivory background to reduce eye strain).
3. **Accessibility:** Over-scale default Typography by 20% independent of OS settings.
4. **Touch & Feel:** Enforce 56dp–64dp touch targets, skip flat ghost buttons in favor of elevated skeuomorphic buttons, and mandate haptic feedback on saves.
5. **Layout:** Ban Hamburger menus (use Bottom Navigation) and eliminate dashboard charts (use Trident numbers: Cash, Supplier Dues, Customer Dues).
6. **Support UI:** Put a professional Vendor Card in Settings with big “Call” and “WhatsApp” buttons; no logos on login/dashboard.
**Alternatives considered:** Default Material 3 styling (rejected for poor accessibility), Chart-based dashboard (rejected for resource consumption and lack of utility to users).
**Supersedes:** —

---

## D3 — Font ownership: core/designsystem
**Date:** 2026-08-29
**Phase:** 0
**Context:** Noto Sans Bengali must live in exactly one module (P0 item 4); candidates were app-res vs core/designsystem.
**Decision:** Bundle the font under `core/designsystem/src/main/res/font` — designsystem owns all shared visual resources; app stays a thin shell.
**Alternatives considered:** app-res placement (rejected: feature modules could not reference it without an app dependency, violating module boundaries).
**Supersedes:** —

---

## D4 — KSP version normalized to 2.3.11
**Date:** 2026-08-29
**Phase:** 0
**Context:** The constitution catalog carried KSP as a ⚠ VERIFY entry; the P0 build required a KSP release that exactly matches the catalog’s Kotlin line.
**Decision:** Resolve KSP to **2.3.11** and update `gradle/libs.versions.toml` accordingly.
**Alternatives considered:** Guessing a `-1.0.x` suffix (forbidden); bumping Kotlin to match a newer KSP (out of scope for P0).
**Supersedes:** —

---

## D5 — AGP 9 built-in Kotlin path; Hilt via KSP
**Date:** 2026-08-29
**Phase:** 0
**Context:** AGP 9.x ships built-in Kotlin support; applying `org.jetbrains.kotlin.android` in Android modules conflicts with it.
**Decision:** Rely on AGP 9’s built-in Kotlin in Android modules; run Hilt’s processor through KSP (no kapt anywhere).
**Alternatives considered:** Applying the Kotlin-Android plugin anyway (rejected: conflicts with AGP 9); kapt for Hilt (rejected: slower, deprecated).
**Supersedes:** —

---

## D6 — Gradle wrapper pinned to 9.3.1
**Date:** 2026-08-29
**Phase:** 0
**Context:** AGP 9.1.1 requires a newer Gradle than the default available in the build environment.
**Decision:** Pin the Gradle wrapper to **9.3.1** in `gradle/wrapper/gradle-wrapper.properties`.
**Alternatives considered:** Letting the environment pick a default (rejected: unreproducible builds).
**Supersedes:** —

---

## D7 — Entity home: core/database owns @Entity; core/domain owns enums + services + repo interfaces
**Date:** 2026-08-30
**Phase:** 1
**Context:** ARCHITECTURE §2 assigns Room schema/DAO/migration to core/database and entity-model/repository-interface/domain-service to core/domain. Room @Entity classes carry Room annotations = build-time coupling to Room.
**Decision:** @Entity data classes live in `core/database`. `core/domain` owns pure enums, repository interfaces, and domain services. core/database depends on core/domain for enums; core/domain never depends on core/database.
**Alternatives considered:** Entities in core/domain with Room annotations (rejected: forces every feature to transitively depend on Room); duplicate DTOs (rejected: pointless boilerplate for a single-tenant-offline app).
**Supersedes:** —

---

## D8 — PIN hashing: PBKDF2-HMAC-SHA256 via javax.crypto (no external crypto dependency)
**Date:** 2026-08-30
**Phase:** 1
**Context:** CONVENTIONS §3 stores `pinHash` + `salt`. The catalog excludes security-crypto. A hashing scheme needed with zero new dependencies on minSdk 26.
**Decision:** PBKDF2-HMAC-SHA256, 10,000 iterations, 256-bit key, per-user random salt stored as hex. Implemented via `javax.crypto.SecretKeyFactory` + `PBEKeySpec`.
**Alternatives considered:** BCrypt (rejected: needs a new dependency); Argon2 (rejected: overkill); plaintext (forbidden).
**Supersedes:** —

---

## D9 — Data-meter P1 foundation: local accumulator + Wi-Fi-only toggle, no Firestore bytes yet
**Date:** 2026-08-30
**Phase:** 1
**Context:** PROGRESS P1 item 5 calls for the data-meter foundation. Firebase is P4 scope.
**Decision:** A `DataMeter` domain service in core/domain with an in-memory byte counter and a Wi-Fi-only toggle persisted in `cloud_sync_state.wifiOnlySync` (default true). No Firebase dependency this phase.
**Alternatives considered:** Deferring entirely to P4 (rejected: PROGRESS P1 item 5 calls for the foundation now); OkHttp interceptor (rejected by item text).
**Supersedes:** —

---

## D10 — SessionManager auto-lock: timestamp-checked, not a background timer
**Date:** 2026-08-30
**Phase:** 1
**Context:** ARCHITECTURE §6 mandates a 2-minute auto-lock for non-OWNER roles. A background timer drains battery on low-end 3GB devices.
**Decision:** SessionManager records `lastInteractionAt` on each UI touch. `isLocked()` compares `now − lastInteractionAt > 2 min`. No background timer. OWNER role is exempt.
**Alternatives considered:** CountDownTimer per activity (rejected: battery cost); a foreground Service (rejected: overkill).
**Supersedes:** —

---

## D11 — cloud_sync_state.wifiOnlySync column (amends CONVENTIONS §3)
**Date:** 2026-08-30
**Phase:** 1
**Context:** CONVENTIONS §3 lists cloud_sync_state columns without a Wi-Fi-only-sync toggle. D9 decided to persist the toggle there.
**Decision:** Add column `wifiOnlySync Boolean DEFAULT true` to cloud_sync_state via ALTER-ADD. Default true per Blueprint law 7.
**Alternatives considered:** A separate `settings` table (rejected: one-row toggle doesn’t justify a new table); SharedPreferences (rejected: breaks Room-as-truth).
**Supersedes:** —

---

## D12 — Gradle JVM memory raised for AGP 9 lint engine (640m/512m → 2g/1g)
**Date:** 2026-08-30
**Phase:** 1
**Context:** CI run #3 failed during `:feature:home:lintAnalyzeDebug` — "Unexpected failure during lint analysis". The `gradle.properties` had `-Xmx640m -XX:MaxMetaspaceSize=512m`.
**Decision:** Raise `org.gradle.jvmargs` to `-Xmx2g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8`. Keep `workers.max=1` and `parallel=false`.
**Alternatives considered:** Disabling lint on CI (rejected: BUILD.md §6 mandates it); lint task isolation (rejected: narrows real checks).
**Supersedes:** —

---

## D13 — Bengali fuzzy search: normalized-title column + LIKE + BengaliNormalizer
**Date:** 2026-08-29
**Phase:** 2a
**Context:** PROGRESS P2 item 1 calls for Bengali-fuzzy-search. Bengali spelling variations make exact LIKE matching unreliable.
**Decision:** Add a `titleBnNormalized` TEXT column populated by a `BengaliNormalizer` domain service that strips vowel signs, chandrabindu/bindu/visarga, hasanta, and converts Bengali digits to Latin. Search uses `LIKE ‘%normalizedQuery%’` on the normalized column.
**Alternatives considered:** Room FTS4 (rejected: overkill for <10k books); SQLite ICU collation (rejected: unreliable); manual Soundex (rejected: Bengali phonetics too complex).
**Supersedes:** —

---

## D14 — Khata statement format: plain-text, WhatsApp-shareable, dual digits
**Date:** 2026-08-29
**Phase:** 2a
**Context:** Blueprint §7.4 mandates shareable স্টেটমেন্ট. D2 banned PNG/Bitmap.
**Decision:** Generate khata statement as a Unicode plain-text string. `KhataStatementBuilder` in `core/domain`. Sharing uses `Intent.ACTION_SEND` with `text/plain`.
**Alternatives considered:** Lightweight PDF (rejected: P3 scope); HTML (rejected: WhatsApp strips HTML).
**Supersedes:** —

---

## D15 — দেনা-মুন accounting treatment: ADJUSTMENT entry bringing balance to zero
**Date:** 2026-08-29
**Phase:** 2a
**Context:** Blueprint §7.4: “১-ট্যাপ দেনা মুন → bad-debt জার্নাল-এন্ট্রি”. khata_entries is append-only.
**Decision:** দেনা-মুন inserts a `KhataEntryEntity` with `type="ADJUSTMENT"`, `amount = −currentDue`, `description="দেনা মুন"`. BackupMapper applies the “Negative Adj: ” prefix when uploading.
**Alternatives considered:** Positive magnitude + special type (rejected); deleting entries (forbidden: append-only); a separate “forgiven” flag (rejected: loses audit trail).
**Superedes:** —

---

## D16 — Room migration v1→v2: ALTER-ADD titleBnNormalized + nameBnNormalized columns
**Date:** 2026-08-29
**Phase:** 2a
**Decision:** Create `Migration1To2` with two `ALTER TABLE … ADD COLUMN` statements. Bump `@Database(version = 2)`. Existing rows get empty strings.
**Alternatives considered:** `fallbackToDestructiveMigration` (forbidden: drops user data).
**Supersedes:** —

---

## D17 — KhataInstallmentDao: new DAO for the existing khata_installments table
**Date:** 2026-08-29
**Phase:** 2a
**Decision:** Add `KhataInstallmentDao` with `@Insert`, `@Query` methods. Add `abstract fun khataInstallmentDao()` to `BoiKhataDatabase`.
**Supersedes:** —

---

## D18 — P2a navigation: bottom nav (Home/Catalog/Khata) via Navigation-Compose
**Date:** 2026-08-29
**Phase:** 2a
**Decision:** `BoiKhataNavHost` in the `app` module with three routes: `home`, `catalog`, `khata`. A `BoiKhataBottomBar` with three NavigationBarItems. Bengali labels from strings.xml.
**Alternatives considered:** A single-screen with tabs (rejected); top-level tabs (rejected: Blueprint mandates bottom nav).
**Supersedes:** —

---

*(Next entry starts at D19. Do not skip numbers; do not reuse a number even for a reverted decision — log the revert as a new entry instead.)*

---

## D19 — VAT calculation: per-line, category-based (books 0% / stationery 15%)
**Date:** 2026-08-30
**Phase:** 2b
**Decision:** `VatCalculator` in `core/domain/sale`. Books = 0%, Stationery = 15%. Line VAT = unitPrice × quantity × vatRate. Discount applied AFTER VAT.
**Supersedes:** —

---

## D20 — Bill number format: INV-YYYYMMDD-NNNN
**Date:** 2026-08-30
**Phase:** 2b
**Decision:** `BillNumberGenerator` generates `INV-YYYYMMDD-NNNN`. NNNN resets daily. Sequence determined by querying max existing bill number for that date from Room.
**Supersedes:** —

---

## D21 — Receipt format: Unicode plain-text, dual digits, WhatsApp-shareable
**Date:** 2026-08-30
**Phase:** 2b
**Decision:** `ReceiptBuilder` in `shared/receipt`. Unicode text receipt. Sharing uses `Intent.ACTION_SEND` with `text/plain`.
**Supersedes:** —

---

## D22 — Partial payment → auto-khata wiring: CREDIT entry linked via khataEntryId
**Date:** 2026-08-30
**Phase:** 2b
**Decision:** `SaleRepository.createBill` handles the entire transaction in one Room `@Transaction`: insert bill, lines, stock_ledger, and if dueAmount > 0 AND customerId != null, insert a `KhataEntryEntity` with `type=CREDIT`.
**Supersedes:** —

---

## D23 — Discount type: PERCENTAGE or FIXED
**Date:** 2026-08-30
**Phase:** 2b
**Decision:** `discountType` stores `"PERCENTAGE"` or `"FIXED"`. `discountAmount` always stores the final calculated value. Discount capped at `subtotal + vatAmount`.
**Supersedes:** —

---

*(Next entry starts at D24. Do not skip numbers; do not reuse a number even for a reverted decision — log the revert as a new entry instead.)*

---

## D24 — Purchase auto-routing: book purchase → stock_ledger (PURCHASE), non-book → expense
**Date:** 2026-08-30
**Phase:** 3a
**Decision:** `PurchaseRouter` domain service: BOOK_PURCHASE → stock_ledger `reason="PURCHASE"`, positive quantity; NON_BOOK_PURCHASE → ExpenseEntity. Both create a `CashbookEntryEntity`.
**Supersedes:** —

---

## D25 — Cashbook auto-population: every money flow creates a cashbook entry
**Date:** 2026-08-30
**Phase:** 3a
**Decision:** Auto-population rules in the repository layer within the same `@Transaction`. Rules: bill payment → INCOME; expense → EXPENSE; khata PAYMENT → INCOME; owner drawing → EXPENSE; book purchase → EXPENSE.
**Supersedes:** —

---

## D26 — ঘরি (staff advance) sub-ledger: expense with special category + per-user balance
**Date:** 2026-08-30
**Phase:** 3a
**Decision:** Seed `expense_categories` row with `nameBn="ঘরি"`. A `GoriBalanceCalculator` pure domain service computes per-user balance. Recovery recorded as `description="ঘরি ফেরত"`.
**Supersedes:** —

---

## D27 — Recurring expense template: next-due computation + manual trigger
**Date:** 2026-08-30
**Phase:** 3a
**Decision:** Implement `RecurringExpenseCalculator` pure domain service + unit tests. Persistence and auto-trigger deferred to P3b.
**Supersedes:** —

---

## D28 — Owner drawing (মালিকের তোলা): separate table, OWNER-only, cashbook EXPENSE
**Date:** 2026-08-30
**Phase:** 3a
**Decision:** `OwnerDrawingRepository` creates `OwnerDrawingEntity` + `CashbookEntryEntity` (EXPENSE, CASH) in one `@Transaction`. OWNER-only.
**Supersedes:** —

---

## D29 — COGS-split P&L: consignment-commission vs purchase-COGS
**Date:** 2026-09-01
**Phase:** 3b
**Decision:** `PnLCalculator` splits COGS into `cogsPurchase` and `cogsConsignment`. consignment-settlement amount is 0.0 until P5.
**Supersedes:** —

---

## D30 — Dual-calendar rollup: Gregorian month + Bengali fiscal year (১ এপ্রিল–৩১ মার্চ)
**Date:** 2026-09-01
**Phase:** 3b
**Decision:** `BengaliFiscalCalendar` pure domain service. Bengali FY = April 1–March 31. Month names: Boishakh through Choitro.
**Supersedes:** —

---

## D31 — Balance-sheet lite: component list per Blueprint (assets, liabilities, equity)
**Date:** 2026-09-01
**Phase:** 3b
**Decision:** `BalanceSheetCalculator` in `core/domain/accounting`. Assets = cash + inventory + receivables + ghori. Liabilities = supplierPayables (0 for P3b). Equity = retainedEarnings − drawings. Accounting identity asserted in tests.
**Supersedes:** —

---

## D32 — Period-lock: closed month immutable; owner-approved adjustment entries only
**Date:** 2026-09-01
**Phase:** 3b
**Decision:** New `period_locks` table via Migration v2→v3. `PeriodLockGuard` checks entry dates before any money-table insert. Read/export paths do NOT consult the guard.
**Superedes:** —

---

## D33 — হিসাব-প্যাক PDF: monthly report set, bank/microfinance-loan-file ready
**Date:** 2026-09-01
**Phase:** 3b
**Decision:** `HisabPackGenerator` builds a structured `HisabPack` data model. PDF renderer in `shared/receipt` uses Android’s `PdfDocument` API with Noto Sans Bengali.
**Supersedes:** —

---

## D34 — Cashbook auto-populate from bill payments + khata collections (completes D25)
**Date:** 2026-09-01
**Phase:** 3b
**Decision:** `SaleRepositoryImpl.createBill` inserts `CashbookEntryEntity` (INCOME) inside the existing `db.withTransaction` when `paidAmount > 0`. `KhataRepositoryImpl.addEntry` gains a `cashbookAccount` parameter.
**Supersedes:** D25 (completes the bill/khata auto-populate that D25 deferred to P3b).

---

## D35 — Recurring-expense persistence + due-reminder + monthly budget alert
**Date:** 2026-09-01
**Phase:** 3b
**Decision:** New `recurring_expenses` and `budgets` tables via Migration v2→v3. `RecurringExpenseReminder` pure service. `BudgetAlertCalculator` pure service. Templates applied manually (no WorkManager auto-trigger this phase).
**Supersedes:** —

---

## D36 — Cash-close “আজকের হিসাব”: daily summary + MFS-fee auto-line + variance
**Date:** 2026-09-01
**Phase:** 3c
**Decision:** `CashCloseCalculator` pure domain service. MFS fee is an estimation line — does NOT auto-create an expense entry. `CashCloseReportBuilder` in `shared/receipt` produces the WhatsApp-shareable text.
**Supersedes:** —

---

## D37 — Accounting UI in feature/reports: P&L screen, balance-sheet, period-lock, budget alerts
**Date:** 2026-09-01
**Phase:** 3c
**Decision:** Fill `feature/reports` with `ReportsViewModel`, `ReportsScreen`, `CashCloseScreen` + `CashCloseViewModel`. Navigation: “reports” and “cash_close” routes reachable from Sale screen.
**Supersedes:** —

---

## D38 — Cash-close + reports navigation: reachable from Sale tab, not a 5th bottom-nav tab
**Date:** 2026-09-01
**Phase:** 3c
**Decision:** Two routes: “reports” and “cash_close”. Both reachable from Sale screen. No new bottom-nav tab. 4-tab invariant preserved.
**Supersedes:** —

---

## D39 — Firebase wiring: google-services.json + catalog deps activate
**Date:** 2026-09-01
**Phase:** 4a
**Decision:** Place google-services.json at `app/`. Apply `com.google.gms.google-services` plugin. Add Firebase deps to `app/build.gradle.kts` and `core/cloud/build.gradle.kts`.
**Supersedes:** —

---

## D40 — Phone-OTP login + claims session + pending-activation state
**Date:** 2026-09-01
**Phase:** 4a
**Decision:** `AuthRepository` interface. `AuthRepositoryImpl` wraps `FirebaseAuth`. `ClaimsSession` pure domain service state machine. `LoginScreen` + `LoginViewModel`. `PendingActivationScreen` with vendor phone +8801711468027.
**Supersedes:** —

---

## D41 — One-time tenant rebind: migrate local “t_1” rows to claims tenantId
**Date:** 2026-09-01
**Phase:** 4a
**Decision:** `TenantRebindDao` with per-table UPDATE methods. `TenantRebindRepository` executes all updates in one `db.withTransaction`. Gated on `isPendingActivation == true` AND `oldTenantId != newTenantId`.
**Supersedes:** —

---

## D42 — License sync: Firestore read + Timestamp parsing + offline fallback
**Date:** 2026-09-01
**Phase:** 4a
**Decision:** `LicenseSyncRepository`. `LicenseTimestampParser` pure domain service. Gate: role != OWNER → return NotOwner with locally cached state. Offline → return Offline with last known state.
**Supersedes:** —

---

## D43 — Subscription banner wiring: local license display reflects synced state
**Date:** 2026-09-01
**Phase:** 4a
**Decision:** `LicenseBanner` composable in app module. OWNER sees “রিফ্রেশ” button. Non-OWNER sees banner but no refresh. Banner does NOT block reads/exports (never-lock rule).
**Supersedes:** —

---

## D44 — Login + pending-activation + banner navigation wiring
**Date:** 2026-09-01
**Phase:** 4a
**Decision:** `MainViewModel` holds `AuthRepository`, `ClaimsSession` state, `TenantRebindRepository`, `LicenseSyncRepository`. `AuthState` sealed type drives which screen to show. Rebind runs BEFORE navigating to main screen.
**Supersedes:** —

---

*(Next entry starts at D45. Do not skip numbers; do not reuse a number even for a reverted decision — log the revert as a new entry instead.)*

---

## D45 — BackupMapper: pure entity→Firestore-map conversion + Negative-Adj prefix + row filtering
**Date:** 2026-09-03
**Phase:** 4b
**Decision:** `BackupMapper` pure object. Stamps `tenantId` from claims. For negative ADJUSTMENT amounts: uploads `abs(amount)` + prepends “Negative Adj: ” to description. `filterNewRows(rows, lastBackupAt)` filters by updatedAt/createdAt.
**Supersedes:** —

---

## D46 — BackupRepository + RestoreRepository: incremental upload + fresh-device restore + choice-screen
**Date:** 2026-09-03
**Phase:** 4b
**Decision:** `BackupRepositoryImpl` commits per-collection WriteBatches (≤450 ops). `RestoreRepositoryImpl` downloads all 10 collections. For both-sides-have-data: return `BothSidesHaveData` — choice screen shown. Rebind guard: backup only after `isPendingActivation == false`.
**Supersedes:** —

---

## D47 — RestoreMapper: pure Firestore-map→entity conversion + Negative-Adj sign flip + round-trip
**Date:** 2026-09-03
**Phase:** 4b
**Decision:** `RestoreMapper` pure object. If description starts with “Negative Adj: ”, flips sign and strips prefix. Handles Long→Int coercion. Round-trip test helper included.
**Supersedes:** —

---

## D48 — Supplier ledger: SupplierEntryEntity + SupplierRepositoryImpl + idempotency key
**Date:** 2026-09-03
**Phase:** 5
**Decision:** `SupplierEntryEntity` with `idempotencyKey` column. `SupplierRepositoryImpl` generates keys. Supplier ledger is append-only (🔒).
**Supersedes:** —

---

## D49 — Consignment settlement: revenue-sharing with publisher on actual sales
**Date:** 2026-09-03
**Phase:** 5
**Decision:** `ConsignmentSettlementCalculator` pure domain service computes publisher share = sum(soldQty × consignmentRate). Settlement creates a `SupplierEntryEntity` with `type=CONSIGNMENT`.
**Supersedes:** —

---

## D50 — Supplier aging: outstanding dues bucketed by days overdue
**Date:** 2026-09-03
**Phase:** 5
**Decision:** `SupplierAgingCalculator` pure domain service. Buckets: current (0–30d), overdue-30 (31–60d), overdue-60 (61–90d), overdue-90 (>90d). Derived from supplier_entries append-only ledger.
**Supersedes:** —

---

## D51 — Master catalog: pre-loaded book list for quick ISBN/title lookup
**Date:** 2026-09-03
**Phase:** 5
**Decision:** `master_catalog` table populated from a bundled JSON asset on first launch. `MasterCatalogRepository` queries it for ISBN lookup + auto-fill. Never backed up (LOCAL-ONLY).
**Supersedes:** —

---

## D52 — Barcode scanner integration: ZXing embedded (no external app dependency)
**Date:** 2026-09-03
**Phase:** 5
**Decision:** Use `com.google.zxing:core` + `journeyapps:zxing-android-embedded` for in-app barcode scanning. No external app dependency — works offline.
**Supersedes:** —

---

## D53 — Supplier UI in feature/supplier: list, add, entry screens
**Date:** 2026-09-03
**Phase:** 5
**Decision:** Fill `feature/supplier` with `SupplierListScreen`, `SupplierAddScreen`, `SupplierEntryScreen`. Navigation: “supplier” route reachable from Sale tab. 4-tab invariant preserved.
**Supersedes:** —

---

## D54 — Supplier statement: plain-text WhatsApp-shareable (follows D14/D21 pattern)
**Date:** 2026-09-03
**Phase:** 5
**Decision:** `SupplierStatementBuilder` in `shared/receipt` produces plain-text supplier ledger statement. Sharing via `Intent.ACTION_SEND` with `text/plain`.
**Supersedes:** —

---

## D55 — Stock alert system: low-stock threshold per book + daily check
**Date:** 2026-09-03
**Phase:** 5
**Decision:** `StockAlertCalculator` pure domain service. Compares current stock against `books.lowStockThreshold`. Low-stock books surfaced in Catalog screen header. No push notification — in-app only.
**Supersedes:** —

---

## D56 — P5 navigation: supplier route added to Sale tab cluster
**Date:** 2026-09-03
**Phase:** 5
**Decision:** Add “supplier” route to `BoiKhataNavigation`. Reachable from Sale screen. No new bottom-nav tab.
**Supersedes:** —

---

## D57 — Audit log: LOCAL-ONLY append, never backed up to Firestore
**Date:** 2026-09-03
**Phase:** 5
**Decision:** `audit_logs` table is LOCAL-ONLY. `AuditLogRepository` inserts entries for OWNER-only destructive operations. Never included in BackupMapper’s 10-collection list.
**Supersedes:** —

---

## D58 — P6 voice setup: device-local TTS, 5-step Bengali script, repeatable from settings
**Date:** 2026-09-04
**Phase:** 6
**Decision:** Use Android’s device-local `TextToSpeech` engine with `Locale("bn")`. Fixed 5-step Bengali script. Completion + Lite mode persisted in app-local preferences keyed by active local user. TTS lifecycle stopped on disposal.
**Supersedes:** —

---

## D59 — Annual trend report: 12-month rolling P&L + top-10 rankings (pure domain)
**Date:** 2026-09-04
**Phase:** 6
**Decision:** Pure domain calculators for 12-month aggregation and top-10 rankings. Repository reads Room data; no Firebase reads added. Report sharing via Unicode text/plain following D14/D21/D54.
**Supersedes:** —

---

## D60 — Monthly data copy is a local WorkManager artifact with foreground sharing
**Date:** 2026-09-04
**Phase:** 6
**Decision:** Local-only monthly-copy worker. Generates UTF-8 CSV in app-scoped storage. UI exposes artifact through FileProvider-backed ACTION_SEND share sheet. Never license-gated.
**Supersedes:** —

---

## D61 — Device-local voice setup and per-user Lite display preference
**Date:** 2026-09-04
**Phase:** 6
**Decision:** Use Android’s device-local TextToSpeech with Locale Bengali. Persist completion and Lite mode in app-local preferences keyed by active local user. Theme exposes Lite branch scaling typography by 1.2.
**Supersedes:** —

---

## D62 — Design-system owns Material 3 for P6 theme enforcement
**Date:** 2026-09-04
**Phase:** 6
**Decision:** Add the catalogued Compose Material 3 dependency to `core/designsystem`; keep all theme policy in `BoiKhataTheme`; no new library or version introduced.
**Alternatives considered:** Keep the pass-through theme (rejected: cannot enforce constitutional design rules); duplicate Material 3 theme code in app/features (rejected: violates single ownership).
**Supersedes:** —

---

## D63 — P7 trial and phone-number migration remain local-first policy services
**Date:** 2026-09-04
**Phase:** 7
**Decision:** Trial eligibility, cap, and expiry as pure domain services. 14-day full-feature trial with hard caps (100 bills/200 books). Backup disabled during trial. Number migration = pure state transition requiring new-number OTP + vendor-issued claims transfer.
**Supersedes:** —

---

## D64 — P7b local trial redemption, foreground sharing, and release evidence
**Date:** 2026-09-04
**Phase:** 7b
**Decision:** Add `trial_redemptions` Room table, migrated additively from schema v4 to v5. Trial usage read from existing Room bill/book counts. Monthly copy worker generates app-scoped CSV. Foreground settings surface performs ACTION_SEND share-sheet handoff.
**Supersedes:** —

---

## D65 — P8 release hardening and pilot controls remain local-first
**Date:** 2026-09-04
**Phase:** 8
**Decision:** Release metadata in Gradle properties/version catalog-compatible constants. R8 on release variant. Version availability = offline-safe local policy. Demo reset = explicit owner-confirmed local destructive operation. Referral codes = deterministic tenant-derived identifiers.
**Supersedes:** —

## D66 — এক্সিট-গেট কর্তৃত্ব ও ফেজ-লেজার পুনর্মিলন

**Date:** 2026-09-05
**Phase:** P10

**Context:**
দুই নথি এক্সিট-গেট নিয়ে পরস্পরবিরোধী। Blueprint §১২-এর রোডম্যাপ-টেবিল বলে P5 = “—”, P6 = “—”, P7 = “Activation ≥৭০%”, P8 = “১০০ টেন্যান্ট-পথ”।
PROGRESS.md বলে P5 = কনসাইনমেন্ট-সেটলেমেন্ট E2E-টেস্ট, P7 = ২০-দোকান-পাইলট APK রেডি, P8 = প্রথম পেইং-টেন্যান্ট লাইভ।

**Decision:**
১। **গেট-মালিকানা:** এক্সিট-গেটের একমাত্র মালিক-ফাইল = PROGRESS.md। Blueprint §১২-এর গেট-কলাম রোডম্যাপ-নির্দেশক, বাধ্যকর নয়।
২। **P5–P8 অবস্থা:** “কোড ডেলিভার্ড, গেট অপ্রমাণিত”।
৩। **P10 = নতুন ফেজ, গেট-শূন্য (gate zero):** ডিজাইন-প্রয়োগ ফেজ।
৪। **গেট-প্রমাণের সংজ্ঞা:** একটি exit-gate কেবলই তখনই চেকড হবে যখন (ক) কম্পাইল সত্যিই চলেছে, (খ) ইউনিট-টেস্টের PASS/FAIL টেবিল আছে, (গ) ডিভাইস/Firebase-নির্ভর অংশ থাকলে তা হয় প্রমাণিত, নয়তো ⚠ CANNOT VERIFY হিসেবে লেখা।

**Alternatives:**
- *Blueprint §১২-কে গেট-মালিক করা:* বাতিল — P5/P6-এ গেট “—”, অর্থাৎ কোনো প্রমাণ-বাধ্যবাধকতাই থাকে না।
- *P5–P8 আন-চেক করে ফেজগুলো পুনরায় চালানো:* বাতিল — কোড সত্যিই ডেলিভার্ড।
- *সংঘর্ষ উপেক্ষা করা:* বাতিল — ARCHITECTURE-এর মেটা-আইনের সরাসরি লঙ্ঘন।

**Supersedes:** কিছুই নয়।

---

## D67 — অনাথ ডিজাইন-আইন দত্তক ও D2-এর CI-প্রয়োগ (P10-এর বিষয়বস্তু)

**Date:** 2026-09-05
**Phase:** P10

**Context:**
ডিভাইসে ইনস্টল-করা বিল্ডের স্ক্রিনশট দেখায়: ডিফল্ট Material 3 ল্যাভেন্ডার surfaceVariant কার্ড, ইংরেজি লেবেল ও বটম-ন্যাভ, ক্লিপড ট্যাব-স্ট্রিপ, এবং সেল-স্ক্রিনে “Supplier” লেবেল উল্লম্বভাবে এক-অক্ষর-প্রতি-লাইন রেন্ডার।

**Decision:**
১। **নতুন ডিজাইন-আইন লেখা হবে না।** D2 + Blueprint §২ + ARCHITECTURE §১ ইতিমধ্যেই আইন। P10 কেবল সেগুলো বাস্তবায়ন করে।
২। **চার অনাথ-আইন দত্তক:** হ্যাপটিক ও ড্যাশবোর্ড-ট্রাইডেন্ট → P10-এর চেকলিস্ট-আইটেম। কুইক-এন্ট্রি ও লো-ব্যাটারি-মোড → P10-এ স্পেক-লক, P11-এ বাস্তবায়ন।
৩। **D2 grep-প্রয়োগযোগ্য হবে** — ARCHITECTURE §৮-এর নিষিদ্ধ-তালিকায় যোগ: ডিফল্ট-M3 বা dynamic-color রেফারেন্স; টাইপ-স্কেলের বাইরে হার্ডকোড `sp`; ৫৬ধপ-র কম ট্যাপ-টার্গেট; যেকোনো চার্ট-লাইব্রেরি; hamburger/NavigationDrawer; হার্ডকোড UI-স্ট্রিং; বাংলা-UI-পাথে ল্যাটিন-অঙ্ক।
৪। **স্ক্রিনশট-টেস্ট গেট।**
৫। **স্ক্রিন-স্বাক্ষর নিয়ম।**

**Alternatives:**
- *P9-এর ভিতরেই ডিজাইন ঠিক করা:* বাতিল — P9 মার্জড ও ক্লোজড।
- *D2-কে supersede করে নতুন ডিজাইন-আইন লেখা:* বাতিল।
- *শুধু কোড-রিভিউয়ে ভরসা:* বাতিল — সাত ফেজ ধরে রিভিউ D2 ধরতে পারেনি।

**Supersedes:** কিছুই নয় (D2 বহাল ও পুনর্নিশ্চিত; D62-এর উপর নির্মিত)।

---

## D68 — BUILD §২ সংশোধন: google-services.json রিপোতে কমিটেড ও repo-safe

**Date:** 2026-09-05
**Phase:** P10

**Context:**
BUILD.md §২ দাবি করে: “ফাইলটি রিপোতে কমিট হয় না (.gitignore); প্রতি AI-সেশনে অ্যাটাচ করে app/-এ বসানো হয়।” যাচাই: `app/google-services.json` কমিটেড — blob `a12d0e2e`, ৭৬২ বাইট। `.gitignore`-এ google-services.json-এর কোনো উল্লেখ নেই।

**Decision:**
১। BUILD.md §২ সংশোধিত হবে। নতুন পাঠ্য: ফাইলটি রিপোতে কমিটেড ও repo-safe।
২। “প্রতি সেশনে অ্যাটাচ করো” নির্দেশ বাতিল।
৩। `.gitignore` অপরিবর্তিত থাকবে।
৪। “google-services.json অনুপস্থিত” আর কখনো বিল্ড-ব্যর্থতার ব্যাখ্যা হিসেবে ব্যবহার করা যাবে না।

**Alternatives:**
- *ফাইলটি .gitignore-এ যোগ করে রিপো থেকে সরানো:* বাতিল — Firebase-Project-Context §১ একে repo-safe ঘোষণা করেছে।
- *BUILD.md-কে সঠিক ধরে নিয়ে ফাইল সরানো:* বাতিল — নথি বাস্তবতার সঙ্গে মিলবে, উল্টোটা নয়।

**Supersedes:** BUILD.md §২-এর google-services.json-সংক্রান্ত বাক্য (ফাইল-সংশোধন বাকি)।

---

## D69 — JUnit 4.13.2 is the sole test framework; JUnit 5 (Jupiter) is forbidden

**Date:** 2026-09-10
**Phase:** Cross-cutting (test infrastructure)
**Context:**
The project has used JUnit 4.13.2 since P0. Every existing test — 296+ across core/domain, core/database, shared/receipt — runs on JUnit 4. No JUnit 5 (Jupiter) dependency exists anywhere.

**Decision:**
1. **JUnit 4.13.2 is the ONLY test framework** for unit tests, DAO/Room tests, and any future instrumented tests.
2. **JUnit 5 (Jupiter) is forbidden** — no `org.junit.jupiter.*` dependency may be added.
3. The `junit` alias in `libs.versions.toml` (currently `junit4 = "4.13.2"`) is the single source of the JUnit version.
4. Test naming stays `<ClassUnderTest>Test`; methods `should <expected> when <condition>` (BUILD.md §5).

**Alternatives considered:**
- *Adopt JUnit 5 (Jupiter):* rejected — no benefit for this project’s test pyramid; migration would touch every test file.
- *Leave implicit:* rejected — implicit conventions get violated; a DECISIONS.md entry makes the rule grep-enforceable.

**Supersedes:** —

---

## D70 — Migration5To6: deterministic idempotencyKey unique index on supplier_entries

**Date:** 2026-09-10
**Phase:** P5 hotfix (B3 bug)
**Context:**
`supplier_entries.idempotencyKey` column exists but is NOT enforced unique at the Room layer. `SupplierRepositoryImpl.kt` generates `idempotencyKey = UUID.randomUUID().toString()` at three call sites (lines 101, 138, 154). Bug B3, noted as `@Ignore` in `SupplierRepositoryImplTest.kt` line 261.

**Decision:**
1. **Add a unique Room index** on `supplier_entries.idempotencyKey` via `@Index(value = ["idempotencyKey"], unique = true)`.
2. **Write `Migration5To6`** — `CREATE UNIQUE INDEX IF NOT EXISTS index_supplier_entries_idempotencyKey ON supplier_entries(idempotencyKey)`.
3. **Bump `@Database` version 5 → 6** and register `Migration5To6` in `DatabaseModule.addMigrations()`.
4. **Idempotency key contract (deterministic):**
   - PURCHASE tied to a supplier bill: `"{supplierId}_{billId}_PURCHASE"`
   - PAYMENT to a supplier: `"{supplierId}_{paymentId}_PAYMENT"`
   - CONSIGNMENT settlement: `"{supplierId}_{settlementId}_SETTLE"`
   - OPENING balance: `"{supplierId}_{openingId}_OPENING"`
   - ADJUSTMENT: `"{supplierId}_{adjustmentReferenceId}_ADJUSTMENT"`
5. **Repository fix is a required follow-up** — current `UUID.randomUUID()` at three call sites must be replaced with the deterministic contract.

**Alternatives considered:**
- *Application-level dedup (query before insert):* rejected — race conditions; Room’s unique index is atomic.
- *Firestore-only enforcement:* rejected — offline-first; Room is the source of truth.
- *Include epochMillis in key:* rejected — non-deterministic; same operation retried later produces a different key.

**Supersedes:** —

---

## D71 — P10 design system spec: color, typography, shape, motion, lite-mode, forbidden patterns

**Date:** 2026-09-10
**Phase:** P10 (Design Rebuild)
**Context:**
D67 established P10 as the design-enforcement phase. D62 explained why BoiKhataTheme was a pass-through. The vendor has now provided the full design system specification.

D71 extends D67. D67 is NOT superseded.

**Decision:**

### §1 Color Palette (four semantic roles; no additions without a new D-entry)

| Role | Hex | Purpose | WCAG AA on #FDFAF6 |
|------|-----|---------|---------------------|
| Surface | `#FDFAF6` (ivory) | App background, card surfaces | — |
| Primary | `#800000` (maroon) | Brand/identity ONLY — app bar, key actions, headings accent | 10.52:1 |
| Semantic Positive | `#1B6E3F` (muted forest green) | Credit amounts, deposit confirmations, positive balances ONLY | **6.03:1** ✅ |
| Semantic Caution | `#9E5C00` (deep amber) | Overdue indicators, debt warnings, period-locked banners ONLY | **5.06:1** ✅ |

- **RULE:** `#800000` maroon is NOT a semantic error/danger color. It is a brand color.
- **RULE:** No additional semantic colors may be introduced without a new DECISIONS.md entry.

### §2 Typography

- **Font family:** Noto Sans Bengali for all text.
- **Amount display:** `tabular-nums` always.
- **headlineSmall** bumped to **28sp** (from M3 default 24sp); `bodyLarge` stays at 16sp.
- Do NOT alter other M3 type roles unless a future D-entry says so.
- **All user-facing strings:** Bengali-first.

### §3 Shape / Cards

- **Corner radius:** 16dp minimum, 20dp maximum.
- **Elevation:** light (1–2dp elevation) OR a soft 1px border. Never combine elevation + border on the same surface.
- **FORBIDDEN:** double shadows, glassmorphism, frosted glass, blurred backgrounds.

### §4 Motion

- **Allowed animation moments (exhaustive):** (a) Ledger cell save; (b) Deposit or payment save; (c) Tab switch.
- **Duration:** 200ms, ease-in-out.
- **FORBIDDEN:** bounce on button press, spring animation on list items, loading skeleton shimmer, entrance animation on every screen navigation.

### §5 Lite Mode Principle

- “Premium” = richer, more accurate data. NOT more widgets or animations.
- App must remain responsive on a ₹5,000 entry-level Android device.

### §6 Forbidden Design Patterns (grep-enforceable list for PR review)

- Neobank purple/blue gradient backgrounds
- Dark “crypto” theme or any dark-primary palette
- English-only microcopy (every string requires a Bangla version)
- Icon-only bottom nav (text labels are mandatory)
- Dense chart dashboards as primary screens
- Visual mimicry of Revolut / Monzo / N26

**Alternatives considered:**
- *Add an error-red semantic color now:* rejected — no current screen requires it.
- *Use M3 default type scale:* rejected — insufficient hierarchy contrast for ledger readability.
- *Allow spring/bounce animations for “delight”:* rejected — entry-level device performance.
- *Use Material green 500 (#4CAF50):* rejected — contrast 3.3:1 on #FDFAF6, fails WCAG AA.

**Supersedes:** — (extends D67; D67 is NOT superseded)

---

## D72 — Lal Khata theme: 22-token M3 lightColorScheme + Bengali typography (PR #31)
**Date:** 2026-09-10
**Phase:** P10
**Context:** D71 specifies the full design system: four semantic colour roles (maroon, ivory, money-green, amber), Bengali font for all 15 M3 text roles, headlineSmall bumped to 28sp, and Lite-mode 1.2x typography scale. BoiKhataTheme.kt existed as a pass-through (D62) that applied no custom colours or fonts. D67 set P10 as the enforcement phase. This entry documents the implementation committed in PR #31 (commit 9933edb). The DECISIONS.md entry was inadvertently omitted from that PR push; appended here in Session #5 PR.
**Decision:**
1. Replace the pass-through with a full `LalKhataColors` lightColorScheme object, all 22 M3 token slots populated per D71 para 1. Dynamic-colour is never activated (no dynamicLightColorScheme path).
2. Replace the default Typography with `LalKhataTypography`: all 15 M3 text roles set to `BengaliFontFamily`; headlineSmall and headlineMedium both at 28sp (D71 para 2).
3. Add `LocalLiteUi = staticCompositionLocalOf { false }` for Lite-mode propagation.
4. `BoiKhataTheme(liteMode: Boolean)` scales bodyLarge, bodyMedium, titleMedium by 1.2x in Lite mode.
5. `secondaryContainer = Color(0xFFB8F0D4)` fixes the nav-bar active indicator, which previously showed M3-default lavender derived from the maroon primary.
**Alternatives considered:** Dynamic-colour (M3 1.2+ feature) rejected: D2 mandates brand-exact colours; dynamic colour overrides them with system wallpaper tones.
**Supersedes:** —

---

## D73 — Fix: surfaceContainer tokens for Card surfaces (lavender regression after PR #31)
**Date:** 2026-09-10
**Phase:** P10
**Context:** After installing the PR #31 APK, Card surfaces (Total Due, Today’s Sales) still rendered with a lavender/purple tint even though surfaceVariant was correctly set to warm ivory. Root cause: Material3 1.2+ changed Card()’s default background token from surfaceVariant to surfaceContainer. Since D72’s LalKhataColors defined surfaceVariant but omitted surfaceContainer and its three siblings (surfaceContainerLow, surfaceContainerHigh, surfaceContainerHighest), M3 computed those tokens from the primary colour (maroon #800000) via its tonal algorithm, producing a lavender tint.
**Decision:** Add four M3 1.2+ surface-container tokens to LalKhataColors in BoiKhataTheme.kt:
- surfaceContainer = Color(0xFFF2EDE7): warm ivory, same as surfaceVariant (Cards default background)
- surfaceContainerLow = Color(0xFFF7F3EE): slightly lighter (bottom sheets)
- surfaceContainerHigh = Color(0xFFEDE7E1): slightly darker (navigation bar bg)
- surfaceContainerHighest = Color(0xFFE8E1DB): darkest (chips, selected state)

All four stay within the warm-ivory tonal range (HSL ~30 deg, S ~0.20-0.25, L ~0.85-0.96), preserving D71 para 1 surface identity.
**Alternatives considered:** Copy surfaceVariant to all four: simpler but loses M3 tonal depth for Cards/Sheets/Chips visual separation. Set all four to background: too flat, no visual hierarchy between surface levels.
**Supersedes:** —

---

## D74 — Fix: Catalog FAB crash — CategoryDropdown fillMaxWidth in weighted Row
**Date:** 2026-09-10
**Phase:** P10
**Context:** Installing the PR #31 APK confirmed a runtime crash (app stops) when navigating to BookAddEditScreen via the FAB on the Catalog tab. Root cause in BookAddEditScreen.kt: CategoryDropdown composable uses OutlinedTextField(modifier = Modifier.fillMaxWidth()). It is placed inside a Row alongside a Modifier.weight(1f) sibling (the edition-year TextField). Compose measures non-weighted Row children first with the full available Row width; fillMaxWidth() consumes that entire width. The weighted sibling then receives negative remaining width and throws IllegalStateException (negative constraint), crashing the app.
**Decision:** Add `modifier: Modifier = Modifier` parameter to `CategoryDropdown`. Apply the modifier to the outer Box (not the inner OutlinedTextField). The inner OutlinedTextField retains fillMaxWidth(): it now fills the Box, which is constrained by the caller. At the call site, pass `modifier = Modifier.weight(1f)` to CategoryDropdown. No change to ConditionDropdown (used in a Column context: fillMaxWidth() is correct there).
**Alternatives considered:** Remove fillMaxWidth() from the TextField: leaves it at intrinsic width, too narrow for a dropdown label. Wrap the entire Row in BoxWithConstraints: over-engineered for a two-cell row.
**Supersedes:** —
