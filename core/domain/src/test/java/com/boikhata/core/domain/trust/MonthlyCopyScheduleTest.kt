package com.boikhata.core.domain.trust

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class MonthlyCopyScheduleTest {
    private val dhaka = ZoneId.of("Asia/Dhaka")

    @Test
    fun `should schedule the next first day when current month first day has passed`() {
        val now = Instant.parse("2026-09-04T04:00:00Z").toEpochMilli()
        val next = Instant.ofEpochMilli(MonthlyCopySchedule.nextFirstDayAt(now, dhaka))
        assertThat(next.toString()).isEqualTo("2026-10-01T03:00:00Z")
    }

    @Test
    fun `should schedule today's first day when before nine in the morning`() {
        val now = Instant.parse("2026-09-01T02:00:00Z").toEpochMilli()
        val next = Instant.ofEpochMilli(MonthlyCopySchedule.nextFirstDayAt(now, dhaka))
        assertThat(next.toString()).isEqualTo("2026-09-01T03:00:00Z")
    }

    @Test
    fun `should identify the first day in the local timezone`() {
        val first = Instant.parse("2026-09-01T00:00:00Z").toEpochMilli()
        val second = Instant.parse("2026-09-02T00:00:00Z").toEpochMilli()
        assertThat(MonthlyCopySchedule.isFirstDay(first, dhaka)).isTrue()
        assertThat(MonthlyCopySchedule.isFirstDay(second, dhaka)).isFalse()
    }
}
