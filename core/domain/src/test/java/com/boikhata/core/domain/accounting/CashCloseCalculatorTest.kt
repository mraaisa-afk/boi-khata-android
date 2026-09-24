package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.enums.PaymentMethod
import com.boikhata.core.domain.model.Expense
import com.boikhata.core.domain.model.ExpenseCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D36: CashCloseCalculator unit tests — MFS-fee estimation + variance computation.
 */
class CashCloseCalculatorTest {

    private fun bill(method: PaymentMethod, paid: Double, due: Double = 0.0) =
        CashCloseCalculator.BillForClose(method, paid, due)

    private fun expense(catId: String, amount: Double) = Expense(
        id = "e1", categoryId = catId, categoryNameBn = "", amount = amount,
        description = "", expenseDate = 0L, receiptPhotoPath = null, userId = "u1",
    )

    private fun category(id: String, name: String) = ExpenseCategory(id, name, "icon", true)

    @Test
    fun `should group sales by payment method`() {
        val bills = listOf(
            bill(PaymentMethod.CASH, 1000.0),
            bill(PaymentMethod.BKASH, 500.0),
            bill(PaymentMethod.CASH, 200.0),
            bill(PaymentMethod.CREDIT, 0.0, due = 300.0),
        )
        val report = CashCloseCalculator.compute(bills, emptyList(), emptyList(), 0.0, 0.0, 0.0, 0L)
        assertThat(report.salesByMethod.cash).isEqualTo(1200.0)
        assertThat(report.salesByMethod.bkash).isEqualTo(500.0)
        assertThat(report.salesByMethod.credit).isEqualTo(300.0)
        assertThat(report.salesByMethod.total).isEqualTo(2000.0)
    }

    @Test
    fun `should group expenses by category with names`() {
        val expenses = listOf(
            expense("c1", 3000.0),
            expense("c1", 2000.0),
            expense("c2", 1000.0),
        )
        val categories = listOf(category("c1", "ভাড়া"), category("c2", "বিদ্যুৎ"))
        val report = CashCloseCalculator.compute(emptyList(), expenses, categories, 0.0, 0.0, 0.0, 0L)
        assertThat(report.expensesByCategory).hasSize(2)
        assertThat(report.expensesByCategory[0].categoryNameBn).isEqualTo("ভাড়া")
        assertThat(report.expensesByCategory[0].total).isEqualTo(5000.0)
        assertThat(report.expensesByCategory[1].total).isEqualTo(1000.0)
        assertThat(report.totalExpenses).isEqualTo(6000.0)
    }

    @Test
    fun `should estimate MFS fee as bkash sales times rate over 100`() {
        val bills = listOf(bill(PaymentMethod.BKASH, 10000.0))
        val report = CashCloseCalculator.compute(bills, emptyList(), emptyList(), 0.0, 0.0, 1.5, 0L)
        // 10000 × 1.5 / 100 = 150
        assertThat(report.mfsFeeEstimated).isEqualTo(150.0)
        assertThat(report.mfsFeeRate).isEqualTo(1.5)
    }

    @Test
    fun `should return zero MFS fee when rate is zero`() {
        val bills = listOf(bill(PaymentMethod.BKASH, 10000.0))
        val report = CashCloseCalculator.compute(bills, emptyList(), emptyList(), 0.0, 0.0, 0.0, 0L)
        assertThat(report.mfsFeeEstimated).isEqualTo(0.0)
    }

    @Test
    fun `should not apply MFS fee to cash sales`() {
        val bills = listOf(bill(PaymentMethod.CASH, 10000.0))
        val report = CashCloseCalculator.compute(bills, emptyList(), emptyList(), 0.0, 0.0, 1.5, 0L)
        // Cash sales → no MFS fee
        assertThat(report.mfsFeeEstimated).isEqualTo(0.0)
    }

    @Test
    fun `should compute variance as system cash minus counted cash`() {
        val report = CashCloseCalculator.compute(emptyList(), emptyList(), emptyList(), 5000.0, 4800.0, 0.0, 0L)
        // system 5000, counted 4800 → variance 200 (short)
        assertThat(report.variance).isEqualTo(200.0)
    }

    @Test
    fun `should label positive variance as short`() {
        val report = CashCloseCalculator.compute(emptyList(), emptyList(), emptyList(), 5000.0, 4800.0, 0.0, 0L)
        assertThat(report.varianceLabelBn).isEqualTo("ঘাটতি")
    }

    @Test
    fun `should label negative variance as over`() {
        val report = CashCloseCalculator.compute(emptyList(), emptyList(), emptyList(), 4800.0, 5000.0, 0.0, 0L)
        // system 4800, counted 5000 → variance -200 (over)
        assertThat(report.variance).isEqualTo(-200.0)
        assertThat(report.varianceLabelBn).isEqualTo("বাড়তি")
    }

