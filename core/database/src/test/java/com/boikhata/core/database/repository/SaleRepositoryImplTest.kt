package com.boikhata.core.database.repository

import androidx.room.withTransaction
import com.boikhata.core.database.BoiKhataDatabase
import com.boikhata.core.database.dao.BillDao
import com.boikhata.core.database.dao.BillPaymentLineDao
import com.boikhata.core.database.dao.BookDao
import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.database.dao.KhataEntryDao
import com.boikhata.core.database.dao.StockLedgerDao
import com.boikhata.core.database.entity.BillEntity
import com.boikhata.core.database.entity.BillLineEntity
import com.boikhata.core.database.entity.BillPaymentLineEntity
import com.boikhata.core.database.entity.BookEntity
import com.boikhata.core.database.entity.CashbookEntryEntity
import com.boikhata.core.database.entity.KhataEntryEntity
import com.boikhata.core.database.entity.StockLedgerEntity
import com.boikhata.core.domain.accounting.PeriodLockChecker
import com.boikhata.core.domain.accounting.PeriodLockGuard
import com.boikhata.core.domain.enums.KhataEntryType
import com.boikhata.core.domain.enums.MfsProvider
import com.boikhata.core.domain.enums.PaymentLineCategory
import com.boikhata.core.domain.enums.PaymentMethod
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.repository.BillLineInput
import com.boikhata.core.domain.repository.PaymentLineSpec
import com.boikhata.core.domain.sale.InsufficientStockException
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
        val bookPrices = mutableMapOf<String, Double>()
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
        override suspend fun getCogsByDateRange(tenantId: String, startOfDay: Long, endOfDay: Long): Double {
            val billIds = bills
                .filter { it.tenantId == tenantId && it.billDate >= startOfDay && it.billDate < endOfDay }
                .map { it.id }
            return lines.filter { it.billId in billIds }
                .sumOf { line -> line.quantity * (bookPrices[line.bookId] ?: 0.0) }
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
        override suspend fun countByReferenceBillId(referenceBillId: String) =
            rows.count { it.referenceBillId == referenceBillId }
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

    private class FakeBillPaymentLineDao : BillPaymentLineDao {
        val rows = mutableListOf<BillPaymentLineEntity>()
        override suspend fun insertAll(lines: List<BillPaymentLineEntity>) { rows.addAll(lines) }
        override suspend fun getByBill(billId: String) = rows.filter { it.billId == billId }
        override suspend fun countForBill(billId: String) = rows.count { it.billId == billId }
        override suspend fun getByBillDateRange(tenantId: String, startOfDay: Long, endOfDay: Long) = emptyList<BillPaymentLineEntity>()
    }

    private class FakeBookDao : BookDao {
        val books = mutableListOf<BookEntity>()
        override suspend fun insert(book: BookEntity) { books.add(book) }
        override suspend fun update(book: BookEntity) { books.replaceAll { if (it.id == book.id) book else it } }
        override suspend fun getActiveByTenant(tenantId: String) = books.filter { it.tenantId == tenantId && it.isActive }
        override suspend fun getAllByTenant(tenantId: String) = books.filter { it.tenantId == tenantId }
        override suspend fun countForTenant(tenantId: String) = books.count { it.tenantId == tenantId }
        override suspend fun getById(id: String) = books.firstOrNull { it.id == id }
        override suspend fun search(tenantId: String, normalizedQuery: String) = emptyList<BookEntity>()
    }

    private object NoLock : PeriodLockChecker {
        override suspend fun getLockedPeriods(tenantId: String): Set<PeriodLockGuard.LockedPeriod> = emptySet()
        override suspend fun assertNotLocked(tenantId: String, date: Long) { /* no-op */ }
    }

    private lateinit var billDao: FakeBillDao
    private lateinit var billPaymentLineDao: FakeBillPaymentLineDao
    private lateinit var stockLedgerDao: FakeStockLedgerDao
    private lateinit var khataEntryDao: FakeKhataEntryDao
    private lateinit var cashbookDao: FakeCashbookDao
    private lateinit var bookDao: FakeBookDao
    private lateinit var repo: SaleRepositoryImpl

    private val lines = listOf(
        BillLineInput(bookId = "misir-ali", bookTitleBn = "মিসির আলী", quantity = 3, unitPrice = 350.0, category = com.boikhata.core.domain.enums.BookCategory.GENERAL),
        BillLineInput(bookId = "himu", bookTitleBn = "হিমু", quantity = 3, unitPrice = 300.0, category = com.boikhata.core.domain.enums.BookCategory.GENERAL),
    ) // subtotal = 1950, books = 0% VAT (D19)

    /** D93 overpayment tests: a clean ৳1000 cart (1 × ৳1000 book, no VAT). */
    private val cart1000 = listOf(
        BillLineInput(bookId = "boi-x", bookTitleBn = "বই", quantity = 1, unitPrice = 1000.0, category = com.boikhata.core.domain.enums.BookCategory.GENERAL),
    )

    @Before
    fun setup() {
        billDao = FakeBillDao()
        billPaymentLineDao = FakeBillPaymentLineDao()
        stockLedgerDao = FakeStockLedgerDao()
        khataEntryDao = FakeKhataEntryDao()
        cashbookDao = FakeCashbookDao()
        bookDao = FakeBookDao()
        // P14: every pre-existing test book gets a generous opening stock (100) so the
        // documented scenarios (মিসির আলী 40→37 etc.) keep exercising the WRITE path,
        // not the new stock gate; the oversell tests below seed their own tight stock.
        listOf(
            Triple("misir-ali", "মিসির আলী", 100),
            Triple("himu", "হিমু", 100),
            Triple("boi-x", "বই", 100),
            Triple("b1", "ক", 100),
            Triple("b2", "খ", 100),
        ).forEach { (id, title, stock) ->
            bookDao.books.add(testBookEntity(id, title, stock))
        }
        val db = mockk<BoiKhataDatabase>(relaxed = true)
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Unit>()) } coAnswers {
            val block = args[1] as suspend () -> Unit
            block.invoke()
        }
        repo = SaleRepositoryImpl(
            db = db,
            billDao = billDao,
            billPaymentLineDao = billPaymentLineDao,
            bookDao = bookDao,
            stockLedgerDao = stockLedgerDao,
            khataEntryDao = khataEntryDao,
            cashbookDao = cashbookDao,
            writeGuard = LicenseWriteGuard(), // GRACE → writes allowed
            periodLockChecker = NoLock,
        )
    }

    /** P14: minimal BookEntity factory for the stock-gate tests. */
    private fun testBookEntity(id: String, titleBn: String, initialStock: Int) = BookEntity(
        id = id,
        tenantId = "t_1",
        isbn = null,
        titleBn = titleBn,
        titleEn = null,
        author = "",
        publisher = "",
        classLevel = "",
        subject = "",
        editionYear = 2026,
        category = "GENERAL",
        condition = "NEW",
        purchasePrice = 50.0,
        sellingPrice = 100.0,
        initialStock = initialStock,
        lowStockThreshold = 3,
        isActive = true,
        titleBnNormalized = titleBn,
        createdAt = 0L,
        updatedAt = 0L,
    )

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

    // ── P12/D92: multi-line payment model ────────────────────────────────────

    @Test
    fun `two paid lines - 600 cash plus 400 mobile bKash - close a 1000 bill with zero due`() = runTest {
        // The owner's required example: total ৳1000 → ৳600 নগদ + ৳400 বিকাশ.
        val thousandOnly = listOf(
            BillLineInput(bookId = "misir-ali", bookTitleBn = "মিসির আলী", quantity = 2, unitPrice = 500.0, category = com.boikhata.core.domain.enums.BookCategory.GENERAL),
        ) // subtotal 1000, 0% VAT
        val billId = repo.createBillWithPaymentLines(
            tenantId = "t_1", customerId = null, customerNameBn = "হাটি ক্রেতা", customerPhone = null,
            userId = "u_1", lines = thousandOnly, discountAmount = 0.0, discountType = "FIXED",
            paidLines = listOf(
                PaymentLineSpec(PaymentLineCategory.CASH, null, 600.0),
                PaymentLineSpec(PaymentLineCategory.MOBILE, MfsProvider.BKASH, 400.0),
            ),
        )

        val bill = billDao.bills.single()
        assertThat(bill.paidAmount).isEqualTo(1000.0)
        assertThat(bill.dueAmount).isEqualTo(0.0)
        assertThat(bill.status).isEqualTo("COMPLETED")
        assertThat(khataEntryDao.rows).isEmpty() // nothing on the khata

        // BOTH payment lines recorded, each with its own cashbook mirror
        val pl = billPaymentLineDao.getByBill(billId)
        assertThat(pl).hasSize(2)
        val cashLine = pl.first { it.method == PaymentLineCategory.CASH.name }
        val mobileLine = pl.first { it.method == PaymentLineCategory.MOBILE.name }
        assertThat(cashLine.amount).isEqualTo(600.0)
        assertThat(mobileLine.amount).isEqualTo(400.0)
        assertThat(mobileLine.provider).isEqualTo(MfsProvider.BKASH.name)

        // MOBILE money goes to the MOBILE bucket — never folded into BKASH
        val cashIncome = cashbookDao.rows.first { it.account == "CASH" }
        val mobileIncome = cashbookDao.rows.first { it.account == "MOBILE" }
        assertThat(cashIncome.amount).isEqualTo(600.0)
        assertThat(mobileIncome.amount).isEqualTo(400.0)
        // link integrity: payment line points at its own mirror row
        assertThat(mobileLine.cashbookEntryId).isEqualTo(mobileIncome.id)
        assertThat(cashLine.cashbookEntryId).isEqualTo(cashIncome.id)
    }

    @Test
    fun `three-way - 600 cash plus 300 mobile plus 100 due - posts the remainder to khata`() = runTest {
        val billId = repo.createBillWithPaymentLines(
            tenantId = "t_1", customerId = "karim", customerNameBn = "করিম", customerPhone = null,
            userId = "u_1", lines = lines, discountAmount = 0.0, discountType = "FIXED",
            // D92 example scaled: paid 900 of 1950 → 1050 বাকি
            paidLines = listOf(
                PaymentLineSpec(PaymentLineCategory.CASH, null, 600.0),
                PaymentLineSpec(PaymentLineCategory.MOBILE, MfsProvider.NAGAD, 300.0),
            ),
        )

        val bill = billDao.bills.single()
        assertThat(bill.paidAmount).isEqualTo(900.0)
        assertThat(bill.dueAmount).isEqualTo(1050.0)
        assertThat(bill.status).isEqualTo("PARTIAL")

        // বাকি → CREDIT khata entry for the customer
        val entry = khataEntryDao.rows.single()
        assertThat(entry.type).isEqualTo(KhataEntryType.CREDIT.name)
        assertThat(entry.amount).isEqualTo(1050.0)
        assertThat(entry.customerId).isEqualTo("karim")

        // 3 rows: two paid lines + one explicit DUE line (no cashbook mirror on DUE)
        val pl = billPaymentLineDao.getByBill(billId)
        assertThat(pl).hasSize(3)
        val dueLine = pl.first { it.method == PaymentLineCategory.DUE.name }
        assertThat(dueLine.amount).isEqualTo(1050.0)
        assertThat(dueLine.cashbookEntryId).isNull()
        assertThat(pl.count { it.method == PaymentLineCategory.DUE.name }).isEqualTo(1)
        assertThat(cashbookDao.rows).hasSize(2) // only the money that moved
    }

    @Test
    fun `validation - walk-in overpay, provider-less mobile, zero amount, due-without-customer all fail fast`() = runTest {
        val paid600 = listOf(PaymentLineSpec(PaymentLineCategory.CASH, null, 600.0))

        // D93: overpay WITHOUT a customer (হাটি ক্রেতা) is rejected with the walk-in message —
        // there is no khata to absorb the excess
        try {
            repo.createBillWithPaymentLines(
                tenantId = "t_1", customerId = null, customerNameBn = "হাটি ক্রেতা", customerPhone = null,
                userId = "u_1", lines = cart1000, discountAmount = 0.0, discountType = "FIXED",
                paidLines = listOf(PaymentLineSpec(PaymentLineCategory.CASH, null, 1500.0)),
            )
            org.junit.Assert.fail("walk-in overpay must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertThat(expected.message!!).isEqualTo("হাটি ক্রেতার জন্য অতিরিক্ত জমা রাখা সম্ভব নয়। অতিরিক্ত ফেরত দিন।")
        }

        // MOBILE line without a provider is rejected
        try {
            repo.createBillWithPaymentLines(
                tenantId = "t_1", customerId = null, customerNameBn = "হাটি ক্রেতা", customerPhone = null,
                userId = "u_1", lines = lines, discountAmount = 0.0, discountType = "FIXED",
                paidLines = listOf(PaymentLineSpec(PaymentLineCategory.MOBILE, null, 500.0)),
            )
            org.junit.Assert.fail("provider-less MOBILE line must be rejected")
        } catch (expected: IllegalArgumentException) { /* প্রোভাইডার নির্বাচন করুন */ }

        // zero/negative amount line is rejected
        try {
            repo.createBillWithPaymentLines(
                tenantId = "t_1", customerId = null, customerNameBn = "হাটি ক্রেতা", customerPhone = null,
                userId = "u_1", lines = lines, discountAmount = 0.0, discountType = "FIXED",
                paidLines = listOf(PaymentLineSpec(PaymentLineCategory.CASH, null, 0.0)),
            )
            org.junit.Assert.fail("zero-amount line must be rejected")
        } catch (expected: IllegalArgumentException) { /* পরিমাণ ০-এর বেশি হতে হবে */ }

        // due > 0 without a customer is rejected (fail-fast, no orphan due)
        try {
            repo.createBillWithPaymentLines(
                tenantId = "t_1", customerId = null, customerNameBn = "হাটি ক্রেতা", customerPhone = null,
                userId = "u_1", lines = lines, discountAmount = 0.0, discountType = "FIXED",
                paidLines = paid600,
            )
            org.junit.Assert.fail("due without customer must be rejected")
        } catch (expected: IllegalArgumentException) { /* বাকি থাকলে ক্রেতা নির্বাচন করুন */ }

        // nothing was written by any failed attempt (D22 atomicity)
        assertThat(billDao.bills).isEmpty()
        assertThat(billPaymentLineDao.rows).isEmpty()
        assertThat(cashbookDao.rows).isEmpty()
        assertThat(stockLedgerDao.rows).isEmpty()
        assertThat(khataEntryDao.rows).isEmpty()
    }

    @Test
    fun `overpayment from a named customer posts the excess to their khata and completes the bill`() = runTest {
        // D93 owner ruling: ইলিয়াস owes ৳1200 from before; he buys a ৳1000 book and
        // hands over ৳1500 — ৳1000 settles the bill, ৳500 is a জমা toward his khata.
        val billId = repo.createBillWithPaymentLines(
            tenantId = "t_1", customerId = "elias", customerNameBn = "ইলিয়াস", customerPhone = null,
            userId = "u_1", lines = cart1000, discountAmount = 0.0, discountType = "FIXED",
            paidLines = listOf(PaymentLineSpec(PaymentLineCategory.CASH, null, 1500.0)),
        )

        // bill: fully paid, no due — the overpay never touches dueAmount
        val bill = billDao.bills.single()
        assertThat(bill.totalAmount).isEqualTo(1000.0)
        assertThat(bill.paidAmount).isEqualTo(1000.0)
        assertThat(bill.dueAmount).isEqualTo(0.0)
        assertThat(bill.status).isEqualTo("COMPLETED")
        assertThat(bill.customerId).isEqualTo("elias")

        // khata: ONE PAYMENT (জমা) entry for the ৳500 excess — SAME transaction
        val entry = khataEntryDao.rows.single()
        assertThat(entry.type).isEqualTo(KhataEntryType.PAYMENT.name)
        assertThat(entry.amount).isEqualTo(500.0)
        assertThat(entry.customerId).isEqualTo("elias")
        assertThat(entry.referenceBillId).isEqualTo(billId)

        // payment lines record the ACTUAL money received; no DUE row (due = 0)
        val pl = billPaymentLineDao.getByBill(billId)
        assertThat(pl).hasSize(1)
        assertThat(pl.single().method).isEqualTo(PaymentLineCategory.CASH.name)
        assertThat(pl.single().amount).isEqualTo(1500.0)

        // cashbook mirrors the physical money (৳1500) — no separate advance INCOME
        val income = cashbookDao.rows.single()
        assertThat(income.type).isEqualTo("INCOME")
        assertThat(income.amount).isEqualTo(1500.0)
        assertThat(income.account).isEqualTo("CASH")
    }

    @Test
    fun `multi-line overpayment posts the whole excess to khata - 600 cash plus 900 bKash on a 1000 bill`() = runTest {
        val billId = repo.createBillWithPaymentLines(
            tenantId = "t_1", customerId = "elias", customerNameBn = "ইলিয়াস", customerPhone = null,
            userId = "u_1", lines = cart1000, discountAmount = 0.0, discountType = "FIXED",
            paidLines = listOf(
                PaymentLineSpec(PaymentLineCategory.CASH, null, 600.0),
                PaymentLineSpec(PaymentLineCategory.MOBILE, MfsProvider.BKASH, 900.0),
            ),
        )

        val bill = billDao.bills.single()
        assertThat(bill.paidAmount).isEqualTo(1000.0)
        assertThat(bill.dueAmount).isEqualTo(0.0)
        assertThat(bill.status).isEqualTo("COMPLETED")

        val entry = khataEntryDao.rows.single()
        assertThat(entry.type).isEqualTo(KhataEntryType.PAYMENT.name)
        assertThat(entry.amount).isEqualTo(500.0) // 1500 received − 1000 bill
        assertThat(entry.referenceBillId).isEqualTo(billId)

        val pl = billPaymentLineDao.getByBill(billId)
        assertThat(pl).hasSize(2)
        assertThat(pl.none { it.method == PaymentLineCategory.DUE.name }).isTrue()
        assertThat(cashbookDao.rows.map { it.amount }.sorted()).containsExactly(600.0, 900.0).inOrder()
    }

    @Test
    fun `cogs query sums quantity times purchase price across cash and credit bills`() = runTest {
        // D94: COGS must count credit sales too — a CREDIT bill is still real revenue
        // with real acquisition cost.
        billDao.bookPrices["b1"] = 100.0
        billDao.bookPrices["b2"] = 50.0
        val cashCart = listOf(
            BillLineInput(bookId = "b1", bookTitleBn = "ক", quantity = 2, unitPrice = 250.0, category = com.boikhata.core.domain.enums.BookCategory.GENERAL),
        )
        val creditCart = listOf(
            BillLineInput(bookId = "b2", bookTitleBn = "খ", quantity = 4, unitPrice = 200.0, category = com.boikhata.core.domain.enums.BookCategory.GENERAL),
        )
        repo.createBillWithPaymentLines(
            tenantId = "t_1", customerId = null, customerNameBn = "হাটি ক্রেতা", customerPhone = null,
            userId = "u_1", lines = cashCart, discountAmount = 0.0, discountType = "FIXED",
            paidLines = listOf(PaymentLineSpec(PaymentLineCategory.CASH, null, 500.0)),
        )
        repo.createBillWithPaymentLines(
            tenantId = "t_1", customerId = "elias", customerNameBn = "ইলিয়াস", customerPhone = null,
            userId = "u_1", lines = creditCart, discountAmount = 0.0, discountType = "FIXED",
            paidLines = emptyList(), // pure বাকি sale — ৳800 on khata
        )

        val cogs = repo.getCogsByDateRange("t_1", 0L, Long.MAX_VALUE)
        assertThat(cogs).isEqualTo(2 * 100.0 + 4 * 50.0) // 400 — the CREDIT bill's cost is included

        // revenue-side proof at the repository boundary: the credit bill is in the day's bills
        val today = repo.getBillsByDate("t_1", 0L, Long.MAX_VALUE)
        assertThat(today).hasSize(2)
        assertThat(today.sumOf { it.totalAmount }).isEqualTo(1300.0)
        val creditBill = today.single { it.dueAmount > 0.0 }
        assertThat(creditBill.totalAmount).isEqualTo(800.0)
    }

    @Test
    fun `legacy createBill maps to equivalent payment lines - NAGAD no longer folds into BKASH bucket`() = runTest {
        val billId = repo.createBill(
            tenantId = "t_1", customerId = "karim", customerNameBn = "করিম", customerPhone = null,
            userId = "u_1", lines = lines, discountAmount = 0.0, discountType = "PERCENTAGE",
            paymentMethod = PaymentMethod.NAGAD, paidAmount = 500.0,
        )
        // legacy NAGAD payment: MOBILE line + NAGAD provider + MOBILE cashbook bucket
        // (+ the DUE line for the 1450 remainder — P12 appends it explicitly)
        val pl = billPaymentLineDao.getByBill(billId)
        assertThat(pl).hasSize(2)
        val mobileLine = pl.first { it.method == PaymentLineCategory.MOBILE.name }
        assertThat(mobileLine.provider).isEqualTo(MfsProvider.NAGAD.name)
        assertThat(mobileLine.amount).isEqualTo(500.0)
        assertThat(pl.first { it.method == PaymentLineCategory.DUE.name }.amount).isEqualTo(1450.0)
        assertThat(cashbookDao.rows.single().account).isEqualTo("MOBILE")
        // denormalized summary stays legacy-parseable
        assertThat(billDao.bills.single().paymentMethod).isEqualTo("NAGAD")
    }

    // ── P14: negative-stock gate (owner device finding: stock went to −9) ──────

    @Test
    fun `oversell is blocked — quantity beyond live stock throws and writes nothing`() = runTest {
        // স্টক ৫ কপি, বিলে ৮ কপি — অবশ্যই ব্লক (RED: pre-P14 createBill happily
        // appended a −8 SALE row and let stock go negative)
        bookDao.books.add(testBookEntity("low-stock", "চিকা সলিপ", initialStock = 5))
        val over = listOf(
            BillLineInput(bookId = "low-stock", bookTitleBn = "চিকা সলিপ", quantity = 8, unitPrice = 100.0, category = com.boikhata.core.domain.enums.BookCategory.GENERAL),
        )

        val thrown = runCatching {
            repo.createBill(
                tenantId = "t_1", customerId = null, customerNameBn = "হাটি ক্রেতা", customerPhone = null,
                userId = "u_1", lines = over, discountAmount = 0.0, discountType = "PERCENTAGE",
                paymentMethod = PaymentMethod.CASH, paidAmount = 800.0,
            )
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(InsufficientStockException::class.java)
        assertThat(thrown!!.message).contains("স্টকে পর্যাপ্ত বই নেই")
        // atomicity: NOTHING was written
        assertThat(billDao.bills).isEmpty()
        assertThat(billDao.lines).isEmpty()
        assertThat(stockLedgerDao.rows).isEmpty()
        assertThat(cashbookDao.rows).isEmpty()
    }

    @Test
    fun `oversell via multi-line payment path is blocked too`() = runTest {
        bookDao.books.add(testBookEntity("low-stock-2", "সয়ং আক", initialStock = 2))
        val over = listOf(
            BillLineInput(bookId = "low-stock-2", bookTitleBn = "সয়ং আক", quantity = 3, unitPrice = 200.0, category = com.boikhata.core.domain.enums.BookCategory.GENERAL),
        )

        val thrown = runCatching {
            repo.createBillWithPaymentLines(
                tenantId = "t_1", customerId = null, customerNameBn = "হাটি ক্রেতা", customerPhone = null,
                userId = "u_1", lines = over, discountAmount = 0.0, discountType = "PERCENTAGE",
                paidLines = listOf(PaymentLineSpec(PaymentLineCategory.CASH, null, 600.0)),
            )
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(InsufficientStockException::class.java)
        assertThat(stockLedgerDao.rows).isEmpty()
        assertThat(billPaymentLineDao.rows).isEmpty()
    }

    @Test
    fun `selling the exact available stock still passes — gate allows selling all remaining copies`() = runTest {
        bookDao.books.add(testBookEntity("exact-stock", "ঠাকুরমার ঝুলি", initialStock = 5))
        val exact = listOf(
            BillLineInput(bookId = "exact-stock", bookTitleBn = "ঠাকুরমার ঝুলি", quantity = 5, unitPrice = 100.0, category = com.boikhata.core.domain.enums.BookCategory.GENERAL),
        )

        val billId = repo.createBill(
            tenantId = "t_1", customerId = null, customerNameBn = "হাটি ক্রেতা", customerPhone = null,
            userId = "u_1", lines = exact, discountAmount = 0.0, discountType = "PERCENTAGE",
            paymentMethod = PaymentMethod.CASH, paidAmount = 500.0,
        )

        assertThat(billId).isNotEmpty()
        assertThat(stockLedgerDao.rows.single().changeQuantity).isEqualTo(-5)
        // stock lands at exactly 0 — never negative
        assertThat(stockLedgerDao.getStockQuantityForBook("exact-stock")).isEqualTo(-5)
    }
}
