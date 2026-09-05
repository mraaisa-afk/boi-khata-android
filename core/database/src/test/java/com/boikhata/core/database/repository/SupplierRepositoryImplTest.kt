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
import com.boikhata.core.domain.enums.LicenseState
import com.boikhata.core.domain.license.LicenseBlockedException
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Ignore
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

    // ── P5 repository coverage: passing tests (current behaviour) ───────

    private class LockedPeriods : PeriodLockChecker {
        override suspend fun getLockedPeriods(tenantId: String): Set<PeriodLockGuard.LockedPeriod> = emptySet()
        override suspend fun assertNotLocked(tenantId: String, date: Long) {
            throw PeriodLockGuard.PeriodLockedException("locked")
        }
    }

    @Test
    fun `addPayment above threshold writes both supplier entry and cashbook EXPENSE mirror`() = runTest {
        val supplierDao = FakeSupplierDao()
        val cashbookDao = FakeCashbookDao()
        val repo = SupplierRepositoryImpl(supplierDao, cashbookDao, writeGuard, NoLock)
        val supplierId = repo.addSupplier("t_1", "সাপ্লায়ার", null, "30", null)

        repo.addPayment("t_1", supplierId, 150.0, "পেমেন্ট", "trx1", "u_1", CashbookAccount.CASH)

        assertThat(supplierDao.entries).hasSize(1)
        assertThat(supplierDao.entries[0].type).isEqualTo("PAYMENT")
        assertThat(supplierDao.entries[0].amount).isEqualTo(150.0)
        assertThat(cashbookDao.entries).hasSize(1)
        assertThat(cashbookDao.entries[0].type).isEqualTo("EXPENSE")
        assertThat(cashbookDao.entries[0].amount).isEqualTo(150.0)
        assertThat(cashbookDao.entries[0].referenceId).isEqualTo(supplierDao.entries[0].id)
    }

    @Test
    fun `addPayment with blank description defaults and appends trxId`() = runTest {
        val supplierDao = FakeSupplierDao()
        val cashbookDao = FakeCashbookDao()
        val repo = SupplierRepositoryImpl(supplierDao, cashbookDao, writeGuard, NoLock)
        val supplierId = repo.addSupplier("t_1", "সাপ্লায়ার", null, "30", null)

        repo.addPayment("t_1", supplierId, 100.0, "", "trx789", "u_1", CashbookAccount.CASH)

        assertThat(supplierDao.entries[0].description).isEqualTo("সাপ্লায়ার পেমেন্ট (trxID: trx789)")
    }

    @Test
    fun `addConsignment and addPurchase with blank descriptions produce Bengali defaults`() = runTest {
        val supplierDao = FakeSupplierDao()
        val repo = SupplierRepositoryImpl(supplierDao, FakeCashbookDao(), writeGuard, NoLock)
        val supplierId = repo.addSupplier("t_1", "সাপ্লায়ার", null, "30", null)

        repo.addConsignment("t_1", supplierId, 200.0, "", "u_1")
        repo.addPurchase("t_1", supplierId, 300.0, "", "u_1")

        assertThat(supplierDao.entries).hasSize(2)
        assertThat(supplierDao.entries[0].description).isEqualTo("কনসাইনমেন্ট গ্রহণ")
        assertThat(supplierDao.entries[1].description).isEqualTo("ক্রয় (বাকি)")
    }

    @Test
    fun `addEntry propagates period lock rejection and inserts nothing`() = runTest {
        val supplierDao = FakeSupplierDao()
        val repo = SupplierRepositoryImpl(supplierDao, FakeCashbookDao(), writeGuard, LockedPeriods())
        val supplierId = repo.addSupplier("t_1", "সাপ্লায়ার", null, "30", null)

        assertThrows(PeriodLockGuard.PeriodLockedException::class.java) {
            kotlinx.coroutines.runBlocking {
                repo.addEntry("t_1", supplierId, 100.0, SupplierEntryType.CONSIGNMENT, "goods", null, 1000L, "u_1")
            }
        }
        // The guard threw before insertEntry — supplier_entries must be empty
        assertThat(supplierDao.entries).isEmpty()
    }

    @Test
    fun `addEntry throws LicenseBlockedException when guard is SOFT_LOCKED and inserts nothing`() = runTest {
        val supplierDao = FakeSupplierDao()
        val blockedGuard = LicenseWriteGuard().apply { updateState(LicenseState.SOFT_LOCKED) }
        val repo = SupplierRepositoryImpl(supplierDao, FakeCashbookDao(), blockedGuard, NoLock)
        val supplierId = repo.addSupplier("t_1", "সাপ্লায়ার", null, "30", null)

        assertThrows(LicenseBlockedException::class.java) {
            kotlinx.coroutines.runBlocking {
                repo.addEntry("t_1", supplierId, 100.0, SupplierEntryType.CONSIGNMENT, "goods", null, 1000L, "u_1")
            }
        }
        assertThat(supplierDao.entries).isEmpty()
    }

    @Test
    fun `getSupplierBalance throws IllegalStateException for unknown supplierId`() = runTest {
        val repo = SupplierRepositoryImpl(FakeSupplierDao(), FakeCashbookDao(), writeGuard, NoLock)

        val ex = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                repo.getSupplierBalance("t_1", "nonexistent", System.currentTimeMillis())
            }
        }
        assertThat(ex).hasMessageThat().isEqualTo("সাপ্লায়ার পাওয়া যায়নি")
    }

    @Test
    fun `getSettlementStatement excludes entry dated exactly on endDate`() = runTest {
        val supplierDao = FakeSupplierDao()
        val repo = SupplierRepositoryImpl(supplierDao, FakeCashbookDao(), writeGuard, NoLock)
        val supplierId = repo.addSupplier("t_1", "সাপ্লায়ার", null, "30", null)

        val t0 = 1000L
        repo.addEntry("t_1", supplierId, 100.0, SupplierEntryType.OPENING, "open", null, t0, "u_1")
        repo.addEntry("t_1", supplierId, 50.0, SupplierEntryType.CONSIGNMENT, "consign", null, t0 + 2000L, "u_1")

        // endDate = t0 + 2000 → entry at t0 + 2000 is excluded (filter is date < endDate)
        val statement = repo.getSettlementStatement("t_1", supplierId, "Shop", null, t0 + 2000L)
        assertThat(statement.entries).hasSize(1)
        assertThat(statement.entries[0].description).isEqualTo("open")
    }

    @Test
    fun `getSettlementReminders omits supplier still inside settlement cycle`() = runTest {
        val supplierDao = FakeSupplierDao()
        val repo = SupplierRepositoryImpl(supplierDao, FakeCashbookDao(), writeGuard, NoLock)
        val supplierId = repo.addSupplier("t_1", "সাপ্লায়ার", null, "30", null)

        val t0 = System.currentTimeMillis()
        repo.addEntry("t_1", supplierId, 500.0, SupplierEntryType.OPENING, "open", null, t0, "u_1")

        // 10 days later — within 30-day cycle, should NOT appear in reminders
        val reminders = repo.getSettlementReminders("t_1", t0 + 10L * 24 * 60 * 60 * 1000)
        assertThat(reminders).isEmpty()

        // 35 days later — past cycle, should appear
        val remindersOverdue = repo.getSettlementReminders("t_1", t0 + 35L * 24 * 60 * 60 * 1000)
        assertThat(remindersOverdue).hasSize(1)
    }

    // ── @Ignore'd tests: assert INTENDED behaviour, will fail today ──────

    @Ignore("B1: getSupplierById has no tenantId predicate, so tenant A's supplier is " +
            "reachable from tenant B. A real fix needs a new DAO query with tenantId. " +
            "Unignore when fixed.")
    @Test
    fun `getSupplierBalance with tenant B and tenant A supplierId should fail not return balance`() = runTest {
        val supplierDao = FakeSupplierDao()
        val cashbookDao = FakeCashbookDao()
        val repo = SupplierRepositoryImpl(supplierDao, cashbookDao, writeGuard, NoLock)

        // Supplier belongs to tenant t_A
        val supplierId = repo.addSupplier("t_A", "সাপ্লায়ার A", null, "30", null)
        repo.addEntry("t_A", supplierId, 500.0, SupplierEntryType.OPENING, "open", null, 1000L, "u_1")

        // Calling with tenant t_B should NOT return a balance for t_A's supplier
        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                repo.getSupplierBalance("t_B", supplierId, System.currentTimeMillis())
            }
        }
    }

    @Ignore("B3: idempotencyKey is UUID.randomUUID() at three call sites, so the same " +
            "logical payment posts twice; supplier_entries has no unique index on " +
            "idempotencyKey either, so the DB will not stop it. A full fix needs a " +
            "deterministic key plus a Room unique index, which is a schema change and " +
            "owner-zone. Unignore when fixed.")
    @Test
    fun `duplicate payment with same trxId should produce exactly one ledger row`() = runTest {
        val supplierDao = FakeSupplierDao()
        val cashbookDao = FakeCashbookDao()
        val repo = SupplierRepositoryImpl(supplierDao, cashbookDao, writeGuard, NoLock)
        val supplierId = repo.addSupplier("t_1", "সাপ্লায়ার", null, "30", null)

        repo.addPayment("t_1", supplierId, 100.0, "পেমেন্ট", "trx_dup", "u_1", CashbookAccount.CASH)
        repo.addPayment("t_1", supplierId, 100.0, "পেমেন্ট", "trx_dup", "u_1", CashbookAccount.CASH)

        val paymentEntries = supplierDao.entries.filter { it.type == "PAYMENT" }
        assertThat(paymentEntries).hasSize(1)
    }

    @Ignore("B5: D53 mirror breaks below the 0.01 threshold — the supplier entry is " +
            "written unconditionally but the cashbook mirror is gated by if (amount > 0.01). " +
            "Unignore when fixed.")
    @Test
    fun `addPayment with amount 0_005 should keep ledger and cashbook in agreement`() = runTest {
        val supplierDao = FakeSupplierDao()
        val cashbookDao = FakeCashbookDao()
        val repo = SupplierRepositoryImpl(supplierDao, cashbookDao, writeGuard, NoLock)
        val supplierId = repo.addSupplier("t_1", "সাপ্লায়ার", null, "30", null)

        repo.addPayment("t_1", supplierId, 0.005, "micro", null, "u_1", CashbookAccount.CASH)

        // Intended: both ledger and cashbook should have the entry
        assertThat(supplierDao.entries).hasSize(1)
        assertThat(cashbookDao.entries).hasSize(1)
    }
}
