# PROGRESS.md — বই খাতা Build Progress

## Workflow Rules (enforced — read before every session)

1. **এক ফেজ = এক PR = সর্বোচ্চ ৫টি batch push = শেষে একবার merge** — no cross-phase commits.
2. **Sequential commits only** — `create_or_update_file` calls must be sequential (never parallel); each commit changes branch HEAD.
3. **JUnit 4.13.2 only (D69)** — JUnit 5 (Jupiter) is forbidden everywhere.
4. **D71 four color roles only** — no new colors without a DECISIONS.md entry.
5. **Append-only logs** — DECISIONS.md and PROGRESS.md are never edited retroactively.
6. **Gate-proof before merge** — compile + test table must exist before a phase PR is merged.
7. **Workflow docs PR** — a PR containing only workflow rules/docs (no code) may be merged by user without gate-proof.
8. **D79 locked** — HomeScreen v2 layout, 4-tab + FAB navigation, and net-profit formula are now constitutional; no deviation without a new D-entry.
9. **D81 locked** — `todaySalesTotal` always uses `paidAmount`; khata PAYMENT collections are always included in home net-profit income.

---

## Phase Log

### P0 — Project Skeleton
- [x] Repo, .gitignore, CONVENTIONS, ARCHITECTURE, BUILD, DECISIONS, PROGRESS scaffolded
- [x] AGP 9 + KSP + Hilt + Room multi-module Gradle build green
- [x] core/designsystem: Lal Khata theme + Noto Sans Bengali font
- [x] Minimum compile-able shell committed
- **PR #1 merged** — CI green ✅

---

### P1 — Auth + Local User + Session
- [x] Local user CRUD (Room)
- [x] PIN hashing (PBKDF2-SHA256, D8)
- [x] SessionManager auto-lock 2-min (D10)
- [x] Data-meter foundation (D9)
- [x] Wi-Fi-only toggle (D11)
- [x] Migration v1->v2 (D16)
- **PR #2 merged** — CI green ✅

---

### P2a — Catalog + Khata
- [x] BookRepository + BookDao (search, CRUD, low-stock)
- [x] BengaliNormalizer + fuzzy search (D13)
- [x] KhataCustomerRepository + KhataEntryDao
- [x] AgingCalculator + KhataStatementBuilder (D14, D15)
- [x] KhataInstallmentDao (D17)
- [x] Bottom nav: Home/Catalog/Khata (D18)
- **PR #3 merged** — CI green ✅

---

### P2b — POS / Billing
- [x] BillRepository + SaleRepositoryImpl (D22, D23)
- [x] VatCalculator (D19)
- [x] BillNumberGenerator (D20)
- [x] ReceiptBuilder plain-text (D21)
- [x] Partial payment -> auto-khata CREDIT wiring
- **PR #4 merged** — CI green ✅

---

### P3a — Expense + Cashbook + Owner Drawing
- [x] ExpenseRepository + PurchaseRouter (D24)
- [x] CashbookRepository + auto-population (D25)
- [x] GoriBalanceCalculator staff-advance (D26)
- [x] RecurringExpenseCalculator pure domain (D27)
- [x] OwnerDrawingRepository (D28)
- **PR #5 merged** — CI green ✅

---

### P3b — Accounting (P&L + Balance Sheet + Period-Lock)
- [x] PnLCalculator COGS split (D29)
- [x] BengaliFiscalCalendar dual-calendar (D30)
- [x] BalanceSheetCalculator (D31)
- [x] PeriodLockGuard + period_locks table (D32)
- [x] HisabPackGenerator (D33)
- [x] Cashbook auto-populate from bill payments (D34)
- [x] RecurringExpense persistence + BudgetAlertCalculator (D35)
- **PR #6 merged** — CI green ✅

---

### P3c — Cash-Close + Reports UI
- [x] CashCloseCalculator + CashCloseReportBuilder (D36)
- [x] ReportsViewModel + ReportsScreen
- [x] CashCloseScreen + CashCloseViewModel (D37, D38)
- **PR #7 merged** — CI green ✅

---

### P4a — Firebase Auth + License Sync
- [x] google-services.json committed (D68)
- [x] Phone-OTP login + ClaimsSession (D40)
- [x] TenantRebindRepository (D41)
- [x] LicenseSyncRepository (D42)
- [x] LicenseBanner (D43)
- [x] MainViewModel AuthState machine (D44)
- **PR #8 merged** — CI green ✅

---

### P4b — Backup + Restore + Subscription
- [x] BackupMapper + BackupRepository (D45, D46)
- [x] RestoreMapper + RestoreRepository (D47)
- [x] SubscriptionRepository
- [x] MasterCatalogRepository (D51)
- **PR #9 merged** — CI green ✅

---

### P5 — Supplier + Mela + Catalog Depth
- [x] SupplierRepository + SupplierEntryEntity (D48)
- [x] ConsignmentSettlementCalculator (D49)
- [x] SupplierAgingCalculator (D50)
- [x] MasterCatalogRepository barcode integration (D52)
- [x] SupplierScreen UI (D53)
- [x] SupplierStatementBuilder (D54)
- [x] StockAlertCalculator (D55)
- [x] MelaRepository + session lifecycle (D56)
- [x] AuditLogRepository LOCAL-ONLY (D57)
- **PR #10 merged** — CI green ✅

---

### P6 — Voice Setup + Annual Reports + Data Export
- [x] Voice setup TTS 5-step Bengali script (D58)
- [x] AnnualTrendReport 12-month rolling (D59)
- [x] MonthlyDataCopy WorkManager artifact (D60)
- [x] Lite mode per-user preference (D61)
- [x] BoiKhataTheme enforced via core/designsystem (D62)
- **PR #11 merged** — CI green ✅

