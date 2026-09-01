package com.boikhata.core.domain.accounting

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D29: PnLCalculator unit tests — COGS split (consignment vs purchase) + P&L computation.
 * The core accounting-correctness rule of this product.
 */
class PnLCalculatorTest {

    private fun line(bookId: String, qty: Int, price: Double, purchasePrice: Double) =
        PnLCalculator.BillLineForPnL(
            bookId = bookId,
            quantity = qty,
            unitPrice = price,
            lineTotal = price * qty,
            vatAmount = 0.0,
            bookPurchasePrice = purchasePrice,
        )

    private fun bill(subtotal: Double, discount: Double, vat: Double, lines: List<PnLCalculator.BillLineForPnL>) =
        PnLCalculator.BillForPnL(
            id = "b1",
            subtotal = subtotal,
            discountAmount = discount,
            vatAmount = vat,
            totalAmount = subtotal + vat - discount,
            paidAmount = subtotal + vat - discount,
            billDate = 0L,
            lines = lines,
        )

    @Test
    fun `should compute revenue from bills`() {
        val bills = listOf(bill(1000.0, 0.0, 0.0, listOf(line("bk1", 1, 1000.0, 600.0))))
        val pnl = PnLCalculator.compute(bills, 0.0, 0.0, 0.0, 2026, 9)
        assertThat(pnl.revenue).isEqualTo(1000.0)
        assertThat(pnl.netRevenue).isEqualTo(1000.0)
    }

    @Test
    fun `should subtract discount from net revenue`() {
        val bills = listOf(bill(1000.0, 100.0, 0.0, listOf(line("bk1", 1, 1000.0, 600.0))))
        val pnl = PnLCalculator.compute(bills, 0.0, 0.0, 0.0, 2026, 9)
        assertThat(pnl.revenue).isEqualTo(1000.0)
        assertThat(pnl.discountAmount).isEqualTo(100.0)
        assertThat(pnl.netRevenue).isEqualTo(900.0)
    }

    @Test
    fun `should compute purchase COGS as purchasePrice times quantity`() {
        // Sold 2 books at 500 each (revenue 1000), each bought at 300 (COGS 600)
        val bills = listOf(bill(1000.0, 0.0, 0.0, listOf(line("bk1", 2, 500.0, 300.0))))
        val pnl = PnLCalculator.compute(bills, 0.0, 0.0, 0.0, 2026, 9)
        assertThat(pnl.cogsPurchase).isEqualTo(600.0)
        assertThat(pnl.totalCogs).isEqualTo(600.0)
        assertThat(pnl.grossProfit).isEqualTo(400.0)
    }

    @Test
    fun `should split COGS into purchase and consignment`() {
        // One book bought at 300 (purchase), one consignment book (purchasePrice 0) with 50 commission
        val bills = listOf(bill(
            1000.0, 0.0, 0.0,
            listOf(
                line("bk1", 1, 500.0, 300.0),  // purchased — COGS 300
                line("bk2", 1, 500.0, 0.0),     // consignment — COGS 0 (commission separate)
            )
        ))
        val pnl = PnLCalculator.compute(bills, 0.0, 0.0, 50.0, 2026, 9)
        assertThat(pnl.cogsPurchase).isEqualTo(300.0)
        assertThat(pnl.cogsConsignment).isEqualTo(50.0)
        assertThat(pnl.totalCogs).isEqualTo(350.0)
        assertThat(pnl.grossProfit).isEqualTo(650.0) // 1000 - 350
    }

    @Test
    fun `should treat consignment book with zero purchasePrice as zero purchase COGS`() {
        val bills = listOf(bill(500.0, 0.0, 0.0, listOf(line("bk2", 1, 500.0, 0.0))))
        val pnl = PnLCalculator.compute(bills, 0.0, 0.0, 75.0, 2026, 9)
        assertThat(pnl.cogsPurchase).isEqualTo(0.0)
        assertThat(pnl.cogsConsignment).isEqualTo(75.0)
        assertThat(pnl.totalCogs).isEqualTo(75.0)
    }

    @Test
    fun `should subtract expenses from gross profit for net profit`() {
        val bills = listOf(bill(1000.0, 0.0, 0.0, listOf(line("bk1", 1, 1000.0, 600.0))))
        val pnl = PnLCalculator.compute(bills, 200.0, 0.0, 0.0, 2026, 9)
        assertThat(pnl.grossProfit).isEqualTo(400.0)
        assertThat(pnl.expenses).isEqualTo(200.0)
        assertThat(pnl.netProfit).isEqualTo(200.0)
    }

