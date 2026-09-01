package com.boikhata.core.database.repository

import androidx.room.withTransaction
import com.boikhata.core.database.BoiKhataDatabase
import com.boikhata.core.database.dao.ExpenseCategoryDao
import com.boikhata.core.database.dao.RecurringExpenseDao
import com.boikhata.core.database.entity.RecurringExpenseEntity
import com.boikhata.core.domain.accounting.PeriodLockChecker
import com.boikhata.core.domain.accounting.RecurringExpenseCalculator
import com.boikhata.core.domain.accounting.RecurringExpenseReminder
import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.model.RecurringExpenseTemplate
import com.boikhata.core.domain.repository.ExpenseRepository
import com.boikhata.core.domain.repository.RecurringExpenseRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D35: RecurringExpenseRepository implementation.
 * Persists recurring templates and applies them (creates actual expense entries).
 */
@Singleton
class RecurringExpenseRepositoryImpl @Inject constructor(
    private val db: BoiKhataDatabase,
    private val recurringExpenseDao: RecurringExpenseDao,
    private val expenseCategoryDao: ExpenseCategoryDao,
    private val expenseRepository: ExpenseRepository,
    private val writeGuard: LicenseWriteGuard,
    private val periodLockChecker: PeriodLockChecker,
) : RecurringExpenseRepository {

    override suspend fun getTemplates(tenantId: String): List<RecurringExpenseTemplate> {
        val categories = expenseCategoryDao.getActiveByTenant(tenantId)
            .associateBy { it.id }
        return recurringExpenseDao.getActiveByTenant(tenantId).map { entity ->
            RecurringExpenseTemplate(
                id = entity.id,
                tenantId = tenantId,
                categoryId = entity.categoryId,
                categoryNameBn = categories[entity.categoryId]?.nameBn ?: "",
                amount = entity.amount,
                description = entity.description,
                frequency = RecurringExpenseCalculator.Frequency.valueOf(entity.frequency),
                lastAppliedDate = entity.lastAppliedDate,
                nextDueDate = entity.nextDueDate,
                isActive = entity.isActive,
            )
        }
    }

    override suspend fun addTemplate(
        tenantId: String,
        categoryId: String,
        amount: Double,
        description: String,
        frequency: RecurringExpenseCalculator.Frequency,
        userId: String,
    ): String {
        writeGuard.assertWriteAllowed()
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        recurringExpenseDao.insert(
            RecurringExpenseEntity(
                id = id,
                tenantId = tenantId,
                categoryId = categoryId,
                amount = amount,
                description = description,
                frequency = frequency.name,
                lastAppliedDate = now, // start counting from now
                nextDueDate = RecurringExpenseCalculator.nextDueDate(frequency, now),
                isActive = true,
                userId = userId,
                createdAt = now,
            )
        )
        return id
    }

    override suspend fun applyTemplate(
        id: String,
        userId: String,
        cashbookAccount: CashbookAccount,
    ): String {
        writeGuard.assertWriteAllowed()
        val template = recurringExpenseDao.getById(id)
            ?: throw IllegalArgumentException("টেমপ্লেট পাওয়া যায়নি: $id")
        // D32: Period-lock check on the apply date
        val now = System.currentTimeMillis()
        periodLockChecker.assertNotLocked(template.tenantId, now)

        // Create the actual expense entry via ExpenseRepository (atomic with cashbook)
        val expenseId = expenseRepository.addExpense(
            tenantId = template.tenantId,
            categoryId = template.categoryId,
            amount = template.amount,
            description = "মাসিক: ${template.description}",
            expenseDate = now,
            receiptPhotoPath = null,
            userId = userId,
            cashbookAccount = cashbookAccount,
        )

        // Update the template's applied dates
        val nextDue = RecurringExpenseReminder.nextDueAfterApply(
            RecurringExpenseCalculator.Frequency.valueOf(template.frequency), now,
        )
        recurringExpenseDao.updateAppliedDates(id, now, nextDue)

        return expenseId
    }

    override suspend fun getDueTemplates(
        tenantId: String,
        now: Long,
    ): List<RecurringExpenseTemplate> {
        val templates = getTemplates(tenantId)
        return RecurringExpenseReminder.findDue(
            templates.map {
                RecurringExpenseReminder.RecurringTemplate(
                    id = it.id,
                    categoryId = it.categoryId,
                    categoryNameBn = it.categoryNameBn,
                    amount = it.amount,
                    description = it.description,
                    frequency = it.frequency,
                    lastAppliedDate = it.lastAppliedDate,
                    nextDueDate = it.nextDueDate,
                )
            },
            now,
        ).map { due ->
            templates.first { it.id == due.id }
        }
    }
}
