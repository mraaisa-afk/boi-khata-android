package com.boikhata.core.domain.accounting

/**
 * D55: Seasonal reorder insight — last-year vs this-year per publisher/supplier.
 * Blueprint §7.5: "মৌসুমি রি-অর্ডার ইনসাইট (গত-বছর বনাম এ-বছর)।"
 *
 * Inputs are plain bill-line quantities grouped by publisher, so it is independently
 * unit-testable (no Room, no Android).
 */
object ReorderInsightCalculator {

    /** A single book-sale line for the insight (from a bill_line + its book's publisher). */
    data class SaleLine(
        val publisher: String,
        val quantity: Int,
    )

    /** The reorder suggestion for a publisher. */
    enum class Suggestion { REORDER, HOLD, DROP, NEW }

    /** A per-publisher reorder insight. */
    data class ReorderInsight(
        val publisher: String,
        val thisYearQty: Int,
        val lastYearQty: Int,
        val deltaQty: Int,
        val growthPercent: Double,
        val suggestion: Suggestion,
    )

    // Suggestion thresholds (percentage growth year-over-year).
    const val GROW_REORDER_THRESHOLD = 25.0
    const val GROW_DROP_THRESHOLD = -25.0

    /**
     * Compare this-year vs last-year sale lines grouped by publisher.
     * @param thisYear sales lines for the current season window
     * @param lastYear sales lines for the same window in the prior year
     */
    fun compare(thisYear: List<SaleLine>, lastYear: List<SaleLine>): List<ReorderInsight> {
        val thisMap = thisYear.groupBy { it.publisher }.mapValues { (_, lines) -> lines.sumOf { it.quantity } }
        val lastMap = lastYear.groupBy { it.publisher }.mapValues { (_, lines) -> lines.sumOf { it.quantity } }

        val publishers = (thisMap.keys + lastMap.keys).toSortedSet()

        return publishers.map { publisher ->
            val thisQty = thisMap[publisher] ?: 0
            val lastQty = lastMap[publisher] ?: 0
            val delta = thisQty - lastQty
            val growth = if (lastQty == 0) {
                if (thisQty > 0) 100.0 else 0.0
            } else {
                ((thisQty - lastQty).toDouble() / lastQty) * 100.0
            }
            val suggestion = when {
                lastQty == 0 && thisQty > 0 -> Suggestion.NEW
                growth >= GROW_REORDER_THRESHOLD -> Suggestion.REORDER
                growth <= GROW_DROP_THRESHOLD -> Suggestion.DROP
                else -> Suggestion.HOLD
            }
            ReorderInsight(
                publisher = publisher,
                thisYearQty = thisQty,
                lastYearQty = lastQty,
                deltaQty = delta,
                growthPercent = round1(growth),
                suggestion = suggestion,
            )
        }
    }

    /** Suggest a reorder quantity for a publisher given its history (simple heuristic). */
    fun suggestReorderQty(insight: ReorderInsight): Int {
        return when (insight.suggestion) {
            Suggestion.REORDER, Suggestion.NEW -> (insight.thisYearQty * 1.2).toInt().coerceAtLeast(1)
            Suggestion.HOLD -> insight.thisYearQty
            Suggestion.DROP -> 0
        }
    }

    private fun round1(v: Double): Double = Math.round(v * 10.0) / 10.0
}
