package com.boikhata.core.domain.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D92 follow-up (2026-09-24 device round, Issues 3/4): restore integrity repair.
 *
 * bills and khata_entries restore INDEPENDENTLY (Firestore has no cross-collection
 * atomicity; BackupRepositoryImpl uploads sequentially). A backup taken between
 * two collection uploads — or one whose khata_entries snapshot predates a sale —
 * restores bills with dueAmount > 0 but NO matching বাকি CREDIT entry. On the
 * owner's device this surfaced as: INV #0002 মোট ৳1900 / জমা ৳1500 / বাকি ৳400
 * with the ৳400 never posted to ইলিয়াস's খাতা (মোট বাকি ৳1660 instead of ৳2060).
 *
 * The planner is PURE: given restored bills + the referenceBillIds already present
 * in khata_entries, return the missing CREDIT entries to create (idempotent).
 */
class KhataRepairPlannerTest {

    private fun bill(
        id: String,
        due: Double,
        customerId: String? = "c1",
        billNumber: String = "INV-20260924-0001",
        date: Long = 1_700_000_000_000,
    ) = KhataRepairPlanner.BillForRepair(
        id = id, billNumber = billNumber, customerId = customerId,
        dueAmount = due, billDate = date,
    )

    @Test
    fun `restored bill with due and customer but no matching credit entry is planned for repair`() {
        val plan = KhataRepairPlanner.planMissingCreditEntries(
            bills = listOf(bill("b1", due = 400.0)),
            existingReferenceBillIds = emptySet(),
        )
        assertThat(plan).hasSize(1)
        val entry = plan.single()
        assertThat(entry.customerId).isEqualTo("c1")
        assertThat(entry.amount).isEqualTo(400.0)
        assertThat(entry.referenceBillId).isEqualTo("b1")
        assertThat(entry.date).isEqualTo(1_700_000_000_000)
    }

    @Test
    fun `bill whose credit entry already exists is skipped - idempotent repair`() {
        val plan = KhataRepairPlanner.planMissingCreditEntries(
            bills = listOf(bill("b1", due = 400.0)),
            existingReferenceBillIds = setOf("b1"),
        )
        assertThat(plan).isEmpty()
    }

    @Test
    fun `fully-paid bills and walk-in bills are skipped`() {
        val plan = KhataRepairPlanner.planMissingCreditEntries(
            bills = listOf(
                bill("b1", due = 0.0),                    // fully paid — nothing to post
                bill("b2", due = 500.0, customerId = null), // walk-in — no khata target
            ),
            existingReferenceBillIds = emptySet(),
        )
        assertThat(plan).isEmpty()
    }

    @Test
    fun `mixed batch repairs only the damaged bills`() {
        val plan = KhataRepairPlanner.planMissingCreditEntries(
            bills = listOf(
                bill("b1", due = 400.0),                                  // damaged → repair
                bill("b2", due = 800.0, customerId = "c2"),               // intact → skip
                bill("b3", due = 60.0),                                   // damaged → repair
            ),
            existingReferenceBillIds = setOf("b2"),
        )
        assertThat(plan.map { it.referenceBillId }).containsExactly("b1", "b3").inOrder()
        assertThat(plan.sumOf { it.amount }).isEqualTo(460.0)
    }
}
