package com.boikhata.core.cloud

import com.boikhata.core.domain.cloud.BackupMapper
import com.boikhata.core.domain.cloud.RestoreMapper
import com.boikhata.core.database.dao.BackupDao
import com.boikhata.core.database.dao.BillDao
import com.boikhata.core.database.dao.BookDao
import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.database.dao.CloudSyncStateDao
import com.boikhata.core.database.dao.ExpenseCategoryDao
import com.boikhata.core.database.dao.ExpenseDao
import com.boikhata.core.database.dao.KhataCustomerDao
import com.boikhata.core.database.dao.KhataEntryDao
import com.boikhata.core.database.dao.OwnerDrawingDao
import com.boikhata.core.database.dao.StockLedgerDao
import com.boikhata.core.database.entity.BillEntity
import com.boikhata.core.database.entity.BillLineEntity
import com.boikhata.core.database.entity.BookEntity
import com.boikhata.core.database.entity.CashbookEntryEntity
import com.boikhata.core.database.entity.ExpenseCategoryEntity
import com.boikhata.core.database.entity.ExpenseEntity
import com.boikhata.core.database.entity.KhataCustomerEntity
import com.boikhata.core.database.entity.KhataEntryEntity
import com.boikhata.core.database.entity.OwnerDrawingEntity
import com.boikhata.core.database.entity.StockLedgerEntity
import com.boikhata.core.domain.enums.Role
import com.boikhata.core.domain.repository.RestoreResult
import com.boikhata.core.domain.repository.RestoreStrategy
import com.boikhata.core.domain.repository.RestoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D46: RestoreRepositoryImpl — fresh-device restore from Firestore.
 *
 * Downloads all tenant-scoped collections, rebuilds Room.
 * Firebase-Project-Context.md §6 constraint #2: field-by-field mapping (never toObjects()).
 * Constraint #4: restore reverses Negative-Adj sign + strips prefix (via RestoreMapper).
 * Constraint #3: avoid whereEqualTo+orderBy — sort client-side.
 *
 * The constitution: never auto-merge when both sides have data — the user chooses.
 * For fresh-device (local DB empty): auto-restore.
 * For both-sides-have-data: return BothSidesHaveData — UI shows choice screen.
 *
 * NOTE: Actual Firestore round-trip requires a real device + network.
 */
