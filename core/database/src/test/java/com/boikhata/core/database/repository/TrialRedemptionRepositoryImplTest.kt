package com.boikhata.core.database.repository

import com.boikhata.core.database.dao.TrialRedemptionDao
import com.boikhata.core.database.entity.TrialRedemptionEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TrialRedemptionRepositoryImplTest {
    @Test
    fun `should persist first redemption and deny same phone when already redeemed`() = runTest {
        val dao = FakeTrialRedemptionDao()
        val repository = TrialRedemptionRepositoryImpl(dao)
        assertThat(repository.redeemIfEligible("t_1", "device-a", "+8801700000000", 10L)).isTrue()
        assertThat(repository.redeemIfEligible("t_1", "device-b", "+8801700000000", 11L)).isFalse()
        assertThat(dao.rows.single().phoneHash).doesNotContain("+8801700000000")
    }

    @Test
    fun `should deny same device even when phone changes`() = runTest {
        val dao = FakeTrialRedemptionDao()
        val repository = TrialRedemptionRepositoryImpl(dao)
        repository.redeemIfEligible("t_1", "device-a", "+8801700000000", 10L)
        assertThat(repository.redeemIfEligible("t_1", "device-a", "+8801800000000", 11L)).isFalse()
    }

    private class FakeTrialRedemptionDao : TrialRedemptionDao {
        val rows = mutableListOf<TrialRedemptionEntity>()
        override suspend fun insert(redemption: TrialRedemptionEntity) { rows += redemption }
        override suspend fun getByTenant(tenantId: String) = rows.filter { it.tenantId == tenantId }
        override suspend fun countByDeviceOrPhone(deviceFingerprint: String, phoneHash: String) =
            rows.count { it.deviceFingerprint == deviceFingerprint || it.phoneHash == phoneHash }
        override suspend fun firstRedeemedAt(tenantId: String): Long? =
            rows.filter { it.tenantId == tenantId }.minOfOrNull { it.redeemedAt }
    }
}
