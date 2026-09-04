package com.boikhata.core.domain.pilot

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit

class PilotPoliciesTest {
    @Test
    fun `should allow full trial writes when within fourteen days and under caps`() {
        val decision = TrialPolicy.evaluate(1_000L, 1_000L + TimeUnit.DAYS.toMillis(13), TrialPolicy.Usage(99, 199))
        assertThat(decision.canWrite).isTrue()
        assertThat(decision.canBackup).isFalse()
        assertThat(decision.readOnly).isFalse()
    }

    @Test
    fun `should become read only when trial expires`() {
        val decision = TrialPolicy.evaluate(1_000L, 1_000L + TrialPolicy.DURATION_MILLIS, TrialPolicy.Usage(0, 0))
        assertThat(decision.reason).isEqualTo(TrialPolicy.Reason.EXPIRED)
        assertThat(decision.readOnly).isTrue()
    }

    @Test
    fun `should become read only when bill cap is reached`() {
        val decision = TrialPolicy.evaluate(1_000L, 2_000L, TrialPolicy.Usage(TrialPolicy.MAX_BILLS, 0))
        assertThat(decision.reason).isEqualTo(TrialPolicy.Reason.BILL_CAP)
    }

    @Test
    fun `should reject redemption when device or phone was already used`() {
        val known = listOf(TrialPolicy.KnownRedemption("device-a", "+8801000000000"))
        assertThat(TrialPolicy.canRedeem(TrialPolicy.Redemption("device-a", "+8802000000000"), known)).isFalse()
        assertThat(TrialPolicy.canRedeem(TrialPolicy.Redemption("device-b", "+8801000000000"), known)).isFalse()
    }

    @Test
    fun `should require otp and rebind before claims transfer`() {
        assertThat(NumberMigrationPolicy.advance(NumberMigrationPolicy.State.IDLE, true, false, false))
            .isEqualTo(NumberMigrationPolicy.State.OTP_VERIFIED)
        assertThat(NumberMigrationPolicy.advance(NumberMigrationPolicy.State.OTP_VERIFIED, true, true, false))
            .isEqualTo(NumberMigrationPolicy.State.REBOUND)
        val finalState = NumberMigrationPolicy.advance(NumberMigrationPolicy.State.REBOUND, true, true, true)
        assertThat(finalState).isEqualTo(NumberMigrationPolicy.State.CLAIMS_TRANSFERRED)
        assertThat(NumberMigrationPolicy.canUseNewNumber(finalState)).isTrue()
    }
}
