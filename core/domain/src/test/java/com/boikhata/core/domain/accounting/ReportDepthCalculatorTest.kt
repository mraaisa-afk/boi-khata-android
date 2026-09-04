package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.model.PnLReport
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReportDepthCalculatorTest {
    @Test
    fun `should aggregate sorted months with month over month change`() {
        val first = pnl(2026, 2, 100.0, 20.0, 10.0)
        val second = pnl(2026, 1, 50.0, 10.0, 5.0)
        val result = ReportDepthCalculator.twelveMonthTrend(listOf(first, second))
        assertThat(result.map { it.month }).containsExactly(1, 2).inOrder()
        assertThat(result[1].salesChangePercent).isEqualTo(100.0)
    }

    @Test
    fun `should rank and combine duplicate top ten rows`() {
        val result = ReportDepthCalculator.topBooks(
            listOf(
                ReportDepthCalculator.RankedItem("বাংলা", 2, 200.0),
                ReportDepthCalculator.RankedItem("বাংলা", 3, 300.0),
                ReportDepthCalculator.RankedItem("গণিত", 8, 800.0),
            ),
        )
        assertThat(result.first()).isEqualTo(ReportDepthCalculator.RankedItem("গণিত", 8, 800.0))
        assertThat(result[1]).isEqualTo(ReportDepthCalculator.RankedItem("বাংলা", 5, 500.0))
    }

    @Test
    fun `should generate only once on the first day of a month`() {
        assertThat(MonthlyCopyTrigger.shouldGenerate(1, 2026, 1, 2026, 1)).isFalse()
        assertThat(MonthlyCopyTrigger.shouldGenerate(1, 2025, 12, 2026, 1)).isTrue()
        assertThat(MonthlyCopyTrigger.shouldGenerate(2, 2025, 12, 2026, 1)).isFalse()
    }

    private fun pnl(year: Int, month: Int, sales: Double, profit: Double, expenses: Double) = PnLReport(
        gregorianYear = year,
        gregorianMonth = month,
        gregorianMonthNameBn = "মাস",
        bengaliFiscalYear = year,
        bengaliMonth = month,
        bengaliMonthNameBn = "মাস",
        revenue = sales,
        discountAmount = 0.0,
        netRevenue = sales,
        cogsPurchase = 0.0,
        cogsConsignment = 0.0,
        totalCogs = 0.0,
        grossProfit = profit,
        expenses = expenses,
        ownerDrawings = 0.0,
        vatCollected = 0.0,
        netProfit = profit,
        marginPercent = 0.0,
    )
}
