package com.boikhata.shared.receipt

import com.boikhata.core.domain.accounting.ReportDepthCalculator

object ReportShareBuilder {
    fun buildTrend(points: List<ReportDepthCalculator.MonthPoint>): String = buildString {
        appendLine("বই খাতা — ১২ মাসের রিপোর্ট")
        points.forEach { point ->
            appendLine("${point.labelBn}: বিক্রি ${money(point.sales)}, লাভ ${money(point.profit)}, খরচ ${money(point.expenses)}")
        }
    }

    fun buildTopTen(title: String, rows: List<ReportDepthCalculator.RankedItem>): String = buildString {
        appendLine("বই খাতা — $title")
        rows.forEachIndexed { index, row ->
            appendLine("${index + 1}. ${row.label} — ${row.quantity}, ${money(row.amount)}")
        }
    }

    fun buildComparison(comparison: ReportDepthCalculator.MonthComparison): String = buildString {
        appendLine("বই খাতা — মাস তুলনা")
        appendLine("${comparison.labelA}  ←→  ${comparison.labelB}")
        appendLine("─────────────────────────────")
        comparison.rows.forEach { row ->
            val sign = when {
                row.deltaPercent > 0 -> "▲"
                row.deltaPercent < 0 -> "▼"
                else -> "="
            }
            appendLine("${row.labelBn}: ${money(row.valueA)} → ${money(row.valueB)}  $sign ${"%.1f".format(java.util.Locale.US, kotlin.math.abs(row.deltaPercent))}%")
        }
    }

    private fun money(amount: Double): String = "৳%.2f".format(java.util.Locale.US, amount)
}
