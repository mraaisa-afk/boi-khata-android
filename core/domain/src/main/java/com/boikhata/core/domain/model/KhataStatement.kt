package com.boikhata.core.domain.model

import com.boikhata.core.domain.aging.AgingResult
import com.boikhata.core.domain.enums.KhataEntryType

/**
 * P2a: A single line in the khata statement (বাকি হিসাব).
 * D14: plain-text statement, WhatsApp-shareable.
 */
data class KhataStatementLine(
    val date: Long,
    val type: KhataEntryType,
    val amount: Double,
    val description: String,
    val runningBalance: Double,
)

/**
 * P2a: The full khata statement for a customer.
 */
data class KhataStatement(
    val customerName: String,
    val customerArea: String?,
    val lines: List<KhataStatementLine>,
    val totalDue: Double,
    val aging: AgingResult,
    val creditLimit: Double,
    val exceedsCreditLimit: Boolean,
)
