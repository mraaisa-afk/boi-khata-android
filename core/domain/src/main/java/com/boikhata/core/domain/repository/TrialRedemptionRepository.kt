package com.boikhata.core.domain.repository

import com.boikhata.core.domain.pilot.TrialPolicy

interface TrialRedemptionRepository {
    suspend fun redeemIfEligible(
        tenantId: String,
        deviceFingerprint: String,
        phoneE164: String,
        redeemedAt: Long,
    ): Boolean

    suspend fun getRedemptions(tenantId: String): List<TrialPolicy.KnownRedemption>

    suspend fun getTrialStartedAt(tenantId: String): Long?
}
