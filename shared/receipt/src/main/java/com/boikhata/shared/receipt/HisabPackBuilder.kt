package com.boikhata.shared.receipt

import com.boikhata.core.domain.model.HisabPack
import com.boikhata.core.domain.model.PnLReport

/**
 * D33: হিসাব-প্যাক text builder — bank/microfinance-loan-file ready.
 * Produces a structured plain-text report from the HisabPack data model.
 * The text can be shared via WhatsApp (like the receipt) or rendered to PDF.
 *
 * Pure function — no Android, no Room. The caller injects formatAmount for
 * locale-aware digit formatting (NumberFormatter).
 *
 * Blueprint §7.7: "মাসিক P&L + ব্যালেন্স-শিট-লাইট + খাতা-aging + ভ্যাট-সামারি =
 * মাসিক হিসাব-প্যাক PDF (A4) — ব্যাংক/মাইক্রোফাইন্যান্স লোন-ফাইল-রেডি।"
 */
object HisabPackBuilder {

    /**
     * Build the হিসাব-প্যাক as Unicode plain text.
     * @param pack the complete accounting pack
     * @param formatAmount lambda: Double → formatted string (e.g. "৳১,২০০")
     * @return plain-text report string
     */
    fun buildPackText(
        pack: HisabPack,
        formatAmount: (Double) -> String,
    ): String {
        val sb = StringBuilder()
        val pnl = pack.pnl

        sb.append("═══════════════════════════════\n")
        sb.append("${pack.shopName}\n")
        sb.append("মাসিক হিসাব-প্যাক\n")
        sb.append("═══════════════════════════════\n")
        // D30: dual-calendar header
        sb.append("গ্রেগরিয়ান: ${pnl.gregorianMonthNameBn} ${pnl.gregorianYear}\n")
        sb.append("বাংলা বর্ষ: ${pnl.bengaliMonthNameBn} (FY ${pnl.bengaliFiscalYear}-${pnl.bengaliFiscalYear + 1})\n")
        sb.append("═══════════════════════════════\n\n")

        // Section 1: P&L with COGS split
        sb.append("── লাভ-ক্ষতি হিসাব (P&L) ──\n")
        sb.append("মোট বিক্রি: ${formatAmount(pnl.revenue)}\n")
        if (pnl.discountAmount > 0.01) {
            sb.append("ছাড়: −${formatAmount(pnl.discountAmount)}\n")
        }
        sb.append("নিট বিক্রি: ${formatAmount(pnl.netRevenue)}\n")
        sb.append("─ COGS স্প্লিট ─\n")
        sb.append("ক্রয়-COGS (কেনা বই): −${formatAmount(pnl.cogsPurchase)}\n")
        sb.append("কনসাইনমেন্ট-কমিশন: −${formatAmount(pnl.cogsConsignment)}\n")
        sb.append("মোট COGS: −${formatAmount(pnl.totalCogs)}\n")
        sb.append("সর্বমোট লাভ: ${formatAmount(pnl.grossProfit)}\n")
        sb.append("খরচ: −${formatAmount(pnl.expenses)}\n")
        sb.append("মালিকের তোলা: −${formatAmount(pnl.ownerDrawings)}\n")
        sb.append("─\n")
        sb.append("নিট লাভ: ${formatAmount(pnl.netProfit)}\n")
        sb.append("মার্জিন: ${formatMargin(pnl.marginPercent)}%\n")
        if (pnl.vatCollected > 0.01) {
            sb.append("ভ্যাট আদায়: ${formatAmount(pnl.vatCollected)}\n")
        }
        sb.append("\n")

        // Section 2: Balance sheet lite
        val bs = pack.balanceSheet
        sb.append("── ব্যালেন্স-শিট (সারসংক্ষেপ) ──\n")
        sb.append("সম্পদ:\n")
        sb.append("  নগদ: ${formatAmount(bs.cash)}\n")
        sb.append("  ইনভেন্টরি: ${formatAmount(bs.inventory)}\n")
        sb.append("  খাতা পাওনা: ${formatAmount(bs.receivables)}\n")
        sb.append("  ঘরি অগ্রিম: ${formatAmount(bs.ghoriAdvances)}\n")
        sb.append("  মোট সম্পদ: ${formatAmount(bs.totalAssets)}\n")
        sb.append("দায়:\n")
        if (bs.supplierPayables > 0.01) {
            sb.append("  সাপ্লায়ার দেনা: ${formatAmount(bs.supplierPayables)}\n")
        }
        sb.append("  মোট দায়: ${formatAmount(bs.totalLiabilities)}\n")
        sb.append("ইক্যুইটি:\n")
        sb.append("  অবধৃত মুনাফা: ${formatAmount(bs.retainedEarnings)}\n")
        sb.append("  মালিকের তোলা (কম): −${formatAmount(bs.lessDrawings)}\n")
        sb.append("  মোট ইক্যুইটি: ${formatAmount(bs.totalEquity)}\n")
        sb.append("\n")

        // Section 3: Khata aging summary
        val aging = pack.agingSummary
        sb.append("── খাতা এজিং সারসংক্ষেপ ──\n")
        sb.append("মোট বাকি: ${formatAmount(aging.totalDue)}\n")
        sb.append("মোট ক্রেতা: ${aging.customerCount}\n")
        sb.append("🟢 <১৫দি: ${formatAmount(aging.greenBucket)}\n")
        sb.append("🟡 ১৫–৩০দি: ${formatAmount(aging.yellowBucket)}\n")
        sb.append("🔴 >৩০দি: ${formatAmount(aging.redBucket)}\n")
        sb.append("\n")

        // Section 4: VAT summary
        val vat = pack.vatSummary
        sb.append("── ভ্যাট সারসংক্ষেপ ──\n")
        sb.append("বই (০%): ${formatAmount(vat.booksVat)}\n")
        sb.append("স্টেশনারি (১৫%): ${formatAmount(vat.stationeryVat)}\n")
        sb.append("মোট ভ্যাট: ${formatAmount(vat.totalVat)}\n")
        sb.append("\n")
        sb.append("═══════════════════════════════\n")
        sb.append("ব্যাংক/মাইক্রোফাইন্যান্স লোন-ফাইল-রেডি\n")
        sb.append("═══════════════════════════════\n")

        return sb.toString()
    }

    private fun formatMargin(pct: Double): String {
        // The caller's formatAmount handles digits; margin is a percentage.
        return String.format("%.1f", pct)
    }
}
