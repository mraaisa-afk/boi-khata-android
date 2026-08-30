package com.boikhata.core.domain.sale

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * D20: Bill number generator — INV-YYYYMMDD-NNNN format.
 * Date-prefixed, zero-padded sequence that resets daily.
 *
 * Pure function — no Android, no Room. The repository queries Room for
 * the max existing sequence for the date, then calls this generator.
 */
object BillNumberGenerator {

    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    /**
     * Generate the next bill number for the given date.
     * @param dateMillis the bill date (epoch-millis)
     * @param currentMaxSequence the highest existing sequence number for this date (0 if none)
     * @return bill number string like "INV-20260830-0001"
     */
    fun generate(dateMillis: Long, currentMaxSequence: Int): String {
        val datePart = dateFormat.format(Date(dateMillis))
        val sequence = currentMaxSequence + 1
        return "INV-$datePart-${String.format("%04d", sequence)}"
    }

    /**
     * Extract the sequence number from an existing bill number.
     * Returns 0 if the format doesn't match (defensive — shouldn't happen).
     */
    fun extractSequence(billNumber: String): Int {
        val parts = billNumber.split("-")
        if (parts.size != 3) return 0
        return parts[2].toIntOrNull() ?: 0
    }

    /**
     * Build the LIKE pattern for querying max bill number for a date.
     * E.g., for 2026-08-30 → "INV-20260830-%"
     */
    fun datePattern(dateMillis: Long): String {
        return "INV-${dateFormat.format(Date(dateMillis))}-%"
    }
}
