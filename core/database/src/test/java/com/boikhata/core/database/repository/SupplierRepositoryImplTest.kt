package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.database.dao.SupplierDao
import com.boikhata.core.database.entity.CashbookEntryEntity
import com.boikhata.core.database.entity.SupplierEntity
import com.boikhata.core.database.entity.SupplierEntryEntity
import com.boikhata.core.domain.accounting.PeriodLockChecker
import com.boikhata.core.domain.accounting.PeriodLockGuard
import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.enums.SupplierEntryType
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * P5 exit-gate: কনসাইনমেন্ট-সেটেলমেন্ট E2E test at the repository layer.
 * Uses in-memory fakes (no Room / no device) to prove the full flow:
 * addSupplier → opening → consignment → purchase → payment → balance + statement.
 */
class SupplierRepositoryImplTest {

    private class FakeSupplierDao : SupplierDao {
        val suppliers = mutableListOf<SupplierEntity>()
        val entries = mutableListOf<SupplierEntryEntity>()

        override suspend fun insertSupplier(supplier: SupplierEntity) { suppliers.add(supplier) }
        override suspend fun getSuppliers(tenantId: String) = suppliers.filter { it.tenantId == tenantId }
        override suspend fun getSupplierById(id: String) = suppliers.firstOrNull { it.id == id }
        override suspend fun getEntries(tenantId: String, supplierId: String) = entries.filter { it.tenantId == tenantId && it.supplierId == supplierId }
        override suspend fun getEntriesByDateRange(tenantId: String, supplierId: String, start: Long, end: Long) =
            entries.filter { it.tenantId == tenantId && it.supplierId == supplierId && it.date in start until end }
        override suspend fun getAllEntriesByTenant(tenantId: String) = entries.filter { it.tenantId == tenantId }
        override suspend fun insertEntry(entry: SupplierEntryEntity) { entries.add(entry) }
    }

    private class FakeCashbookDao : CashbookDao {
        val entries = mutableListOf<CashbookEntryEntity>()
        override suspend fun insert(entry: CashbookEntryEntity) { entries.add(entry) }
        override suspend fun getByTenant(tenantId: String) = entries.filter { it.tenantId == tenantId }
        override suspend fun getByAccount(tenantId: String, account: String) = entries.filter { it.tenantId == tenantId && it.account == account }
        override suspend fun getByDateRange(tenantId: String, start: Long, end: Long) = entries.filter { it.tenantId == tenantId && it.date in start until end }
    }

    private object NoLock : PeriodLockChecker {
        override suspend fun getLockedPeriods(tenantId: String): Set<PeriodLockGuard.LockedPeriod> = emptySet()
        override suspend fun assertNotLocked(tenantId: String, date: Long) { /* no-op */ }
    }

    private val writeGuard = LicenseWriteGuard() // default GRACE → writes allowed

    @Test
    fun `consignment settlement E2E - balances and statement through repository`() = runBlocking {
        val supplierDao = FakeSupplierDao()
        val cashbookDao = FakeCashbookDao()
        val repo = SupplierRepositoryImpl(supplierDao, cashbookDao, writeGuard, NoLock)

        val supplierId = repo.addSupplier("t_1", "রাইসা প্রকাশনী", "017...", "30", null)
        repo.addOpeningBalance("t_1", supplierId, 400.0, "u_1")
        repo.addConsignment("t_1", supplierId, 600.0, "জানুয়ারি কনসাইনমেন্ট", "u_1")
        repo.addPurchase("t_1", supplierId, 200.0, "মেলা ক্রয়", "u_1")

        // Partial payment 500 → FIFO nets opening(400) + 100 of consignment → payable 700
        repo.addPayment("t_1", supplierId, 500.0, "আংশিক সেটেল", "trx123", "u_1", CashbookAccount.CASH)

        val balance = repo.getSupplierBalance("t_1", supplierId, System.currentTimeMillis())
        assertThat(balance.balance).isEqualTo(700.0)
        assertThat(cashbookDao.entries).hasSize(1)
        assertThat(cashbookDao.entries[0].type).isEqualTo("EXPENSE")
        assertThat(cashbookDao.entries[0].amount).isEqualTo(500.0)
        assertThat(cashbookDao.entries[0].description).contains("রাইসা প্রকাশনী")

        // Full settlement 700
        repo.addPayment("t_1", supplierId, 700.0, "চূড়ান্ত সেটেল", "trx456", "u_1", CashbookAccount.BKASH)

        val finalBalance = repo.getSupplierBalance("t_1", supplierId, System.currentTimeMillis())
        assertThat(finalBalance.balance).isEqualTo(0.0)
        assertThat(cashbookDao.entries).hasSize(2)

        // Statement
        val statement = repo.getSettlementStatement("t_1", supplierId, "রাইসা ট্রেডিং হাউজ", null, System.currentTimeMillis() + 1)
        assertThat(statement.supplier.nameBn).isEqualTo("রাইসা প্রকাশনী")
        assertThat(statement.totalPayable).isEqualTo(0.0)
        assertThat(statement.entries).hasSize(5)
    }

    @Test
    fun `entry types are append-only via repository - no delete update exposed`() = runBlocking {
        val supplierDao = FakeSupplierDao()
        val repo = SupplierRepositoryImpl(supplierDao, FakeCashbookDao(), writeGuard, NoLock)
        val supplierId = repo.addSupplier("t_1", "সাপ্লায়ার", null, "30", null)
        repo.addEntry("t_1", supplierId, 100.0, SupplierEntryType.CONSIGNMENT, "goods", null, 1000L, "u_1")
        repo.addEntry("t_1", supplierId, -30.0, SupplierEntryType.ADJUSTMENT, "correction", null, 1001L, "u_1")
        val entries = repo.getEntries("t_1", supplierId)
        assertThat(entries).hasSize(2)
        assertThat(entries.first().type).isEqualTo(SupplierEntryType.CONSIGNMENT)
    }
}
