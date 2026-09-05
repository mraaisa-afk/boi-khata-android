# ROOM_MIGRATION_LEDGER.md — Boi-Khata Room Migration History

> **Rules:**
> - Append-only. Never edit past migration entries.
> - The agent reads this BEFORE any schema-change task.
> - The agent appends a new entry AFTER each schema bump, in the same PR as the migration class.
> - Any change to `TenantRebindPlanner.ALL_TENANT_TABLES` must update the registry below in the same PR.
> - Guardrails G15, G16 and G17 apply to everything in this file.
>
> **Verified against the repository on 2026-09-05** (commit `ca5fc7f`), reading
> `BoiKhataDatabase.kt`, the `migration/` package, and `TenantRebindPlanner.kt`.

---

## Current Schema Version

**Current Room DB version: v5**

- Database class: `core/database/src/main/java/com/boikhata/core/database/BoiKhataDatabase.kt`
- `exportSchema = true`
- Entities registered: **24**
- Last migration: `Migration4To5`
- Next migration would be: `Migration5To6`

### Naming convention

Migration classes in this repo are named `Migration<N>To<N+1>`:

| Class | File |
| --- | --- |
| `Migration1To2` | `migration/Migration1To2.kt` |
| `Migration2To3` | `migration/Migration2To3.kt` |
| `Migration3To4` | `migration/Migration3To4.kt` |
| `Migration4To5` | `migration/Migration4To5.kt` |

> Do **not** write `MigrationV1_V2`. That form does not exist in this codebase.

---

## Version History

Sourced from the KDoc block on `BoiKhataDatabase`.

| Version | Migration class | Change | Decision |
| --- | --- | --- | --- |
| v1 | none (initial creation) | Initial schema | — |
| v2 | `Migration1To2` | Normalised columns | D16 |
| v3 | `Migration2To3` | Accounting tables | D32, D35 |
| v4 | `Migration3To4` | `mela_sessions` table | D57 |
| v5 | `Migration4To5` | `trial_redemptions` table | D64 |

> **Known gap:** the per-migration SQL was not read line by line during this audit.
> Before relying on the exact column-level changes for v2 and v3, open the migration
> files themselves. The version-to-decision mapping above comes from the database
> class KDoc and is authoritative for *what* changed, not for *how*.

---

## Registered Entities (v5)

All 24 entities declared in the `@Database` annotation:

`TenantEntity`, `UserEntity`, `DeviceEntity`, `CloudSyncStateEntity`, `BookEntity`,
`StockLedgerEntity`, `BillEntity`, `BillLineEntity`, `KhataCustomerEntity`,
`KhataEntryEntity`, `KhataInstallmentEntity`, `ExpenseCategoryEntity`, `ExpenseEntity`,
`CashbookEntryEntity`, `OwnerDrawingEntity`, `SupplierEntity`, `SupplierEntryEntity`,
`MasterCatalogEntity`, `AuditLogEntity`, `PeriodLockEntity`, `RecurringExpenseEntity`,
`BudgetEntity`, `MelaSessionEntity`, `TrialRedemptionEntity`.

---

## TenantRebindPlanner.ALL_TENANT_TABLES Registry

> **G16:** any change to this list requires reading this file first, updating the table
> below in the same PR, and updating the test that asserts the total count.

**Verified count: 23 tables.**

Source: `core/domain/src/main/java/com/boikhata/core/domain/cloud/TenantRebindPlanner.kt`
(decision **D41**, Firebase-Project-Context constraint 12).

| # | Table |
| --- | --- |
| 1 | `tenants` (PK `id` = the tenantId; handled by `rebindTenants()`) |
| 2 | `users` |
| 3 | `devices` |
| 4 | `cloud_sync_state` |
| 5 | `books` |
| 6 | `stock_ledger` |
| 7 | `bills` |
| 8 | `bill_lines` |
| 9 | `khata_customers` |
| 10 | `khata_entries` |
| 11 | `khata_installments` |
| 12 | `expense_categories` |
| 13 | `expenses` |
| 14 | `cashbook_entries` |
| 15 | `owner_drawings` |
| 16 | `suppliers` |
| 17 | `supplier_entries` |
| 18 | `audit_logs` |
| 19 | `period_locks` |
| 20 | `recurring_expenses` |
| 21 | `budgets` |
| 22 | `mela_sessions` |
| 23 | `trial_redemptions` |

### Excluded table

| Table | Why excluded |
| --- | --- |
| `master_catalog` | Shared catalogue with no `tenantId` column. Firestore rule is `read: if isAuthenticated()`, so it is not tenant-scoped. |

**Arithmetic check:** 23 tenant-scoped tables + 1 shared table = 24 registered entities. The list is complete.

---

## Migration Writing Rules

- Migration bodies contain raw SQL only — no Kotlin or Room types inside `migrate()`
- Prefer idempotent DDL: `CREATE TABLE IF NOT EXISTS`, `DROP TABLE IF EXISTS`
- Every migration needs a `MigrationTestHelper` test: version bump, no data loss, correct column mapping
- NEVER use `fallbackToDestructiveMigration()` (G17)
- Renaming a table: create the new one, `INSERT INTO new SELECT ... FROM old`, then drop the old one
- A new tenant-scoped table must be added to `ALL_TENANT_TABLES` **and** to the registry above in the same PR
- `TenantRebindPlanner` is a pure object with no Android and no Room dependency — keep it that way so it stays independently unit-testable

---

*Last updated: 2026-09-05 · Maintained by: Agent (append) + Builder/Sakira (review)*
*Referenced by: AGENT_PLAYBOOK.md Step 4, AGENT_GUARDRAILS.md G15-G17, DEFINITION_OF_DONE.md Gate 3*
