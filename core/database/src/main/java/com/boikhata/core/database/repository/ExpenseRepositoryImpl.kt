package com.boikhata.core.database.repository

import androidx.room.withTransaction
import com.boikhata.core.database.BoiKhataDatabase
import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.database.dao.ExpenseCategoryDao
import com.boikhata.core.database.dao.ExpenseDao
import com.boikhata.core.database.dao.StockLedgerDao
import com.boikhata.core.database.entity.CashbookEntryEntity
import com.boikhata.core.database.entity.ExpenseEntity
import com.boikhata.core.database.entity.StockLedgerEntity
import com.boikhata.core.domain.accounting.PeriodLockChecker
import com.boikhata.core.domain.accounting.PurchaseRouter
import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.model.Expense
import com.boikhata.core.domain.model.ExpenseCategory
import com.boikhata.core.domain.repository.ExpenseRepository
import java.util.UUID
import javax.inject.Inject

/**
 * P3a: ExpenseRepository implementation.
 * D24: Book purchase → stock_ledger (PURCHASE), non-book → expense.
 * D25: Every money flow creates a cashbook entry (atomic).
 * D32: Period-lock check before write.
 */
class ExpenseRepositoryImpl @Inject constructor(
    private val db: BoiKhataDatabase,
    private val expenseDao: ExpenseDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
    private val stockLedgerDao: StockLedgerDao,
    private val cashbookDao: CashbookDao,
    private val writeGuard: LicenseWriteGuard,
    private val periodLockChecker: PeriodLockChecker,
) : ExpenseRepository {

    override suspend fun getCategories(tenantId: String): List<ExpenseCategory> {
        return expenseCategoryDao.getActiveByTenant(tenantId).map {
            ExpenseCategory(it.id, it.nameBn, it.icon, it.isActive)
        }
    }

    override suspend fun getExpenses(tenantId: String): List<Expense> {
        return expenseDao.getByTenant(tenantId).map { it.toDomain() }
    }

    override suspend fun getExpensesByDateRange(tenantId: String, start: Long, end: Long): List<Expense> {
        return expenseDao.getByDateRange(tenantId, start, end).map { it.toDomain() }
    }

    override suspend fun getExpensesByCategory(tenantId: String, categoryId: String): List<Expense> {
        return expenseDao.getByCategory(tenantId, categoryId).map { it.toDomain() }
    }

    override suspend fun getExpensesByCategoryAndUser(tenantId: String, categoryId: String, userId: String): List<Expense> {
        return expenseDao.getByCategoryAndUser(tenantId, categoryId, userId).map { it.toDomain() }
    }

    /**
     * D25: Add expense + cashbook EXPENSE entry in one atomic transaction.
     */
    override suspend fun addExpense(
        tenantId: String,
        categoryId: String,
        amount: Double,
        description: String,
        expenseDate: Long,
        receiptPhotoPath: String?,
        userId: String,
        cashbookAccount: CashbookAccount,
    ): String {
        writeGuard.assertWriteAllowed()
        // D32: Period-lock check
        periodLockChecker.assertNotLocked(tenantId, expenseDate)

        val expenseId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        db.withTransaction {
            // 1. Insert expense
            expenseDao.insert(
                ExpenseEntity(
                    id = expenseId,
                    tenantId = tenantId,
                    categoryId = categoryId,
                    amount = amount,
                    description = description,
                    expenseDate = expenseDate,
                    receiptPhotoPath = receiptPhotoPath,
                    userId = userId,
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            )
            // 2. D25: Auto-populate cashbook
            cashbookDao.insert(
                CashbookEntryEntity(
                    id = UUID.randomUUID().toString(),
                    tenantId = tenantId,
                    account = cashbookAccount.name,
                    type = "EXPENSE",
                    amount = amount,
                    description = description,
                    referenceId = expenseId,
                    date = now,
                    userId = userId,
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            )
        }
        return expenseId
    }

    /**
     * D24: Book purchase → stock_ledger (PURCHASE), NOT expense.
     * D25: Cashbook EXPENSE entry for the money outflow (atomic).
     */
    override suspend fun addBookPurchase(
        tenantId: String,
        bookId: String,
        quantity: Int,
        unitPrice: Double,
        description: String,
        userId: String,
        cashbookAccount: CashbookAccount,
    ): String {
        writeGuard.assertWriteAllowed()
        // D32: Period-lock check
        val now = System.currentTimeMillis()
        periodLockChecker.assertNotLocked(tenantId, now)

        val stockEntryId = UUID.randomUUID().toString()
        val cashbookEntryId = UUID.randomUUID().toString()
        val totalAmount = unitPrice * quantity

        db.withTransaction {
            // D24: Route to inventory (stock_ledger PURCHASE)
            stockLedgerDao.insert(
                StockLedgerEntity(
                    id = stockEntryId,
                    tenantId = tenantId,
                    bookId = bookId,
                    changeQuantity = quantity, // positive = stock in
                    reason = "PURCHASE",
                    referenceId = null,
                    userId = userId,
                    timestamp = now,
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            )
            // D25: Cashbook reflects money outflow
            cashbookDao.insert(
                CashbookEntryEntity(
                    id = cashbookEntryId,
                    tenantId = tenantId,
                    account = cashbookAccount.name,
                    type = "EXPENSE",
                    amount = totalAmount,
                    description = "বই ক্রয়: $description",
                    referenceId = stockEntryId,
                    date = now,
                    userId = userId,
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            )
        }
        return stockEntryId
    }

    private fun ExpenseEntity.toDomain(): Expense {
        // Category name is not stored on the expense row — the ViewModel resolves it
        return Expense(
            id = id,
            categoryId = categoryId,
            categoryNameBn = "", // resolved by ViewModel via getCategories
            amount = amount,
            description = description,
            expenseDate = expenseDate,
            receiptPhotoPath = receiptPhotoPath,
            userId = userId,
        )
    }
}
