package com.boikhata.core.domain.p8

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class P8PoliciesTest {
    @Test fun `should report update when latest version code is higher`() {
        assertThat(VersionPolicy.updateAvailable(3, VersionPolicy.Release(4, "0.8.1", "https://example.invalid/app.apk"))).isTrue()
        assertThat(VersionPolicy.updateAvailable(4, VersionPolicy.Release(4, "0.8.1", null))).isFalse()
    }

    @Test fun `should generate stable referral code when tenant id is unchanged`() {
        assertThat(ReferralCodeGenerator.codeForTenant("tenant-a")).isEqualTo(ReferralCodeGenerator.codeForTenant("tenant-a"))
        assertThat(ReferralCodeGenerator.codeForTenant("tenant-a")).isNotEqualTo(ReferralCodeGenerator.codeForTenant("tenant-b"))
    }

    @Test fun `should enforce lite device limit when active count reaches two`() {
        assertThat(DeviceGroupPolicy.canAddDevice(1)).isTrue()
        assertThat(DeviceGroupPolicy.canAddDevice(2)).isFalse()
    }

    @Test fun `should require confirmation before demo reset`() {
        assertThat(DemoResetPolicy.request().requiresConfirmation).isTrue()
    }

    @Test fun `should allow founders pricing only for first fifty tenants`() {
        assertThat(FoundersClubPolicy.isEligible(1)).isTrue()
        assertThat(FoundersClubPolicy.isEligible(50)).isTrue()
        assertThat(FoundersClubPolicy.isEligible(51)).isFalse()
    }
}
