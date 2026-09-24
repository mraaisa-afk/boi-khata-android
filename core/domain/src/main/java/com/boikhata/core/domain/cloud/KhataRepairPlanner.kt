package com.boikhata.core.domain.cloud

/**
 * D92 follow-up (2026-09-24 device round): restore-time khata integrity repair.
 *
 * bills and khata_entries restore independently (Firestore has no cross-collection
 * atomicity). A backup whose khata_entries snapshot predates a sale — or one taken
 * mid-upload — restores bills with dueAmount > 0 but no matching বাকি CREDIT entry,
 * silently understating the customer's মোট বাকি (device evidence: INV #0002's ৳400
 * remainder absent from ইলিয়াস's খাতা).
 *
 * Pure planner: returns the missing CREDIT entries to create. The caller
 * (RestoreRepositoryImpl) re-checks idempotency via the DAO count and inserts.
 */
object KhataRepairPlanner {

    /** A restored bill reduced to the fields the repair decision needs. */
    data class BillForRepair(
        val id: String,
        val billNumber: String,
        val customerId: String?,
        val dueAmount: Double,
        val billDate: Long,
    )

    /** One missing বাকি CREDIT entry to write. */
    data class PlannedEntry(
        val customerId: String,
        val amount: Double,
        val referenceBillId: String,
        val billNumber: String,
        val date: Long,
    )

    /**
     * A bill needs repair iff: due > 0.01, it has a customer, and NO khata entry
     * references it. Bills that are fully paid (due == 0) and walk-in bills
     * (customerId == null — pre-D93 these were rejected anyway) are skipped.
     */
    fun planMissingCreditEntries(
        bills: List<BillForRepair>,
        existingReferenceBillIds: Set<String>,
    ): List<PlannedEntry> {
        return bills
            .filter { it.dueAmount > 0.01 && it.customerId != null && it.id !in existingReferenceBillIds }
            .map { bill ->
                PlannedEntry(
                    customerId = bill.customerId!!,
                    amount = bill.dueAmount,
                    referenceBillId = bill.id,
                    billNumber = bill.billNumber,
                    date = bill.billDate,
                )
            }
    }
}
