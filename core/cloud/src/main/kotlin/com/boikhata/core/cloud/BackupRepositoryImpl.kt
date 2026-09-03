package com.boikhata.core.cloud

import com.boikhata.core.domain.cloud.BackupMapper
import com.boikhata.core.database.dao.BackupDao
import com.boikhata.core.database.dao.CloudSyncStateDao
import com.boikhata.core.domain.enums.Role
import com.boikhata.core.domain.repository.BackupResult
import com.boikhata.core.domain.repository.BackupRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D46: BackupRepositoryImpl — incremental cloud backup.
 *
 * Firebase-Project-Context.md §6 constraints:
 * #5: per-collection WriteBatch (≤450 ops), one denied write fails the whole batch.
 * #6: incremental only (lastBackupAt filter) — re-uploading = update = denied on append-only.
 * §2: every document carries tenantId from claims.
 * §3: 10 backup-scope collections; audit_logs NEVER uploaded.
 *
 * Gate: role == OWNER (rules deny non-OWNER writes on most collections).
 * Gate: isPendingActivation == false (rebind must be done first).
 *
 * NOTE: Actual Firestore round-trip requires a real device + network.
 * The sandbox cannot verify runtime Firestore behavior.
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val backupDao: BackupDao,
    private val cloudSyncStateDao: CloudSyncStateDao,
) : BackupRepository {

    companion object {
        private const val MAX_BATCH_SIZE = 450 // constraint #5
    }

    override suspend fun backup(tenantId: String, role: Role): BackupResult {
        // Gate: OWNER-only (rules deny non-OWNER writes on most collections)
        if (role != Role.OWNER) return BackupResult.NotOwner

        // Gate: rebind must be done first
        val syncState = cloudSyncStateDao.get()
        if (syncState?.isPendingActivation == true) return BackupResult.RebindNeeded

        val lastBackupAt = syncState?.lastBackupAt
        val now = System.currentTimeMillis()
        var totalRowsUploaded = 0
        var collectionsBackedUp = 0
        val collectionErrors = mutableMapOf<String, String>()

        // Per-collection backup (constraint #5: per-collection batches)
        for (collectionName in BackupMapper.ALL_BACKUP_COLLECTIONS) {
            try {
                val rowsUploaded = backupCollection(collectionName, tenantId, lastBackupAt)
                totalRowsUploaded += rowsUploaded
                collectionsBackedUp++
            } catch (e: Exception) {
                collectionErrors[collectionName] = e.message ?: "unknown error"
            }
        }

        // Update lastBackupAt only if all collections succeeded
        return if (collectionErrors.isEmpty()) {
            cloudSyncStateDao.updateLastBackupAt(now, System.currentTimeMillis())
            BackupResult.Success(collectionsBackedUp, totalRowsUploaded, now)
        } else {
            BackupResult.Partial(collectionsBackedUp, totalRowsUploaded, collectionErrors)
        }
    }

    private suspend fun backupCollection(
        collectionName: String,
        tenantId: String,
        lastBackupAt: Long?,
    ): Int {
        // Read all rows for this collection
        val rows = readRowsForCollection(collectionName, tenantId)
        if (rows.isEmpty()) return 0

        // Filter to only new/changed rows (incremental — constraint #6)
        val filteredRows = BackupMapper.filterNewRows(collectionName, rows, lastBackupAt)
        if (filteredRows.isEmpty()) return 0

        // Commit in batches of ≤450 (constraint #5)
        var uploaded = 0
        for (batch in filteredRows.chunked(MAX_BATCH_SIZE)) {
            val writeBatch = firestore.batch()
            for (row in batch) {
                val docId = row["id"] as? String ?: continue
                val docRef = firestore.collection(collectionName).document(docId)
                writeBatch.set(docRef, row)
            }
            writeBatch.commit().await()
            uploaded += batch.size
        }
        return uploaded
    }

    private suspend fun readRowsForCollection(
        collectionName: String,
        tenantId: String,
    ): List<Map<String, Any?>> {
        return when (collectionName) {
            BackupMapper.COL_BOOKS -> backupDao.getBooksForBackup(tenantId).map {
                BackupMapper.bookToMap(
                    id = it.id, tenantId = tenantId, isbn = it.isbn,
                    titleBn = it.titleBn, titleEn = it.titleEn, author = it.author,
                    publisher = it.publisher, classLevel = it.classLevel, subject = it.subject,
                    editionYear = it.editionYear, category = it.category, condition = it.condition,
                    purchasePrice = it.purchasePrice, sellingPrice = it.sellingPrice,
                    initialStock = it.initialStock, lowStockThreshold = it.lowStockThreshold,
                    isActive = it.isActive, createdAt = it.createdAt, updatedAt = it.updatedAt,
                )
            }
            BackupMapper.COL_STOCK_LEDGER -> backupDao.getStockLedgerForBackup(tenantId).map {
                BackupMapper.stockLedgerToMap(
                    id = it.id, tenantId = tenantId, bookId = it.bookId,
                    changeQuantity = it.changeQuantity, reason = it.reason,
                    referenceId = it.referenceId, userId = it.userId,
                    timestamp = it.timestamp, idempotencyKey = it.idempotencyKey,
                )
            }
            BackupMapper.COL_BILLS -> backupDao.getBillsForBackup(tenantId).map {
                BackupMapper.billToMap(
                    id = it.id, tenantId = tenantId, billNumber = it.billNumber,
                    customerId = it.customerId, customerNameBn = it.customerNameBn,
                    customerPhone = it.customerPhone, userId = it.userId,
                    subtotal = it.subtotal, discountAmount = it.discountAmount,
                    discountType = it.discountType, vatAmount = it.vatAmount,
                    totalAmount = it.totalAmount, paymentMethod = it.paymentMethod,
                    paidAmount = it.paidAmount, dueAmount = it.dueAmount,
                    khataEntryId = it.khataEntryId, billDate = it.billDate,
                    status = it.status, idempotencyKey = it.idempotencyKey,
                )
            }
            BackupMapper.COL_BILL_LINES -> backupDao.getBillLinesForBackup(tenantId).map {
                BackupMapper.billLineToMap(
                    id = it.id, tenantId = tenantId, billId = it.billId,
                    bookId = it.bookId, bookTitleBn = it.bookTitleBn,
                    quantity = it.quantity, unitPrice = it.unitPrice,
                    lineTotal = it.lineTotal, vatAmount = it.vatAmount,
                )
            }
            BackupMapper.COL_KHATA_CUSTOMERS -> backupDao.getKhataCustomersForBackup(tenantId).map {
                BackupMapper.khataCustomerToMap(
                    id = it.id, tenantId = tenantId, nameBn = it.nameBn,
                    phone = it.phone, address = it.address, creditLimit = it.creditLimit,
                    isActive = it.isActive, createdAt = it.createdAt, updatedAt = it.updatedAt,
                )
            }
            BackupMapper.COL_KHATA_ENTRIES -> backupDao.getKhataEntriesForBackup(tenantId).map {
                BackupMapper.khataEntryToMap(
                    id = it.id, tenantId = tenantId, customerId = it.customerId,
                    amount = it.amount, type = it.type, description = it.description,
                    referenceBillId = it.referenceBillId, collectedByUserId = it.collectedByUserId,
                    date = it.date, idempotencyKey = it.idempotencyKey,
                )
            }
            BackupMapper.COL_EXPENSES -> backupDao.getExpensesForBackup(tenantId).map {
                BackupMapper.expenseToMap(
                    id = it.id, tenantId = tenantId, categoryId = it.categoryId,
                    amount = it.amount, description = it.description,
                    expenseDate = it.expenseDate, receiptPhotoPath = it.receiptPhotoPath,
                    userId = it.userId, idempotencyKey = it.idempotencyKey,
                )
            }
            BackupMapper.COL_CASHBOOK_ENTRIES -> backupDao.getCashbookEntriesForBackup(tenantId).map {
                BackupMapper.cashbookEntryToMap(
                    id = it.id, tenantId = tenantId, account = it.account,
                    type = it.type, amount = it.amount, description = it.description,
                    referenceId = it.referenceId, date = it.date, userId = it.userId,
                    idempotencyKey = it.idempotencyKey,
                )
            }
            BackupMapper.COL_EXPENSE_CATEGORIES -> backupDao.getExpenseCategoriesForBackup(tenantId).map {
                BackupMapper.expenseCategoryToMap(
                    id = it.id, tenantId = tenantId, nameBn = it.nameBn,
                    icon = it.icon, isActive = it.isActive,
                )
            }
            BackupMapper.COL_OWNER_DRAWINGS -> backupDao.getOwnerDrawingsForBackup(tenantId).map {
                BackupMapper.ownerDrawingToMap(
                    id = it.id, tenantId = tenantId, amount = it.amount,
                    description = it.description, drawingDate = it.drawingDate,
                    userId = it.userId, idempotencyKey = it.idempotencyKey,
                )
            }
            else -> emptyList()
        }
    }
}
