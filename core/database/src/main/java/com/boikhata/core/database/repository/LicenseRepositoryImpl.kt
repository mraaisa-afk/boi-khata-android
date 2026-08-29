package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.CloudSyncStateDao
import com.boikhata.core.domain.enums.LicenseState
import com.boikhata.core.domain.license.GraceState
import com.boikhata.core.domain.license.LicensePolicy
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.repository.LicenseRepository
import javax.inject.Inject

class LicenseRepositoryImpl @Inject constructor(
    private val cloudSyncStateDao: CloudSyncStateDao,
    private val writeGuard: LicenseWriteGuard,
) : LicenseRepository {

    override suspend fun getLicenseState(tenantId: String): LicenseState {
        val state = cloudSyncStateDao.get()
        ?: return LicenseState.GRACE // C9: GRACE default, never ACTIVE
        val licenseState = LicenseState.valueOf(state.licenseState)
        writeGuard.updateState(licenseState)
        return licenseState
    }

    override suspend fun evaluateCurrentGrace(tenantId: String, now: Long): GraceState {
        val state = cloudSyncStateDao.get()
        ?: return LicensePolicy.evaluateGrace(null, null, now)
        return LicensePolicy.evaluateGrace(
            lastVerified = state.lastBackupAt,
            lastPayment = state.licenseExpiresAt,
            now = now,
        )
    }

    override fun getWriteGuard(): LicenseWriteGuard = writeGuard

    override suspend fun setWifiOnlySync(tenantId: String, enabled: Boolean) {
        cloudSyncStateDao.updateWifiOnlySync(enabled, System.currentTimeMillis())
    }

    override suspend fun isWifiOnlySync(tenantId: String): Boolean {
        return cloudSyncStateDao.get()?.wifiOnlySync ?: true
    }
}
