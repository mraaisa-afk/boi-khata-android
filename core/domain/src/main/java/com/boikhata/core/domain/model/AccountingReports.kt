package com.boikhata.core.domain.model

/**
 * D29/D30/D31: Accounting report domain models for P3b.
 * Pure data — no Room, no Android. Used by the calculators + the হিসাব-প্যাক.
 */

// ── P&L (D29) ────────────────────────────────────────────────────────────────

/** A single line item in a P&L report. */
data class PnLLine(
    val labelBn: String,
    val labelEn: String,
    val amount: Double,
)

/** Monthly P&L with the COGS split (D29) + dual-calendar labels (D30). */
data class PnLReport(
    val gregorianYear: Int,
    val gregorianMonth: Int, // 1..12
    val gregorianMonthNameBn: String,
    val bengaliFiscalYear: Int,
    val bengaliMonth: Int, // 1..12
    val bengaliMonthNameBn: String,
    // Revenue
    val revenue: Double,
    val discountAmount: Double,
    val netRevenue: Double, // revenue - discount
    // COGS split (D29 — the core accounting-correctness rule)
    val cogsPurchase: Double, // books bought outright — COGS at sale time
    val cogsConsignment: Double, // commission/fee for consignment books sold
    val totalCogs: Double,
    val grossProfit: Double, // netRevenue - totalCogs
    // Operating expenses
    val expenses: Double, // sum of all expense entries in the period
    val ownerDrawings: Double, // equity withdrawals (not an expense, shown separately)
    // VAT
    val vatCollected: Double, // VAT charged on sales
    // Bottom line
    val netProfit: Double, // grossProfit - expenses (drawings are equity, not expense)
    val marginPercent: Double, // netProfit / netRevenue * 100 (0 if netRevenue is 0)
) {
    /** P&L as ordered line items for rendering. */
    fun toLines(): List<PnLLine> = listOf(
        PnLLine("মোট বিক্রি", "Revenue", revenue),
        PnLLine("ছাড়", "Discount", -discountAmount),
        PnLLine("নিট বিক্রি", "Net Revenue", netRevenue),
        PnLLine("ক্রয়-COGS (কেনা বই)", "Purchase COGS", -cogsPurchase),
        PnLLine("কনসাইনমেন্ট-কমিশন", "Consignment Commission", -cogsConsignment),
        PnLLine("মোট COGS", "Total COGS", -totalCogs),
        PnLLine("সর্বমোট লাভ", "Gross Profit", grossProfit),
        PnLLine("খরচ", "Expenses", -expenses),
        PnLLine("মালিকের তোলা", "Owner Drawings", -ownerDrawings),
        PnLLine("ভ্যাট আদায়", "VAT Collected", vatCollected),
        PnLLine("নিট লাভ", "Net Profit", netProfit),
    )
}

// ── Balance Sheet Lite (D31) ─────────────────────────────────────────────────

data class BalanceSheetComponent(
    val labelBn: String,
    val labelEn: String,
    val amount: Double,
)

/** Lite balance sheet (D31) — point-in-time snapshot as of a date. */
data class BalanceSheetLite(
    val asOfDate: Long,
    val gregorianDateLabelBn: String,
    // Assets
    val cash: Double, // sum of cashbook balances (CASH + BKASH + BANK)
    val inventory: Double, // stock on hand × purchasePrice
    val receivables: Double, // khata customer due balances
    val ghoriAdvances: Double, // staff advance sub-ledger (D26)
    val totalAssets: Double,
    // Liabilities
    val supplierPayables: Double, // denā — P5 scope, 0.0 for P3b
    val totalLiabilities: Double,
    // Equity
    val retainedEarnings: Double, // accumulated net profit
    val lessDrawings: Double, // accumulated owner drawings
    val totalEquity: Double, // retainedEarnings - lessDrawings
) {
    /** Assert the accounting identity: Assets = Liabilities + Equity. */
    fun isBalanced(): Boolean {
        return kotlin.math.abs(totalAssets - (totalLiabilities + totalEquity)) < 0.01
    }

    fun assetLines(): List<BalanceSheetComponent> = listOf(
        BalanceSheetComponent("নগদ (ক্যাশ+বিকাশ+ব্যাংক)", "Cash (Cash+bKash+Bank)", cash),
        BalanceSheetComponent("ইনভেন্টরি", "Inventory", inventory),
        BalanceSheetComponent("খাতা পাওনা", "Receivables", receivables),
        BalanceSheetComponent("ঘরি অগ্রিম", "Ghori Advances", ghoriAdvances),
        BalanceSheetComponent("মোট সম্পদ", "Total Assets", totalAssets),
    )

    fun liabilityLines(): List<BalanceSheetComponent> = listOf(
        BalanceSheetComponent("সাপ্লায়ার দেনা", "Supplier Payables", supplierPayables),
        BalanceSheetComponent("মোট দায়", "Total Liabilities", totalLiabilities),
    )

    fun equityLines(): List<BalanceSheetComponent> = listOf(
        BalanceSheetComponent("অবধৃত মুনাফা", "Retained Earnings", retainedEarnings),
        BalanceSheetComponent("মালিকের তোলা (কম)", "Less: Drawings", -lessDrawings),
        BalanceSheetComponent("মোত ইক্যুইটি", "Total Equity", totalEquity),
    )
}

// ── Khata Aging Summary (for হিসাব-প্যাক) ──────────────────────────────────

data class KhataAgingSummary(
    val totalDue: Double,
    val greenBucket: Double, // <15 days
    val yellowBucket: Double, // 15-30 days
    val redBucket: Double, // >30 days
    val customerCount: Int,
)

// ── VAT Summary (for হিসাব-প্যাক) ────────────────────────────────────────────

data class VatSummary(
    val booksVat: Double, // books 0% — always 0.0
    val stationeryVat: Double, // stationery 15%
    val totalVat: Double,
)

// ── হিসাব-প্যাক (D33) ──────────────────────────────────────────────────────

/** The complete monthly accounting pack — bank/microfinance-loan-file ready. */
data class HisabPack(
    val shopName: String,
    val pnl: PnLReport,
    val balanceSheet: BalanceSheetLite,
    val agingSummary: KhataAgingSummary,
    val vatSummary: VatSummary,
)

// ── Period Lock (D32) ─────────────────────────────────────────────────────────

data class PeriodLock(
    val id: String,
    val tenantId: String,
    val periodYear: Int,
    val periodMonth: Int,
    val lockedAt: Long,
    val lockedByUserId: String,
)

// ── Recurring Expense Template (D35) ──────────────────────────────────────────

data class RecurringExpenseTemplate(
    val id: String,
    val tenantId: String,
    val categoryId: String,
    val categoryNameBn: String,
    val amount: Double,
    val description: String,
    val frequency: com.boikhata.core.domain.accounting.RecurringExpenseCalculator.Frequency,
    val lastAppliedDate: Long,
    val nextDueDate: Long,
    val isActive: Boolean,
)