    @Test
    fun `should label zero variance as matched`() {
        val report = CashCloseCalculator.compute(emptyList(), emptyList(), emptyList(), 5000.0, 5000.0, 0.0, 0L)
        assertThat(report.variance).isEqualTo(0.0)
        assertThat(report.varianceLabelBn).isEqualTo("মিলেছে")
    }

    @Test
    fun `should produce report lines for sharing`() {
        val bills = listOf(bill(PaymentMethod.CASH, 1000.0))
        val report = CashCloseCalculator.compute(bills, emptyList(), emptyList(), 1000.0, 1000.0, 0.0, 0L)
        val lines = report.toLines()
        assertThat(lines.size).isEqualTo(12) // P12: + ব্যাংক + মোবাইল (অন্যান্য) rows
        assertThat(lines.map { it.labelEn }).containsAtLeast("Cash Sales", "Total Sales", "Variance")
    }

    @Test
    fun `should handle empty day with zero totals`() {
        val report = CashCloseCalculator.compute(emptyList(), emptyList(), emptyList(), 0.0, 0.0, 0.0, 0L)
        assertThat(report.salesByMethod.total).isEqualTo(0.0)
        assertThat(report.totalExpenses).isEqualTo(0.0)
        assertThat(report.mfsFeeEstimated).isEqualTo(0.0)
        assertThat(report.variance).isEqualTo(0.0)
        assertThat(report.varianceLabelBn).isEqualTo("মিলেছে")
    }

    // ── P12/D92: per-line salesByMethod aggregation ─────────────────────────

    @Test
    fun `bills with payment lines aggregate per line - splits land in the right buckets`() {
        val mixed = CashCloseCalculator.BillForClose(
            paymentMethod = PaymentMethod.MOBILE,
            paidAmount = 900.0,
            dueAmount = 100.0,
            lines = listOf(
                CashCloseCalculator.LineForClose("CASH", null, 600.0),
                CashCloseCalculator.LineForClose("MOBILE", "NAGAD", 300.0),
                CashCloseCalculator.LineForClose("DUE", null, 100.0),
            ),
        )
        val bankBill = CashCloseCalculator.BillForClose(
            paymentMethod = PaymentMethod.BANK,
            paidAmount = 250.0,
            dueAmount = 0.0,
            lines = listOf(CashCloseCalculator.LineForClose("BANK", null, 250.0)),
        )
        val report = CashCloseCalculator.compute(listOf(mixed, bankBill), emptyList(), emptyList(), 0.0, 0.0, 0.0, 0L)
        assertThat(report.salesByMethod.cash).isEqualTo(600.0)
        assertThat(report.salesByMethod.nagad).isEqualTo(300.0)
        assertThat(report.salesByMethod.bank).isEqualTo(250.0)
        assertThat(report.salesByMethod.credit).isEqualTo(100.0)
        assertThat(report.salesByMethod.total).isEqualTo(1250.0)
    }

    @Test
    fun `mobile lines on other providers land in mobileOther and bkash lines feed the fee estimate`() {
        val b = CashCloseCalculator.BillForClose(
            paymentMethod = PaymentMethod.MOBILE,
            paidAmount = 700.0,
            dueAmount = 0.0,
            lines = listOf(
                CashCloseCalculator.LineForClose("MOBILE", "BKASH", 400.0),
                CashCloseCalculator.LineForClose("MOBILE", "ROCKET", 200.0),
                CashCloseCalculator.LineForClose("MOBILE", "UPAY", 100.0),
            ),
        )
        val report = CashCloseCalculator.compute(listOf(b), emptyList(), emptyList(), 0.0, 0.0, 1.5, 0L)
        assertThat(report.salesByMethod.bkash).isEqualTo(400.0)
        assertThat(report.salesByMethod.mobileOther).isEqualTo(300.0)
        // fee still estimates on bKash sales only (disclosed in D92)
        assertThat(report.mfsFeeEstimated).isEqualTo(6.0)
    }

    @Test
    fun `legacy bills without lines still aggregate from the bill columns`() {
        val bills = listOf(bill(PaymentMethod.CASH, 1000.0), bill(PaymentMethod.CREDIT, 0.0, due = 300.0))
        val report = CashCloseCalculator.compute(bills, emptyList(), emptyList(), 0.0, 0.0, 0.0, 0L)
        assertThat(report.salesByMethod.cash).isEqualTo(1000.0)
        assertThat(report.salesByMethod.credit).isEqualTo(300.0)
        assertThat(report.salesByMethod.total).isEqualTo(1300.0)
    }
}
