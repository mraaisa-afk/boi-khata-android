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
    // P12/D92 additive buckets (defaulted so legacy constructors keep compiling):
    val bank: Double = 0.0, // ব্যাংক paid lines
    val mobileOther: Double = 0.0, // MOBILE lines on providers other than bKash/Nagad (Rocket/Upay/other)
    // D93 (owner ruling 2026-09-24): the overpaid portion of a bill that went to
    // the customer's খাতা as a জমা — money received today but NOT today's sales.
    val khataAdvance: Double = 0.0,
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
        // D94 label ruling 2026-09-24: নগদে X / [provider] হতে X / বাকিতে X
        PnLLine("নগদে বিক্রি", "Cash Sales", salesByMethod.cash),
        PnLLine("বিকাশ হতে বিক্রি", "bKash Sales", salesByMethod.bkash),
        PnLLine("নগদ (Nagad) হতে বিক্রি", "Nagad Sales", salesByMethod.nagad),
        PnLLine("ব্যাংক হতে বিক্রি", "Bank Sales", salesByMethod.bank),
        PnLLine("মোবাইল ব্যাংকিং (অন্যান্য) হতে বিক্রি", "Mobile Banking Sales (other)", salesByMethod.mobileOther),
        PnLLine("বাকিতে বিক্রি", "Credit Sales", salesByMethod.credit),
        PnLLine("খাতায় জমা (অতিরিক্ত)", "Khata Advance (overpay)", salesByMethod.khataAdvance),
        PnLLine("মোট বিক্রি", "Total Sales", salesByMethod.total),
        PnLLine("মোট খরচ", "Total Expenses", -totalExpenses),
        PnLLine("MFS-ফি (আনুমানিক)", "MFS Fee (est.)", -mfsFeeEstimated),
        PnLLine("হিসাব-অনুযায়ী নগদ", "System Cash", systemCashInHand),
        PnLLine("গোনা নগদ", "Counted Cash", countedCash),
        PnLLine("ফারাক", "Variance", variance),
    )
}
