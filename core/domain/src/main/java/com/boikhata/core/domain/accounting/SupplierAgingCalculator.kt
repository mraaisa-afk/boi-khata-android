package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.aging.AgingBucket
import com.boikhata.core.domain.aging.EntryAllocation
import com.boikhata.core.domain.enums.SupplierEntryType
import com.boikhata.core.domain.model.Supplier
import com.boikhata.core.domain.model.SupplierAgingResult
import com.boikhata.core.domain.model.SupplierBalance
import com.boikhata.core.domain.model.SupplierEntry
import com.boikhata.core.domain.model.SupplierStatement
import com.boikhata.core.domain.model.SupplierStatementLine

/**
 * D52: Supplier payable aging — FIFO over supplier_entries, the mirror of the khata
 * AgingCalculator (which is for receivables). For a payable (দেনা), OPENING / CONSIGNMENT /
 * PURCHASE add to what the shop owes; PAYMENT reduces; ADJUSTMENT adds (positive) or reduces
 * (negative), allocated FIFO against the oldest remaining credit.
 *
 * Blueprint §7.4/§7.5: 🟢 <১৫দি · 🟡 ১৫–৩০ · 🔴 >৩০ (same three-bucket scheme as khata).
 * Pure function — no Android, no Room. Independently unit-testable.
 */
object SupplierAgingCalculator {

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    /** Parse a settlement-cycle label (e.g. "৩০ দিন", "30 days") into days; default 30 on failure. */
    fun settlementCycleDays(supplier: Supplier): Int {
        return parseDays(supplier.settlementCycle)
    }

    /** Parse a Bengali/Latin day count from a string. Returns 30 if not parseable. */
    fun parseDays(label: String): Int {
        val digits = label.filter { it.isDigit() || it in '০'..'৯' }
        if (digits.isEmpty()) return 30
        val value = digits.toEnglishDigits().toIntOrNull() ?: 30
        return value.coerceAtLeast(0)
    }

    /**
     * Compute the payable balance + aging for a supplier's entries.
     * FIFO: PAYMENT (and negative ADJUSTMENT) reduce the oldest OPENING/CONSIGNMENT/PURCHASE first.
     */
    fun calculate(entries: List<SupplierEntry>, now: Long): SupplierAgingResult {
        val sorted = entries.sortedBy { it.date }

        val credits = mutableListOf<EntryAllocation>()
        for (e in sorted) {
            when (e.type) {
                SupplierEntryType.OPENING,
                SupplierEntryType.CONSIGNMENT,
                SupplierEntryType.PURCHASE,
                -> credits.add(EntryAllocation(e.id, e.amount, e.amount))

                SupplierEntryType.PAYMENT -> allocatePayment(credits, e.amount)

                SupplierEntryType.ADJUSTMENT -> {
                    if (e.amount >= 0) credits.add(EntryAllocation(e.id, e.amount, e.amount))
                    else allocatePayment(credits, -e.amount)
                }
            }
        }

        val totalPayable = credits.sumOf { it.remainingAfterAllocation }
        val oldestUnpaid = credits.firstOrNull { it.remainingAfterAllocation > 0.001 }

        val ageDays = if (oldestUnpaid != null) {
            ((now - sorted.first { it.id == oldestUnpaid.entryId }.date) / MILLIS_PER_DAY).coerceAtLeast(0)
        } else 0L

        val bucket = when {
            totalPayable <= 0.001 -> AgingBucket.NONE
            ageDays < 15 -> AgingBucket.GREEN
            ageDays in 15..30 -> AgingBucket.YELLOW
            else -> AgingBucket.RED
        }

        return SupplierAgingResult(
            totalPayable = totalPayable,
            oldestUnpaidDate = oldestUnpaid?.let { findDate(sorted, it.entryId) },
            ageDays = ageDays,
            bucket = bucket,
            allocation = credits,
        )
    }

    /** Per-supplier balance with the settlement-cycle reminder flag. */
    fun supplierBalance(supplier: Supplier, entry: SupplierEntry, now: Long): SupplierBalance {
        val aging = calculate(listOf(entry), now)
        val cycleDays = settlementCycleDays(supplier)
        val overdue = (aging.ageDays - cycleDays).coerceAtLeast(0)
        return SupplierBalance(
            supplier = supplier,
            balance = aging.totalPayable,
            ageDays = aging.ageDays,
            bucket = aging.bucket,
            overdueForDays = overdue,
            reminderDue = aging.totalPayable > 0.001 && overdue > 0,
        )
    }

    /** Aggregate across multiple suppliers. */
    fun summarize(balances: List<SupplierBalance>): SupplierAgingSummary {
        var green = 0.0
        var yellow = 0.0
        var red = 0.0
        for (b in balances) {
            when (b.bucket) {
                AgingBucket.GREEN -> green += b.balance
                AgingBucket.YELLOW -> yellow += b.balance
                AgingBucket.RED -> red += b.balance
                AgingBucket.NONE -> { /* no payable */ }
            }
        }
        return SupplierAgingSummary(
            totalPayable = balances.sumOf { it.balance },
            supplierCount = balances.count { it.balance > 0.001 },
            greenBucket = green,
            yellowBucket = yellow,
            redBucket = red,
        )
    }

    /**
     * Build a supplier settlement statement with a running payable balance per entry.
     * @param filteredEntries entries sorted by date (the caller filters the date range)
     */
    fun buildStatement(
        shopName: String,
        supplier: Supplier,
        entries: List<SupplierEntry>,
        startDate: Long?,
        endDate: Long,
    ): SupplierStatement {
        val sorted = entries.sortedBy { it.date }
        var running = 0.0
        val lines = sorted.map { e ->
            val delta = when (e.type) {
                SupplierEntryType.PAYMENT -> -e.amount
                SupplierEntryType.ADJUSTMENT -> if (e.amount >= 0) e.amount else e.amount
                else -> e.amount
            }
            running += delta
            SupplierStatementLine(
                date = e.date,
                type = e.type,
                description = e.description,
                amount = e.amount,
                runningBalance = running,
            )
        }
        val aging = calculate(sorted, endDate)
        return SupplierStatement(
            shopName = shopName,
            supplier = supplier,
            startDate = startDate,
            endDate = endDate,
            entries = lines,
            totalPayable = aging.totalPayable,
            ageDays = aging.ageDays,
            bucket = aging.bucket,
        )
    }

    // ── helpers ────────────────────────────────────────────────────────────
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

    private fun findDate(sorted: List<SupplierEntry>, entryId: String): Long =
        sorted.first { it.id == entryId }.date

    private fun String.toEnglishDigits(): String = buildString(length) {
        this@toEnglishDigits.forEach { ch ->
            append(
                when (ch) {
                    '০' -> '0'; '১' -> '1'; '২' -> '2'; '৩' -> '3'; '৪' -> '4'
                    '৫' -> '5'; '৬' -> '6'; '৭' -> '7'; '৮' -> '8'; '৯' -> '9'
                    else -> ch
                }
            )
        }
    }
}
