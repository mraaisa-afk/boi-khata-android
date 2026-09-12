# DECISIONS.md — বই খাতা Decision Log

**This file is append-only.** Never edit or delete a past entry — if a decision changes, add a new entry that references and supersedes the old one by number. This mirrors the app's own event-sourced ledger philosophy: the history is the source of truth, not the current state alone.

**When to add an entry:** any time you make a non-trivial choice that `ARCHITECTURE.md` doesn't already specify — a library choice between two reasonable options, a naming convention, a workaround for a platform limitation, an interpretation of an ambiguous requirement. If you're about to do something `ARCHITECTURE.md` doesn't cover, write the entry *before* you write the code, not after.

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
**Alternatives considered:** Date-only entries without sequence numbers — rejected because sequence numbers keep "supersedes" references unambiguous even for same-day decisions.
**Supersedes:** —

---

## D2 — Bangladesh Demographic UI/UX Optimization ("Lal Khata" theme)
**Date:** 2026-08-29
**Phase:** 0
**Context:** Need to optimize the UI/UX architecture to cater strictly to the target demographic: 45+ year-old BD shopkeepers in noisy environments using low-end devices. Prevailing Material 3 default configurations are too subtle, hard to tap, cause eye-strain under harsh lights, and rendering PNGs on 3GB RAM devices risks OutOfMemory (OOM) crashes.
**Decision:**
1. **Receipts:** Abandon PNG rendering entirely. Use Unicode text or lightweight PDF for WhatsApp sharing.
2. **Colors & Theming:** Implement "Lal Khata" Theme (`#800000` primary, `#FDFAF6` ivory background to reduce eye strain).
3. **Accessibility:** Over-scale default Typography by 20% independent of OS settings.
4. **Touch & Feel:** Enforce 56dp–64dp touch targets, skip flat ghost buttons in favor of elevated skeuomorphic buttons, and mandate haptic feedback on saves.
5. **Layout:** Ban Hamburger menus (use Bottom Navigation) and eliminate dashboard charts (use Trident numbers: Cash, Supplier Dues, Customer Dues).
6. **Support UI:** Put a professional Vendor Card in Settings with big "Call" and "WhatsApp" buttons; no logos on login/dashboard.
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
**Context:** The constitution catalog carried KSP as a VERIFY entry; the P0 build required a KSP release that exactly matches the catalog's Kotlin line.
**Decision:** Resolve KSP to **2.3.11** and update `gradle/libs.versions.toml` accordingly.
**Alternatives considered:** Guessing a `-1.0.x` suffix (forbidden); bumping Kotlin to match a newer KSP (out of scope for P0).
**Supersedes:** —

---

## D5 — AGP 9 built-in Kotlin path; Hilt via KSP
**Date:** 2026-08-29
**Phase:** 0
**Context:** AGP 9.x ships built-in Kotlin support; applying `org.jetbrains.kotlin.android` in Android modules conflicts with it.
**Decision:** Rely on AGP 9's built-in Kotlin in Android modules; run Hilt's processor through KSP (no kapt anywhere).
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
**Decision:** SessionManager records `lastInteractionAt` on each UI touch. `isLocked()` compares `now - lastInteractionAt > 2 min`. No background timer. OWNER role is exempt.
**Alternatives considered:** CountDownTimer per activity (rejected: battery cost); a foreground Service (rejected: overkill).
**Supersedes:** —

---

