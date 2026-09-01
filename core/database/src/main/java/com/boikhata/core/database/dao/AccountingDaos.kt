package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boikhata.core.database.entity.BudgetEntity
import com.boikhata.core.database.entity.PeriodLockEntity
import com.boikhata.core.database.entity.RecurringExpenseEntity

// ── D32: Period Lock ──────────────────────────────────────────────────────────

@Dao
interface PeriodLockDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(lock: PeriodLockEntity)

    @Query("SELECT * FROM period_locks WHERE tenantId = :tenantId ORDER BY periodYear DESC, periodMonth DESC")
    suspend fun getByTenant(tenantId: String): List<PeriodLockEntity>

    @Query("SELECT * FROM period_locks WHERE tenantId = :tenantId AND periodYear = :year AND periodMonth = :month LIMIT 1")
    suspend fun findLock(tenantId: String, year: Int, month: Int): PeriodLockEntity?

    @Query("SELECT * FROM period_locks WHERE tenantId = :tenantId")
    suspend fun getAllForTenant(tenantId: String): List<PeriodLockEntity>
}

// ── D35: Recurring Expense ────────────────────────────────────────────────────

@Dao
interface RecurringExpenseDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(template: RecurringExpenseEntity)

    @Query("UPDATE recurring_expenses SET lastAppliedDate = :lastAppliedDate, nextDueDate = :nextDueDate WHERE id = :id")
    suspend fun updateAppliedDates(id: String, lastAppliedDate: Long, nextDueDate: Long)

    @Query("UPDATE recurring_expenses SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: String, isActive: Boolean)

    @Query("SELECT * FROM recurring_expenses WHERE tenantId = :tenantId AND isActive = 1 ORDER BY nextDueDate ASC")
    suspend fun getActiveByTenant(tenantId: String): List<RecurringExpenseEntity>

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getById(id: String): RecurringExpenseEntity?
}

// ── D35: Budget ───────────────────────────────────────────────────────────────

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE tenantId = :tenantId AND isActive = 1")
    suspend fun getActiveByTenant(tenantId: String): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE tenantId = :tenantId AND categoryId = :categoryId LIMIT 1")
    suspend fun getByCategory(tenantId: String, categoryId: String): BudgetEntity?

    @Query("UPDATE budgets SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: String, isActive: Boolean)
}
