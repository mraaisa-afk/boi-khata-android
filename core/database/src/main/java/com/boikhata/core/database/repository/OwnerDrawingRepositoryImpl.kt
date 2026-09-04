package com.boikhata.core.database.repository

import androidx.room.withTransaction
import com.boikhata.core.database.BoiKhataDatabase
import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.database.dao.OwnerDrawingDao
import com.boikhata.core.database.entity.CashbookEntryEntity
import com.boikhata.core.database.entity.OwnerDrawingEntity
import com.boikhata.core.domain.accounting.PeriodLockChecker
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.model.OwnerDrawing
import com.boikhata.core.domain.repository.OwnerDrawingRepository
import java.util.UUID
import javax.inject.Inject

/**
 * P3a: OwnerDrawingRepository implementation.
 * D28: Separate table, OWNER-only, cashbook EXPENSE auto-populate (atomic).
 * D32: Period-lock check before write.
 */
class OwnerDrawingRepositoryImpl @Inject constructor(
    private val db: BoiKhataDatabase,
    private val ownerDrawingDao: OwnerDrawingDao,
    private val cashbookDao: CashbookDao,
    private val writeGuard: LicenseWriteGuard,
    private val periodLockChecker: PeriodLockChecker,
) : OwnerDrawingRepository {

    override suspend fun getDrawings(tenantId: String): List<OwnerDrawing> {
        return ownerDrawingDao.getByTenant(tenantId).map {
            OwnerDrawing(it.id, it.amount, it.description, it.drawingDate, it.userId)
        }
    }

    override suspend fun getDrawingsByDateRange(tenantId: String, start: Long, end: Long): List<OwnerDrawing> {
        return ownerDrawingDao.getByDateRange(tenantId, start, end).map {
            OwnerDrawing(it.id, it.amount, it.description, it.drawingDate, it.userId)
        }
    }

    /**
     * D28: Create drawing + cashbook EXPENSE entry in one atomic transaction.
     */
    override suspend fun createDrawing(
        tenantId: String,
        amount: Double,
        description: String,
        userId: String,
    ): String {
        writeGuard.assertWriteAllowed()
        // D32: Period-lock check
        val now = System.currentTimeMillis()
        periodLockChecker.assertNotLocked(tenantId, now)

        val drawingId = UUID.randomUUID().toString()

        db.withTransaction {
            // 1. Insert drawing
            ownerDrawingDao.insert(
                OwnerDrawingEntity(
                    id = drawingId,
                    tenantId = tenantId,
                    amount = amount,
                    description = description,
                    drawingDate = now,
                    userId = userId,
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            )
            // 2. D25: Auto-populate cashbook (CASH/EXPENSE)
            cashbookDao.insert(
                CashbookEntryEntity(
                    id = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    account = "CASH", // drawings are typically cash
                    type = "EXPENSE",
                    amount = amount,
                    description = "মালিকের তোলা: $description",
                    referenceId = drawingId,
                    date = now,
                    userId = userId,
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            )
        }
        return drawingId
    }
}
