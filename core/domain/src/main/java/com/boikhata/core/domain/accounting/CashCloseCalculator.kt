package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.enums.MfsProvider
import com.boikhata.core.domain.enums.PaymentLineCategory
import com.boikhata.core.domain.enums.PaymentMethod
import com.boikhata.core.domain.model.CashCloseReport
import com.boikhata.core.domain.model.Expense
import com.boikhata.core.domain.model.ExpenseCategory
import com.boikhata.core.domain.model.SalesByMethod
import com.boikhata.core.domain.model.ExpenseCategoryTotal
import java.util.Calendar
import java.util.TimeZone

/**
 * D36: Cash-close calculator — daily "আজকের হিসাব".
 * Blueprint §7.6: "মাধ্যম-ভিত্তিক বিক্রি + খরচ (শ্রেণিভিত্তিক) + MFS-ফি অটো-লাইন +
 * গোনা-বনাম-হিসাব ভ্যারিয়েন্স।"
 *
 * The MFS fee rate is estimatable + owner-overridable, NEVER silently hardcoded.
 * The calculator receives the rate as a parameter (default 0.0 = no fee).
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 */
object CashCloseCalculator {

    /** A bill's payment-method + paid amount for cash-close grouping. */
    data class BillForClose(
        val paymentMethod: PaymentMethod,
        val paidAmount: Double,
        val dueAmount: Double,
        // P12/D92: authoritative payment lines when the bill has them (post-v7
        // checkout). Empty for legacy bills → the bill-level columns are used.
        val lines: List<LineForClose> = emptyList(),
    )

    /** One bill_payment_lines row reduced for the calculator (pure values). */
    data class LineForClose(
        val method: String, // PaymentLineCategory name
        val provider: String?, // MfsProvider name (MOBILE lines only)
        val amount: Double,
    )

    /**
     * Compute the daily cash-close report.
     *
     * @param bills the day's bills (payment method + paid + due)
     * @param expenses the day's expenses
     * @param expenseCategories the tenant's expense categories (for name lookup)
     * @param cashbookCashBalance the derived CASH account balance (system cash-in-hand)
     * @param countedCash the owner's physical cash count
     * @param mfsFeeRate the MFS fee rate as a percentage (e.g. 1.5 = 1.5%); default 0.0
     * @param date the close date (epoch-millis)
     */
    fun compute(
        bills: List<BillForClose>,
        expenses: List<Expense>,
        expenseCategories: List<ExpenseCategory>,
        cashbookCashBalance: Double,
        countedCash: Double,
        mfsFeeRate: Double,
        date: Long,
    ): CashCloseReport {
        // Sales by payment method. P12/D92: bills WITH payment lines aggregate
        // per line (2-way/3-way splits land in the right bucket); legacy bills
        // (no lines) fall back to the bill-level columns.
        var cash = 0.0
        var bkash = 0.0
        var nagad = 0.0
        var bank = 0.0
        var mobileOther = 0.0
        var credit = 0.0
        for (bill in bills) {
            if (bill.lines.isEmpty()) {
                when (bill.paymentMethod) {
                    PaymentMethod.CASH -> cash += bill.paidAmount
                    PaymentMethod.BKASH -> bkash += bill.paidAmount
                    PaymentMethod.NAGAD -> nagad += bill.paidAmount
                    PaymentMethod.CREDIT -> credit += bill.dueAmount
                    PaymentMethod.BANK -> bank += bill.paidAmount
                    PaymentMethod.MOBILE -> mobileOther += bill.paidAmount
                }
            } else {
                for (line in bill.lines) {
                    when (line.method) {
                        PaymentLineCategory.CASH.name -> cash += line.amount
                        PaymentLineCategory.BANK.name -> bank += line.amount
                        PaymentLineCategory.MOBILE.name -> when (line.provider) {
                            MfsProvider.BKASH.name -> bkash += line.amount
                            MfsProvider.NAGAD.name -> nagad += line.amount
                            else -> mobileOther += line.amount
                        }
                        PaymentLineCategory.DUE.name -> credit += line.amount
                    }
                }
            }
        }
        val total = cash + bkash + nagad + bank + mobileOther + credit
        val salesByMethod = SalesByMethod(
            cash = cash, bkash = bkash, nagad = nagad, credit = credit,
            total = total, bank = bank, mobileOther = mobileOther,
        )

        // Expenses by category
        val categoryMap = expenseCategories.associateBy { it.id }
        val expensesByCategory = expenses.groupBy { it.categoryId }
            .map { (categoryId, list) ->
                ExpenseCategoryTotal(
                    categoryId = categoryId,
                    categoryNameBn = categoryMap[categoryId]?.nameBn ?: "",
                    total = list.sumOf { it.amount },
                )
            }
            .sortedByDescending { it.total }
        val totalExpenses = expenses.sumOf { it.amount }

        // MFS fee: estimated = bKash sales × rate / 100 (bKash is the dominant MFS).
        // P12: bKash MOBILE lines land in the same bucket, so the estimate covers
        // them too (Nagad/Rocket/Upay fees still NOT estimated — disclosed in D92).
        val mfsFeeEstimated = if (mfsFeeRate > 0) {
            (bkash * mfsFeeRate / 100.0)
        } else 0.0

        // Variance: system cash-in-hand − counted cash
        val variance = round2(cashbookCashBalance - countedCash)
        val varianceLabelBn = when {
            variance > 0.01 -> "ঘাটতি" // system says more than counted → short
            variance < -0.01 -> "বাড়তি" // system says less than counted → over
            else -> "মিলেছে" // matched
        }

        val dateLabelBn = BengaliFiscalCalendar.gregorianMonthNameBnForDate(date)

        return CashCloseReport(
            date = date,
            dateLabelBn = dateLabelBn,
            salesByMethod = salesByMethod,
            expensesByCategory = expensesByCategory,
            totalExpenses = round2(totalExpenses),
            mfsFeeEstimated = round2(mfsFeeEstimated),
            mfsFeeRate = mfsFeeRate,
            systemCashInHand = round2(cashbookCashBalance),
            countedCash = round2(countedCash),
            variance = variance,
            varianceLabelBn = varianceLabelBn,
        )
    }

    private fun round2(v: Double): Double {
        return Math.round(v * 100.0) / 100.0
    }
}
