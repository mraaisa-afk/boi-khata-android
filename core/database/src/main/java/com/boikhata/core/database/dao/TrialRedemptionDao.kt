package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.boikhata.core.database.entity.TrialRedemptionEntity

@Dao
interface TrialRedemptionDao {
    @Insert
    suspend fun insert(redemption: TrialRedemptionEntity)

    @Query("SELECT * FROM trial_redemptions WHERE tenantId = :tenantId")
    suspend fun getByTenant(tenantId: String): List<TrialRedemptionEntity>

    @Query("SELECT COUNT(*) FROM trial_redemptions WHERE deviceFingerprint = :deviceFingerprint OR phoneHash = :phoneHash")
    suspend fun countByDeviceOrPhone(deviceFingerprint: String, phoneHash: String): Int

    @Query("SELECT MIN(redeemedAt) FROM trial_redemptions WHERE tenantId = :tenantId")
    suspend fun firstRedeemedAt(tenantId: String): Long?
}
