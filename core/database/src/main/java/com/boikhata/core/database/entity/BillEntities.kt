package com.boikhata.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** CONVENTIONS §3: bills(id PK, tenantId, billNumber, customerId?, customerNameBn, customerPhone?,
 *  userId, subtotal, discountAmount, discountType, vatAmount, totalAmount, paymentMethod,
 *  paidAmount, dueAmount, khataEntryId?, billDate, status, idempotencyKey) */
@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val billNumber: String,
    val customerId: String?,
    val customerNameBn: String,
    val customerPhone: String?,
    val userId: String,
    val subtotal: Double,
    val discountAmount: Double,
    val discountType: String,
    val vatAmount: Double,
    val totalAmount: Double,
    val paymentMethod: String,
    val paidAmount: Double,
    val dueAmount: Double,
    val khataEntryId: String?,
    val billDate: Long,
    val status: String,
    val idempotencyKey: String,
)

/** CONVENTIONS §3: bill_lines(id PK, tenantId, billId, bookId, bookTitleBn, quantity,
 *  unitPrice, lineTotal, vatAmount) */
@Entity(tableName = "bill_lines")
data class BillLineEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val billId: String,
    val bookId: String,
    val bookTitleBn: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double,
    val vatAmount: Double,
)
