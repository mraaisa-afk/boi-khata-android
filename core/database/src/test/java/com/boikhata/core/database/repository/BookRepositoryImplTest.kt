package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.BookDao
import com.boikhata.core.database.dao.StockLedgerDao
import com.boikhata.core.database.entity.BookEntity
import com.boikhata.core.database.entity.BookStockDelta
import com.boikhata.core.database.entity.StockLedgerEntity
import com.boikhata.core.domain.enums.BookCategory
import com.boikhata.core.domain.enums.BookCondition
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * B-005 regression: current stock must be derived as initialStock + stock_ledger
 * delta on EVERY read path. Device evidence: opening stock 40 + a 3-unit sale
 * appended −3 SALE rows to the ledger, yet the stock tab kept showing 40 because
 * the UI read the static initialStock column (D79 "PR E" deferral never landed).
 */
class BookRepositoryImplTest {

    private class FakeBookDao : BookDao {
        val books = mutableListOf<BookEntity>()
        override suspend fun insert(book: BookEntity) { books.add(book) }
        override suspend fun update(book: BookEntity) { books.replaceAll { if (it.id == book.id) book else it } }
        override suspend fun getActiveByTenant(tenantId: String) = books.filter { it.tenantId == tenantId && it.isActive }
        override suspend fun getAllByTenant(tenantId: String) = books.filter { it.tenantId == tenantId }
        override suspend fun countForTenant(tenantId: String) = books.count { it.tenantId == tenantId }
        override suspend fun getById(id: String) = books.firstOrNull { it.id == id }
        override suspend fun search(tenantId: String, normalizedQuery: String) =
            books.filter { it.tenantId == tenantId && it.isActive && it.titleBn.contains(normalizedQuery) }
    }

    private class FakeStockLedgerDao : StockLedgerDao {
        val rows = mutableListOf<StockLedgerEntity>()
        override suspend fun insert(entry: StockLedgerEntity) { rows.add(entry) }
        override suspend fun getByBook(tenantId: String, bookId: String) =
            rows.filter { it.tenantId == tenantId && it.bookId == bookId }
        override suspend fun getStockQuantityForBook(bookId: String) =
            rows.filter { it.bookId == bookId }.sumOf { it.changeQuantity }
        override suspend fun getDeltasByTenant(tenantId: String) =
            rows.filter { it.tenantId == tenantId }
                .groupBy { it.bookId }
                .map { (bookId, entries) -> BookStockDelta(bookId, entries.sumOf { it.changeQuantity }) }
    }

    private fun book(id: String, tenantId: String = "t_1", initialStock: Int, threshold: Int = 5) = BookEntity(
        id = id, tenantId = tenantId, isbn = null, titleBn = "মিসির আলী", titleEn = null,
        author = "হুমায়ূন আহমেদ", publisher = "", classLevel = "", subject = "", editionYear = 2024,
        category = BookCategory.GENERAL.name, condition = BookCondition.NEW.name,
        purchasePrice = 200.0, sellingPrice = 350.0, initialStock = initialStock,
        lowStockThreshold = threshold, isActive = true,
        titleBnNormalized = "মisir", createdAt = 0L, updatedAt = 0L,
    )

    private fun saleRow(bookId: String, qty: Int) = StockLedgerEntity(
        id = bookId + "-sale", tenantId = "t_1", bookId = bookId, changeQuantity = -qty,
        reason = "SALE", referenceId = "bill-1", userId = "u_1", timestamp = 1L, idempotencyKey = "k-$bookId",
    )

    @Test
    fun `current stock reflects sale ledger rows - 40 opening minus 3 sold = 37`() = runTest {
        val bookDao = FakeBookDao()
        val ledgerDao = FakeStockLedgerDao()
        val repo = BookRepositoryImpl(bookDao, ledgerDao, LicenseWriteGuard())
        val id = "b-1"
        bookDao.books.add(book(id, initialStock = 40))

        // BEFORE any sale: displayed stock == opening stock 40
        assertThat(repo.getBooks("t_1").single().currentStock).isEqualTo(40)

        // D22 checkout appends the SALE row (−3)
        ledgerDao.rows.add(saleRow(id, qty = 3))

        val after = repo.getBooks("t_1").single()
        assertThat(after.initialStock).isEqualTo(40)   // opening value unchanged
        assertThat(after.currentStock).isEqualTo(37)   // 40 − 3 ← the fix
    }

    @Test
    fun `getBook and searchBooks also derive live stock`() = runTest {
        val bookDao = FakeBookDao()
        val ledgerDao = FakeStockLedgerDao()
        val repo = BookRepositoryImpl(bookDao, ledgerDao, LicenseWriteGuard())
        val id = "b-2"
        bookDao.books.add(book(id, initialStock = 30))
        ledgerDao.rows.add(saleRow(id, qty = 3))

        assertThat(repo.getBook("t_1", id)!!.currentStock).isEqualTo(27) // 30 − 3
        assertThat(repo.searchBooks("t_1", "").single().currentStock).isEqualTo(27)
    }

    @Test
    fun `low-stock summary uses live stock not the initialStock proxy`() = runTest {
        val bookDao = FakeBookDao()
        val ledgerDao = FakeStockLedgerDao()
        val repo = BookRepositoryImpl(bookDao, ledgerDao, LicenseWriteGuard())
        val id = "b-3"
        bookDao.books.add(book(id, initialStock = 10, threshold = 5))
        ledgerDao.rows.add(saleRow(id, qty = 8)) // live stock = 2 ≤ threshold

        val summary = repo.getLowStockBookSummaries("t_1").single()
        assertThat(summary.currentStock).isEqualTo(2)
    }
}
