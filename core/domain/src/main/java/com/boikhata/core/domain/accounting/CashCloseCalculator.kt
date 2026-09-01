package com.boikhata.core.domain.accounting

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
        // Sales by payment method
        val cash = bills.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.paidAmount }
        val bkash = bills.filter { it.paymentMethod == PaymentMethod.BKASH }.sumOf { it.paidAmount }
        val nagad = bills.filter { it.paymentMethod == PaymentMethod.NAGAD }.sumOf { it.paidAmount }
        val credit = bills.filter { it.paymentMethod == PaymentMethod.CREDIT }.sumOf { it.dueAmount }
        val total = cash + bkash + nagad + credit
        val salesByMethod = SalesByMethod(cash, bkash, nagad, credit, total)

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

        // MFS fee: estimated = BKASH sales × rate / 100 (bKash is the dominant MFS)
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
