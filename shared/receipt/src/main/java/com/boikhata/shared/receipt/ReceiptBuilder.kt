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
     * @param paymentLines P12/D92: the bill's payment lines (paid + DUE) with
     *   display labels — when non-empty the receipt renders one জমা row per
     *   line (2-way/3-way splits) instead of the single legacy জমা/মাধ্যম rows.
     * @return plain-text receipt string
     */
    fun buildReceiptText(
        bill: Bill,
        lines: List<BillLine>,
        shopName: String,
        formatAmount: (Double) -> String,
        formatDate: (Long) -> String,
        paymentLines: List<PaymentLineDisplay> = emptyList(),
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
        sb.append("সর্বমোট: ${formatAmount(bill.subtotal)}\n")
        if (bill.discountAmount > 0.01) {
            val discountLabel = if (bill.discountType == "PERCENTAGE") "ছাড় (%)" else "ছাড়"
            sb.append("$discountLabel: −${formatAmount(bill.discountAmount)}\n")
        }
        if (bill.vatAmount > 0.01) {
            sb.append("ভ্যাট: ${formatAmount(bill.vatAmount)}\n")
        }
        sb.append("━━━━━━━━━━━━━━━\n")
        sb.append("মোট: ${formatAmount(bill.totalAmount)}\n")
        if (paymentLines.isEmpty()) {
            sb.append("জমা: ${formatAmount(bill.paidAmount)}\n")
        } else {
            // P12/D92: one জমা row per payment line (2-way/3-way splits).
            for (pl in paymentLines) {
                sb.append("${pl.labelBn}: ${formatAmount(pl.amount)}\n")
            }
        }
        if (bill.dueAmount > 0.01 && paymentLines.isEmpty()) {
            sb.append("বাকি: ${formatAmount(bill.dueAmount)}\n")
        }
        if (paymentLines.isEmpty()) {
            sb.append("মাধ্যম: ${paymentMethodLabel(bill.paymentMethod)}\n")
        }
        sb.append("━━━━━━━━━━━━━━━\n")
        sb.append("ধন্যবাদ\n")

        return sb.toString()
    }

    /** P12/D92: display label for a payment-line category (+ provider). */
    fun paymentLineLabel(category: String, provider: String?): String {
        val categoryName = try {
            com.boikhata.core.domain.enums.PaymentLineCategory.valueOf(category)
        } catch (_: IllegalArgumentException) {
            return category
        }
        return when (categoryName) {
            com.boikhata.core.domain.enums.PaymentLineCategory.CASH -> "জমা (নগদ)"
            com.boikhata.core.domain.enums.PaymentLineCategory.BANK -> "জমা (ব্যাংক)"
            com.boikhata.core.domain.enums.PaymentLineCategory.MOBILE -> {
                val providerName = try {
                    provider?.let { com.boikhata.core.domain.enums.MfsProvider.valueOf(it) }
                } catch (_: IllegalArgumentException) {
                    null
                }
                when (providerName) {
                    com.boikhata.core.domain.enums.MfsProvider.BKASH -> "জমা (বিকাশ)"
                    com.boikhata.core.domain.enums.MfsProvider.NAGAD -> "জমা (নগদ (Nagad))"
                    com.boikhata.core.domain.enums.MfsProvider.ROCKET -> "জমা (রকেট)"
                    com.boikhata.core.domain.enums.MfsProvider.UPAY -> "জমা (উপায়)"
                    else -> "জমা (মোবাইল ব্যাংকিং)"
                }
            }
            com.boikhata.core.domain.enums.PaymentLineCategory.DUE -> "বাকি"
        }
    }

    /** P12/D92: one receipt line for a payment row (label pre-resolved). */
    data class PaymentLineDisplay(val labelBn: String, val amount: Double)

    private fun paymentMethodLabel(method: PaymentMethod): String = when (method) {
        PaymentMethod.CASH -> "নগদ"
        PaymentMethod.BKASH -> "বিকাশ"
        PaymentMethod.NAGAD -> "নগদ (Nagad)"
        PaymentMethod.CREDIT -> "বাকি (খাতা)"
        // P12: summary values for multi-line bills (line rows carry the detail)
        PaymentMethod.BANK -> "ব্যাংক"
        PaymentMethod.MOBILE -> "মোবাইল ব্যাংকিং"
    }
}