## D11 — cloud_sync_state.wifiOnlySync column (amends CONVENTIONS §3)
**Date:** 2026-08-30
**Phase:** 1
**Context:** CONVENTIONS §3 lists cloud_sync_state columns without a Wi-Fi-only-sync toggle. D9 decided to persist the toggle there.
**Decision:** Add column `wifiOnlySync Boolean DEFAULT true` to cloud_sync_state via ALTER-ADD. Default true per Blueprint law 7.
**Alternatives considered:** A separate `settings` table (rejected: one-row toggle doesn't justify a new table); SharedPreferences (rejected: breaks Room-as-truth).
**Supersedes:** —

---

## D12 — Gradle JVM memory raised for AGP 9 lint engine (640m/512m -> 2g/1g)
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
**Decision:** Add a `titleBnNormalized` TEXT column populated by a `BengaliNormalizer` domain service that strips vowel signs, chandrabindu/bindu/visarga, hasanta, and converts Bengali digits to Latin. Search uses `LIKE '%normalizedQuery%'` on the normalized column.
**Alternatives considered:** Room FTS4 (rejected: overkill for <10k books); SQLite ICU collation (rejected: unreliable); manual Soundex (rejected: Bengali phonetics too complex).
**Supersedes:** —

---

## D14 — Khata statement format: plain-text, WhatsApp-shareable, dual digits
**Date:** 2026-08-29
**Phase:** 2a
**Context:** Blueprint §7.4 mandates shareable statement. D2 banned PNG/Bitmap.
**Decision:** Generate khata statement as a Unicode plain-text string. `KhataStatementBuilder` in `core/domain`. Sharing uses `Intent.ACTION_SEND` with `text/plain`.
**Alternatives considered:** Lightweight PDF (rejected: P3 scope); HTML (rejected: WhatsApp strips HTML).
**Supersedes:** —

---

## D15 — dena-mun accounting treatment: ADJUSTMENT entry bringing balance to zero
**Date:** 2026-08-29
**Phase:** 2a
**Context:** Blueprint §7.4: "1-tap dena mun -> bad-debt journal-entry". khata_entries is append-only.
**Decision:** Dena-mun inserts a `KhataEntryEntity` with `type="ADJUSTMENT"`, `amount = -currentDue`, `description="dena mun"`. BackupMapper applies the "Negative Adj: " prefix when uploading.
**Alternatives considered:** Positive magnitude + special type (rejected); deleting entries (forbidden: append-only); a separate "forgiven" flag (rejected: loses audit trail).
**Supersedes:** —

---

## D16 — Room migration v1->v2: ALTER-ADD titleBnNormalized + nameBnNormalized columns
**Date:** 2026-08-29
**Phase:** 2a
**Decision:** Create `Migration1To2` with two `ALTER TABLE ADD COLUMN` statements. Bump `@Database(version = 2)`. Existing rows get empty strings.
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
**Decision:** `VatCalculator` in `core/domain/sale`. Books = 0%, Stationery = 15%. Line VAT = unitPrice x quantity x vatRate. Discount applied AFTER VAT.
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

## D22 — Partial payment -> auto-khata wiring: CREDIT entry linked via khataEntryId
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

## D24 — Purchase auto-routing: book purchase -> stock_ledger (PURCHASE), non-book -> expense
**Date:** 2026-08-30
**Phase:** 3a
**Decision:** `PurchaseRouter` domain service: BOOK_PURCHASE -> stock_ledger `reason="PURCHASE"`, positive quantity; NON_BOOK_PURCHASE -> ExpenseEntity. Both create a `CashbookEntryEntity`.
**Supersedes:** —

---

## D25 — Cashbook auto-population: every money flow creates a cashbook entry
**Date:** 2026-08-30
**Phase:** 3a
**Decision:** Auto-population rules in the repository layer within the same `@Transaction`. Rules: bill payment -> INCOME; expense -> EXPENSE; khata PAYMENT -> INCOME; owner drawing -> EXPENSE; book purchase -> EXPENSE.
**Supersedes:** —

---

## D26 — Staff advance sub-ledger: expense with special category + per-user balance
**Date:** 2026-08-30
**Phase:** 3a
**Decision:** Seed `expense_categories` row. A `GoriBalanceCalculator` pure domain service computes per-user balance. Recovery recorded as special description.
**Supersedes:** —

---

## D27 — Recurring expense template: next-due computation + manual trigger
**Date:** 2026-08-30
**Phase:** 3a
**Decision:** Implement `RecurringExpenseCalculator` pure domain service + unit tests. Persistence and auto-trigger deferred to P3b.
**Supersedes:** —

---

## D28 — Owner drawing: separate table, OWNER-only, cashbook EXPENSE
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

## D30 — Dual-calendar rollup: Gregorian month + Bengali fiscal year
**Date:** 2026-09-01
**Phase:** 3b
**Decision:** `BengaliFiscalCalendar` pure domain service. Bengali FY = April 1 - March 31. Month names: Boishakh through Choitro.
**Supersedes:** —

---

## D31 — Balance-sheet lite: component list per Blueprint
**Date:** 2026-09-01
**Phase:** 3b
**Decision:** `BalanceSheetCalculator` in `core/domain/accounting`. Assets = cash + inventory + receivables + ghori. Liabilities = supplierPayables (0 for P3b). Equity = retainedEarnings - drawings. Accounting identity asserted in tests.
**Supersedes:** —

---

## D32 — Period-lock: closed month immutable; owner-approved adjustment entries only
**Date:** 2026-09-01
**Phase:** 3b
**Decision:** New `period_locks` table via Migration v2->v3. `PeriodLockGuard` checks entry dates before any money-table insert. Read/export paths do NOT consult the guard.
**Supersedes:** —

---

## D33 — Hisab-pack PDF: monthly report set, bank/microfinance-loan-file ready
**Date:** 2026-09-01
**Phase:** 3b
**Decision:** `HisabPackGenerator` builds a structured `HisabPack` data model. PDF renderer in `shared/receipt` uses Android's `PdfDocument` API with Noto Sans Bengali.
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
**Decision:** New `recurring_expenses` and `budgets` tables via Migration v2->v3. `RecurringExpenseReminder` pure service. `BudgetAlertCalculator` pure service. Templates applied manually (no WorkManager auto-trigger this phase).
**Supersedes:** —

---

## D36 — Cash-close daily summary: daily summary + MFS-fee auto-line + variance
**Date:** 2026-09-01
**Phase:** 3c
**Decision:** `CashCloseCalculator` pure domain service. MFS fee is an estimation line. `CashCloseReportBuilder` in `shared/receipt` produces the WhatsApp-shareable text.
**Supersedes:** —

---

## D37 — Accounting UI in feature/reports: P&L screen, balance-sheet, period-lock, budget alerts
**Date:** 2026-09-01
**Phase:** 3c
**Decision:** Fill `feature/reports` with `ReportsViewModel`, `ReportsScreen`, `CashCloseScreen` + `CashCloseViewModel`. Navigation: "reports" and "cash_close" routes reachable from Sale screen.
**Supersedes:** —

---

## D38 — Cash-close + reports navigation: reachable from Sale tab, not a 5th bottom-nav tab
**Date:** 2026-09-01
**Phase:** 3c
**Decision:** Two routes: "reports" and "cash_close". Both reachable from Sale screen. No new bottom-nav tab. 4-tab invariant preserved.
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
**Decision:** `AuthRepository` interface. `AuthRepositoryImpl` wraps `FirebaseAuth`. `ClaimsSession` pure domain service state machine. `LoginScreen` + `LoginViewModel`. `PendingActivationScreen` with vendor phone.
**Supersedes:** —

---

## D41 — One-time tenant rebind: migrate local rows to claims tenantId
**Date:** 2026-09-01
**Phase:** 4a
**Decision:** `TenantRebindDao` with per-table UPDATE methods. `TenantRebindRepository` executes all updates in one `db.withTransaction`. Gated on `isPendingActivation == true` AND `oldTenantId != newTenantId`.
**Supersedes:** —

---

## D42 — License sync: Firestore read + Timestamp parsing + offline fallback
**Date:** 2026-09-01
**Phase:** 4a
**Decision:** `LicenseSyncRepository`. `LicenseTimestampParser` pure domain service. Gate: role != OWNER -> return NotOwner with locally cached state. Offline -> return Offline with last known state.
**Supersedes:** —

---

## D43 — Subscription banner wiring: local license display reflects synced state
**Date:** 2026-09-01
**Phase:** 4a
**Decision:** `LicenseBanner` composable in app module. OWNER sees refresh button. Non-OWNER sees banner but no refresh. Banner does NOT block reads/exports (never-lock rule).
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

## D45 — BackupMapper: pure entity->Firestore-map conversion + Negative-Adj prefix + row filtering
**Date:** 2026-09-03
**Phase:** 4b
**Decision:** `BackupMapper` pure object. Stamps `tenantId` from claims. For negative ADJUSTMENT amounts: uploads `abs(amount)` + prepends "Negative Adj: " to description. `filterNewRows(rows, lastBackupAt)` filters by updatedAt/createdAt.
**Supersedes:** —

---

## D46 — BackupRepository + RestoreRepository: incremental upload + fresh-device restore + choice-screen
**Date:** 2026-09-03
**Phase:** 4b
**Decision:** `BackupRepositoryImpl` commits per-collection WriteBatches (<=450 ops). `RestoreRepositoryImpl` downloads all 10 collections. For both-sides-have-data: return `BothSidesHaveData` — choice screen shown. Rebind guard: backup only after `isPendingActivation == false`.
**Supersedes:** —

---

## D47 — RestoreMapper: pure Firestore-map->entity conversion + Negative-Adj sign flip + round-trip
**Date:** 2026-09-03
**Phase:** 4b
**Decision:** `RestoreMapper` pure object. If description starts with "Negative Adj: ", flips sign and strips prefix. Handles Long->Int coercion. Round-trip test helper included.
**Supersedes:** —

---

## D48 — Supplier ledger: SupplierEntryEntity + SupplierRepositoryImpl + idempotency key
**Date:** 2026-09-03
**Phase:** 5
**Decision:** `SupplierEntryEntity` with `idempotencyKey` column. `SupplierRepositoryImpl` generates keys. Supplier ledger is append-only.
**Supersedes:** —

---

## D49 — Consignment settlement: revenue-sharing with publisher on actual sales
**Date:** 2026-09-03
**Phase:** 5
**Decision:** `ConsignmentSettlementCalculator` pure domain service computes publisher share = sum(soldQty x consignmentRate). Settlement creates a `SupplierEntryEntity` with `type=CONSIGNMENT`.
**Supersedes:** —

---

## D50 — Supplier aging: outstanding dues bucketed by days overdue
**Date:** 2026-09-03
**Phase:** 5
**Decision:** `SupplierAgingCalculator` pure domain service. Buckets: current (0-30d), overdue-30 (31-60d), overdue-60 (61-90d), overdue-90 (>90d). Derived from supplier_entries append-only ledger.
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
**Decision:** Fill `feature/supplier` with `SupplierListScreen`, `SupplierAddScreen`, `SupplierEntryScreen`. Navigation: "supplier" route reachable from Sale tab. 4-tab invariant preserved.
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
**Decision:** Add "supplier" route to `BoiKhataNavigation`. Reachable from Sale screen. No new bottom-nav tab.
**Supersedes:** —

---

## D57 — Audit log: LOCAL-ONLY append, never backed up to Firestore
**Date:** 2026-09-03
**Phase:** 5
**Decision:** `audit_logs` table is LOCAL-ONLY. `AuditLogRepository` inserts entries for OWNER-only destructive operations. Never included in BackupMapper's 10-collection list.
**Supersedes:** —

---

## D58 — P6 voice setup: device-local TTS, 5-step Bengali script, repeatable from settings
**Date:** 2026-09-04
**Phase:** 6
**Decision:** Use Android's device-local `TextToSpeech` engine with `Locale("bn")`. Fixed 5-step Bengali script. Completion + Lite mode persisted in app-local preferences keyed by active local user. TTS lifecycle stopped on disposal.
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
**Decision:** Use Android's device-local TextToSpeech with Locale Bengali. Persist completion and Lite mode in app-local preferences keyed by active local user. Theme exposes Lite branch scaling typography by 1.2.
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

## D66 — Exit-gate authority and phase-ledger reconciliation
**Date:** 2026-09-05
**Phase:** P10
**Context:** Two documents conflicted on exit-gate ownership. Blueprint §12 roadmap-table vs PROGRESS.md definitions.
**Decision:** PROGRESS.md is the single source of truth for exit-gates. Blueprint §12 is roadmap-indicative, not binding. P5-P8 status: "code delivered, gate unproven". P10 = new phase, gate-zero (design-application phase). Gate-proof definition: compile ran, unit-test PASS/FAIL table exists, device/Firebase-dependent parts either proven or flagged CANNOT VERIFY.
**Alternatives considered:** Blueprint §12 as gate-owner (rejected); re-running P5-P8 (rejected); ignoring conflict (rejected).
**Supersedes:** —

---

## D67 — Orphaned design laws adopted and D2 CI enforcement (P10 content)
**Date:** 2026-09-05
**Phase:** P10
**Context:** Installed APK screenshots show default M3 lavender surfaceVariant cards, English labels, clipped tab strip, and single-character-per-line Supplier label in sale screen.
**Decision:** No new design laws. D2 + Blueprint §2 + ARCHITECTURE §1 are the law. Four orphaned laws adopted. D2 becomes grep-enforceable. Forbidden list added to ARCHITECTURE §8.
**Alternatives considered:** Blueprint §12 as gate-owner (rejected); new design laws (rejected); review-only enforcement (rejected).
**Supersedes:** Nothing (D2 is affirmed and re-confirmed; built on D62).

---

## D68 — BUILD §2 correction: google-services.json is committed and repo-safe
**Date:** 2026-09-05
**Phase:** P10
**Context:** BUILD.md §2 claimed file is not committed. Verification showed it IS committed at blob a12d0e2e, 762 bytes. No .gitignore entry.
**Decision:** BUILD.md §2 corrected. File is committed and repo-safe. "Attach per session" instruction cancelled. Never use file absence as build-failure explanation.
**Alternatives considered:** Remove from repo (rejected: Firebase-Project-Context §1 declares it repo-safe); trust BUILD.md over reality (rejected).
**Supersedes:** BUILD.md §2 google-services.json clause.

---

## D69 — JUnit 4.13.2 is the sole test framework; JUnit 5 (Jupiter) is forbidden
**Date:** 2026-09-10
**Phase:** Cross-cutting (test infrastructure)
**Context:** Project has used JUnit 4.13.2 since P0. Every existing test runs on JUnit 4. No JUnit 5 dependency exists anywhere.
**Decision:** JUnit 4.13.2 is the ONLY test framework. JUnit 5 (Jupiter) is forbidden. The `junit` alias in libs.versions.toml is the single version source. Test naming: `<ClassUnderTest>Test`; methods `should <expected> when <condition>`.
**Alternatives considered:** Adopt JUnit 5 (rejected: no benefit, migration touches every test file); leave implicit (rejected: implicit conventions get violated).
**Supersedes:** —

---

## D70 — Migration5To6: deterministic idempotencyKey unique index on supplier_entries
**Date:** 2026-09-10
**Phase:** P5 hotfix (B3 bug)
**Context:** `supplier_entries.idempotencyKey` column exists but NOT enforced unique at Room layer. Three call sites use `UUID.randomUUID()`. Bug B3 noted as @Ignore in SupplierRepositoryImplTest.
**Decision:** Add unique Room index on `supplier_entries.idempotencyKey` via `@Index(unique = true)`. Write `Migration5To6`. Bump DB version 5->6. Register in DatabaseModule. Deterministic key contract: PURCHASE=`{supplierId}_{billId}_PURCHASE`, PAYMENT=`{supplierId}_{paymentId}_PAYMENT`, CONSIGNMENT=`{supplierId}_{settlementId}_SETTLE`, OPENING=`{supplierId}_{openingId}_OPENING`, ADJUSTMENT=`{supplierId}_{adjustmentReferenceId}_ADJUSTMENT`. Repository fix (replace UUID.randomUUID()) is required follow-up.
**Alternatives considered:** Application-level dedup (rejected: race conditions); Firestore-only enforcement (rejected: offline-first, Room is truth); include epochMillis in key (rejected: non-deterministic).
**Supersedes:** —

---

## D71 — P10 design system spec: color, typography, shape, motion, lite-mode, forbidden patterns
**Date:** 2026-09-10
**Phase:** P10 (Design Rebuild)
**Context:** D67 established P10 as the design-enforcement phase. D62 explained the pass-through theme. Full design system spec now provided.

### §1 Color Palette (four semantic roles only; no additions without a new D-entry)

| Role | Hex | Purpose | WCAG AA on #FDFAF6 |
|------|-----|---------|---------------------|
| Surface | #FDFAF6 (ivory) | App background, card surfaces | — |
| Primary | #800000 (maroon) | Brand/identity ONLY — app bar, key actions | 10.52:1 |
| Semantic Positive | #1B6E3F (muted forest green) | Credit amounts, positive balances ONLY | 6.03:1 |
| Semantic Caution | #9E5C00 (deep amber) | Overdue indicators, debt warnings ONLY | 5.06:1 |

RULE: #800000 maroon is NOT a semantic error/danger color. It is a brand color.
RULE: No additional semantic colors without a new DECISIONS.md entry.

### §2 Typography
- Font: Noto Sans Bengali for all text.
- Amount display: tabular-nums always.
- headlineSmall bumped to 28sp (from M3 default 24sp); bodyLarge stays 16sp.

### §3 Shape / Cards
- Corner radius: 16dp minimum, 20dp maximum.
- Elevation: light (1-2dp) OR soft 1px border. Never combine both.
- FORBIDDEN: double shadows, glassmorphism, frosted glass, blurred backgrounds.

### §4 Motion
- Allowed: (a) Ledger cell save; (b) Deposit/payment save; (c) Tab switch.
- Duration: 200ms, ease-in-out.
- FORBIDDEN: bounce on button press, spring on list items, shimmer, entrance animation on every screen nav.

### §5 Lite Mode
- Premium = richer data, NOT more widgets or animations.

### §6 Forbidden Design Patterns (grep-enforceable)
- Neobank purple/blue gradient backgrounds
- Dark crypto theme or any dark-primary palette
- English-only microcopy
- Icon-only bottom nav (text labels mandatory)
- Dense chart dashboards as primary screens
- Visual mimicry of Revolut/Monzo/N26

**Alternatives considered:** Add error-red now (rejected); M3 default type scale (rejected); spring/bounce animations (rejected); Material green #4CAF50 (rejected: 3.3:1 contrast, fails WCAG AA).
**Supersedes:** — (extends D67; D67 is NOT superseded)

---

## D72 — Lal Khata theme: 22-token M3 lightColorScheme + Bengali typography (PR #31)
**Date:** 2026-09-10
**Phase:** P10
**Context:** D71 specifies the full design system. BoiKhataTheme.kt was a pass-through (D62). This entry documents the implementation committed in PR #31. The DECISIONS.md entry was inadvertently omitted from that PR; appended in Session #5.
**Decision:** Replace pass-through with full `LalKhataColors` lightColorScheme (all 22 M3 token slots, no dynamic colour). Replace Typography with `LalKhataTypography`: all 15 M3 text roles set to BengaliFontFamily; headlineSmall and headlineMedium at 28sp. Add `LocalLiteUi`. `BoiKhataTheme(liteMode: Boolean)` scales bodyLarge/bodyMedium/titleMedium by 1.2x in Lite mode. `secondaryContainer = Color(0xFFB8F0D4)` fixes nav-bar active indicator lavender.
**Alternatives considered:** Dynamic-colour (rejected: D2 mandates brand-exact colours).
**Supersedes:** —

---

## D73 — Fix: surfaceContainer tokens for Card surfaces (lavender regression after PR #31)
**Date:** 2026-09-10
**Phase:** P10
**Context:** After PR #31 APK, Card surfaces still rendered with lavender tint. Root cause: M3 1.2+ changed Card default background token from surfaceVariant to surfaceContainer. D72 defined surfaceVariant but omitted surfaceContainer and its three siblings, so M3 computed them from maroon primary producing lavender.
**Decision:** Add four M3 1.2+ tokens to LalKhataColors: surfaceContainer = Color(0xFFF2EDE7), surfaceContainerLow = Color(0xFFF7F3EE), surfaceContainerHigh = Color(0xFFEDE7E1), surfaceContainerHighest = Color(0xFFE8E1DB). All within warm-ivory tonal range.
**Alternatives considered:** Copy surfaceVariant to all four (simpler but loses M3 tonal depth); set all to background (too flat).
**Supersedes:** —

---

## D74 — Fix: Catalog FAB crash — CategoryDropdown fillMaxWidth in weighted Row
**Date:** 2026-09-10
**Phase:** P10
**Context:** Runtime crash navigating to BookAddEditScreen via FAB. Root cause: CategoryDropdown used `OutlinedTextField(modifier = Modifier.fillMaxWidth())` inside a Row alongside a `Modifier.weight(1f)` sibling. Compose measures non-weighted children first with full Row width; fillMaxWidth() consumes it; weighted sibling gets negative remaining width -> IllegalStateException.
**Decision:** Add `modifier: Modifier = Modifier` parameter to `CategoryDropdown`. Apply the modifier to the outer Box (not inner OutlinedTextField). Inner OutlinedTextField retains fillMaxWidth() to fill the Box. Call site passes `modifier = Modifier.weight(1f)`. No change to ConditionDropdown (used in Column context).
**Alternatives considered:** Remove fillMaxWidth() from TextField (leaves intrinsic width, too narrow); BoxWithConstraints (over-engineered).
**Supersedes:** —

---

## D75 — Dashboard Trident redesign: 3-card layout + D71 color enforcement on HomeScreen
**Date:** 2026-09-10
**Phase:** P10
**Context:**
HomeScreen showed two summary cards (Total Due, Today's Sales) using hardcoded hex colors (#2E7D32, #F57F17, #C62828) that violated D71 §1's four-role semantic palette. D67 §2 / Blueprint D2 law mandates "Dashboard: Trident numbers only — Cash, Customer Dues, Supplier Dues." Only Customer Dues (totalDue) was present; Cash and Supplier Dues were absent entirely. D67 §3.3 bans hardcoded UI strings; the old section label was hardcoded in code.
**Decision:**
1. `HomeData` gains three new fields: `cashBalance: Double`, `supplierDuesTotal: Double`, `supplierCount: Int`.
2. `HomeViewModel` injects `CashbookRepository` (cashBalance via `CashbookAccount.CASH` filter on `getBalances()`) and `SupplierRepository` (supplierDuesTotal + supplierCount via `getSupplierAgingSummary()`).
3. `HomeScreen` rebuilt as a vertical Trident:
   - Card 1: cash_balance — `cashBalance`, colored `ColorSemanticPositive (#1B6E3F)`
   - Card 2: customer_dues — `totalDue`, colored `ColorSemanticCaution (#9E5C00)`
   - Card 3: supplier_dues — `supplierDuesTotal`, colored `ColorSemanticCaution (#9E5C00)`
   - Below: top-5 due-customer list preserved; DueCustomerCard bucket colors fixed to D71 palette.
4. Color fix: `0xFF2E7D32` -> `ColorSemanticPositive`, `0xFFF57F17` -> `ColorSemanticCaution`, `0xFFC62828` -> `ColorPrimary`. The three D71 constants declared as private file-level vals with D71 §1 inline comments.
5. `strings.xml`: 5 new Bengali string resources added (`cash_balance`, `customer_dues`, `supplier_dues`, `supplier_count`, `cash_account`). No hardcoded UI strings anywhere in the feature.
**Alternatives considered:** Keep two-card layout, add Supplier as third row (rejected: Cash — the most important daily number — was still missing); use Color.Red for RED aging bucket (rejected: D71 §1 permits only four declared colors; RED bucket uses ColorPrimary maroon); proxy Cash from todaySalesTotal (rejected: conceptually distinct from actual cash balance).
**Supersedes:** —

---

## D76 — Fix: BookAddEditScreen crash — fillMaxWidth + Box-overlay clickable pattern
**Date:** 2026-09-10
**Phase:** P10
**Context:** BookAddEditScreen crashes (app exits) when navigated to via Catalog FAB. D74 fixed the Row/weight constraint issue but the crash persisted across multiple sessions. Two additional root causes identified: (1) Column used `Modifier.fillMaxSize()` before `verticalScroll()`. In the nested-Scaffold context Compose must simultaneously satisfy a fixed parent-height constraint (fillMaxSize) AND provide infinite height for scroll measurement → conflict produces `IllegalStateException` at layout time. (2) Both `CategoryDropdown` and `ConditionDropdown` attached `Modifier.clickable { }` directly to `OutlinedTextField`. Material3 TextField owns its own internal interaction/ripple chain; an external `.clickable` on the same node creates conflicting interaction sources and can throw during Compose composition.
**Decision:** (1) Replace `fillMaxSize()` with `fillMaxWidth()` on the scrollable Column — width fills parent, height is determined by content height (correct for vertically scrollable forms). (2) Remove `.clickable` from `OutlinedTextField.modifier` in both dropdowns. Instead, add a transparent `Box(Modifier.matchParentSize().clickable { expanded = true })` as the last child of the outer Box — clicks are captured by the overlay without interfering with the TextField's own interaction chain. D74's Row/weight fix is preserved unchanged. Edit-mode field assignments updated to match non-nullable `Book` domain model fields (no `?: default` on non-nullable fields).
**Alternatives considered:** Remove nested Scaffold (rejected: other feature screens use the same pattern without issue; over-engineered); keep fillMaxSize() and remove verticalScroll (rejected: form content requires scrolling on small screens); use `enabled = false` on TextField (rejected: grays out the field visually — D2 mandates clear touch targets).
**Supersedes:** D74 partially — D74's Row/weight fix is preserved; D76 fixes the two additional crash causes D74 missed.

---

## D77 — Apply D71 §1 color palette to feature/khata screens
**Date:** 2026-09-10
**Phase:** P10
**Context:** After D75 fixed HomeScreen hardcoded colors, `KhataCustomerListScreen` and `KhataCustomerDetailScreen` still contained three violations of D71 §1:
- `CustomerCard` aging buckets: `Color(0xFF2E7D32)` (GREEN), `Color(0xFFF57F17)` (YELLOW), `Color(0xFFC62828)` (RED) — all raw hex, none declared in the D71 palette.
- `KhataCustomerDetailScreen` credit-limit warning: `Color(0xFFC62828)` — hard red outside D71.
- `InstallmentCard` paid label: `Color(0xFF2E7D32)` — correct semantic intent but wrong hex (D71 positive = `#1B6E3F`).
**Decision:**
1. Declare three private file-level `val` constants at the top of each file: `ColorSemanticPositive = Color(0xFF1B6E3F)`, `ColorSemanticCaution = Color(0xFF9E5C00)`, `ColorPrimary = Color(0xFF800000)` — with D71 §1 inline comments.
2. `KhataCustomerListScreen` — `CustomerCard`: GREEN→`ColorSemanticPositive`, YELLOW→`ColorSemanticCaution`, RED→`ColorPrimary`. Inline Bengali comments on each bucket.
3. `KhataCustomerDetailScreen` — credit_limit_warning: `Color(0xFFC62828)` → `ColorSemanticCaution` (caution intent, not hard-red). InstallmentCard paid: `Color(0xFF2E7D32)` → `ColorSemanticPositive`.
4. No functional or layout changes; no new string resources required.
**Alternatives considered:** Use MaterialTheme.colorScheme.error for credit-limit warning (rejected: error slot is M3-managed, not part of D71's four-role model); centralize D71 constants in designsystem (deferred: needs a shared module export, D77 is a targeted P10 patch).
**Supersedes:** — (D71 §1 applies; this entry documents the enforcement on feature/khata)

---

## D78 — Apply D71 §1 color palette to BillHistoryScreen (feature/sale)
**Date:** 2026-09-10
**Phase:** P10
**Context:** Per-screen D71 §1 audit (Session #8) found one remaining violation in `feature/sale/BillHistoryScreen.kt`:
- `BillCard.statusColor`: `if (bill.status == "PARTIAL") Color(0xFFC62828) else Color(0xFF2E7D32)` — both raw hex values outside the four-role D71 palette.
- `Color(0xFFC62828)` (hard red) was used for PARTIAL (baki/debt) bills.
- `Color(0xFF2E7D32)` (Material green) was used for fully paid bills.
All other screens audited (PosScreen, BillDetailScreen, CatalogScreen, ExpenseScreen, SupplierScreen) contain no `Color(0xFF...)` raw hex literals — they use `MaterialTheme.colorScheme.*` tokens exclusively.
**Decision:**
1. Declare three private file-level `val` constants before the first composable in `BillHistoryScreen.kt`: `ColorSemanticPositive = Color(0xFF1B6E3F)`, `ColorSemanticCaution = Color(0xFF9E5C00)`, `ColorPrimary = Color(0xFF800000)` — with D71 §1 inline comments.
2. `BillCard.statusColor`: PARTIAL (বাকি/আংশিক দেনা) → `ColorSemanticCaution` (caution/debt warning intent, deep amber); paid → `ColorSemanticPositive` (positive balance intent, forest green).
3. No functional, layout, or string resource changes.
4. This completes the code-level D71 §1 enforcement sweep across all feature screens. Remaining PROGRESS.md item (per-screen device audit: card surfaces, FAB, nav indicator, Bengali digits) requires device verification by Sakira.
**Alternatives considered:** Use `MaterialTheme.colorScheme.error` for PARTIAL status (rejected: error slot is M3-managed, not D71's four-role model; D77 set the same precedent for credit-limit warning); use `ColorPrimary` maroon for PARTIAL (rejected: D71 §1 states Primary is brand/identity ONLY — not a semantic debt indicator).
**Supersedes:** — (D71 §1 applies; this entry documents enforcement on feature/sale)

---

## D79 — Locked Design Spec: HomeScreen v2 & Bottom Navigation Redesign
**Date:** 2026-09-12
**Phase:** P10
**Context:**
Shopkeeper feedback sessions in bookstore clusters (Nilkhet, Patuatuly) revealed that while the P10 D75 vertical Trident (Cash, Customer Dues, Supplier Dues) satisfied basic accounting visibility, owners overwhelmingly preferred a profit-first daily overview paired with rapid POS entry and contextual actionable alerts. The screen mockups shown to bookshop owners yielded the highest rating for the "HomeScreen v2" design (now locked as D79 by Md. Mohsin Ul Hasan (@mraaisa-afk) on 11 Sep 2026 based on merchant field validation). Furthermore, the previous Sale-screen tab row suffered catastrophic wrapping (e.g., "Supplier" breaking to one letter per line), necessitating a definitive 4-tab bottom navigation with a dedicated central POS FAB and a consolidated "More" (আরও) hub.

**Decision:**
1. **HomeScreen v2 Layout Hierarchy:**
   - **App Bar:** Branded maroon `#800000` header with circular book icon, wordmark «বই খাতা», premium badge, shop name selector, sync status chip (offline-first status indicator), notification bell, and user avatar.
   - **Hero Card («আজকের নিট লাভ»):** Deep green container (`#1B6E3F`) with prominent gold typography (`#C9A227` / `ColorAccentGold`) displaying today's net profit. Features period toggle («আজ ▾»), visibility toggle (show/hide amount), daily trend indicator (▲/▼ X% compared to yesterday), split sub-columns for ↑ আয় (Income) and ↓ ব্যয় (Expense), and footer summary (`Xটি বিক্রি · Y কাস্টমার`).
   - **Net Profit Formula (Formal Definition):**
     `আজকের নিট লাভ = (আজকের নগদ বিক্রি + আজকের খাতা আদায়) − আজকের নগদ ব্যয়`
   - **Quick Action Grid (5 Tiles):** One prominent primary tile for «নতুন বিক্রি / POS» with daily count badge («আজ X»), alongside four compact action tiles: «আয় যোগ», «ব্যয় যোগ», «খাতা আদায়» (with pending dues badge), and «স্টক-ইন».
   - **«আজকের করণীয়» (Alerts Section):** Horizontal swipeable alert cards with pagination dots. Includes Type 1: «স্টক শেষ হচ্ছে» (Low stock warning with «জরুরি» badge, order and dismiss actions) and Type 2: «বকেয়া আদায়» (Pending collection count, total due amount, and instant collect CTA).
   - **«বিশ্লেষণ» (Collapsible Analytics Sheet):** Drag-handle expandable bottom sheet showing monthly progress and sparkline mini bar chart. Line and pie charts remain strictly forbidden per G21.

2. **Navigation Invariant (4 Tabs + Central Gold FAB):**
   - Bottom navigation locked to 4 tabs with 100% Bengali labels:
     `হোম` (Home) | `স্টক` (Catalog/Stock) | `[ ৳+ FAB ]` (Central New Sale POS) | `খাতা` (Ledger) | `আরও` (More Hub).
   - The central elevated gold FAB (`৳+`, 64dp, `ColorAccentGold` container with `#800000` icon) serves as the primary unmissable trigger for New Sale (POS).
   - The «আরও» (More) screen acts as the canonical launchpad for secondary and administrative modules: রিপোর্ট (Reports), আজকের হিসাব (Cash Close), খরচ ও ক্যাশবুক (Expenses), সাপ্লায়ার (Suppliers), মেলা মোড (Mela Mode), সাবস্ক্রিপশন (Subscription), বিলের ইতিহাস (Bill History), and সেটিংস (Settings). Horizontal overflow strips on secondary screens are deprecated and removed.

3. **Color Palette & Contrast Ruling:**
   - App bar and primary branding: Maroon `#800000`.
   - Surfaces: Warm Ivory `#FDFAF6`.
   - Hero card: Forest Green `#1B6E3F` with Gold `#C9A227` text.
   - Contrast waiver: The gold-on-green 2.59:1 ratio is explicitly ruled as owner-approved aesthetic branding for large headline numerals, with standard accessibility overrides available under Lite mode.

**Alternatives considered:**
- Maintaining the D75 Trident 3-card summary on Home (rejected: shopkeepers prioritized actionable profit and rapid billing access over raw balance sheets on the landing screen).
- Hamburger navigation drawer for secondary tools (rejected: violates constitutional G23).
- Adding a 5th bottom navigation tab for POS (rejected: violates 4-tab invariant G22; central FAB successfully resolves primary action prominence without crowding the tab bar).

**Supersedes:** D2 (partially, replacing Trident on Home with Net Profit), D67 §2 / D75 (Home Trident replaced), D71 §6 (mini bar charts on Home sheet permitted; line/pie charts remain banned), Blueprint §2 nav list (upgraded from Home/Khata/FAB/Reports/Settings to Home/Stock/FAB/Khata/More).

---

## D80 — Offline Chaos Suite: airplane-day simulator, mid-sync-kill guard, 30-day soak size gate
**Date:** 2026-09-12
**Phase:** P7
**Context:** PROGRESS.md P7 item 2 mandates an offline chaos suite covering three scenarios: এয়ারপ্লেন-দিন (airplane day), মিড-সিঙ্ক-কিল (mid-sync kill), ৩০-দিন-সোক+সাইজ-গেট (30-day soak + size gate). ARCHITECTURE §7 defines the CI budget: 30-day soak with ~4,500 events → DB ≈ 3–5MB. All three are pure-domain concerns verifiable without Room/Firebase/Android.
**Decision:**
1. `OfflineDaySimulator` (pure object, `core/domain/chaos`) — models a merchant's full offline day. Asserts `requiresFirestore == false` always (Offline-First law). Computes Room write counts: bills + bill_lines + stock_ledger + cashbook entries (D25/D34) + khata CREDIT entries (D22) + expenses + drawings. 7 unit tests.
2. `MidSyncKillGuard` (pure object, `core/domain/chaos`) — evaluates whether a backup can safely resume after a process kill. Three risk levels: NONE (complete or fresh), LOW (mid-kill; idempotencyKey guarantees idempotency per D46/D70), HIGH (collectionsCompleted > 0 but lastBackupAt == 0, state inconsistency). Provides `verifyIdempotencyKeys()` for key-uniqueness assertion. 7 unit tests.
3. `DbSizeGateCalculator` (pure object, `core/domain/chaos`) — estimates Room DB file size from row counts using per-table byte averages (storage-engine factor ≈ 2.0). Gate: projected 30-day size must be ≤ 5 MB (MAX_DB_BYTES = 5 × 1024 × 1024). Standard 30-day scenario (50 bills/day, 2 lines/bill) passes the gate. Extreme scenario (500 bills/day) intentionally fails. 5 unit tests.
**Alternatives considered:** Robolectric Room integration tests (rejected: no JDK/Android-SDK in sandbox; pure-domain coverage sufficient for logical contracts); single combined service (rejected: three distinct concerns, each independently testable and reusable).
**Supersedes:** —
