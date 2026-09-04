package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.BookDao
import com.boikhata.core.database.dao.MelaDao
import com.boikhata.core.database.dao.StockLedgerDao
import com.boikhata.core.database.entity.MelaSessionEntity
import com.boikhata.core.database.entity.StockLedgerEntity
import com.boikhata.core.domain.accounting.MelaStockCalculator
import com.boikhata.core.domain.accounting.PeriodLockChecker
import com.boikhata.core.domain.enums.StockChangeReason
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.model.LowStockAlert
import com.boikhata.core.domain.model.MelaSession
import com.boikhata.core.domain.model.MelaStockLine
import com.boikhata.core.domain.model.OversellAlert
import com.boikhata.core.domain.repository.MelaRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D56-D57: Mela mode repository — book-fair/seasonal session lifecycle (start/pause/resume/end)
 * + MELA_IN/MELA_OUT stock-cycle moves + low-stock / oversell alerts.
 *
 * A paused session blocks new MELA_IN/MELA_OUT stock moves (MelaPausedException) but keeps
 * reads/stats open. Stock is always derived from the stock_ledger (ARCH §4, never stored).
 */
@Singleton
class MelaRepositoryImpl @Inject constructor(
    private val melaDao: MelaDao,
    private val stockLedgerDao: StockLedgerDao,
    private val bookDao: BookDao,
    private val writeGuard: LicenseWriteGuard,
    private val periodLockChecker: PeriodLockChecker,
) : MelaRepository {

    /** Thrown when the mela is paused and a stock move is attempted. */
    class MelaPausedException(message: String) : Exception(message)

    override suspend fun getCurrentSession(tenantId: String): MelaSession? {
        return melaDao.getActiveSession(tenantId)?.toDomain()
    }

    override suspend fun startSession(
        tenantId: String,
        nameBn: String,
        location: String,
        startDate: Long,
        endDate: Long,
        userId: String,
    ): String {
        writeGuard.assertWriteAllowed()
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val existing = melaDao.getActiveSession(tenantId)
        // D57: only one active session at a time — end the previous before starting a new one.
        if (existing != null && existing.isActive) {
            melaDao.updateState(existing.id, false, false, null, now)
        }
        melaDao.insert(
            MelaSessionEntity(
                id = id,
                tenantId = tenantId,
                nameBn = nameBn,
                location = location,
                startDate = startDate,
                endDate = endDate,
                isActive = true,
                isPaused = false,
                pauseReason = null,
                createdAt = now,
                updatedAt = now,
            )
        )
        return id
    }

    override suspend fun pauseSession(tenantId: String, userId: String, reason: String?): Boolean {
        writeGuard.assertWriteAllowed()
        val session = melaDao.getActiveSession(tenantId) ?: return false
        melaDao.updateState(session.id, true, true, reason, System.currentTimeMillis())
        return true
    }

    override suspend fun resumeSession(tenantId: String, userId: String): Boolean {
        writeGuard.assertWriteAllowed()
        val session = melaDao.getActiveSession(tenantId) ?: return false
        melaDao.updateState(session.id, true, false, null, System.currentTimeMillis())
        return true
    }

    override suspend fun endSession(tenantId: String, userId: String): Boolean {
        writeGuard.assertWriteAllowed()
        val session = melaDao.getActiveSession(tenantId) ?: return false
        melaDao.updateState(session.id, false, false, null, System.currentTimeMillis())
        return true
    }

    override suspend fun isSessionPaused(tenantId: String): Boolean {
        return melaDao.getActiveSession(tenantId)?.isPaused ?: false
    }

    override suspend fun moveStock(
        tenantId: String,
        bookId: String,
        quantity: Int,
        direction: StockChangeReason,
        userId: String,
    ): String {
        if (direction != StockChangeReason.MELA_IN && direction != StockChangeReason.MELA_OUT) {
            throw IllegalArgumentException("MELA_IN বা MELA_OUT হতে হবে")
        }
        writeGuard.assertWriteAllowed()
        if (isSessionPaused(tenantId)) {
            throw MelaPausedException("মেলা স্থগিত আছে — নতুন স্টক স্থানান্তর বন্ধ। পড়া ও রিপোর্ট খোলা আছে।")
        }
        periodLockChecker.assertNotLocked(tenantId, System.currentTimeMillis())

        // D56: MELA_IN = move stock to the mela stall (+), MELA_OUT = bring back (−).
        val signed = if (direction == StockChangeReason.MELA_IN) quantity.coerceAtLeast(0) else -quantity.coerceAtLeast(0)
        val id = UUID.randomUUID().toString()
        stockLedgerDao.insert(
            StockLedgerEntity(
                id = id,
                tenantId = tenantId,
                bookId = bookId,
                changeQuantity = signed,
                reason = direction.name,
                referenceId = melaDao.getActiveSession(tenantId)?.id,
                userId = userId,
                timestamp = System.currentTimeMillis(),
                idempotencyKey = UUID.randomUUID().toString(),
            )
        )
        return id
    }

    override suspend fun getLowStockAlerts(tenantId: String): List<LowStockAlert> {
        val books = bookDao.getActiveByTenant(tenantId)
        val (melaStocks, shopStocks) = stockByBook(tenantId, books)
        return MelaStockCalculator.lowStockAlerts(
            books = books.map { it.id to it.titleBn },
            melaStocks = melaStocks,
            shopStocks = shopStocks,
        )
    }

    override suspend fun getOversellAlerts(tenantId: String): List<OversellAlert> {
        val books = bookDao.getActiveByTenant(tenantId)
        val (melaStocks, shopStocks) = stockByBook(tenantId, books)
        val netStocks = melaStocks.keys.associateWith { (melaStocks[it] ?: 0) + (shopStocks[it] ?: 0) }
        return MelaStockCalculator.oversellAlerts(
            books = books.map { it.id to it.titleBn },
            netStocks = netStocks,
        )
    }

    override suspend fun getMelaStockReport(tenantId: String): List<MelaStockLine> {
        val books = bookDao.getActiveByTenant(tenantId)
        val movesByBook = books.associate { book ->
            val moves = stockLedgerDao.getByBook(tenantId, book.id)
                .map {
                    MelaStockCalculator.StockMove(
                        reason = runCatching { StockChangeReason.valueOf(it.reason) }.getOrDefault(StockChangeReason.ADJUSTMENT),
                        changeQuantity = it.changeQuantity,
                    )
                }
            (book.id to book.titleBn) to moves
        }
        return MelaStockCalculator.buildReport(movesByBook)
    }

    // ── helpers ────────────────────────────────────────────────────────────
    /** Pair of (melaStock, shopStock) maps keyed by bookId. shopStock = net − mela. */
    private suspend fun stockByBook(
        tenantId: String,
        books: List<com.boikhata.core.database.entity.BookEntity>,
    ): Pair<Map<String, Int>, Map<String, Int>> {
        val melaStocks = mutableMapOf<String, Int>()
        val netStocks = mutableMapOf<String, Int>()
        for (book in books) {
            val moves = stockLedgerDao.getByBook(tenantId, book.id).map {
                MelaStockCalculator.StockMove(
                    reason = runCatching { StockChangeReason.valueOf(it.reason) }.getOrDefault(StockChangeReason.ADJUSTMENT),
                    changeQuantity = it.changeQuantity,
                )
            }
            val net = MelaStockCalculator.netStock(moves)
            val mela = MelaStockCalculator.melaStockLine(book.id, book.titleBn, moves).atMela
            netStocks[book.id] = net
            melaStocks[book.id] = mela
        }
        val shopStocks = netStocks.mapValues { (id, net) -> net - (melaStocks[id] ?: 0) }
        return melaStocks.toMap() to shopStocks
    }

    private fun MelaSessionEntity.toDomain() = MelaSession(
        id = id,
        tenantId = tenantId,
        nameBn = nameBn,
        location = location,
        startDate = startDate,
        endDate = endDate,
        isActive = isActive,
        isPaused = isPaused,
        pauseReason = pauseReason,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
