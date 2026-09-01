package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.BillDao
import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.database.dao.ExpenseCategoryDao
import com.boikhata.core.database.dao.ExpenseDao
import com.boikhata.core.domain.accounting.CashCloseCalculator
import com.boikhata.core.domain.accounting.CashbookBalanceCalculator
import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.enums.PaymentMethod
import com.boikhata.core.domain.model.CashCloseReport
import com.boikhata.core.domain.repository.CashCloseRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D36: CashCloseRepository implementation.
 * Reads the day's bills + expenses + cashbook balances and delegates to CashCloseCalculator.
 */
@Singleton
class CashCloseRepositoryImpl @Inject constructor(
    private val billDao: BillDao,
    private val expenseDao: ExpenseDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
    private val cashbookDao: CashbookDao,
) : CashCloseRepository {

    override suspend fun getDailyClose(
        tenantId: String,
        startOfDay: Long,
        endOfDay: Long,
        mfsFeeRate: Double,
        countedCash: Double,
    ): CashCloseReport {
        val bills = billDao.getByDateRange(tenantId, startOfDay, endOfDay)
        val expenses = expenseDao.getByDateRange(tenantId, startOfDay, endOfDay)
            .map { com.boikhata.core.domain.model.Expense(
                id = it.id, categoryId = it.categoryId, categoryNameBn = "",
                amount = it.amount, description = it.description,
                expenseDate = it.expenseDate, receiptPhotoPath = it.receiptPhotoPath,
                userId = it.userId,
            ) }
        val categories = expenseCategoryDao.getActiveByTenant(tenantId)
            .map { com.boikhata.core.domain.model.ExpenseCategory(it.id, it.nameBn, it.icon, it.isActive) }

        // System cash-in-hand: derived CASH balance from cashbook up to end of day
        val cashEntries = cashbookDao.getByDateRange(tenantId, 0, endOfDay)
            .map { com.boikhata.core.domain.model.CashbookEntry(
                id = it.id,
                account = CashbookAccount.valueOf(it.account),
                type = com.boikhata.core.domain.enums.CashbookEntryType.valueOf(it.type),
                amount = it.amount, description = it.description,
                referenceId = it.referenceId, date = it.date, userId = it.userId,
            ) }
        val cashBalance = CashbookBalanceCalculator.calculateBalance(cashEntries, CashbookAccount.CASH).balance

        val billsForClose = bills.map {
            CashCloseCalculator.BillForClose(
                paymentMethod = PaymentMethod.valueOf(it.paymentMethod),
                paidAmount = it.paidAmount,
                dueAmount = it.dueAmount,
            )
        }

        return CashCloseCalculator.compute(
            bills = billsForClose,
            expenses = expenses,
            expenseCategories = categories,
            cashbookCashBalance = cashBalance,
            countedCash = countedCash,
            mfsFeeRate = mfsFeeRate,
            date = startOfDay,
        )
    }
}
