package com.boikhata.core.domain.model

/**
 * P2a: Domain model for a khata installment (planned-future payment).
 * CONVENTIONS §3: khata_installments(id, tenantId, customerId, khataEntryId, dueDate, amount, isPaid)
 */
data class KhataInstallment(
    val id: String,
    val customerId: String,
    val khataEntryId: String,
    val dueDate: Long,
    val amount: Double,
    val isPaid: Boolean,
)
