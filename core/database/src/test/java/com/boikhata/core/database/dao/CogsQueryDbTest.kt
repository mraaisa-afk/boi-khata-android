package com.boikhata.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.boikhata.core.database.BoiKhataDatabase
import com.boikhata.core.database.entity.BillEntity
import com.boikhata.core.database.entity.BillLineEntity
import com.boikhata.core.database.entity.BookEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * D94 (2026-09-24 owner ruling): the COGS query runs against a REAL in-memory
 * Room database (not the fake-DAO suite) because the JOIN is the logic under
 * test: bill_lines × books.purchasePrice over ALL bills in the window —
 * credit/বাকি sales included (a credit sale has real acquisition cost even
 * though no cash moved). Deleted books contribute 0 (LEFT JOIN + COALESCE).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CogsQueryDbTest {

    private lateinit var db: BoiKhataDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BoiKhataDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun book(id: String, purchasePrice: Double, tenantId: String = "t_1") = BookEntity(
        id = id, tenantId = tenantId, isbn = null, titleBn = id, titleEn = null,
        author = "", publisher = "", classLevel = "", subject = "", editionYear = 2026,
        category = "GENERAL", condition = "NEW", purchasePrice = purchasePrice,
        sellingPrice = purchasePrice * 2, initialStock = 10, lowStockThreshold = 2,
        isActive = true, createdAt = 0L, updatedAt = 0L,
    )

    private fun bill(id: String, total: Double, paid: Double, due: Double, date: Long = 100L) = BillEntity(
        id = id, tenantId = "t_1", billNumber = "INV-$id", customerId = null,
        customerNameBn = "হাটি ক্রেতা", customerPhone = null, userId = "u_1",
        subtotal = total, discountAmount = 0.0, discountType = "FIXED", vatAmount = 0.0,
        totalAmount = total, paymentMethod = "CASH", paidAmount = paid, dueAmount = due,
        khataEntryId = null, billDate = date, status = "COMPLETED", idempotencyKey = "k-$id",
    )

    private fun line(billId: String, bookId: String, qty: Int, unitPrice: Double) = BillLineEntity(
        id = "l-$billId-$bookId", tenantId = "t_1", billId = billId, bookId = bookId,
        bookTitleBn = bookId, quantity = qty, unitPrice = unitPrice, lineTotal = unitPrice * qty,
        vatAmount = 0.0,
    )

    @Test
    fun `cogs joins lines to books across cash and credit bills and survives deleted books`() = runTest {
        db.bookDao().insert(book("b1", purchasePrice = 100.0))
        db.bookDao().insert(book("b2", purchasePrice = 50.0))
        // third book is intentionally NOT inserted — simulates deletion after sale

        val billDao = db.billDao()
        // cash bill: 2 × b1
        billDao.insert(bill("bill-cash", total = 500.0, paid = 500.0, due = 0.0))
        billDao.insertLines(listOf(line("bill-cash", "b1", qty = 2, unitPrice = 250.0)))
        // credit bill: 4 × b2 (nothing received — still real COGS, D94)
        billDao.insert(
            bill("bill-credit", total = 800.0, paid = 0.0, due = 800.0).copy(paymentMethod = "CREDIT", status = "PARTIAL")
        )
        billDao.insertLines(listOf(line("bill-credit", "b2", qty = 4, unitPrice = 200.0)))
        // bill whose book was deleted: 3 × ghost (purchase price unknown → 0)
        billDao.insert(bill("bill-ghost", total = 300.0, paid = 300.0, due = 0.0))
        billDao.insertLines(listOf(line("bill-ghost", "ghost", qty = 3, unitPrice = 100.0)))

        val cogs = billDao.getCogsByDateRange("t_1", startOfDay = 0L, endOfDay = 1000L)
        assertThat(cogs).isEqualTo(2 * 100.0 + 4 * 50.0) // 400 — deleted book contributes 0

        // tenant isolation
        assertThat(billDao.getCogsByDateRange("t_other", 0L, 1000L)).isEqualTo(0.0)
        // window isolation
        assertThat(billDao.getCogsByDateRange("t_1", 200L, 1000L)).isEqualTo(0.0)
    }
}
