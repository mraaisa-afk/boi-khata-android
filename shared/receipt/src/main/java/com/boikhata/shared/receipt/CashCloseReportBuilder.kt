package com.boikhata.shared.receipt

import com.boikhata.core.domain.model.CashCloseReport

/**
 * D36: Cash-close report text builder — "আজকের হিসাব" WhatsApp-shareable.
 * Blueprint §7.6: "→ WhatsApp-শেয়ার।"
 *
 * Pure function — no Android, no Room. The caller injects formatAmount for
 * locale-aware digit formatting (NumberFormatter).
 */
object CashCloseReportBuilder {

    /**
     * Build the cash-close report as Unicode plain text.
     * @param report the cash-close domain model
     * @param formatAmount lambda: Double → formatted string (e.g. "৳১,২০০")
     * @return plain-text report string
     */
    fun buildCloseText(
        report: CashCloseReport,
        formatAmount: (Double) -> String,
    ): String {
        val sb = StringBuilder()
        sb.append("═══════════════════════════════\n")
        sb.append("আজকের হিসাব\n")
        sb.append("═══════════════════════════════\n")
        sb.append("তারিখ: ${report.dateLabelBn}\n")
        sb.append("═══════════════════════════════\n\n")

        // Sales by method
        sb.append("── বিক্রি (মাধ্যম অনুযায়ী) ──\n")
        sb.append("নগদ: ${formatAmount(report.salesByMethod.cash)}\n")
        sb.append("বিকাশ: ${formatAmount(report.salesByMethod.bkash)}\n")
        sb.append("নগদ (Nagad): ${formatAmount(report.salesByMethod.nagad)}\n")
        sb.append("বাকি: ${formatAmount(report.salesByMethod.credit)}\n")
        sb.append("মোট বিক্রি: ${formatAmount(report.salesByMethod.total)}\n\n")

        // Expenses by category
        sb.append("── খরচ (শ্রেণি অনুযায়ী) ──\n")
        if (report.expensesByCategory.isEmpty()) {
            sb.append("আজ কোনো খরচ নেই\n")
        } else {
            for (cat in report.expensesByCategory) {
                sb.append("${cat.categoryNameBn}: ${formatAmount(cat.total)}\n")
            }
        }
        sb.append("মোট খরচ: ${formatAmount(report.totalExpenses)}\n\n")

        // MFS fee
        if (report.mfsFeeEstimated > 0.01) {
            sb.append("── MFS-ফি (আনুমানিক) ──\n")
            sb.append("হার: ${report.mfsFeeRate}%\n")
            sb.append("ফি: ${formatAmount(report.mfsFeeEstimated)}\n\n")
        }

        // Cash variance
        sb.append("── নগদ মিলান ──\n")
        sb.append("হিসাব-অনুযায়ী: ${formatAmount(report.systemCashInHand)}\n")
        sb.append("গোনা নগদ: ${formatAmount(report.countedCash)}\n")
        sb.append("ভ্যারিয়েন্স: ${formatAmount(report.variance)} (${report.varianceLabelBn})\n")
        sb.append("\n")
        sb.append("═══════════════════════════════\n")

        return sb.toString()
    }
}
