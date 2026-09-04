package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.BudgetDao
import com.boikhata.core.database.dao.ExpenseCategoryDao
import com.boikhata.core.database.dao.ExpenseDao
import com.boikhata.core.database.entity.BudgetEntity
import com.boikhata.core.domain.accounting.BengaliFiscalCalendar
import com.boikhata.core.domain.accounting.BudgetAlertCalculator
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.repository.BudgetRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D35: BudgetRepository implementation.
 * Monthly budget limits per category + alert computation.
 */
@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val expenseDao: ExpenseDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
    private val writeGuard: LicenseWriteGuard,
) : BudgetRepository {

    override suspend fun getBudgets(
        tenantId: String,
    ): List<BudgetAlertCalculator.Budget> {
        val categories = expenseCategoryDao.getActiveByTenant(tenantId)
            .associateBy { it.id }
        return budgetDao.getActiveByTenant(tenantId).map { entity ->
            BudgetAlertCalculator.Budget(
                categoryId = entity.categoryId,
                categoryNameBn = categories[entity.categoryId]?.nameBn ?: "",
                monthlyLimit = entity.monthlyLimit,
            )
        }
    }

    override suspend fun setBudget(
        tenantId: String,
        categoryId: String,
        monthlyLimit: Double,
    ): String {
        writeGuard.assertWriteAllowed()
        // Upsert: if a budget for this category exists, update it; else insert
        val existing = budgetDao.getByCategory(tenantId, categoryId)
        val id = existing?.id ?: UUID.randomUUID().toString()
        budgetDao.insert(
            BudgetEntity(
                id = id,
                tenantId = tenantId,
                categoryId = categoryId,
                monthlyLimit = monthlyLimit,
                isActive = true,
            )
        )
        return id
    }

    override suspend fun getMonthlyAlerts(
        tenantId: String,
        year: Int,
        month: Int,
    ): List<BudgetAlertCalculator.BudgetAlert> {
        val start = BengaliFiscalCalendar.gregorianMonthStart(year, month)
        val end = BengaliFiscalCalendar.gregorianMonthEndExclusive(year, month)
        val expenses = expenseDao.getByDateRange(tenantId, start, end)
            .map { com.boikhata.core.domain.model.Expense(
                id = it.id, categoryId = it.categoryId, categoryNameBn = "",
                amount = it.amount, description = it.description,
                expenseDate = it.expenseDate, receiptPhotoPath = it.receiptPhotoPath,
                userId = it.userId,
            ) }
        val actuals = BudgetAlertCalculator.aggregateByCategory(expenses)
        val budgets = getBudgets(tenantId)
        return BudgetAlertCalculator.computeAlerts(budgets, actuals, 0.8)
    }
}
