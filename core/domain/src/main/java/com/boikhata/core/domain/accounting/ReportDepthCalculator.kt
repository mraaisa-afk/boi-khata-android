package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.model.PnLReport

/** Pure P6 report aggregation; all inputs originate from local Room reads. */
object ReportDepthCalculator {
    data class MonthPoint(
        val year: Int,
        val month: Int,
        val labelBn: String,
        val sales: Double,
        val profit: Double,
        val expenses: Double,
        val salesChangePercent: Double,
        val profitChangePercent: Double,
        val expenseChangePercent: Double,
    )

    data class RankedItem(val label: String, val quantity: Int, val amount: Double)

    fun twelveMonthTrend(reports: List<PnLReport>): List<MonthPoint> {
        val ordered = reports.sortedWith(compareBy<PnLReport> { it.gregorianYear }.thenBy { it.gregorianMonth })
        return ordered.mapIndexed { index, report ->
            val previous = ordered.getOrNull(index - 1)
            MonthPoint(
                year = report.gregorianYear,
                month = report.gregorianMonth,
                labelBn = "${report.gregorianMonthNameBn} ${report.gregorianYear}",
                sales = report.netRevenue,
                profit = report.netProfit,
                expenses = report.expenses,
                salesChangePercent = changePercent(previous?.netRevenue, report.netRevenue),
                profitChangePercent = changePercent(previous?.netProfit, report.netProfit),
                expenseChangePercent = changePercent(previous?.expenses, report.expenses),
            )
        }
    }

    fun topBooks(rows: List<RankedItem>, limit: Int = 10): List<RankedItem> = rank(rows, limit)

    fun topCustomers(rows: List<RankedItem>, limit: Int = 10): List<RankedItem> = rank(rows, limit)

    fun topExpenseCategories(rows: List<RankedItem>, limit: Int = 10): List<RankedItem> = rank(rows, limit)

    private fun rank(rows: List<RankedItem>, limit: Int): List<RankedItem> {
        require(limit > 0)
        return rows.groupBy { it.label }
            .map { (label, values) -> RankedItem(label, values.sumOf { it.quantity }, values.sumOf { it.amount }) }
            .sortedWith(compareByDescending<RankedItem> { it.quantity }.thenByDescending { it.amount }.thenBy { it.label })
            .take(limit)
    }

    private fun changePercent(previous: Double?, current: Double): Double {
        if (previous == null || kotlin.math.abs(previous) < 0.005) return 0.0
        return round2((current - previous) / kotlin.math.abs(previous) * 100.0)
    }

    private fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0
}

object MonthlyCopyTrigger {
    fun shouldGenerate(dayOfMonth: Int, lastGeneratedYear: Int?, lastGeneratedMonth: Int?, year: Int, month: Int): Boolean {
        if (dayOfMonth != 1) return false
        return lastGeneratedYear != year || lastGeneratedMonth != month
    }
}
