package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Query

/**
 * D41: TenantRebindDao — UPDATE tenantId on every table in one Room transaction.
 * One method per table (Room can't do dynamic table names).
 * Firebase-Project-Context.md §6 constraint #12: migrate all local "t_1" rows
 * to the claims tenantId BEFORE any backup.
 */
@Dao
interface TenantRebindDao {

    // tenants table: PK = id (which IS the tenantId) — UPDATE the id column
    @Query("UPDATE tenants SET id = :newTenantId WHERE id = :oldTenantId")
    suspend fun rebindTenants(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE users SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindUsers(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE devices SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindDevices(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE cloud_sync_state SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindCloudSyncState(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE books SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindBooks(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE stock_ledger SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindStockLedger(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE bills SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindBills(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE bill_lines SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindBillLines(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE khata_customers SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindKhataCustomers(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE khata_entries SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindKhataEntries(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE khata_installments SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindKhataInstallments(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE expense_categories SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindExpenseCategories(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE expenses SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindExpenses(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE cashbook_entries SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindCashbookEntries(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE owner_drawings SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindOwnerDrawings(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE suppliers SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindSuppliers(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE supplier_entries SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindSupplierEntries(oldTenantId: String, newTenantId: String): Int

    // master_catalog: no tenantId column (shared catalog per Firestore rules — read: isAuthenticated)
    // No rebind needed for master_catalog

    @Query("UPDATE audit_logs SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindAuditLogs(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE period_locks SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindPeriodLocks(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE recurring_expenses SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindRecurringExpenses(oldTenantId: String, newTenantId: String): Int

    @Query("UPDATE budgets SET tenantId = :newTenantId WHERE tenantId = :oldTenantId")
    suspend fun rebindBudgets(oldTenantId: String, newTenantId: String): Int
}
