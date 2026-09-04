package com.boikhata.core.domain.trust

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** P7b scheduling policy: one one-time request is chained to the next local month's first day. */
object MonthlyCopySchedule {
    fun nextFirstDayAt(nowMillis: Long, zone: ZoneId): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val next = ZonedDateTime.of(now.year, now.monthValue, 1, 9, 0, 0, 0, zone)
            .let { if (it.toInstant().toEpochMilli() > nowMillis) it else it.plusMonths(1) }
        return next.toInstant().toEpochMilli()
    }

    fun isFirstDay(nowMillis: Long, zone: ZoneId): Boolean =
        Instant.ofEpochMilli(nowMillis).atZone(zone).dayOfMonth == 1
}
