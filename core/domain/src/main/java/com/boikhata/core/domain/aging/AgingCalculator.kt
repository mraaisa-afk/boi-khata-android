package com.boikhata.core.domain.aging

import com.boikhata.core.domain.enums.KhataEntryType

/**
 * ARCH §4: Aging = FIFO — পেমেন্ট প্রাচীনতম CREDIT-এ কাটে;
 * প্রথম অপরিশোধিত CREDIT-এর তারিখ থেকে দিন।
 * Blueprint §7.4: 🟢 <১৫দি · 🟡 ১৫–৩০ · 🔴 >৩০।
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 */

data class KhataEntry(
    val id: String,
    val type: KhataEntryType,
    val amount: Double,
    val date: Long, // epoch-millis
)

data class AgingResult(
    val totalDue: Double,
    val oldestUnpaidDate: Long?,
    val ageDays: Long,
    val bucket: AgingBucket,
    /** Remaining balance per entry after FIFO allocation (for debugging/display). */
    val allocation: List<EntryAllocation>,
)

enum class AgingBucket { GREEN, YELLOW, RED, NONE }

data class EntryAllocation(
    val entryId: String,
    val originalAmount: Double,
    val remainingAfterAllocation: Double,
)

object AgingCalculator {

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    /**
     * Compute the due balance and aging for a customer's khata entries.
     *
     * FIFO allocation: payments (PAYMENT) + adjustments (ADJUSTMENT) reduce
     * the oldest CREDIT/OPENING first. OPENING is treated as an initial credit.
     *
     * Aging = days from the oldest entry that still has a remaining balance
     * to `now`. If nothing is due, bucket = NONE.
     */
    fun calculate(entries: List<KhataEntry>, now: Long): AgingResult {
        val sorted = entries.sortedBy { it.date }

        // Build credit buckets (oldest first): CREDIT and OPENING add to due;
        // PAYMENT reduces; ADJUSTMENT can be + or - (we treat amount sign as given:
        // a negative ADJUSTMENT reduces due, positive increases).
        val credits = mutableListOf<EntryAllocation>()
        for (e in sorted) {
            when (e.type) {
                KhataEntryType.CREDIT, KhataEntryType.OPENING -> credits.add(
                    EntryAllocation(e.id, e.amount, e.amount)
                )
                KhataEntryType.PAYMENT -> allocatePayment(credits, e.amount)
                KhataEntryType.ADJUSTMENT -> {
                    if (e.amount >= 0) credits.add(EntryAllocation(e.id, e.amount, e.amount))
                    else allocatePayment(credits, -e.amount)
                }
            }
        }

        val totalDue = credits.sumOf { it.remainingAfterAllocation }
        val oldestUnpaid = credits.firstOrNull { it.remainingAfterAllocation > 0.001 }

        val ageDays = if (oldestUnpaid != null) {
            ((now - sorted.first { it.id == oldestUnpaid.entryId }.date) / MILLIS_PER_DAY).coerceAtLeast(0)
        } else 0L

        val bucket = when {
            totalDue <= 0.001 -> AgingBucket.NONE
            ageDays < 15 -> AgingBucket.GREEN
            ageDays in 15..30 -> AgingBucket.YELLOW
            else -> AgingBucket.RED
        }

        return AgingResult(totalDue, oldestUnpaid?.let { findDate(sorted, it.entryId) }, ageDays, bucket, credits)
    }

    private fun allocatePayment(credits: MutableList<EntryAllocation>, paymentAmount: Double) {
        var remaining = paymentAmount
        for (i in credits.indices) {
            if (remaining <= 0.001) break
            val credit = credits[i]
            if (credit.remainingAfterAllocation <= 0.001) continue
            val applied = minOf(credit.remainingAfterAllocation, remaining)
            credits[i] = credit.copy(remainingAfterAllocation = credit.remainingAfterAllocation - applied)
            remaining -= applied
        }
    }

    private fun findDate(sorted: List<KhataEntry>, entryId: String): Long =
        sorted.first { it.id == entryId }.date
}
