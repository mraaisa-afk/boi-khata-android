package com.boikhata.shared.receipt

import com.boikhata.core.domain.enums.PaymentMethod
import com.boikhata.core.domain.model.Bill
import com.boikhata.core.domain.model.BillLine

/**
 * D21: Receipt builder — Unicode plain-text, dual digits, WhatsApp-shareable.
 * D2: No PNG/Bitmap (OOM risk on 3GB devices).
 *
 * Pure function — no Android, no Room. The caller injects formatAmount/formatDate
 * lambdas for locale-aware digit formatting (NumberFormatter).
 *
 * Blueprint §7.3: "WhatsApp-শেয়ার (টেক্সট) = প্রাথমিক; দ্বৈত-অঙ্ক"
 */
object ReceiptBuilder {

    /**
     * Build a shareable receipt as Unicode plain text.
     *
     * @param bill the bill domain model
     * @param lines the bill lines
     * @param shopName the shop/tenant name
     * @param formatAmount lambda: Double → formatted string (e.g. "৳১,২০০")
     * @param formatDate lambda: Long → formatted date string (e.g. "৩০/০৮/২০২৬")
     * @return plain-text receipt string
     */
    fun buildReceiptText(
        bill: Bill,
        lines: List<BillLine>,
        shopName: String,
        formatAmount: (Double) -> String,
        formatDate: (Long) -> String,
    ): String {
        val sb = StringBuilder()
        sb.append("━━━━━━━━━━━━━━━\n")
        sb.append("$shopName\n")
        sb.append("━━━━━━━━━━━━━━━\n")
        sb.append("বিল নম্বর: ${bill.billNumber}\n")
        sb.append("তারিখ: ${formatDate(bill.billDate)}\n")
        if (bill.customerNameBn.isNotBlank() && bill.customerNameBn != "হাটি ক্রেতা") {
            sb.append("ক্রেতা: ${bill.customerNameBn}\n")
        }
        bill.customerPhone?.let { if (it.isNotBlank()) sb.append("ফোন: $it\n") }
        sb.append("━━━━━━━━━━━━━━━\n")

        for (line in lines) {
            sb.append("${line.bookTitleBn}\n")
            sb.append("  ${line.quantity} × ${formatAmount(line.unitPrice)} = ${formatAmount(line.lineTotal)}")
            if (line.vatAmount > 0.01) {
                sb.append(" (+ভ্যাট ${formatAmount(line.vatAmount)})")
            }
            sb.append("\n")
        }

        sb.append("━━━━━━━━━━━━━━━\n")
        sb.append("উপমুট: ${formatAmount(bill.subtotal)}\n")
        if (bill.discountAmount > 0.01) {
            val discountLabel = if (bill.discountType == "PERCENTAGE") "ছাড়" else "ছাড়"
            sb.append("$discountLabel: −${formatAmount(bill.discountAmount)}\n")
        }
        if (bill.vatAmount > 0.01) {
            sb.append("ভ্যাট: ${formatAmount(bill.vatAmount)}\n")
        }
        sb.append("━━━━━━━━━━━━━━━\n")
        sb.append("মোট: ${formatAmount(bill.totalAmount)}\n")
        sb.append("জমা: ${formatAmount(bill.paidAmount)}\n")
        if (bill.dueAmount > 0.01) {
            sb.append("বাকি: ${formatAmount(bill.dueAmount)}\n")
        }
        sb.append("মাধ্যম: ${paymentMethodLabel(bill.paymentMethod)}\n")
        sb.append("━━━━━━━━━━━━━━━\n")
        sb.append("ধন্যবাদ\n")

        return sb.toString()
    }

    private fun paymentMethodLabel(method: PaymentMethod): String = when (method) {
        PaymentMethod.CASH -> "নগদ"
        PaymentMethod.BKASH -> "বিকাশ"
        PaymentMethod.NAGAD -> "নগদ"
        PaymentMethod.CREDIT -> "বাকি (খাতা)"
    }
}
