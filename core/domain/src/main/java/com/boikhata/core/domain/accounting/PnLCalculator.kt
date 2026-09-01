package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.model.PnLReport

/**
 * D29: P&L calculator with the consignment-vs-purchase COGS split.
 * Blueprint §7.7: "কনসাইনমেন্ট বনাম ক্রয়-COGS স্প্লিট (কনসাইনমেন্ট = খরচ নয়, কমিশন;
 * ক্রয় = বিক্রির মুহূর্তে COGS) — এই স্প্লিট ছাড়া P&L ভুল।"
 *
 * Inputs are plain data (no Room, no Android) so this is independently unit-testable.
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 */
object PnLCalculator {

    /** A bill line for P&L computation. */
    data class BillLineForPnL(
        val bookId: String,
        val quantity: Int,
        val unitPrice: Double, // selling price
        val lineTotal: Double, // unitPrice * quantity (pre-VAT)
        val vatAmount: Double,
        val bookPurchasePrice: Double, // the shop's acquisition cost for this book
    )

    /** A bill for P&L computation. */
    data class BillForPnL(
        val id: String,
        val subtotal: Double,
        val discountAmount: Double,
        val vatAmount: Double,
        val totalAmount: Double,
        val paidAmount: Double,
        val billDate: Long,
        val lines: List<BillLineForPnL>,
    )

    /**
     * Compute the monthly P&L.
     *
     * @param bills all bills in the period (full bill + lines + book purchase prices)
     * @param expensesTotal sum of all expense entries in the period
     * @param ownerDrawingsTotal sum of all owner drawings in the period
     * @param consignmentCommission commission/fee owed to publishers for consignment
     *        books sold in the period (P5 supplies this; 0.0 until P5)
     * @param gregorianYear the Gregorian year of the report month
     * @param gregorianMonth the Gregorian month (1..12) of the report
     */
    fun compute(
        bills: List<BillForPnL>,
        expensesTotal: Double,
        ownerDrawingsTotal: Double,
        consignmentCommission: Double,
        gregorianYear: Int,
        gregorianMonth: Int,
    ): PnLReport {
        val revenue = bills.sumOf { it.subtotal }
        val discountAmount = bills.sumOf { it.discountAmount }
        val netRevenue = revenue - discountAmount

        // D29: COGS split.
        // Purchase-COGS = sum over all bill lines of (book.purchasePrice * quantity).
        // Books acquired by PURCHASE have a non-zero purchasePrice.
        // Books acquired by CONSIGNMENT have purchasePrice = 0 (the shop doesn't pay upfront);
        // their cost is the commission, which is the consignmentCommission input.
        val cogsPurchase = bills.flatMap { it.lines }.sumOf {
            it.bookPurchasePrice * it.quantity
        }
        val cogsConsignment = consignmentCommission
        val totalCogs = cogsPurchase + cogsConsignment

        val grossProfit = netRevenue - totalCogs

        val expenses = expensesTotal
        // Owner drawings are equity withdrawals, NOT expenses — shown separately.
        val netProfit = grossProfit - expenses

        val vatCollected = bills.sumOf { it.vatAmount }

        val marginPercent = if (netRevenue > 0.01) {
            (netProfit / netRevenue) * 100.0
        } else 0.0

        val bengaliFy = BengaliFiscalCalendar.toBengaliFiscalYearStart(
            BengaliFiscalCalendar.gregorianMonthStart(gregorianYear, gregorianMonth)
        )
        val bengaliMonth = BengaliFiscalCalendar.gregorianToBengaliMonth(gregorianMonth)

        return PnLReport(
            gregorianYear = gregorianYear,
            gregorianMonth = gregorianMonth,
            gregorianMonthNameBn = BengaliFiscalCalendar.gregorianMonthNameBn(gregorianMonth),
            bengaliFiscalYear = bengaliFy,
            bengaliMonth = bengaliMonth,
            bengaliMonthNameBn = BengaliFiscalCalendar.bengaliMonthName(bengaliMonth),
            revenue = round2(revenue),
            discountAmount = round2(discountAmount),
            netRevenue = round2(netRevenue),
            cogsPurchase = round2(cogsPurchase),
            cogsConsignment = round2(cogsConsignment),
            totalCogs = round2(totalCogs),
            grossProfit = round2(grossProfit),
            expenses = round2(expenses),
            ownerDrawings = round2(ownerDrawingsTotal),
            vatCollected = round2(vatCollected),
            netProfit = round2(netProfit),
            marginPercent = round2(marginPercent),
        )
    }

    private fun round2(v: Double): Double {
        return Math.round(v * 100.0) / 100.0
    }
}
