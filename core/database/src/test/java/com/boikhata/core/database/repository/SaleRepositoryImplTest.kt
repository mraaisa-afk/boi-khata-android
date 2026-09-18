package com.boikhata.core.database.repository

import androidx.room.withTransaction
import com.boikhata.core.database.BoiKhataDatabase
import com.boikhata.core.database.dao.BillDao
import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.database.dao.KhataEntryDao
import com.boikhata.core.database.dao.StockLedgerDao
import com.boikhata.core.database.entity.BillEntity
import com.boikhata.core.database.entity.BillLineEntity
import com.boikhata.core.database.entity.CashbookEntryEntity
import com.boikhata.core.database.entity.KhataEntryEntity
import com.boikhata.core.database.entity.StockLedgerEntity
import com.boikhata.core.domain.accounting.PeriodLockChecker
import com.boikhata.core.domain.accounting.PeriodLockGuard
import com.boikhata.core.domain.enums.KhataEntryType
import com.boikhata.core.domain.enums.PaymentMethod
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.repository.BillLineInput
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * B-005/B-006 regression: the D22 atomic checkout transaction.
 *
 * Owner device evidence (2026-09-18): opening stock মিসির আলী=40, হিমু=30; a cash
 * sale of 3+3 units left the stock tab unchanged and the customer's khata showed
 * «কোনো লেনদেন নেই». These tests dump the actual table contents after createBill:
 *
 *  - stock_ledger MUST receive −3 rows per line (the decrement is recorded; the
 *    display bug was the read side — see BookRepositoryImplTest).
 *  - a FULLY-PAID cash sale writes NO khata entry (due == 0 by design) — the
 *    customer history fix is the read-side getBillsByCustomer + khata UI.
 *  - a PARTIAL payment writes the CREDIT khata entry for the due remainder.
 *  - cashbook receives the INCOME entry for the paid amount.
 */
class SaleRepositoryImplTest {

    private class FakeBillDao : BillDao {
        val bills = mutableListOf<BillEntity>()
        val lines = mutableListOf<BillLineEntity>()
        override suspend fun insert(bill: BillEntity) { bills.add(bill) }
        override suspend fun insertLines(lines: List<BillLineEntity>) { this.lines.addAll(lines) }
        override suspend fun getByDateRange(tenantId: String, startOfDay: Long, endOfDay: Long) =
            bills.filter { it.tenantId == tenantId && it.billDate >= startOfDay && it.billDate < endOfDay }
        override suspend fun getByTenant(tenantId: String) = bills.filter { it.tenantId == tenantId }
        override suspend fun getByCustomer(tenantId: String, customerId: String) =
            bills.filter { it.tenantId == tenantId && it.customerId == customerId }
        override suspend fun getById(billId: String) = bills.firstOrNull { it.id == billId }
        override suspend fun getLinesByBill(billId: String) = lines.filter { it.billId == billId }
        override suspend fun getMaxBillNumber(tenantId: String, pattern: String): String? = null
        override suspend fun countForTenant(tenantId: String) = bills.count { it.tenantId == tenantId }
        override suspend fun updateKhataEntryId(billId: String, khataEntryId: String) {
            bills.replaceAll { if (it.id == billId) it.copy(khataEntryId = khataEntryId) else it }
        }
    }

    private class FakeStockLedgerDao : StockLedgerDao {
        val rows = mutableListOf<StockLedgerEntity>()
        override suspend fun insert(entry: StockLedgerEntity) { rows.add(entry) }
        override suspend fun getByBook(tenantId: String, bookId: String) =
            rows.filter { it.tenantId == tenantId && it.bookId == bookId }
        override suspend fun getStockQuantityForBook(bookId: String) =
            rows.filter { it.bookId == bookId }.sumOf { it.changeQuantity }
        override suspend fun getDeltasByTenant(tenantId: String) = rows
            .filter { it.tenantId == tenantId }
            .groupBy { it.bookId }
            .map { (bookId, entries) ->
                com.boikhata.core.database.entity.BookStockDelta(bookId, entries.sumOf { it.changeQuantity })
            }
    }

    private class FakeKhataEntryDao : KhataEntryDao {
        val rows = mutableListOf<KhataEntryEntity>()
        override suspend fun insert(entry: KhataEntryEntity) { rows.add(entry) }
        override suspend fun getByCustomer(tenantId: String, customerId: String) =
            rows.filter { it.tenantId == tenantId && it.customerId == customerId }
        override suspend fun getByTenant(tenantId: String) = rows.filter { it.tenantId == tenantId }
        override suspend fun getPaymentSumByDateRange(tenantId: String, start: Long, end: Long) =
            rows.filter { it.tenantId == tenantId && it.type == "PAYMENT" && it.amount > 0 && it.date >= start && it.date <= end }
                .sumOf { it.amount }
    }

    private class FakeCashbookDao : CashbookDao {
        val rows = mutableListOf<CashbookEntryEntity>()
        override suspend fun insert(entry: CashbookEntryEntity) { rows.add(entry) }
        override suspend fun getByTenant(tenantId: String) = rows.filter { it.tenantId == tenantId }
        override suspend fun getByAccount(tenantId: String, account: String) =
            rows.filter { it.tenantId == tenantId && it.account == account }
        override suspend fun getByDateRange(tenantId: String, start: Long, end: Long) =
            rows.filter { it.tenantId == tenantId && it.date >= start && it.date < end }
    }

    private object NoLock : PeriodLockChecker {
        override suspend fun getLockedPeriods(tenantId: String): Set<PeriodLockGuard.LockedPeriod> = emptySet()
        override suspend fun assertNotLocked(tenantId: String, date: Long) { /* no-op */ }
    }