---

### P7 — Trial + Phone Migration + Offline Chaos
- [x] TrialEligibilityService + TrialCapEnforcer (D63)
- [x] PhoneNumberMigration state machine (D63)
- [x] trial_redemptions table + Migration v4->v5 (D64)
- [x] Offline Chaos Suite — OfflineDaySimulator, MidSyncKillGuard, DbSizeGateCalculator (D80)
- **PR merged** — CI green ✅

---

### P8 — Release Hardening + Pilot Controls
- [x] R8 release configuration
- [x] Version constants in Gradle
- [x] DemoReset owner-confirmed local destructive operation
- [x] ReferralCode deterministic tenant-derived identifiers (D65)
- **PR merged** — CI green ✅

---

### P10 — Design Rebuild + Formula Fixes (active phase)

**Exit gate:** Every P10 PR must compile green. Gate-proof = compile + test table.

#### Batch 1 — Design System Foundation
- [x] Lal Khata 22-token M3 lightColorScheme (D72)
- [x] Bengali typography 15-role (D72)
- [x] LiteMode scaling (D61/D72)
- [x] secondaryContainer nav-indicator fix (D72)
- **PR #31 merged** — CI green ✅

#### Batch 2 — Card Surface + Catalog Crash Fixes
- [x] surfaceContainer 4-token ivory fix (D73)
- [x] CategoryDropdown fillMaxWidth Row/weight crash fix (D74)
- [x] BookAddEditScreen fillMaxSize->fillMaxWidth + Box-overlay clickable crash fix (D76)
- **PR merged** — CI green ✅

#### Batch 3 — D71 Color Enforcement Sweep
- [x] HomeScreen hardcoded-hex -> D71 palette (D75)
- [x] HomeData Trident 3 fields: cashBalance, supplierDuesTotal, supplierCount (D75)
- [x] HomeViewModel: CashbookRepository + SupplierRepository injection (D75)
- [x] KhataCustomerListScreen aging bucket colors (D77)
- [x] KhataCustomerDetailScreen credit-limit warning color (D77)
- [x] BillHistoryScreen statusColor fix (D78)
- **PR merged** — CI green ✅

#### Batch 4 — Workflow Rules PR (docs only)
- [x] CONVENTIONS.md — workflow rules section appended
- [x] ARCHITECTURE.md — forbidden-pattern §8 and exit-gate §9 added
- **PR #49 merged** — Merge commit `d53474ae`, CI Passing ✅

#### Batch 5 — HomeScreen v2 + D79 Navigation (in-progress / pending merge)
- [x] HomeScreen v2: HeroCard, QuickActionGrid, AlertsSection, AnalyticsSheet (D79)
- [x] BoiKhataNavigation: 4-tab + central FAB (D79)
- [x] MoreScreen: 8-destination grid (D79)
- [x] HomeData: todaySalesTotal, todayExpenseTotal, todayBillCount, topDueCustomers, yesterdayNetProfit, lowStockAlerts, monthNetProfit, monthAnalytics (D79)
- [x] HomeViewModel: D79 full data load (D79)
- [x] ColorAccentGold `#C9A227` declared in core/designsystem (D79)
- **Branch `agent/p5-exit-gate`** — PR #50 open, awaiting user merge

#### Batch 6 — D81 Formula Fix (current batch — branch: agent/p10-d81-formula-fix)
- [x] `KhataEntryDao.getPaymentSumByDateRange` — new DAO aggregate query (D81)
- [x] `HomeData.todayKhataCollection: Double = 0.0` added (D81)
- [x] `KhataRepository.getKhataCollectionByDateRange` — interface method added (D81)
- [x] `KhataRepositoryImpl.getKhataCollectionByDateRange` — implementation added (D81)
- [x] `HomeViewModel`: `todaySalesTotal` corrected to `paidAmount`; `todayKhataCollection` queried and passed (D81)
- [x] `HomeScreen.HeroCard`: `netProfit = todayIncome - expense` where `todayIncome = sales + khataCollection` (D81)
- [x] `DECISIONS.md`: D81 entry appended
- [x] `PROGRESS.md`: D81 logged (this entry)
- **PR to be opened** — awaiting user review + merge

---

## Key Decisions Summary (quick ref)
| D# | Topic |
|-----|-------|
| D2 | Lal Khata UI/UX theme, Bengali-first, 56dp touch targets |
| D13 | BengaliNormalizer fuzzy search |
| D15 | dena-mun = ADJUSTMENT entry (append-only) |
| D22 | Partial payment → auto khata CREDIT |
| D25 | Cashbook auto-population from all money flows |
| D34 | Cashbook from bill payments + khata collections |
| D69 | JUnit 4.13.2 only, JUnit 5 forbidden |
| D70 | supplier_entries.idempotencyKey unique index |
| D71 | Four color roles only; full design system spec |
| D72 | 22-token M3 lightColorScheme + Bengali typography |
| D75 | Home Trident 3-card layout + D71 color enforcement |
| D79 | HomeScreen v2 locked spec + 4-tab+FAB navigation |
| D80 | Offline Chaos Suite (OfflineDaySimulator, MidSyncKillGuard, DbSizeGateCalculator) |
| D81 | D79 formula fix: paidAmount + khata collection |

---

## Active Branches
| Branch | Status |
|--------|--------|
| `agent/p5-exit-gate` | PR #50 open — awaiting user merge |
| `agent/p10-d81-formula-fix` | Batch 6 commits done — PR to open |

---

## Notion PR Review DB
| PR # | Branch | Status |
|------|--------|--------|
| #49 | docs/workflow-stacked-push | Merged ✅ (Notion row: logged) |
| #50 | agent/p5-exit-gate | Open — awaiting merge |
