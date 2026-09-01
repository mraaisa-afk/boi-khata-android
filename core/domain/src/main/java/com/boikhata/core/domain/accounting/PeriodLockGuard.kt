package com.boikhata.core.domain.accounting

import java.util.Calendar
import java.util.TimeZone

/**
 * D32: Period-lock guard — checks whether a date falls in a locked accounting period.
 * Blueprint §7.7: "পিরিয়ড-লক: বন্ধ মাস অপরিবর্তনীয় (মালিক-অনুমোদিত অ্যাডজাস্টমেন্ট ছাড়া)।"
 *
 * The guard is consulted by write repositories before any money-table insert.
 * Read/export paths do NOT consult the guard (never-lock rule, Three Laws §3).
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 */
object PeriodLockGuard {

    private val utc = TimeZone.getTimeZone("UTC")

    /**
     * A locked period record (year, month).
     */
    data class LockedPeriod(val year: Int, val month: Int) {
        fun key(): String = "$year-$month"
    }

    /**
     * Check if a date falls in any locked period.
     * @param lockedPeriods the set of locked (year, month) pairs
     * @param date the entry date to check (epoch-millis)
     * @return true if the date's year-month is in the locked set
     */
    fun isLocked(lockedPeriods: Set<LockedPeriod>, date: Long): Boolean {
        val (year, month) = yearMonth(date)
        return lockedPeriods.any { it.year == year && it.month == month }
    }

    /**
     * Extract (year, month) from an epoch-millis date.
     */
    fun yearMonth(date: Long): Pair<Int, Int> {
        val cal = Calendar.getInstance(utc)
        cal.clear()
        cal.timeInMillis = date
        return Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }
}

/** Thrown when a write targets a locked accounting period. */
class PeriodLockedException(val year: Int, val month: Int) : Exception(
    "পিরিয়ড তালাবদ্ধ: $year-$month মাস বন্ধ। মালিক-অনুমোদিত অ্যাডজাস্টমেন্ট ছাড়া পরিবর্তন সম্ভব নয়।"
)
