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
 * Repository computes currentStock from stock ledger; feature module needs no Room.
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
 * নিট লাভ = todaySalesTotal − todayExpenseTotal (সরল নগদ বিয়োগ, বাকি বাদ).
 * yesterdayNetProfit used for ▲/▼ trend delta badge.
 * D75 Trident arms retained: cashBalance, totalDue, supplierDuesTotal.
 * PR D: lowStockAlerts added for «আজকের করণীয়» section.
 * PR E: monthNetProfit + monthAnalytics added for «বিশ্লেষণ» mini bar sparkline.
 */
data class HomeData(
    val totalDue: Double,
    val dueCustomerCount: Int,
    val todaySalesTotal: Double,
    val todayExpenseTotal: Double = 0.0,
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
