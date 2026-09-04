package com.boikhata.core.domain.accounting

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * D32: PeriodLockGuard unit tests — locked-period immutability + never-lock on read.
 */
class PeriodLockGuardTest {

    private val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

    private fun millis(year: Int, month: Int, day: Int): Long {
        cal.clear()
        cal.set(year, month - 1, day, 12, 0, 0)
        return cal.timeInMillis
    }

    @Test
    fun `should report locked when date is in a locked period`() {
        val locked = setOf(PeriodLockGuard.LockedPeriod(2026, 8))
        val date = millis(2026, 8, 15)
        assertThat(PeriodLockGuard.isLocked(locked, date)).isTrue()
    }

    @Test
    fun `should report not locked when date is in an open period`() {
        val locked = setOf(PeriodLockGuard.LockedPeriod(2026, 8))
        val date = millis(2026, 9, 15)
        assertThat(PeriodLockGuard.isLocked(locked, date)).isFalse()
    }

    @Test
    fun `should report not locked when no periods are locked`() {
        val date = millis(2026, 8, 15)
        assertThat(PeriodLockGuard.isLocked(emptySet(), date)).isFalse()
    }

    @Test
    fun `should check multiple locked periods`() {
        val locked = setOf(
            PeriodLockGuard.LockedPeriod(2026, 7),
            PeriodLockGuard.LockedPeriod(2026, 8),
        )
        assertThat(PeriodLockGuard.isLocked(locked, millis(2026, 7, 31))).isTrue()
        assertThat(PeriodLockGuard.isLocked(locked, millis(2026, 8, 1))).isTrue()
        assertThat(PeriodLockGuard.isLocked(locked, millis(2026, 9, 1))).isFalse()
    }

    @Test
    fun `should extract year and month from date`() {
        val (year, month) = PeriodLockGuard.yearMonth(millis(2026, 9, 15))
        assertThat(year).isEqualTo(2026)
        assertThat(month).isEqualTo(9)
    }

    @Test
    fun `should lock by year-month not by day`() {
        val locked = setOf(PeriodLockGuard.LockedPeriod(2026, 8))
        // Any day in August 2026 is locked
        assertThat(PeriodLockGuard.isLocked(locked, millis(2026, 8, 1))).isTrue()
        assertThat(PeriodLockGuard.isLocked(locked, millis(2026, 8, 31))).isTrue()
    }

    @Test
    fun `should not lock adjacent months`() {
        val locked = setOf(PeriodLockGuard.LockedPeriod(2026, 8))
        assertThat(PeriodLockGuard.isLocked(locked, millis(2026, 7, 31))).isFalse()
        assertThat(PeriodLockGuard.isLocked(locked, millis(2026, 9, 1))).isFalse()
    }

    @Test
    fun `should produce LockedPeriod key as year-month`() {
        val lp = PeriodLockGuard.LockedPeriod(2026, 8)
        assertThat(lp.key()).isEqualTo("2026-8")
    }

    @Test
    fun `PeriodLockedException should carry year and month`() {
        val ex = PeriodLockedException(2026, 8)
        assertThat(ex.year).isEqualTo(2026)
        assertThat(ex.month).isEqualTo(8)
        assertThat(ex.message).contains("2026-8")
    }
}