    private lateinit var billDao: FakeBillDao
    private lateinit var stockLedgerDao: FakeStockLedgerDao
    private lateinit var khataEntryDao: FakeKhataEntryDao
    private lateinit var cashbookDao: FakeCashbookDao
    private lateinit var repo: SaleRepositoryImpl

    private val lines = listOf(
        BillLineInput(bookId = "misir-ali", bookTitleBn = "মিসির আলী", quantity = 3, unitPrice = 350.0, category = com.boikhata.core.domain.enums.BookCategory.GENERAL),
        BillLineInput(bookId = "himu", bookTitleBn = "হিমু", quantity = 3, unitPrice = 300.0, category = com.boikhata.core.domain.enums.BookCategory.GENERAL),
    ) // subtotal = 1950, books = 0% VAT (D19)

    @Before
    fun setup() {
        billDao = FakeBillDao()
        stockLedgerDao = FakeStockLedgerDao()
        khataEntryDao = FakeKhataEntryDao()
        cashbookDao = FakeCashbookDao()
        val db = mockk<BoiKhataDatabase>(relaxed = true)
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Unit>()) } coAnswers {
            val block = args[1] as suspend () -> Unit
            block.invoke()
        }
        repo = SaleRepositoryImpl(
            db = db,
            billDao = billDao,
            stockLedgerDao = stockLedgerDao,
            khataEntryDao = khataEntryDao,
            cashbookDao = cashbookDao,
            writeGuard = LicenseWriteGuard(), // GRACE → writes allowed
            periodLockChecker = NoLock,
        )
    }

    @After
    fun teardown() { unmockkAll() }

    @Test
    fun `full-cash sale appends stock ledger rows and cashbook income but no khata entry`() = runTest {
        val billId = repo.createBill(
            tenantId = "t_1", customerId = "karim", customerNameBn = "করিম", customerPhone = null,
            userId = "u_1", lines = lines, discountAmount = 0.0, discountType = "PERCENTAGE",
            paymentMethod = PaymentMethod.CASH, paidAmount = 1950.0,
        )

        // ── stock decrement evidence (the actual before/after data dump) ──
        val misir = stockLedgerDao.rows.filter { it.bookId == "misir-ali" }
        val himu = stockLedgerDao.rows.filter { it.bookId == "himu" }
        assertThat(misir.single().changeQuantity).isEqualTo(-3)
        assertThat(misir.single().reason).isEqualTo("SALE")
        assertThat(misir.single().referenceId).isEqualTo(billId)
        assertThat(himu.single().changeQuantity).isEqualTo(-3)
        // opening stock 40 → 40 + (−3) = 37; opening 30 → 30 + (−3) = 27
        assertThat(stockLedgerDao.getStockQuantityForBook("misir-ali")).isEqualTo(-3)
        assertThat(stockLedgerDao.getDeltasByTenant("t_1").first { it.bookId == "misir-ali" }.delta).isEqualTo(-3)

        // ── khata: full cash payment → due 0 → NO khata entry (B-006 root cause) ──
        assertThat(khataEntryDao.rows).isEmpty()
        val bill = billDao.bills.single()
        assertThat(bill.dueAmount).isEqualTo(0.0)
        assertThat(bill.status).isEqualTo("COMPLETED")
        assertThat(bill.customerId).isEqualTo("karim") // bill IS customer-linked (queried by B-006 fix)

        // ── cashbook: money moved → INCOME 1950 ──
        val income = cashbookDao.rows.single()
        assertThat(income.type).isEqualTo("INCOME")
        assertThat(income.amount).isEqualTo(1950.0)
        assertThat(income.account).isEqualTo("CASH")
    }

    @Test
    fun `partial payment posts the due remainder as a CREDIT khata entry`() = runTest {
        repo.createBill(
            tenantId = "t_1", customerId = "karim", customerNameBn = "করিম", customerPhone = null,
            userId = "u_1", lines = lines, discountAmount = 0.0, discountType = "PERCENTAGE",
            paymentMethod = PaymentMethod.CASH, paidAmount = 500.0,
        )

        // paid 500 of 1950 → due 1450 → CREDIT entry + PARTIAL status
        val entry = khataEntryDao.rows.single()
        assertThat(entry.type).isEqualTo(KhataEntryType.CREDIT.name)
        assertThat(entry.amount).isEqualTo(1450.0)
        assertThat(entry.customerId).isEqualTo("karim")
        val bill = billDao.bills.single()
        assertThat(bill.paidAmount).isEqualTo(500.0)
        assertThat(bill.dueAmount).isEqualTo(1450.0)
        assertThat(bill.status).isEqualTo("PARTIAL")
        assertThat(bill.khataEntryId).isEqualTo(entry.id)
        // cashbook: only the money that actually moved
        assertThat(cashbookDao.rows.single().amount).isEqualTo(500.0)
    }

    @Test
    fun `getBillsByCustomer returns the customer-linked bill for the khata history`() = runTest {
        repo.createBill(
            tenantId = "t_1", customerId = "karim", customerNameBn = "করিম", customerPhone = null,
            userId = "u_1", lines = lines, discountAmount = 0.0, discountType = "PERCENTAGE",
            paymentMethod = PaymentMethod.CASH, paidAmount = 1950.0,
        )
        val history = repo.getBillsByCustomer("t_1", "karim")
        assertThat(history).hasSize(1)
        assertThat(history.single().customerNameBn).isEqualTo("করিম")
        assertThat(history.single().totalAmount).isEqualTo(1950.0)
        assertThat(repo.getBillsByCustomer("t_1", "someone-else")).isEmpty()
    }
}
