package com.boikhata.core.database.repository

import androidx.room.withTransaction
import com.boikhata.core.database.BoiKhataDatabase
import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.database.dao.KhataCustomerDao
import com.boikhata.core.database.dao.KhataEntryDao
import com.boikhata.core.database.dao.KhataInstallmentDao
import com.boikhata.core.database.entity.CashbookEntryEntity
import com.boikhata.core.database.entity.KhataCustomerEntity
import com.boikhata.core.database.entity.KhataEntryEntity
import com.boikhata.core.database.entity.KhataInstallmentEntity
import com.boikhata.core.domain.accounting.PeriodLockChecker
import com.boikhata.core.domain.accounting.PeriodLockGuard
import com.boikhata.core.domain.aging.AgingCalculator
import com.boikhata.core.domain.aging.KhataEntry
import com.boikhata.core.domain.enums.KhataEntryType
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * U-002 regression: «পূর্বের বাকি» on add-customer. The OPENING infrastructure
 * (KhataEntryType.OPENING, AgingCalculator credit treatment, D34 no-cashbook rule)
 * existed since the schema landed but no write path ever used it for khata
 * customers (the supplier module had addOpeningBalance; khata did not).
 * Drives the REAL repository with in-memory fakes — same template as
 * SaleRepositoryImplTest / SupplierRepositoryImplTest.
 */
class KhataRepositoryImplTest {

    private class FakeKhataCustomerDao : KhataCustomerDao {
        val customers = mutableListOf<KhataCustomerEntity>()
        override suspend fun insert(customer: KhataCustomerEntity) { customers.add(customer) }
        override suspend fun update(customer: KhataCustomerEntity) { /* unused */ }
        override suspend fun getActiveByTenant(tenantId: String) =
            customers.filter { it.tenantId == tenantId && it.isActive }
        override fun getActiveByTenantFlow(tenantId: String): Flow<List<KhataCustomerEntity>> =
            flowOf(customers.filter { it.tenantId == tenantId && it.isActive })
        override suspend fun getById(id: String) = customers.firstOrNull { it.id == id }
        override suspend fun search(tenantId: String, normalizedQuery: String) =
            getActiveByTenant(tenantId).filter { it.nameBn.contains(normalizedQuery) }
    }

    private class FakeKhataEntryDao : KhataEntryDao {
        val entries = mutableListOf<KhataEntryEntity>()
        override suspend fun insert(entry: KhataEntryEntity) { entries.add(entry) }
        override suspend fun getByCustomer(tenantId: String, customerId: String) =
            entries.filter { it.tenantId == tenantId && it.customerId == customerId }
        override suspend fun getByTenant(tenantId: String) =
            entries.filter { it.tenantId == tenantId }
        override suspend fun getPaymentSumByDateRange(tenantId: String, start: Long, end: Long) =
            entries.filter {
                it.tenantId == tenantId && it.type == "PAYMENT" && it.amount > 0 &&
                    it.date in start..end
            }.sumOf { it.amount }
    }

    private class FakeKhataInstallmentDao : KhataInstallmentDao {
        val installments = mutableListOf<KhataInstallmentEntity>()
        override suspend fun insert(installment: KhataInstallmentEntity) { installments.add(installment) }
        override suspend fun insertAll(installments: List<KhataInstallmentEntity>) { this.installments.addAll(installments) }
        override suspend fun getByCustomer(tenantId: String, customerId: String) = installments.filter { it.tenantId == tenantId && it.customerId == customerId }
        override suspend fun getByEntry(tenantId: String, entryId: String) = installments.filter { it.khataEntryId == entryId }
        override suspend fun markPaid(id: String) { /* unused */ }
        override suspend fun countUnpaidByCustomer(tenantId: String, customerId: String) =
            installments.count { it.tenantId == tenantId && it.customerId == customerId && !it.isPaid }
    }

    private class FakeCashbookDao : CashbookDao {
        val entries = mutableListOf<CashbookEntryEntity>()
        override suspend fun insert(entry: CashbookEntryEntity) { entries.add(entry) }
        override suspend fun getByTenant(tenantId: String) = entries.filter { it.tenantId == tenantId }
        override suspend fun getByAccount(tenantId: String, account: String) = entries.filter { it.tenantId == tenantId && it.account == account }
        override suspend fun getByDateRange(tenantId: String, start: Long, end: Long) = entries.filter { it.tenantId == tenantId && it.date in start..end }
    }

