package com.boikhata.core.domain.model

import com.boikhata.core.domain.enums.SupplierEntryType

/**
 * D51-D54: Supplier (দেনা/পাবলিশার payable ledger) domain models.
 * Pure data — no Room, no Android. Used by SupplierRepository + SupplierStatementBuilder.
 */

data class Supplier(
    val id: String,
    val tenantId: String,
    val nameBn: String,
    val phone: String?,
    val settlementCycle: String, // e.g. "৩০ দিন" — human label; days parsed for reminders
    val notes: String?,
)

/** A single supplier_entries row (append-only 🔒). */
data class SupplierEntry(
    val id: String,
    val tenantId: String,
    val supplierId: String,
    val amount: Double,
    val type: SupplierEntryType,
    val description: String,
    val referenceId: String?,
    val date: Long, // epoch-millis
)

/** D52: Supplier payable aging result (FIFO over supplier_entries). */
data class SupplierAgingResult(
    val totalPayable: Double,
    val oldestUnpaidDate: Long?, // epoch-millis of the oldest unpaid credit
    val ageDays: Long,
    val bucket: com.boikhata.core.domain.aging.AgingBucket,
    /** Remaining payable per entry after FIFO allocation (for display/trace). */
    val allocation: List<com.boikhata.core.domain.aging.EntryAllocation>,
)

/** Total supplier dues for a supplier — balance + aging + settlement-cycle reminder. */
data class SupplierBalance(
    val supplier: Supplier,
    val balance: Double,
    val ageDays: Long,
    val bucket: com.boikhata.core.domain.aging.AgingBucket,
    val overdueForDays: Long, // age - settlementCycleDays (0 if within cycle)
    val reminderDue: Boolean, // true when overdueForDays > 0
)

/** Aggregate supplier aging summary across all suppliers. */
data class SupplierAgingSummary(
    val totalPayable: Double,
    val supplierCount: Int,
    val greenBucket: Double, // <15d
    val yellowBucket: Double, // 15-30d
    val redBucket: Double, // >30d
)

/** D54: A supplier settlement statement (rendered as shareable text). */
data class SupplierStatement(
    val shopName: String,
    val supplier: Supplier,
    val startDate: Long?,
    val endDate: Long,
    val entries: List<SupplierStatementLine>,
    val totalPayable: Double,
    val ageDays: Long,
    val bucket: com.boikhata.core.domain.aging.AgingBucket,
)

/** A single line in the settlement statement with its running payable balance. */
data class SupplierStatementLine(
    val date: Long,
    val type: SupplierEntryType,
    val description: String,
    val amount: Double,
    val runningBalance: Double,
)
