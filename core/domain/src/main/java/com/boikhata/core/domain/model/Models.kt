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
 * D75: Trident dashboard data model.
 * Three Trident arms: cashBalance (নগদ), totalDue (গ্রাহক বাকি), supplierDuesTotal (সাপ্লায়ার পাওনা).
 * Blueprint §2: Trident numbers only — no chart, no additional metric, no percentage.
 */
data class HomeData(
    val totalDue: Double,
    val dueCustomerCount: Int,
    val todaySalesTotal: Double,
    val todayBillCount: Int,
    val topDueCustomers: List<KhataCustomerDue>,
    val cashBalance: Double,        // D75: নগদ ব্যালেন্স (CASH account balance from cashbook)
    val supplierDuesTotal: Double,  // D75: সাপ্লায়ার পাওনা (total supplier payable)
    val supplierCount: Int,         // D75: number of suppliers with outstanding balance
)

data class KhataCustomerDue(
    val customer: KhataCustomer,
    val dueAmount: Double,
    val ageDays: Long,
    val agingBucket: String, // GREEN / YELLOW / RED / NONE
)
