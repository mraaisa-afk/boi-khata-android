package com.boikhata.core.domain.model

import com.boikhata.core.domain.enums.PaymentMethod

/**
 * P2b: Full bill domain model with lines.
 * CONVENTIONS §3: bills table schema.
 */
data class Bill(
    val id: String,
    val billNumber: String,
    val customerId: String?,
    val customerNameBn: String,
    val customerPhone: String?,
    val userId: String,
    val subtotal: Double,
    val discountAmount: Double,
    val discountType: String, // PERCENTAGE or FIXED (D23)
    val vatAmount: Double,
    val totalAmount: Double,
    val paymentMethod: PaymentMethod,
    val paidAmount: Double,
    val dueAmount: Double,
    val khataEntryId: String?,
    val billDate: Long,
    val status: String, // COMPLETED, PARTIAL
)

/**
 * P2b: A single line in a bill.
 */
data class BillLine(
    val id: String,
    val billId: String,
    val bookId: String,
    val bookTitleBn: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double,
    val vatAmount: Double,
)
