package com.boikhata.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * D32: period_locks(id PK, tenantId, periodYear, periodMonth, lockedAt, lockedByUserId)
 * A locked period = immutable money tables for that year-month.
 * Read/export stays open on locked periods (never-lock rule).
 */
@Entity(tableName = "period_locks")
data class PeriodLockEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val periodYear: Int,
    val periodMonth: Int, // 1..12
    val lockedAt: Long,
    val lockedByUserId: String,
)

/**
 * D35: recurring_expenses(id PK, tenantId, categoryId, amount, description, frequency,
 * lastAppliedDate, nextDueDate, isActive, userId, createdAt)
 * A recurring-expense template that generates actual expense entries on apply.
 */
@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val categoryId: String,
    val amount: Double,
    val description: String,
    val frequency: String, // RecurringExpenseCalculator.Frequency name
    val lastAppliedDate: Long, // epoch-millis
    val nextDueDate: Long, // epoch-millis
    val isActive: Boolean,
    val userId: String,
    val createdAt: Long,
)

/**
 * D35: budgets(id PK, tenantId, categoryId, monthlyLimit, isActive)
 * A monthly budget limit per expense category.
 */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val categoryId: String,
    val monthlyLimit: Double,
    val isActive: Boolean,
)
