package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.BookDao
import com.boikhata.core.database.dao.StockLedgerDao
import com.boikhata.core.database.entity.BookEntity
import com.boikhata.core.domain.enums.BookCategory
import com.boikhata.core.domain.enums.BookCondition
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.model.Book
import com.boikhata.core.domain.model.LowStockBookSummary
import com.boikhata.core.domain.pilot.TrialPolicy
import com.boikhata.core.domain.repository.BookRepository
import com.boikhata.core.domain.text.BengaliNormalizer
import java.util.UUID
import javax.inject.Inject

/**
 * P2a: BookRepository implementation — local Room-only (offline-first).
 * Master-catalog import is P4/Firebase; this phase is local entry.
 *
 * B-005: every read path now derives `currentStock = initialStock + stock_ledger delta`.
 * The D22 sale flow appends negative SALE rows to the append-only ledger, but until
 * this fix the UI read the static `initialStock` column (D79 note deferred the
 * ledger join to a "PR E" that never landed) — so stock never appeared to change
 * after a sale.
 */
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val stockLedgerDao: StockLedgerDao,
    private val writeGuard: LicenseWriteGuard,
) : BookRepository {

    private suspend fun ledgerDeltas(tenantId: String): Map<String, Int> =
        stockLedgerDao.getDeltasByTenant(tenantId).associate { it.bookId to it.delta }

    private fun BookEntity.toDomain(deltas: Map<String, Int>) = Book(
        id               = id,
        isbn             = isbn,
        titleBn          = titleBn,
        titleEn          = titleEn,
        author           = author,
        publisher        = publisher,
        classLevel       = classLevel,
        subject          = subject,
        editionYear      = editionYear,
        category         = BookCategory.valueOf(category),
        condition        = BookCondition.valueOf(condition),
        purchasePrice    = purchasePrice,
        sellingPrice     = sellingPrice,
        initialStock     = initialStock,
        lowStockThreshold = lowStockThreshold,
        isActive         = isActive,
        currentStock     = initialStock + (deltas[id] ?: 0), // B-005: live stock
    )

    override suspend fun getBooks(tenantId: String): List<Book> {
        val deltas = ledgerDeltas(tenantId)
        return bookDao.getActiveByTenant(tenantId).map { it.toDomain(deltas) }
    }

    override suspend fun searchBooks(tenantId: String, normalizedQuery: String): List<Book> {
        if (normalizedQuery.isBlank()) return getBooks(tenantId)
        val deltas = ledgerDeltas(tenantId)
        return bookDao.search(tenantId, normalizedQuery).map { it.toDomain(deltas) }
    }

    override suspend fun getBook(tenantId: String, id: String): Book? {
        val entity = bookDao.getById(id) ?: return null
        val delta = stockLedgerDao.getDeltasByTenant(entity.tenantId)
            .firstOrNull { it.bookId == id }?.delta ?: 0
        return entity.toDomain(mapOf(id to delta))
    }

    override suspend fun addBook(
        tenantId: String,
        isbn: String?,
        titleBn: String,
        titleEn: String?,
        author: String,
        publisher: String,
        classLevel: String,
        subject: String,
        editionYear: Int,
        category: BookCategory,
        condition: BookCondition,
        purchasePrice: Double,
        sellingPrice: Double,
        initialStock: Int,
        lowStockThreshold: Int,
    ): String {
        writeGuard.assertWriteAllowed()
        TrialPolicy.assertCanAddBook(TrialPolicy.Usage(0, bookDao.countForTenant(tenantId)))
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        bookDao.insert(
            BookEntity(
                id = id,
                tenantId = tenantId,
                isbn = isbn,
                titleBn = titleBn,
                titleEn = titleEn,
                author = author,
                publisher = publisher,
                classLevel = classLevel,
                subject = subject,
                editionYear = editionYear,
                category = category.name,
                condition = condition.name,
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                initialStock = initialStock,
                lowStockThreshold = lowStockThreshold,
                isActive = true,
                titleBnNormalized = BengaliNormalizer.normalize(titleBn),
                createdAt = now,
                updatedAt = now,
            )
        )
        return id
    }

    override suspend fun updateBook(
        tenantId: String,
        id: String,
        isbn: String?,
        titleBn: String,
        titleEn: String?,
        author: String,
        publisher: String,
        classLevel: String,
        subject: String,
        editionYear: Int,
        category: BookCategory,
        condition: BookCondition,
        purchasePrice: Double,
        sellingPrice: Double,
        lowStockThreshold: Int,
        isActive: Boolean,
    ) {
        writeGuard.assertWriteAllowed()
        val existing = bookDao.getById(id) ?: return
        bookDao.update(
            existing.copy(
                isbn = isbn,
                titleBn = titleBn,
                titleEn = titleEn,
                author = author,
                publisher = publisher,
                classLevel = classLevel,
                subject = subject,
                editionYear = editionYear,
                category = category.name,
                condition = condition.name,
                purchasePrice = purchasePrice,
                sellingPrice = sellingPrice,
                lowStockThreshold = lowStockThreshold,
                isActive = isActive,
                titleBnNormalized = BengaliNormalizer.normalize(titleBn),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    /**
     * D79 PR D: Returns active books whose live stock (B-005: initialStock + ledger
     * delta, not the initialStock proxy) ≤ lowStockThreshold, sorted most-critical first.
     */
    override suspend fun getLowStockBookSummaries(tenantId: String): List<LowStockBookSummary> {
        val deltas = ledgerDeltas(tenantId)
        return bookDao.getActiveByTenant(tenantId)
            .map { entity -> entity to (entity.initialStock + (deltas[entity.id] ?: 0)) }
            .filter { (entity, liveStock) -> liveStock <= entity.lowStockThreshold }
            .sortedBy { (_, liveStock) -> liveStock }
            .map { (entity, liveStock) ->
                LowStockBookSummary(
                    bookId           = entity.id,
                    bookTitleBn      = entity.titleBn,
                    classLevel       = entity.classLevel,
                    currentStock     = liveStock,
                    lowStockThreshold = entity.lowStockThreshold,
                )
            }
    }

    private fun BookEntity.toDomain(): Book = toDomain(emptyMap())
}
