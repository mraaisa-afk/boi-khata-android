package com.boikhata.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** CONVENTIONS §3: expense_categories(id PK, tenantId, nameBn, icon, isActive) */
@Entity(tableName = "expense_categories")
data class ExpenseCategoryEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val nameBn: String,
    val icon: String,
    val isActive: Boolean,
)

/** CONVENTIONS §3: expenses(id PK, tenantId, categoryId, amount, description, expenseDate,
 *  receiptPhotoPath?, userId, idempotencyKey) */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val categoryId: String,
    val amount: Double,
    val description: String,
    val expenseDate: Long,
    val receiptPhotoPath: String?,
    val userId: String,
    val idempotencyKey: String,
)

/** CONVENTIONS §3 🔒: cashbook_entries(id PK, tenantId, account, type, amount, description,
 *  referenceId?, date, userId, idempotencyKey) — append-only */
@Entity(tableName = "cashbook_entries")
data class CashbookEntryEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val account: String,
    val type: String,
    val amount: Double,
    val description: String,
    val referenceId: String?,
    val date: Long,
    val userId: String,
    val idempotencyKey: String,
)

/** CONVENTIONS §3: owner_drawings(id PK, tenantId, amount, description, drawingDate, userId,
 *  idempotencyKey) */
@Entity(tableName = "owner_drawings")
data class OwnerDrawingEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val amount: Double,
    val description: String,
    val drawingDate: Long,
    val userId: String,
    val idempotencyKey: String,
)
