package com.boikhata.core.domain.cloud

/**
 * D41: TenantRebindPlanner — produces the rebind plan for migrating local "t_1" rows
 * to the claims tenantId. Firebase-Project-Context.md §6 constraint #12.
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 */
object TenantRebindPlanner {

    /** A table to rebind. */
    data class RebindTable(
        val tableName: String,
        val hasTenantId: Boolean,
    )

    /** The plan: old tenantId → new tenantId + the list of tables to update. */
    data class RebindPlan(
        val oldTenantId: String,
        val newTenantId: String,
        val tables: List<RebindTable>,
    )

    /**
     * All tables that carry a tenantId column (CONVENTIONS §3 + P3b tables).
     * NOTE: master_catalog is excluded — it's a shared catalog (no tenantId column),
     * per Firestore rules: `read: if isAuthenticated()` (not tenant-scoped).
     * The tenants table uses `id` as PK (= the tenantId), handled by rebindTenants().
     */
    val ALL_TENANT_TABLES = listOf(
        "tenants",           // PK = id (= tenantId)
        "users",
        "devices",
        "cloud_sync_state",
        "books",
        "stock_ledger",
        "bills",
        "bill_lines",
        "khata_customers",
        "khata_entries",
        "khata_installments",
        "expense_categories",
        "expenses",
        "cashbook_entries",
        "owner_drawings",
        "suppliers",
        "supplier_entries",
        "audit_logs",
        "period_locks",
        "recurring_expenses",
        "budgets",
    )

    /**
     * Build the rebind plan.
     * @param oldTenantId the local seed tenantId (typically "t_1")
     * @param newTenantId the claims tenantId from the ID token
     */
    fun plan(oldTenantId: String, newTenantId: String): RebindPlan {
        return RebindPlan(
            oldTenantId = oldTenantId,
            newTenantId = newTenantId,
            tables = ALL_TENANT_TABLES.map { RebindTable(it, true) },
        )
    }

    /**
     * Check if a rebind is needed.
     * Skip if old == new (already bound or same tenant).
     */
    fun shouldRebind(oldTenantId: String, newTenantId: String): Boolean {
        return oldTenantId != newTenantId
    }
}
