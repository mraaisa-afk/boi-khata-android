package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.accounting.ReorderInsightCalculator.SaleLine
import com.boikhata.core.domain.accounting.ReorderInsightCalculator.Suggestion
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D55: Seasonal reorder insight — this-year vs last-year per publisher.
 */
class ReorderInsightCalculatorTest {

    @Test
    fun `should flag REORDER when this-year grew over 25 percent`() {
        val insights = ReorderInsightCalculator.compare(
            thisYear = listOf(SaleLine("রাইসা", 130), SaleLine("অন্যপ্রকাশ", 10)),
            lastYear = listOf(SaleLine("রাইসা", 100), SaleLine("অন্যপ্রকাশ", 5)),
        )
        val raisa = insights.first { it.publisher == "রাইসা" }
        assertThat(raisa.growthPercent).isEqualTo(30.0)
        assertThat(raisa.suggestion).isEqualTo(Suggestion.REORDER)
    }

    @Test
    fun `should flag HOLD for modest change`() {
        val insights = ReorderInsightCalculator.compare(
            thisYear = listOf(SaleLine("রাইসা", 110)),
            lastYear = listOf(SaleLine("রাইসা", 100)),
        )
        assertThat(insights.first { it.publisher == "রাইসา" }.suggestion).isEqualTo(Suggestion.HOLD)
    }

    @Test
    fun `should flag DROP when this-year fell over 25 percent`() {
        val insights = ReorderInsightCalculator.compare(
            thisYear = listOf(SaleLine("রাইসা", 60)),
            lastYear = listOf(SaleLine("রাইসা", 100)),
        )
        assertThat(insights.first { it.publisher == "রাইসা" }.suggestion).isEqualTo(Suggestion.DROP)
    }

    @Test
    fun `should flag NEW when this-year positive and last-year zero`() {
        val insights = ReorderInsightCalculator.compare(
            thisYear = listOf(SaleLine("নতুন পাবলিশার", 20)),
            lastYear = emptyList(),
        )
        assertThat(insights.first { it.publisher == "নতুন পাবলিশার" }.suggestion).isEqualTo(Suggestion.NEW)
    }

    @Test
    fun `should include publishers present only last year with DROP`() {
        val insights = ReorderInsightCalculator.compare(
            thisYear = emptyList(),
            lastYear = listOf(SaleLine("পুরনো", 100)),
        )
        assertThat(insights.first { it.publisher == "পুরনো" }.suggestion).isEqualTo(Suggestion.DROP)
        assertThat(insights.first { it.publisher == "পুরনো" }.growthPercent).isEqualTo(-100.0)
    }

    @Test
    fun `should suggest reorder quantity based on suggestion`() {
        val insight = ReorderInsightCalculator.ReorderInsight(
            publisher = "P", thisYearQty = 100, lastYearQty = 50,
            deltaQty = 50, growthPercent = 100.0, suggestion = Suggestion.REORDER,
        )
        assertThat(ReorderInsightCalculator.suggestReorderQty(insight)).isAtLeast(100)
        val drop = insight.copy(suggestion = Suggestion.DROP)
        assertThat(ReorderInsightCalculator.suggestReorderQty(drop)).isEqualTo(0)
    }
}
