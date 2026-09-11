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
    val classLevel: String,      // e.g. "এচএসসি", "এসএসসি", "Class 8"
    val currentStock: Int,       // effective stock from stock ledger
    val lowStockThreshold: Int,  // per-book configured threshold (default 3)
)

/**
 * D79 §2.2: HomeScreen hero card data model.
 * নিট লাভ = todaySalesTotal − todayExpenseTotal (সরল নগদ বিয়োগ, বাকি বাদ).
 * yesterdayNetProfit used for ▲/▼ trend delta badge.
 * D75 Trident arms retained: cashBalance, totalDue, supplierDuesTotal.
 * PR D: lowStockAlerts added for «আজকের করণীয়» section.
 */
data class HomeData(
    val totalDue: Double,
    val dueCustomerCount: Int,
    val todaySalesTotal: Double,                           // D79: আয়
    val todayExpenseTotal: Double = 0.0,                   // D79: ব্যয়
    val todayBillCount: Int,
    val topDueCustomers: List<KhataCustomerDue>,
    val cashBalance: Double,                               // D75: নগদ ব্যালেন্স
    val supplierDuesTotal: Double,                         // D75: সাপ্লায়ার পাওনা
    val supplierCount: Int,                                // D75
    val yesterdayNetProfit: Double = 0.0,                  // D79 §2.2: trend delta
    val lowStockAlerts: List<LowStockBookSummary> = emptyList(), // D79 PR D: আজকের করণীয়
)

data class KhataCustomerDue(
    val customer: KhataCustomer,
    val dueAmount: Double,
    val ageDays: Long,
    val agingBucket: String, // GREEN / YELLOW / RED / NONE
)
