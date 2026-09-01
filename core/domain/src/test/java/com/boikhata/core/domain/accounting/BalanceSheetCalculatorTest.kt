package com.boikhata.core.domain.accounting

import com.google.common.truth.Truth.assertThat
import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.enums.CashbookEntryType
import com.boikhata.core.domain.model.CashbookBalance
import org.junit.Test

/**
 * D31: BalanceSheetCalculator unit tests — lite balance sheet, accounting identity.
 */
class BalanceSheetCalculatorTest {

    private fun balance(account: CashbookAccount, bal: Double) = CashbookBalance(
        account = account,
        income = bal,
        expense = 0.0,
        balance = bal,
    )

    @Test
    fun `should compute cash as sum of all account balances`() {
        val balances = listOf(
            balance(CashbookAccount.CASH, 5000.0),
            balance(CashbookAccount.BKASH, 3000.0),
            balance(CashbookAccount.BANK, 2000.0),
        )
        val bs = BalanceSheetCalculator.compute(balances, emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, 0L)
        assertThat(bs.cash).isEqualTo(10000.0)
    }

    @Test
    fun `should compute inventory as purchasePrice times stockQuantity`() {
        val inventory = listOf(
            BalanceSheetCalculator.BookInventory("bk1", 100.0, 10),  // 1000
            BalanceSheetCalculator.BookInventory("bk2", 50.0, 4),    // 200
        )
        val bs = BalanceSheetCalculator.compute(emptyList(), inventory, 0.0, 0.0, 0.0, 0.0, 0.0, 0L)
        assertThat(bs.inventory).isEqualTo(1200.0)
    }

    @Test
    fun `should compute total assets as cash plus inventory plus receivables plus ghori`() {
        val balances = listOf(balance(CashbookAccount.CASH, 5000.0))
        val inventory = listOf(BalanceSheetCalculator.BookInventory("bk1", 100.0, 10))
        val bs = BalanceSheetCalculator.compute(
            balances, inventory,
            receivables = 2000.0,
            ghoriAdvances = 500.0,
            supplierPayables = 0.0,
            retainedEarnings = 0.0,
            totalDrawings = 0.0,
            asOfDate = 0L,
        )
        assertThat(bs.totalAssets).isEqualTo(8500.0) // 5000 + 1000 + 2000 + 500
    }

    @Test
    fun `should set supplier payables as liabilities`() {
        val bs = BalanceSheetCalculator.compute(
            emptyList(), emptyList(), 0.0, 0.0,
            supplierPayables = 3000.0, retainedEarnings = 0.0, totalDrawings = 0.0, asOfDate = 0L,
        )
        assertThat(bs.totalLiabilities).isEqualTo(3000.0)
    }

    @Test
    fun `should compute equity as retained earnings minus drawings`() {
        val bs = BalanceSheetCalculator.compute(
            emptyList(), emptyList(), 0.0, 0.0, 0.0,
            retainedEarnings = 10000.0, totalDrawings = 3000.0, asOfDate = 0L,
        )
        assertThat(bs.retainedEarnings).isEqualTo(10000.0)
        assertThat(bs.lessDrawings).isEqualTo(3000.0)
        assertThat(bs.totalEquity).isEqualTo(7000.0)
    }

    @Test
    fun `should satisfy accounting identity when balanced`() {
        // Assets = 10000, Liabilities = 3000, Equity = 7000 → 10000 = 3000 + 7000
        val balances = listOf(balance(CashbookAccount.CASH, 10000.0))
        val bs = BalanceSheetCalculator.compute(
            balances, emptyList(), 0.0, 0.0,
            supplierPayables = 3000.0, retainedEarnings = 10000.0, totalDrawings = 3000.0,
            asOfDate = 0L,
        )
        assertThat(bs.isBalanced()).isTrue()
    }

    @Test
    fun `should detect unbalanced sheet`() {
        val balances = listOf(balance(CashbookAccount.CASH, 5000.0))
        val bs = BalanceSheetCalculator.compute(
            balances, emptyList(), 0.0, 0.0,
            supplierPayables = 3000.0, retainedEarnings = 10000.0, totalDrawings = 3000.0,
            asOfDate = 0L,
        )
        // Assets 5000, Liab+Equity = 3000 + 7000 = 10000 → not balanced
        assertThat(bs.isBalanced()).isFalse()
    }

    @Test
    fun `should produce asset liability and equity line lists`() {
        val bs = BalanceSheetCalculator.compute(
            listOf(balance(CashbookAccount.CASH, 1000.0)),
            listOf(BalanceSheetCalculator.BookInventory("bk1", 100.0, 5)),
            receivables = 500.0, ghoriAdvances = 200.0,
            supplierPayables = 300.0, retainedEarnings = 2000.0, totalDrawings = 800.0,
            asOfDate = 0L,
        )
        assertThat(bs.assetLines().size).isEqualTo(5)
        assertThat(bs.liabilityLines().size).isEqualTo(2)
        assertThat(bs.equityLines().size).isEqualTo(3)
    }

    @Test
    fun `should label the snapshot with gregorian month name`() {
        val bs = BalanceSheetCalculator.compute(
            emptyList(), emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0,
            asOfDate = BengaliFiscalCalendar.gregorianMonthStart(2026, 9),
        )
        assertThat(bs.gregorianDateLabelBn).isEqualTo("সেপ্টেম্বর")
    }
}
