package com.boikhata.shared.receipt

import com.boikhata.core.domain.model.BalanceSheetLite
import com.boikhata.core.domain.model.HisabPack
import com.boikhata.core.domain.model.KhataAgingSummary
import com.boikhata.core.domain.model.PnLReport
import com.boikhata.core.domain.model.VatSummary
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D33: HisabPackBuilder unit tests — হিসাব-প্যাক text report generation.
 */
class HisabPackBuilderTest {

    private fun fmt(d: Double) = "৳${d.toInt()}"

    private fun samplePnL() = PnLReport(
        gregorianYear = 2026, gregorianMonth = 9,
        gregorianMonthNameBn = "সেপ্টেম্বর",
        bengaliFiscalYear = 2026, bengaliMonth = 6,
        bengaliMonthNameBn = "আশ্বিন",
        revenue = 100000.0, discountAmount = 5000.0, netRevenue = 95000.0,
        cogsPurchase = 40000.0, cogsConsignment = 5000.0, totalCogs = 45000.0,
        grossProfit = 50000.0, expenses = 20000.0, ownerDrawings = 10000.0,
        vatCollected = 3000.0, netProfit = 30000.0, marginPercent = 31.6,
    )

    private fun sampleBalanceSheet() = BalanceSheetLite(
        asOfDate = 0L, gregorianDateLabelBn = "সেপ্টেম্বর",
        cash = 50000.0, inventory = 80000.0, receivables = 30000.0,
        ghoriAdvances = 5000.0, totalAssets = 165000.0,
        supplierPayables = 0.0, totalLiabilities = 0.0,
        retainedEarnings = 100000.0, lessDrawings = 35000.0, totalEquity = 65000.0,
    )

    private fun sampleAging() = KhataAgingSummary(
        totalDue = 30000.0, greenBucket = 10000.0, yellowBucket = 15000.0,
        redBucket = 5000.0, customerCount = 12,
    )

    private fun sampleVat() = VatSummary(
        booksVat = 0.0, stationeryVat = 3000.0, totalVat = 3000.0,
    )

    private fun samplePack() = HisabPack(
        shopName = "রাইসা বুক স্টোর",
        pnl = samplePnL(),
        balanceSheet = sampleBalanceSheet(),
        agingSummary = sampleAging(),
        vatSummary = sampleVat(),
    )

    @Test
    fun `should include shop name and title`() {
        val text = HisabPackBuilder.buildPackText(samplePack(), ::fmt)
        assertThat(text).contains("রাইসা বুক স্টোর")
        assertThat(text).contains("মাসিক হিসাব-প্যাক")
    }

    @Test
    fun `should include dual calendar header`() {
        val text = HisabPackBuilder.buildPackText(samplePack(), ::fmt)
        assertThat(text).contains("গ্রেগরিয়ান: সেপ্টেম্বর 2026")
        assertThat(text).contains("বাংলা বর্ষ: আশ্বিন")
        assertThat(text).contains("FY 2026-2027")
    }

    @Test
    fun `should include PnL with COGS split lines`() {
        val text = HisabPackBuilder.buildPackText(samplePack(), ::fmt)
        assertThat(text).contains("লাভ-ক্ষতি হিসাব")
        assertThat(text).contains("মোট বিক্রি: ৳100000")
        assertThat(text).contains("ক্রয়-COGS")
        assertThat(text).contains("কনসাইনমেন্ট-কমিশন")
        assertThat(text).contains("নিট লাভ: ৳30000")
    }

    @Test
    fun `should include balance sheet section`() {
        val text = HisabPackBuilder.buildPackText(samplePack(), ::fmt)
        assertThat(text).contains("ব্যালেন্স-শিট")
        assertThat(text).contains("মোট সম্পদ: ৳165000")
        assertThat(text).contains("মোট ইক্যুইটি: ৳65000")
    }

    @Test
    fun `should include khata aging summary`() {
        val text = HisabPackBuilder.buildPackText(samplePack(), ::fmt)
        assertThat(text).contains("খাতা এজিং সারসংক্ষেপ")
        assertThat(text).contains("মোট বাকি: ৳30000")
        assertThat(text).contains("মোট ক্রেতা: 12")
    }

    @Test
    fun `should include VAT summary`() {
        val text = HisabPackBuilder.buildPackText(samplePack(), ::fmt)
        assertThat(text).contains("ভ্যাট সারসংক্ষেপ")
        assertThat(text).contains("বই (০%)")
        assertThat(text).contains("স্টেশনারি (১৫%)")
        assertThat(text).contains("মোট ভ্যাট: ৳3000")
    }

    @Test
    fun `should include loan-file-ready footer`() {
        val text = HisabPackBuilder.buildPackText(samplePack(), ::fmt)
        assertThat(text).contains("ব্যাংক/মাইক্রোফাইন্যান্স লোন-ফাইল-রেডি")
    }

    @Test
    fun `should omit discount line when discount is zero`() {
        val pack = samplePack().copy(
            pnl = samplePnL().copy(discountAmount = 0.0, netRevenue = 100000.0)
        )
        val text = HisabPackBuilder.buildPackText(pack, ::fmt)
        assertThat(text).doesNotContain("ছাড়:")
    }

    @Test
    fun `should omit supplier payables when zero`() {
        val text = HisabPackBuilder.buildPackText(samplePack(), ::fmt)
        // supplierPayables = 0 → the line should not appear
        assertThat(text).doesNotContain("সাপ্লায়ার দেনা:")
    }
}