    @Test
    fun `should not subtract owner drawings from net profit`() {
        // Drawings are equity withdrawals, NOT expenses
        val bills = listOf(bill(1000.0, 0.0, 0.0, listOf(line("bk1", 1, 1000.0, 600.0))))
        val pnl = PnLCalculator.compute(bills, 200.0, 150.0, 0.0, 2026, 9)
        assertThat(pnl.netProfit).isEqualTo(200.0) // 400 gross - 200 expense
        assertThat(pnl.ownerDrawings).isEqualTo(150.0) // shown separately, not in netProfit
    }

    @Test
    fun `should compute margin percent from net profit and net revenue`() {
        val bills = listOf(bill(1000.0, 0.0, 0.0, listOf(line("bk1", 1, 1000.0, 600.0))))
        val pnl = PnLCalculator.compute(bills, 200.0, 0.0, 0.0, 2026, 9)
        // netProfit 200, netRevenue 1000 → 20%
        assertThat(pnl.marginPercent).isEqualTo(20.0)
    }

    @Test
    fun `should return zero margin when net revenue is zero`() {
        val bills = emptyList<PnLCalculator.BillForPnL>()
        val pnl = PnLCalculator.compute(bills, 0.0, 0.0, 0.0, 2026, 9)
        assertThat(pnl.marginPercent).isEqualTo(0.0)
        assertThat(pnl.netProfit).isEqualTo(0.0)
    }

    @Test
    fun `should sum VAT collected from all bills`() {
        val bills = listOf(
            bill(500.0, 0.0, 75.0, listOf(line("bk1", 1, 500.0, 300.0))),
            bill(300.0, 0.0, 45.0, listOf(line("bk2", 1, 300.0, 200.0))),
        )
        val pnl = PnLCalculator.compute(bills, 0.0, 0.0, 0.0, 2026, 9)
        assertThat(pnl.vatCollected).isEqualTo(120.0)
    }

    @Test
    fun `should aggregate COGS across multiple bills and lines`() {
        val bills = listOf(
            bill(800.0, 0.0, 0.0, listOf(
                line("bk1", 2, 400.0, 250.0),  // COGS 500
                line("bk2", 1, 0.0, 0.0).copy(lineTotal = 0.0, unitPrice = 0.0), // skip zero-line edge
            )),
            bill(200.0, 0.0, 0.0, listOf(line("bk3", 1, 200.0, 120.0))),  // COGS 120
        )
        // Filter the zero line out by computing on real lines only
        val realBills = bills.map { b ->
            b.copy(lines = b.lines.filter { it.lineTotal > 0 }, subtotal = b.lines.filter { it.lineTotal > 0 }.sumOf { it.lineTotal })
        }
        val pnl = PnLCalculator.compute(realBills, 0.0, 0.0, 0.0, 2026, 9)
        assertThat(pnl.cogsPurchase).isEqualTo(620.0) // 500 + 120
        assertThat(pnl.revenue).isEqualTo(1000.0) // 800 + 200
    }

    @Test
    fun `should label the report with dual calendar for September`() {
        val pnl = PnLCalculator.compute(emptyList(), 0.0, 0.0, 0.0, 2026, 9)
        assertThat(pnl.gregorianMonth).isEqualTo(9)
        assertThat(pnl.gregorianMonthNameBn).isEqualTo("সেপ্টেম্বর")
        // September → Bengali month 6 (Ashwin), FY 2026
        assertThat(pnl.bengaliMonth).isEqualTo(6)
        assertThat(pnl.bengaliMonthNameBn).isEqualTo("আশ্বিন")
        assertThat(pnl.bengaliFiscalYear).isEqualTo(2026)
    }

    @Test
    fun `should label the report with dual calendar for January`() {
        // January 2027 → Bengali month 10 (Magh), FY 2026
        val pnl = PnLCalculator.compute(emptyList(), 0.0, 0.0, 0.0, 2027, 1)
        assertThat(pnl.gregorianMonth).isEqualTo(1)
        assertThat(pnl.bengaliMonth).isEqualTo(10)
        assertThat(pnl.bengaliMonthNameBn).isEqualTo("মাঘ")
        assertThat(pnl.bengaliFiscalYear).isEqualTo(2026)
    }

    @Test
    fun `should produce PnL lines including the COGS split`() {
        val bills = listOf(bill(1000.0, 50.0, 0.0, listOf(line("bk1", 1, 1000.0, 600.0))))
        val pnl = PnLCalculator.compute(bills, 100.0, 0.0, 30.0, 2026, 9)
        val lines = pnl.toLines()
        val labels = lines.map { it.labelEn }
        assertThat(labels).containsAtLeast("Purchase COGS", "Consignment Commission", "Total COGS")
        assertThat(lines.size).isEqualTo(11)
    }
}
