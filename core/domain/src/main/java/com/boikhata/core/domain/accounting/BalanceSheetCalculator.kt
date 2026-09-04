package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.model.BalanceSheetLite
import com.boikhata.core.domain.model.CashbookBalance

/**
 * D31: Balance-sheet lite calculator.
 * Blueprint §7.7: "ব্যালেন্স-শিট-লাইট" — bank/microfinance-loan-file ready.
 *
 * Assets = cash + inventory + receivables + ঘরি advances.
 * Liabilities = supplier payables (P5 scope, 0.0 for P3b).
 * Equity = retained earnings − owner drawings.
 * Accounting identity: Assets = Liabilities + Equity.
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 */
object BalanceSheetCalculator {

    /** A book's inventory position for balance-sheet valuation. */
    data class BookInventory(
        val bookId: String,
        val purchasePrice: Double,
        val stockQuantity: Int, // current on-hand quantity (sum of stock_ledger)
    )

    /**
     * Compute the lite balance sheet as of a point in time.
     *
     * @param cashbookBalances derived balances for all three accounts
     * @param inventory list of books with their current stock + purchasePrice
     * @param receivables total khata customer due balances
     * @param ghoriAdvances total ঘরি net advance balance (D26)
     * @param supplierPayables total supplier denā (0.0 for P3b — P5 scope)
     * @param retainedEarnings accumulated net profit (sum of all prior P&L netProfit)
     * @param totalDrawings accumulated owner drawings (all time)
     * @param asOfDate the snapshot date (epoch-millis)
     */
    fun compute(
        cashbookBalances: List<CashbookBalance>,
        inventory: List<BookInventory>,
        receivables: Double,
        ghoriAdvances: Double,
        supplierPayables: Double,
        retainedEarnings: Double,
        totalDrawings: Double,
        asOfDate: Long,
    ): BalanceSheetLite {
        val cash = cashbookBalances.sumOf { it.balance }
        val inventoryValue = inventory.sumOf { it.purchasePrice * it.stockQuantity }
        val totalAssets = cash + inventoryValue + receivables + ghoriAdvances

        val totalLiabilities = supplierPayables

        val totalEquity = retainedEarnings - totalDrawings

        val dateLabel = BengaliFiscalCalendar.gregorianMonthNameBnForDate(asOfDate)

        return BalanceSheetLite(
            asOfDate = asOfDate,
            gregorianDateLabelBn = dateLabel,
            cash = round2(cash),
            inventory = round2(inventoryValue),
            receivables = round2(receivables),
            ghoriAdvances = round2(ghoriAdvances),
            totalAssets = round2(totalAssets),
            supplierPayables = round2(supplierPayables),
            totalLiabilities = round2(totalLiabilities),
            retainedEarnings = round2(retainedEarnings),
            lessDrawings = round2(totalDrawings),
            totalEquity = round2(totalEquity),
        )
    }

    private fun round2(v: Double): Double {
        return Math.round(v * 100.0) / 100.0
    }
}
