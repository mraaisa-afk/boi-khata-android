package com.boikhata.core.database.repository

import androidx.room.withTransaction
import com.boikhata.core.database.BoiKhataDatabase
import com.boikhata.core.database.dao.CloudSyncStateDao
import com.boikhata.core.database.dao.TenantRebindDao
import com.boikhata.core.domain.cloud.TenantRebindPlanner
import com.boikhata.core.domain.repository.TenantRebindRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D41: TenantRebindRepository implementation.
 * Firebase-Project-Context.md §6 constraint #12: migrate all local "t_1" rows
 * to the claims tenantId in one Room transaction BEFORE any backup.
 */
@Singleton
class TenantRebindRepositoryImpl @Inject constructor(
    private val db: BoiKhataDatabase,
    private val rebindDao: TenantRebindDao,
    private val cloudSyncStateDao: CloudSyncStateDao,
) : TenantRebindRepository {

    override suspend fun rebind(oldTenantId: String, newTenantId: String): Int {
        if (!TenantRebindPlanner.shouldRebind(oldTenantId, newTenantId)) return 0

        var totalUpdated = 0
        db.withTransaction {
            totalUpdated += rebindDao.rebindTenants(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindUsers(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindDevices(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindCloudSyncState(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindBooks(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindStockLedger(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindBills(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindBillLines(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindKhataCustomers(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindKhataEntries(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindKhataInstallments(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindExpenseCategories(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindExpenses(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindCashbookEntries(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindOwnerDrawings(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindSuppliers(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindSupplierEntries(oldTenantId, newTenantId)
            // master_catalog: no tenantId column (shared catalog per Firestore rules) — skip
            totalUpdated += rebindDao.rebindAuditLogs(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindPeriodLocks(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindRecurringExpenses(oldTenantId, newTenantId)
            totalUpdated += rebindDao.rebindBudgets(oldTenantId, newTenantId)

            // Update cloud_sync_state to reflect the rebind
            val state = cloudSyncStateDao.get()
            if (state != null) {
                cloudSyncStateDao.upsert(
                    state.copy(
                        tenantId = newTenantId,
                        isPendingActivation = false,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            }
        }
        return totalUpdated
    }
}
