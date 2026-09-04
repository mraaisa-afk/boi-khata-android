package com.boikhata.shared.receipt

import com.boikhata.core.domain.enums.SupplierEntryType
import com.boikhata.core.domain.model.SupplierStatement
import com.boikhata.core.domain.model.SupplierStatementLine

/**
 * D54: Supplier/publisher settlement statement builder — Unicode plain-text, WhatsApp-shareable.
 *
 * Blueprint §7.5: "পাবলিশার-স্টেটমেন্ট" — the দোকানী-পাবলিশার matching moment for consignment /
 * credit purchases. D2 bans PNG/Bitmap (OOM); D14 established Unicode plain-text for shareable
 * statements. PDF rendering is a DEFERRED follow-up (shared/receipt PDF is P3b accounting-pack scope).
 *
 * Pure function — no Android, no Room. The caller injects formatAmount/formatDate for
 * locale-aware digit formatting (NumberFormatter).
 */
object SupplierStatementBuilder {

    /** Human-readable Bengali label for a supplier entry type. */
    fun typeLabel(type: SupplierEntryType): String = when (type) {
        SupplierEntryType.OPENING -> "উদ্বোধনী দেনা"
        SupplierEntryType.CONSIGNMENT -> "কনসাইনমেন্ট গ্রহণ"
        SupplierEntryType.PURCHASE -> "ক্রয় (বাকি)"
        SupplierEntryType.PAYMENT -> "পেমেন্ট"
        SupplierEntryType.ADJUSTMENT -> "সমন্বয়"
    }

    /**
     * Build the settlement statement as Unicode plain text.
     * @param statement the data model (supplier + entries + totals + aging)
     * @param formatAmount lambda: Double → formatted string (e.g. "৳১,২০০")
     * @param formatDate lambda: Long → formatted date string (e.g. "05/09/2026")
     */
    fun buildStatementText(
        statement: SupplierStatement,
        formatAmount: (Double) -> String,
        formatDate: (Long) -> String,
    ): String {
        val sb = StringBuilder()
        sb.append("═══════════════════════════════\n")
        sb.append("${statement.shopName}\n")
        sb.append("সাপ্লায়ার সেটেলমেন্ট স্টেটমেন্ট\n")
        sb.append("═══════════════════════════════\n")
        sb.append("সাপ্লায়ার: ${statement.supplier.nameBn}\n")
        statement.supplier.phone?.takeIf { it.isNotBlank() }?.let { sb.append("ফোন: $it\n") }
        sb.append("সেটেলমেন্ট চক্র: ${statement.supplier.settlementCycle}\n")
        val rangeLabel = when {
            statement.startDate != null ->
                "${formatDate(statement.startDate)} – ${formatDate(statement.endDate)}"
            else -> "সূচনা – ${formatDate(statement.endDate)}"
        }
        sb.append("মেয়াদ: $rangeLabel\n")
        sb.append("═══════════════════════════════\n\n")

        sb.append("── লেনদেন ──\n")
        if (statement.entries.isEmpty()) {
            sb.append("কোনো লেনদেন নেই।\n")
        } else {
            statement.entries.forEach { line ->
                sb.append(formatLine(line, formatAmount, formatDate))
            }
        }
        sb.append("\n")

        sb.append("──────\n")
        sb.append("মোট দেনা (payable): ${formatAmount(statement.totalPayable)}\n")
        val agingLabel = when (statement.bucket) {
            com.boikhata.core.domain.aging.AgingBucket.GREEN -> "🟢 <১৫ দিন"
            com.boikhata.core.domain.aging.AgingBucket.YELLOW -> "🟡 ১৫–৩০ দিন"
            com.boikhata.core.domain.aging.AgingBucket.RED -> "🔴 >৩০ দিন"
            com.boikhata.core.domain.aging.AgingBucket.NONE -> "কোনো দেনা নেই"
        }
        sb.append("এজিং: $agingLabel (${statement.ageDays} দিন)\n")
        sb.append("═══════════════════════════════\n")
        sb.append("ধন্যবাদ\n")

        return sb.toString()
    }

    private fun formatLine(
        line: SupplierStatementLine,
        formatAmount: (Double) -> String,
        formatDate: (Long) -> String,
    ): String {
        val sign = if (line.type == SupplierEntryType.PAYMENT) "-" else "+"
        return "${formatDate(line.date)}  ${typeLabel(line.type).padEnd(20)}  " +
            "$sign${formatAmount(line.amount)}  (বাকি: ${formatAmount(line.runningBalance)})\n"
    }
}
