package com.boikhata.core.domain.khata

import com.boikhata.core.domain.aging.AgingCalculator
import com.boikhata.core.domain.aging.KhataEntry
import com.boikhata.core.domain.enums.KhataEntryType
import com.boikhata.core.domain.model.KhataCustomer
import com.boikhata.core.domain.model.KhataStatement
import com.boikhata.core.domain.model.KhataStatementLine

/**
 * D14: Builds a shareable khata statement (বাকি হিসাব) as plain text.
 * Blueprint §7.4: "শেয়ারেবল স্টেটমেন্ট — দোকানী-কাস্টমার মিলনের মুহূর্ত".
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 * The statement text is WhatsApp-shareable via Intent.ACTION_SEND (text/plain).
 */
object KhataStatementBuilder {

    /**
     * Build the structured statement data (for UI display + text generation).
     */
    fun buildStatement(
        customer: KhataCustomer,
        entries: List<KhataEntry>,
        now: Long,
    ): KhataStatement {
        val aging = AgingCalculator.calculate(entries, now)
        val sorted = entries.sortedBy { it.date }

        val lines = mutableListOf<KhataStatementLine>()
        var runningBalance = 0.0

        for (e in sorted) {
            runningBalance = when (e.type) {
                KhataEntryType.CREDIT, KhataEntryType.OPENING -> runningBalance + e.amount
                KhataEntryType.PAYMENT -> runningBalance - e.amount
                KhataEntryType.ADJUSTMENT -> {
                    if (e.amount >= 0) runningBalance + e.amount
                    else runningBalance + e.amount // negative reduces
                }
            }
            lines.add(
                KhataStatementLine(
                    date = e.date,
                    type = e.type,
                    amount = e.amount,
                    description = typeLabel(e.type),
                    runningBalance = runningBalance,
                )
            )
        }

        return KhataStatement(
            customerName = customer.nameBn,
            customerArea = customer.address,
            lines = lines,
            totalDue = aging.totalDue,
            aging = aging,
            creditLimit = customer.creditLimit,
            exceedsCreditLimit = aging.totalDue > customer.creditLimit && customer.creditLimit > 0,
        )
    }

    /**
     * Generate the plain-text statement for WhatsApp sharing.
     * Bengali labels, dual digits handled by the caller (NumberFormatter).
     */
    fun toText(
        statement: KhataStatement,
        shopName: String,
        formatAmount: (Double) -> String,
        formatDate: (Long) -> String,
    ): String {
        val sb = StringBuilder()
        sb.append("━━━━━━━━━━━━━━━\n")
        sb.append("$shopName\n")
        sb.append("বাকি হিসাব (খাতা)\n")
        sb.append("━━━━━━━━━━━━━━━\n")
        sb.append("নাম: ${statement.customerName}\n")
        statement.customerArea?.let { if (it.isNotBlank()) sb.append("এলাকা: $it\n") }
        sb.append("━━━━━━━━━━━━━━━\n")

        if (statement.lines.isEmpty()) {
            sb.append("কোনো লেনদেন নেই\n")
        } else {
            for (line in statement.lines) {
                val sign = when (line.type) {
                    KhataEntryType.CREDIT, KhataEntryType.OPENING -> "+"
                    KhataEntryType.PAYMENT -> "-"
                    KhataEntryType.ADJUSTMENT -> if (line.amount >= 0) "+" else "-"
                }
                val displayAmount = if (line.amount < 0) formatAmount(-line.amount) else formatAmount(line.amount)
                sb.append("${formatDate(line.date)}  ${line.description}  $sign$displayAmount  →  বাকি: ${formatAmount(line.runningBalance)}\n")
            }
        }

        sb.append("━━━━━━━━━━━━━━━\n")
        sb.append("মোট বাকি: ${formatAmount(statement.totalDue)}\n")

        if (statement.exceedsCreditLimit) {
            sb.append("⚠ ক্রেডিট লিমিট ছাড়িয়েছে\n")
        }

        val ageDays = statement.aging.ageDays
        if (statement.totalDue > 0.01 && ageDays > 0) {
            sb.append("বাকি প্রাচীনতা: $ageDays দিন\n")
        }

        sb.append("━━━━━━━━━━━━━━━\n")
        return sb.toString()
    }

    private fun typeLabel(type: KhataEntryType): String = when (type) {
        KhataEntryType.CREDIT -> "বাকি"
        KhataEntryType.PAYMENT -> "জমা"
        KhataEntryType.ADJUSTMENT -> "সমন্বয়"
        KhataEntryType.OPENING -> "পূর্ববর্তী"
    }
}
