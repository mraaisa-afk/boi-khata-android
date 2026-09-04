package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.TrialRedemptionDao
import com.boikhata.core.database.entity.TrialRedemptionEntity
import com.boikhata.core.domain.pilot.TrialPolicy
import com.boikhata.core.domain.repository.TrialRedemptionRepository
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

class TrialRedemptionRepositoryImpl @Inject constructor(
    private val dao: TrialRedemptionDao,
) : TrialRedemptionRepository {
    override suspend fun redeemIfEligible(
        tenantId: String,
        deviceFingerprint: String,
        phoneE164: String,
        redeemedAt: Long,
    ): Boolean {
        require(deviceFingerprint.isNotBlank())
        require(phoneE164.isNotBlank())
        val phoneHash = hash(phoneE164)
        if (dao.countByDeviceOrPhone(deviceFingerprint, phoneHash) > 0) return false
        dao.insert(TrialRedemptionEntity(UUID.randomUUID().toString(), tenantId, deviceFingerprint, phoneHash, redeemedAt))
        return true
    }

    override suspend fun getRedemptions(tenantId: String): List<TrialPolicy.KnownRedemption> =
        dao.getByTenant(tenantId).map { TrialPolicy.KnownRedemption(it.deviceFingerprint, it.phoneHash) }

    override suspend fun getTrialStartedAt(tenantId: String): Long? = dao.firstRedeemedAt(tenantId)

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.trim().lowercase().toByteArray())
        .joinToString("") { "%02x".format(it) }
}
