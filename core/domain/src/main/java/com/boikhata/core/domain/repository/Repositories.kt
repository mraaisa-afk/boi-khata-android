package com.boikhata.core.domain.repository

import com.boikhata.core.domain.license.GraceState
import com.boikhata.core.domain.license.LicenseWriteGuard

/**
 * Repository interfaces — core/domain owns the contracts; core/database + core/cloud
 * provide the implementations. Feature modules depend on these interfaces (CONVENTIONS §6).
 */

interface UserRepository {
    suspend fun getUsers(tenantId: String): List<com.boikhata.core.domain.model.User>
    suspend fun verifyPin(tenantId: String, pin: String): com.boikhata.core.domain.model.User?
}

interface KhataRepository {
    suspend fun getCustomers(tenantId: String): List<com.boikhata.core.domain.model.KhataCustomer>
    suspend fun getEntries(tenantId: String, customerId: String): List<com.boikhata.core.domain.aging.KhataEntry>
}

interface BillRepository {
    suspend fun getBillsByDate(tenantId: String, startOfDay: Long, endOfDay: Long): List<com.boikhata.core.domain.model.BillSummary>
    suspend fun getTopBills(tenantId: String, limit: Int): List<com.boikhata.core.domain.model.BillSummary>
}

interface LicenseRepository {
    suspend fun getLicenseState(tenantId: String): com.boikhata.core.domain.enums.LicenseState
    suspend fun evaluateCurrentGrace(tenantId: String, now: Long): GraceState
    fun getWriteGuard(): LicenseWriteGuard
    suspend fun setWifiOnlySync(tenantId: String, enabled: Boolean)
    suspend fun isWifiOnlySync(tenantId: String): Boolean
}
