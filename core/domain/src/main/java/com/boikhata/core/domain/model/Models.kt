package com.boikhata.core.domain.model

import com.boikhata.core.domain.enums.Role

/** Domain models — no Room dependency. Used by repository interfaces + ViewModels. */

data class User(
    val id: String,
    val tenantId: String,
    val name: String,
    val role: Role,
    val isActive: Boolean,
)

data class KhataCustomer(
    val id: String,
    val nameBn: String,
    val phone: String?,
    val address: String?,
    val creditLimit: Double,
    val isActive: Boolean,
)

data class BillSummary(
    val id: String,
    val billNumber: String,
    val customerNameBn: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,
    val billDate: Long,
    val status: String,
)

/**
 * D79 PR D: Low-stock alert summary for HomeScreen «আজকের করণীয়» alerts section.
 * Repository computes currentStock from stock ledger; feature module is shielded from Room.
 */
data class LowStockBookSummary(
    val bookId: String,
    val bookTitleBn: String,
    val classLevel: String,
    val currentStock: Int,
    val lowStockThreshold: Int,
)

/** D79 PR E: daily net-profit point for HomeScreen «বিশ্লেষণ» mini bar sparkline. */
data class HomeAnalyticsPoint(
    val dayOfMonth: Int,
    val netProfit: Double,
)

/**
 * D79 §2.2: HomeScreen hero card data model.
 * D81: Full D79 formula — নিট লাভ = (todaySalesTotal + todayKhataCollection) − todayExpenseTotal.
 *   todaySalesTotal  = sum of bills.paidAmount (cash actually received; credit portion excluded).
 *   todayKhataCollection = sum of PAYMENT-type khata entries for today (খাতা আদায়).
 *   todayExpenseTotal    = sum of today's cash expenses.
 * yesterdayNetProfit used for ▲/▼ trend delta badge.
 * D75 Trident arms retained: cashBalance, totalDue, supplierDuesTotal.
 * PR D: lowStockAlerts added for «আজকের করণীয়» section.
 * PR E: monthNetProfit + monthAnalytics added for «বিশ্লেষণ» mini bar sparkline.
 */
data class HomeData(
    val totalDue: Double,
    val dueCustomerCount: Int,
    /** D94 (owner ruling 2026-09-24): ALL bill totals — নগদ + মোবাইল + বাকি. A
     *  credit sale is still revenue (replaces the D81 paidAmount basis). */
    val todaySalesTotal: Double,
    /** D81: sum of khata PAYMENT entries for today — খাতা আদায় (kept for its own stat;
     *  NOT part of revenue — collections of old dues were already revenue when sold). */
    val todayKhataCollection: Double = 0.0,
    val todayExpenseTotal: Double = 0.0,
    /** D94: Σ(bill_lines.quantity × books.purchasePrice) for today's sold items. */
    val todayCogs: Double = 0.0,
    /** D94: (revenue − COGS) − expenses — the accounting-correct net profit. */
    val todayNetProfit: Double = 0.0,
    val todayBillCount: Int,
    val topDueCustomers: List<KhataCustomerDue>,
    val cashBalance: Double,
    val supplierDuesTotal: Double,
    val supplierCount: Int,
    val yesterdayNetProfit: Double = 0.0,
    val lowStockAlerts: List<LowStockBookSummary> = emptyList(),
    val monthNetProfit: Double = 0.0,
    val monthAnalytics: List<HomeAnalyticsPoint> = emptyList(),
)

data class KhataCustomerDue(
    val customer: KhataCustomer,
    val dueAmount: Double,
    val ageDays: Long,
    val agingBucket: String,
)