    private object NoLock : PeriodLockChecker {
        override suspend fun getLockedPeriods(tenantId: String): Set<PeriodLockGuard.LockedPeriod> = emptySet()
        override suspend fun assertNotLocked(tenantId: String, date: Long) { /* no-op */ }
    }

    private val writeGuard = LicenseWriteGuard() // default GRACE → writes allowed

    private lateinit var customerDao: FakeKhataCustomerDao
    private lateinit var entryDao: FakeKhataEntryDao
    private lateinit var cashbookDao: FakeCashbookDao
    private lateinit var repo: KhataRepositoryImpl

    @Before
    fun setup() {
        customerDao = FakeKhataCustomerDao()
        entryDao = FakeKhataEntryDao()
        cashbookDao = FakeCashbookDao()
        val db = mockk<BoiKhataDatabase>(relaxed = true)
        mockkStatic("androidx.room.RoomDatabaseKt")
        // withTransaction is generic <R>; the U-002 block RETURNS the customerId
        // (String), so the stub must preserve the block's value — a () -> Unit
        // coercion makes the answer return kotlin.Unit and the real call crashes
        // with a ClassCastException (SaleRepositoryImplTest's block returns Unit,
        // which is why its () -> Unit stub works there).
        coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            val block = args[1] as suspend () -> Any?
            block.invoke()
        }
        repo = KhataRepositoryImpl(
            db = db,
            khataCustomerDao = customerDao,
            khataEntryDao = entryDao,
            khataInstallmentDao = FakeKhataInstallmentDao(),
            cashbookDao = cashbookDao,
            writeGuard = writeGuard,
            periodLockChecker = NoLock,
        )
    }

    @Test
    fun `addCustomerWithOpeningDue writes customer and OPENING entry atomically, no cashbook mirror`() = runTest {
        val customerId = repo.addCustomerWithOpeningDue(
            tenantId = "t_1",
            nameBn = "করিম মামা",
            phone = null,
            address = "বাজার রোড",
            creditLimit = 5000.0,
            openingDue = 800.0,
            collectedByUserId = "u_1",
        )

        // Customer row
        assertThat(customerDao.customers).hasSize(1)
        assertThat(customerDao.customers[0].id).isEqualTo(customerId)
        assertThat(customerDao.customers[0].tenantId).isEqualTo("t_1")

        // OPENING entry: the previous due from the paper khata
        assertThat(entryDao.entries).hasSize(1)
        val entry = entryDao.entries[0]
        assertThat(entry.type).isEqualTo(KhataEntryType.OPENING.name)
        assertThat(entry.amount).isEqualTo(800.0)
        assertThat(entry.description).isEqualTo("পূর্বের বাকি")
        assertThat(entry.tenantId).isEqualTo("t_1")
        assertThat(entry.customerId).isEqualTo(customerId)

        // D34: OPENING is a receivable, NOT a cash flow — no cashbook mirror
        assertThat(cashbookDao.entries).isEmpty()

        // Aging: OPENING must count as due (the whole point of the feature)
        val aging = AgingCalculator.calculate(
            entryDao.getByCustomer("t_1", customerId).map {
                KhataEntry(id = it.id, type = KhataEntryType.valueOf(it.type), amount = it.amount, date = it.date, description = it.description)
            },
            System.currentTimeMillis(),
        )
        assertThat(aging.totalDue).isEqualTo(800.0)
    }

    @Test
    fun `addCustomerWithOpeningDue with zero due writes no khata entry`() = runTest {
        val customerId = repo.addCustomerWithOpeningDue(
            tenantId = "t_1", nameBn = "রহিম", phone = null, address = null,
            creditLimit = 0.0, openingDue = 0.0, collectedByUserId = "u_1",
        )
        assertThat(customerDao.customers).hasSize(1)
        assertThat(entryDao.entries).isEmpty()
        assertThat(customerId).isNotEmpty()
    }
}
