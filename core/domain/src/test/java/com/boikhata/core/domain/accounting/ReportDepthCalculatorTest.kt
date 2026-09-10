package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.model.PnLReport
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReportDepthCalculatorTest {

    // ── 12-month trend ────────────────────────────────────────────────────────

    @Test
    fun `should aggregate sorted months with month over month change`() {
        val first = pnl(2026, 2, 100.0, 20.0, 10.0)
        val second = pnl(2026, 1, 50.0, 10.0, 5.0)
        val result = ReportDepthCalculator.twelveMonthTrend(listOf(first, second))
        assertThat(result.map { it.month }).containsExactly(1, 2).inOrder()
        assertThat(result[1].salesChangePercent).isEqualTo(100.0)
    }

    @Test
    fun `first month in trend should have zero change percent`() {
        val result = ReportDepthCalculator.twelveMonthTrend(listOf(pnl(2026, 1, 100.0, 20.0, 10.0)))
        assertThat(result[0].salesChangePercent).isEqualTo(0.0)
        assertThat(result[0].profitChangePercent).isEqualTo(0.0)
    }

    // ── top-10 rankings ───────────────────────────────────────────────────────

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
    fun `topBooks should respect limit parameter`() {
        val rows = (1..15).map { ReportDepthCalculator.RankedItem("বই $it", it, it * 100.0) }
        val result = ReportDepthCalculator.topBooks(rows, limit = 5)
        assertThat(result).hasSize(5)
    }

    // ── monthly copy trigger ─────────────────────────────────────────────────

    @Test
    fun `should generate only once on the first day of a month`() {
        assertThat(MonthlyCopyTrigger.shouldGenerate(1, 2026, 1, 2026, 1)).isFalse()
        assertThat(MonthlyCopyTrigger.shouldGenerate(1, 2025, 12, 2026, 1)).isTrue()
        assertThat(MonthlyCopyTrigger.shouldGenerate(2, 2025, 12, 2026, 1)).isFalse()
    }

    // ── P6 comparison ─────────────────────────────────────────────────────────

    @Test
    fun `compare should return 6 rows`() {
        val a = pnl(2026, 7, 100.0, 20.0, 10.0)
        val b = pnl(2026, 8, 120.0, 30.0, 8.0)
        val result = ReportDepthCalculator.compare(a, b)
        assertThat(result.rows).hasSize(6)
    }

    @Test
    fun `compare deltaPercent positive when B is higher`() {
        val a = pnl(2026, 7, 100.0, 20.0, 10.0)
        val b = pnl(2026, 8, 200.0, 40.0, 10.0)
        val result = ReportDepthCalculator.compare(a, b)
        val row = result.rows.first { it.labelBn == "নিট বিক্রি" }
        assertThat(row.deltaPercent).isEqualTo(100.0)
    }

    @Test
    fun `compare deltaPercent negative when B is lower`() {
        val a = pnl(2026, 8, 200.0, 40.0, 10.0)
        val b = pnl(2026, 9, 100.0, 20.0, 10.0)
        val result = ReportDepthCalculator.compare(a, b)
        val row = result.rows.first { it.labelBn == "নিট বিক্রি" }
        assertThat(row.deltaPercent).isEqualTo(-50.0)
    }

    @Test
    fun `compare deltaPercent zero when A is zero`() {
        val a = pnl(2026, 7, 0.0, 0.0, 0.0)
        val b = pnl(2026, 8, 100.0, 20.0, 10.0)
        val result = ReportDepthCalculator.compare(a, b)
        val row = result.rows.first { it.labelBn == "নিট বিক্রি" }
        assertThat(row.deltaPercent).isEqualTo(0.0)
    }

    @Test
    fun `compare same month produces zero deltas`() {
        val a = pnl(2026, 7, 150.0, 30.0, 20.0)
        val result = ReportDepthCalculator.compare(a, a)
        result.rows.forEach { row ->
            assertThat(row.deltaPercent).isEqualTo(0.0)
        }
    }

    @Test
    fun `compare labels contain year`() {
        val a = pnl(2026, 7, 100.0, 20.0, 10.0)
        val b = pnl(2026, 8, 120.0, 30.0, 8.0)
        val result = ReportDepthCalculator.compare(a, b)
        assertThat(result.labelA).contains("2026")
        assertThat(result.labelB).contains("2026")
    }

    // ── helper ────────────────────────────────────────────────────────────────

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
        marginPercent = if (sales > 0.0) profit / sales * 100.0 else 0.0,
    )
}
