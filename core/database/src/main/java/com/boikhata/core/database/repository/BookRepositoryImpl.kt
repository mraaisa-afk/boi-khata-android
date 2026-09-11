package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.BookDao
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
 */
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val writeGuard: LicenseWriteGuard,
) : BookRepository {

    override suspend fun getBooks(tenantId: String): List<Book> {
        return bookDao.getActiveByTenant(tenantId).map { it.toDomain() }
    }

    override suspend fun searchBooks(tenantId: String, normalizedQuery: String): List<Book> {
        if (normalizedQuery.isBlank()) return getBooks(tenantId)
        return bookDao.search(tenantId, normalizedQuery).map { it.toDomain() }
    }

    override suspend fun getBook(tenantId: String, id: String): Book? {
        return bookDao.getById(id)?.toDomain()
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
     * D79 PR D: Returns active books where initialStock ≤ lowStockThreshold,
     * sorted by stock ascending (most critical first).
     *
     * NOTE: Uses initialStock as a proxy for current stock.
     * Real current stock = initialStock + stock_ledger delta.
     * Stock-ledger join is deferred to PR E (requires StockLedgerDao query).
     * This is safe for MVP: books start low and get restocked via stock-in flow.
     */
    override suspend fun getLowStockBookSummaries(tenantId: String): List<LowStockBookSummary> {
        return bookDao.getActiveByTenant(tenantId)
            .filter { it.initialStock <= it.lowStockThreshold }
            .sortedBy { it.initialStock }
            .map {
                LowStockBookSummary(
                    bookId           = it.id,
                    bookTitleBn      = it.titleBn,
                    classLevel       = it.classLevel,
                    currentStock     = it.initialStock, // proxy — see note above
                    lowStockThreshold = it.lowStockThreshold,
                )
            }
    }

    private fun BookEntity.toDomain() = Book(
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
    )
}
