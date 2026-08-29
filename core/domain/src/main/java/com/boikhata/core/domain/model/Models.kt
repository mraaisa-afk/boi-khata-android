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

data class HomeData(
    val totalDue: Double,
    val dueCustomerCount: Int,
    val todaySalesTotal: Double,
    val todayBillCount: Int,
    val topDueCustomers: List<KhataCustomerDue>,
)

data class KhataCustomerDue(
    val customer: KhataCustomer,
    val dueAmount: Double,
    val ageDays: Long,
    val agingBucket: String, // GREEN / YELLOW / RED / NONE
)
