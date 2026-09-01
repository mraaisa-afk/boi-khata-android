package com.boikhata.shared.receipt

import com.boikhata.core.domain.model.CashCloseReport
import com.boikhata.core.domain.model.ExpenseCategoryTotal
import com.boikhata.core.domain.model.SalesByMethod
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D36: CashCloseReportBuilder unit tests — WhatsApp-share text generation.
 */
class CashCloseReportBuilderTest {

    private fun fmt(d: Double) = "৳${d.toInt()}"

    private fun sampleReport(
        variance: Double = 0.0,
        varianceLabel: String = "মিলেছে",
        mfsFee: Double = 0.0,
        expenses: List<ExpenseCategoryTotal> = emptyList(),
    ) = CashCloseReport(
        date = 0L,
        dateLabelBn = "সেপ্টেম্বর",
        salesByMethod = SalesByMethod(cash = 5000.0, bkash = 3000.0, nagad = 0.0, credit = 1000.0, total = 9000.0),
        expensesByCategory = expenses,
        totalExpenses = expenses.sumOf { it.total },
        mfsFeeEstimated = mfsFee,
        mfsFeeRate = if (mfsFee > 0) 1.5 else 0.0,
        systemCashInHand = 5000.0,
        countedCash = 5000.0 - variance,
        variance = variance,
        varianceLabelBn = varianceLabel,
    )

    @Test
    fun `should include title and date`() {
        val text = CashCloseReportBuilder.buildCloseText(sampleReport(), ::fmt)
        assertThat(text).contains("আজকের হিসাব")
        assertThat(text).contains("তারিখ: সেপ্টেম্বর")
    }

    @Test
    fun `should include sales by method`() {
        val text = CashCloseReportBuilder.buildCloseText(sampleReport(), ::fmt)
        assertThat(text).contains("নগদ: ৳5000")
        assertThat(text).contains("বিকাশ: ৳3000")
        assertThat(text).contains("বাকি: ৳1000")
        assertThat(text).contains("মোট বিক্রি: ৳9000")
    }

    @Test
    fun `should show no expenses message when empty`() {
        val text = CashCloseReportBuilder.buildCloseText(sampleReport(), ::fmt)
        assertThat(text).contains("আজ কোনো খরচ নেই")
    }

    @Test
    fun `should list expenses by category when present`() {
        val expenses = listOf(
            ExpenseCategoryTotal("c1", "ভাড়া", 2000.0),
            ExpenseCategoryTotal("c2", "বিদ্যুৎ", 500.0),
        )
        val text = CashCloseReportBuilder.buildCloseText(sampleReport(expenses = expenses), ::fmt)
        assertThat(text).contains("ভাড়া: ৳2000")
        assertThat(text).contains("বিদ্যুৎ: ৳500")
        assertThat(text).contains("মোট খরচ: ৳2500")
    }

    @Test
    fun `should omit MFS fee section when fee is zero`() {
        val text = CashCloseReportBuilder.buildCloseText(sampleReport(mfsFee = 0.0), ::fmt)
        assertThat(text).doesNotContain("MFS-ফি")
    }

    @Test
    fun `should include MFS fee section when fee is non-zero`() {
        val text = CashCloseReportBuilder.buildCloseText(sampleReport(mfsFee = 45.0), ::fmt)
        assertThat(text).contains("MFS-ফি (আনুমানিক)")
        assertThat(text).contains("হার: 1.5%")
        assertThat(text).contains("ফি: ৳45")
    }

    @Test
    fun `should include cash variance section`() {
        val text = CashCloseReportBuilder.buildCloseText(sampleReport(), ::fmt)
        assertThat(text).contains("নগদ মিলান")
        assertThat(text).contains("হিসাব-অনুযায়ী: ৳5000")
        assertThat(text).contains("গোনা নগদ: ৳5000")
        assertThat(text).contains("মিলেছে")
    }

    @Test
    fun `should show variance label in report`() {
        val report = sampleReport(variance = 200.0, varianceLabel = "ঘাটতি")
        val text = CashCloseReportBuilder.buildCloseText(report, ::fmt)
        assertThat(text).contains("ঘাটতি")
    }
}
