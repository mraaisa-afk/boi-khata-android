package com.boikhata.core.domain.license

import com.boikhata.core.domain.enums.LicenseState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LicensePolicyTest {

    private val DAY = 24L * 60 * 60 * 1000
    private val NOW = 1_700_000_000_000L // fixed reference

    // ── isInsideFestivalWindow ──────────────────────────────────────────────

    @Test
    fun `should return true when now is inside a festival window`() {
        val windows = listOf(FestivalWindow(NOW - DAY, NOW + DAY))
        assertThat(LicensePolicy.isInsideFestivalWindow(windows, NOW)).isTrue()
    }

    @Test
    fun `should return false when now is before all windows`() {
        val windows = listOf(FestivalWindow(NOW + DAY, NOW + 2 * DAY))
        assertThat(LicensePolicy.isInsideFestivalWindow(windows, NOW)).isFalse()
    }

    @Test
    fun `should return false when now is after all windows`() {
        val windows = listOf(FestivalWindow(NOW - 3 * DAY, NOW - 2 * DAY))
        assertThat(LicensePolicy.isInsideFestivalWindow(windows, NOW)).isFalse()
    }

    @Test
    fun `should return false for empty windows list`() {
        assertThat(LicensePolicy.isInsideFestivalWindow(emptyList(), NOW)).isFalse()
    }

    @Test
    fun `should return true when now is at window boundary (inclusive)`() {
        val windows = listOf(FestivalWindow(NOW, NOW + DAY))
        assertThat(LicensePolicy.isInsideFestivalWindow(windows, NOW)).isTrue()
    }

    // ── evaluateGrace — grace boundary ───────────────────────────────────────

    @Test
    fun `should return GRACE when now is within 14 days of last verified`() {
        val lastVerified = NOW
        val result = LicensePolicy.evaluateGrace(lastVerified, null, NOW + 5 * DAY)
        assertThat(result.state).isEqualTo(LicenseState.GRACE)
        assertThat(result.isWriteBlocked).isFalse()
        assertThat(result.isReadBlocked).isFalse()
    }

    @Test
    fun `should return GRACE when now is within 14 days of last payment`() {
        val lastPayment = NOW
        val result = LicensePolicy.evaluateGrace(null, lastPayment, NOW + 10 * DAY)
        assertThat(result.state).isEqualTo(LicenseState.GRACE)
    }

    @Test
    fun `should use max of lastVerified and lastPayment as anchor`() {
        val lastVerified = NOW - 10 * DAY
        val lastPayment = NOW
        // anchor = NOW (payment is later), grace ends at NOW + 14d
        val result = LicensePolicy.evaluateGrace(lastVerified, lastPayment, NOW + 5 * DAY)
        assertThat(result.state).isEqualTo(LicenseState.GRACE)
    }

    @Test
    fun `should return SOFT_LOCKED when grace expires but before 30 days`() {
        val lastVerified = NOW
        val result = LicensePolicy.evaluateGrace(lastVerified, null, NOW + 20 * DAY)
        assertThat(result.state).isEqualTo(LicenseState.SOFT_LOCKED)
        assertThat(result.isWriteBlocked).isTrue()
        assertThat(result.isReadBlocked).isFalse() // never-lock
    }

    @Test
    fun `should return SUSPENDED when past 30 days`() {
        val lastVerified = NOW
        val result = LicensePolicy.evaluateGrace(lastVerified, null, NOW + 45 * DAY)
        assertThat(result.state).isEqualTo(LicenseState.SUSPENDED)
        assertThat(result.isWriteBlocked).isTrue()
        assertThat(result.isReadBlocked).isFalse() // never-lock even when suspended
    }

    // ── never-lock: paid tenant never read-only ──────────────────────────────

    @Test
    fun `should never block reads even when suspended`() {
        val lastVerified = NOW
        val result = LicensePolicy.evaluateGrace(lastVerified, null, NOW + 90 * DAY)
        assertThat(result.isReadBlocked).isFalse()
    }

    @Test
    fun `should never block reads at grace boundary exactly`() {
        val lastVerified = NOW
        val result = LicensePolicy.evaluateGrace(lastVerified, null, NOW + 14 * DAY)
        // exactly at grace end → SOFT_LOCKED, but reads still open
        assertThat(result.isReadBlocked).isFalse()
    }

    // ── festival protection ──────────────────────────────────────────────────

    @Test
    fun `should stay GRACE during festival even if grace expired`() {
        val lastVerified = NOW
        val windows = listOf(FestivalWindow(NOW + 20 * DAY - 1000, NOW + 20 * DAY + 1000))
        val result = LicensePolicy.evaluateGrace(NOW, null, NOW + 20 * DAY, isInsideFestival = true)
        assertThat(result.state).isEqualTo(LicenseState.GRACE)
        assertThat(result.isWriteBlocked).isFalse()
    }

    @Test
    fun `should SOFT_LOCK when grace expired and NOT inside festival`() {
        val lastVerified = NOW
        val result = LicensePolicy.evaluateGrace(lastVerified, null, NOW + 20 * DAY, isInsideFestival = false)
        assertThat(result.state).isEqualTo(LicenseState.SOFT_LOCKED)
    }

    // ── null inputs ──────────────────────────────────────────────────────────

    @Test
    fun `should treat null lastVerified and null lastPayment as epoch zero`() {
        val result = LicensePolicy.evaluateGrace(null, null, NOW)
        // anchor = 0, grace ended long ago → SOFT_LOCKED or SUSPENDED
        assertThat(result.state).isNotEqualTo(LicenseState.GRACE)
        assertThat(result.isReadBlocked).isFalse()
    }

    // ── evaluateContactDegrade — 35-day rule ─────────────────────────────────

    @Test
    fun `should return true for contact degrade when no server contact`() {
        assertThat(LicensePolicy.evaluateContactDegrade(null, NOW)).isTrue()
    }

    @Test
    fun `should return false when server contact within 35 days`() {
        assertThat(LicensePolicy.evaluateContactDegrade(NOW - 10 * DAY, NOW)).isFalse()
    }

    @Test
    fun `should return true when server contact over 35 days ago`() {
        assertThat(LicensePolicy.evaluateContactDegrade(NOW - 36 * DAY, NOW)).isTrue()
    }

    @Test
    fun `should return false when server contact exactly 35 days ago`() {
        assertThat(LicensePolicy.evaluateContactDegrade(NOW - 35 * DAY, NOW)).isFalse()
    }
}
