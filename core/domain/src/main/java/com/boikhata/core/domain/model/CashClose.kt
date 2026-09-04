package com.boikhata.core.domain.model

/**
 * D36: Cash-close "আজকের হিসাব" domain models.
 * Blueprint §7.6: daily summary + MFS-fee auto-line + variance.
 */

/** Sales broken down by payment method. */
data class SalesByMethod(
    val cash: Double,
    val bkash: Double,
    val nagad: Double,
    val credit: Double, // sales on khata (due)
    val total: Double,
)

/** One expense category total for the day. */
data class ExpenseCategoryTotal(
    val categoryId: String,
    val categoryNameBn: String,
    val total: Double,
)

/** The complete daily cash-close report. */
data class CashCloseReport(
    val date: Long,
    val dateLabelBn: String,
    val salesByMethod: SalesByMethod,
    val expensesByCategory: List<ExpenseCategoryTotal>,
    val totalExpenses: Double,
    val mfsFeeEstimated: Double, // BKASH sales × rate / 100
    val mfsFeeRate: Double, // the rate used (percentage, owner-overridable)
    val systemCashInHand: Double, // derived from cashbook (CASH balance)
    val countedCash: Double, // owner's physical count (input)
    val variance: Double, // systemCashInHand − countedCash
    val varianceLabelBn: String, // "ঘাটতি" (short) or "বাড়তি" (over) or "মিলেছে" (matched)
) {
    fun toLines(): List<PnLLine> = listOf(
        PnLLine("নগদ বিক্রি", "Cash Sales", salesByMethod.cash),
        PnLLine("বিকাশ বিক্রি", "bKash Sales", salesByMethod.bkash),
        PnLLine("নগদ বিক্রি (নগদ)", "Nagad Sales", salesByMethod.nagad),
        PnLLine("বাকি বিক্রি", "Credit Sales", salesByMethod.credit),
        PnLLine("মোট বিক্রি", "Total Sales", salesByMethod.total),
        PnLLine("মোট খরচ", "Total Expenses", -totalExpenses),
        PnLLine("MFS-ফি (আনুমানিক)", "MFS Fee (est.)", -mfsFeeEstimated),
        PnLLine("হিসাব-অনুযায়ী নগদ", "System Cash", systemCashInHand),
        PnLLine("গোনা নগদ", "Counted Cash", countedCash),
        PnLLine("ভ্যারিয়েন্স", "Variance", variance),
    )
}
