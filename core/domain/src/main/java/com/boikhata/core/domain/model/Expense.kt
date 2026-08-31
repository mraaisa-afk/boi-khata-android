package com.boikhata.core.domain.model

import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.enums.CashbookEntryType

/**
 * P3a: Domain models for expense + cashbook + owner drawing.
 */

data class ExpenseCategory(
    val id: String,
    val nameBn: String,
    val icon: String,
    val isActive: Boolean,
)

data class Expense(
    val id: String,
    val categoryId: String,
    val categoryNameBn: String,
    val amount: Double,
    val description: String,
    val expenseDate: Long,
    val receiptPhotoPath: String?,
    val userId: String,
)

data class CashbookEntry(
    val id: String,
    val account: CashbookAccount,
    val type: CashbookEntryType,
    val amount: Double,
    val description: String,
    val referenceId: String?,
    val date: Long,
    val userId: String,
)

data class CashbookBalance(
    val account: CashbookAccount,
    val income: Double,
    val expense: Double,
    val balance: Double,
)

data class OwnerDrawing(
    val id: String,
    val amount: Double,
    val description: String,
    val drawingDate: Long,
    val userId: String,
)
