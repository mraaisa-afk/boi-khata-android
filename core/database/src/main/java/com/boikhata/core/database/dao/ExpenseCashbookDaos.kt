package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boikhata.core.database.entity.CashbookEntryEntity
import com.boikhata.core.database.entity.ExpenseCategoryEntity
import com.boikhata.core.database.entity.ExpenseEntity
import com.boikhata.core.database.entity.OwnerDrawingEntity

@Dao
interface ExpenseCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: ExpenseCategoryEntity)

    @Query("SELECT * FROM expense_categories WHERE tenantId = :tenantId AND isActive = 1 ORDER BY nameBn")
    suspend fun getActiveByTenant(tenantId: String): List<ExpenseCategoryEntity>

    @Query("SELECT * FROM expense_categories WHERE id = :id")
    suspend fun getById(id: String): ExpenseCategoryEntity?
}

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE tenantId = :tenantId ORDER BY expenseDate DESC")
    suspend fun getByTenant(tenantId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE tenantId = :tenantId AND expenseDate >= :start AND expenseDate < :end ORDER BY expenseDate DESC")
    suspend fun getByDateRange(tenantId: String, start: Long, end: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE tenantId = :tenantId AND categoryId = :categoryId ORDER BY expenseDate DESC")
    suspend fun getByCategory(tenantId: String, categoryId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE tenantId = :tenantId AND userId = :userId AND categoryId = :categoryId ORDER BY expenseDate DESC")
    suspend fun getByCategoryAndUser(tenantId: String, categoryId: String, userId: String): List<ExpenseEntity>
}

@Dao
interface CashbookDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: CashbookEntryEntity)

    @Query("SELECT * FROM cashbook_entries WHERE tenantId = :tenantId ORDER BY date DESC")
    suspend fun getByTenant(tenantId: String): List<CashbookEntryEntity>

    @Query("SELECT * FROM cashbook_entries WHERE tenantId = :tenantId AND account = :account ORDER BY date DESC")
    suspend fun getByAccount(tenantId: String, account: String): List<CashbookEntryEntity>

    @Query("SELECT * FROM cashbook_entries WHERE tenantId = :tenantId AND date >= :start AND date < :end ORDER BY date DESC")
    suspend fun getByDateRange(tenantId: String, start: Long, end: Long): List<CashbookEntryEntity>
}

@Dao
interface OwnerDrawingDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(drawing: OwnerDrawingEntity)

    @Query("SELECT * FROM owner_drawings WHERE tenantId = :tenantId ORDER BY drawingDate DESC")
    suspend fun getByTenant(tenantId: String): List<OwnerDrawingEntity>

    @Query("SELECT * FROM owner_drawings WHERE tenantId = :tenantId AND drawingDate >= :start AND drawingDate < :end ORDER BY drawingDate DESC")
    suspend fun getByDateRange(tenantId: String, start: Long, end: Long): List<OwnerDrawingEntity>
}