@Singleton
class RestoreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val backupDao: BackupDao,
    private val bookDao: BookDao,
    private val stockLedgerDao: StockLedgerDao,
    private val billDao: BillDao,
    private val khataCustomerDao: KhataCustomerDao,
    private val khataEntryDao: KhataEntryDao,
    private val expenseDao: ExpenseDao,
    private val cashbookDao: CashbookDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
    private val ownerDrawingDao: OwnerDrawingDao,
    private val cloudSyncStateDao: CloudSyncStateDao,
) : RestoreRepository {

    override suspend fun checkAndRestore(
        tenantId: String,
        role: Role,
        strategy: RestoreStrategy,
    ): RestoreResult {
        if (role != Role.OWNER) return RestoreResult.NotOwner

        // Check if local DB has data
        val localHasData = backupDao.countBooks(tenantId) > 0 ||
            backupDao.countBills(tenantId) > 0 ||
            backupDao.countKhataEntries(tenantId) > 0 ||
            backupDao.countCashbookEntries(tenantId) > 0

        if (localHasData && strategy == RestoreStrategy.KEEP_LOCAL) {
            return RestoreResult.Success(0) // user chose to keep local
        }

        if (localHasData && strategy != RestoreStrategy.CLOUD_OVERWRITES_LOCAL) {
            // Both sides have data and user hasn't chosen cloud-overwrites — require choice
            return RestoreResult.BothSidesHaveData
        }

        return try {
            var totalRestored = 0
            totalRestored += restoreBooks(tenantId)
            totalRestored += restoreStockLedger(tenantId)
            totalRestored += restoreBills(tenantId)
            totalRestored += restoreBillLines(tenantId)
            totalRestored += restoreKhataCustomers(tenantId)
            totalRestored += restoreKhataEntries(tenantId)
            totalRestored += restoreExpenses(tenantId)
            totalRestored += restoreCashbookEntries(tenantId)
            totalRestored += restoreExpenseCategories(tenantId)
            totalRestored += restoreOwnerDrawings(tenantId)

            cloudSyncStateDao.updateLastRestoreAt(System.currentTimeMillis(), System.currentTimeMillis())
            RestoreResult.Success(totalRestored)
        } catch (e: Exception) {
            RestoreResult.Error(e.message ?: "restore error")
        }
    }

    private suspend fun restoreBooks(tenantId: String): Int {
        val snapshot = firestore.collection(BackupMapper.COL_BOOKS)
            .whereEqualTo("tenantId", tenantId)
            .get()
            .await()
        var count = 0
        for (doc in snapshot.documents) {
            val data = doc.data ?: continue
            val fields = RestoreMapper.bookFromMap(data)
            bookDao.insert(BookEntity(
                id = fields.id, tenantId = fields.tenantId, isbn = fields.isbn,
                titleBn = fields.titleBn, titleEn = fields.titleEn, author = fields.author,
                publisher = fields.publisher, classLevel = fields.classLevel,
                subject = fields.subject, editionYear = fields.editionYear,
                category = fields.category, condition = fields.condition,
                purchasePrice = fields.purchasePrice, sellingPrice = fields.sellingPrice,
                initialStock = fields.initialStock, lowStockThreshold = fields.lowStockThreshold,
                isActive = fields.isActive, createdAt = fields.createdAt, updatedAt = fields.updatedAt,
            ))
            count++
        }
        return count
    }

    private suspend fun restoreStockLedger(tenantId: String): Int {
        val snapshot = firestore.collection(BackupMapper.COL_STOCK_LEDGER)
            .whereEqualTo("tenantId", tenantId)
            .get()
            .await()
        var count = 0
        for (doc in snapshot.documents) {
            val data = doc.data ?: continue
            val fields = RestoreMapper.stockLedgerFromMap(data)
            stockLedgerDao.insert(StockLedgerEntity(
                id = fields.id, tenantId = fields.tenantId, bookId = fields.bookId,
                changeQuantity = fields.changeQuantity, reason = fields.reason,
                referenceId = fields.referenceId, userId = fields.userId,
                timestamp = fields.timestamp, idempotencyKey = fields.idempotencyKey,
            ))
            count++
        }
        return count
    }

    private suspend fun restoreBills(tenantId: String): Int {
        val snapshot = firestore.collection(BackupMapper.COL_BILLS)
            .whereEqualTo("tenantId", tenantId)
            .get()
            .await()
        var count = 0
        for (doc in snapshot.documents) {
            val data = doc.data ?: continue
            val fields = RestoreMapper.billFromMap(data)
            billDao.insert(BillEntity(
                id = fields.id, tenantId = fields.tenantId, billNumber = fields.billNumber,
                customerId = fields.customerId, customerNameBn = fields.customerNameBn,
                customerPhone = fields.customerPhone, userId = fields.userId,
                subtotal = fields.subtotal, discountAmount = fields.discountAmount,
                discountType = fields.discountType, vatAmount = fields.vatAmount,
                totalAmount = fields.totalAmount, paymentMethod = fields.paymentMethod,
                paidAmount = fields.paidAmount, dueAmount = fields.dueAmount,
                khataEntryId = fields.khataEntryId, billDate = fields.billDate,
                status = fields.status, idempotencyKey = fields.idempotencyKey,
            ))
            count++
        }
        return count
    }

    private suspend fun restoreBillLines(tenantId: String): Int {
        val snapshot = firestore.collection(BackupMapper.COL_BILL_LINES)
            .whereEqualTo("tenantId", tenantId)
            .get()
            .await()
        var count = 0
        for (doc in snapshot.documents) {
            val data = doc.data ?: continue
            val fields = RestoreMapper.billLineFromMap(data)
            billDao.insertLines(listOf(BillLineEntity(
                id = fields.id, tenantId = fields.tenantId, billId = fields.billId,
                bookId = fields.bookId, bookTitleBn = fields.bookTitleBn,
                quantity = fields.quantity, unitPrice = fields.unitPrice,
                lineTotal = fields.lineTotal, vatAmount = fields.vatAmount,
            )))
            count++
        }
        return count
    }

    private suspend fun restoreKhataCustomers(tenantId: String): Int {
        val snapshot = firestore.collection(BackupMapper.COL_KHATA_CUSTOMERS)
            .whereEqualTo("tenantId", tenantId)
            .get()
            .await()
        var count = 0
        for (doc in snapshot.documents) {
            val data = doc.data ?: continue
            val fields = RestoreMapper.khataCustomerFromMap(data)
            khataCustomerDao.insert(KhataCustomerEntity(
                id = fields.id, tenantId = fields.tenantId, nameBn = fields.nameBn,
                phone = fields.phone, address = fields.address, creditLimit = fields.creditLimit,
                isActive = fields.isActive, createdAt = fields.createdAt, updatedAt = fields.updatedAt,
            ))
            count++
        }
        return count
    }

    private suspend fun restoreKhataEntries(tenantId: String): Int {
        val snapshot = firestore.collection(BackupMapper.COL_KHATA_ENTRIES)
            .whereEqualTo("tenantId", tenantId)
            .get()
            .await()
        var count = 0
        for (doc in snapshot.documents) {
            val data = doc.data ?: continue
            val fields = RestoreMapper.khataEntryFromMap(data)
            khataEntryDao.insert(KhataEntryEntity(
                id = fields.id, tenantId = fields.tenantId, customerId = fields.customerId,
                amount = fields.amount, type = fields.type, description = fields.description,
                referenceBillId = fields.referenceBillId, collectedByUserId = fields.collectedByUserId,
                date = fields.date, idempotencyKey = fields.idempotencyKey,
            ))
            count++
        }
        return count
    }

    private suspend fun restoreExpenses(tenantId: String): Int {
        val snapshot = firestore.collection(BackupMapper.COL_EXPENSES)
            .whereEqualTo("tenantId", tenantId)
            .get()
            .await()
        var count = 0
        for (doc in snapshot.documents) {
            val data = doc.data ?: continue
            val fields = RestoreMapper.expenseFromMap(data)
            expenseDao.insert(ExpenseEntity(
                id = fields.id, tenantId = fields.tenantId, categoryId = fields.categoryId,
                amount = fields.amount, description = fields.description,
                expenseDate = fields.expenseDate, receiptPhotoPath = fields.receiptPhotoPath,
                userId = fields.userId, idempotencyKey = fields.idempotencyKey,
            ))
            count++
        }
        return count
    }

    private suspend fun restoreCashbookEntries(tenantId: String): Int {
        val snapshot = firestore.collection(BackupMapper.COL_CASHBOOK_ENTRIES)
            .whereEqualTo("tenantId", tenantId)
            .get()
            .await()
        var count = 0
        for (doc in snapshot.documents) {
            val data = doc.data ?: continue
            val fields = RestoreMapper.cashbookEntryFromMap(data)
            cashbookDao.insert(CashbookEntryEntity(
                id = fields.id, tenantId = fields.tenantId, account = fields.account,
                type = fields.type, amount = fields.amount, description = fields.description,
                referenceId = fields.referenceId, date = fields.date, userId = fields.userId,
                idempotencyKey = fields.idempotencyKey,
            ))
            count++
        }
        return count
    }

    private suspend fun restoreExpenseCategories(tenantId: String): Int {
        val snapshot = firestore.collection(BackupMapper.COL_EXPENSE_CATEGORIES)
            .whereEqualTo("tenantId", tenantId)
            .get()
            .await()
        var count = 0
        for (doc in snapshot.documents) {
            val data = doc.data ?: continue
            val fields = RestoreMapper.expenseCategoryFromMap(data)
            expenseCategoryDao.insert(ExpenseCategoryEntity(
                id = fields.id, tenantId = fields.tenantId, nameBn = fields.nameBn,
                icon = fields.icon, isActive = fields.isActive,
            ))
            count++
        }
        return count
    }

    private suspend fun restoreOwnerDrawings(tenantId: String): Int {
        val snapshot = firestore.collection(BackupMapper.COL_OWNER_DRAWINGS)
            .whereEqualTo("tenantId", tenantId)
            .get()
            .await()
        var count = 0
        for (doc in snapshot.documents) {
            val data = doc.data ?: continue
            val fields = RestoreMapper.ownerDrawingFromMap(data)
            ownerDrawingDao.insert(OwnerDrawingEntity(
                id = fields.id, tenantId = fields.tenantId, amount = fields.amount,
                description = fields.description, drawingDate = fields.drawingDate,
                userId = fields.userId, idempotencyKey = fields.idempotencyKey,
            ))
            count++
        }
        return count
    }
}
