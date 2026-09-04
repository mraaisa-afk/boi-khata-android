package com.boikhata.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** P7b anti-farm fact; no cloud write and no business-money mutation. */
@Entity(
    tableName = "trial_redemptions",
    indices = [],
)
data class TrialRedemptionEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val deviceFingerprint: String,
    val phoneHash: String,
    val redeemedAt: Long,
)
