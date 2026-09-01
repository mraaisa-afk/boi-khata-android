package com.boikhata.core.domain.accounting

import java.util.Calendar
import java.util.TimeZone

/**
 * D30: Dual-calendar rollup — Gregorian month + Bengali fiscal year (১ এপ্রিল–৩১ মার্চ).
 * Blueprint §6 + §7.7: "সব মাসিক/বার্ষিক রিপোর্ট দ্বৈত: গ্রেগরিয়ান + বাংলা বর্ষ (১ এপ্রিল–৩১ মার্চ)।"
 *
 * The Bengali fiscal year runs April 1 – March 31 (Gregorian terms).
 * Bengali month index 1-12 maps to Gregorian April-March.
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 */
object BengaliFiscalCalendar {

    private val utc = TimeZone.getTimeZone("UTC")

    /** Bengali month names (Bangla) — index 1..12 → Boishakh..Choitro. */
    private val bengaliMonthNames = listOf(
        "বৈশাখ", "জ্যৈষ্ঠ", "আষাঢ়", "শ্রাবণ", "ভাদ্র", "আশ্বিন",
        "কার্তিক", "অগ্রহায়ণ", "পৌষ", "মাঘ", "ফাল্গুন", "চৈত্র",
    )

    /** Gregorian month names (Bangla) — index 1..12 → January..December. */
    private val gregorianMonthNamesBn = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর",
    )

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    /**
     * Map a Gregorian date to its Bengali fiscal-year start year.
     * The fiscal year starts April 1. A date in Jan-Mar belongs to the FY that started last April;
     * a date in Apr-Dec belongs to the FY that started this April.
     * @return the Gregorian calendar year in which the FY started (e.g. 2026 for FY 2026-27)
     */
    fun toBengaliFiscalYearStart(date: Long): Int {
        val cal = Calendar.getInstance(utc)
        cal.clear()
        cal.timeInMillis = date
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1 // 1..12
        return if (month >= 4) year else year - 1
    }

    /**
     * Bengali fiscal-year label (the start year — used as the FY identifier).
     */
    fun bengaliFiscalYearLabel(date: Long): String {
        return toBengaliFiscalYearStart(date).toString()
    }

    /**
     * Map a Gregorian date to its Bengali month index (1..12).
     * April = 1 (Boishakh), March = 12 (Choitro).
     */
    fun toBengaliMonth(date: Long): Int {
        val cal = Calendar.getInstance(utc)
        cal.clear()
        cal.timeInMillis = date
        val month = cal.get(Calendar.MONTH) + 1 // 1..12
        // April(4)→1, May(5)→2, ..., March(3)→12
        return if (month >= 4) month - 3 else month + 9
    }

    /** Bengali month name for a Bengali month index (1..12). */
    fun bengaliMonthName(bengaliMonth: Int): String {
        return bengaliMonthNames[bengaliMonth - 1]
    }

    /** Bengali month name for a Gregorian date. */
    fun bengaliMonthNameForDate(date: Long): String {
        return bengaliMonthName(toBengaliMonth(date))
    }

    /** Gregorian month name (Bangla) for a Gregorian month index (1..12). */
    fun gregorianMonthNameBn(gregorianMonth: Int): String {
        return gregorianMonthNamesBn[gregorianMonth - 1]
    }

    /** Gregorian month name (Bangla) for a Gregorian date. */
    fun gregorianMonthNameBnForDate(date: Long): String {
        val cal = Calendar.getInstance(utc)
        cal.clear()
        cal.timeInMillis = date
        return gregorianMonthNameBn(cal.get(Calendar.MONTH) + 1)
    }

    /**
     * Start of a Gregorian month (epoch-millis, UTC midnight).
     * @param year the Gregorian year
     * @param month 1..12
     */
    fun gregorianMonthStart(year: Int, month: Int): Long {
        val cal = Calendar.getInstance(utc)
        cal.clear()
        cal.set(year, month - 1, 1, 0, 0, 0)
        return cal.timeInMillis
    }

    /**
     * End of a Gregorian month (exclusive — the first millis of the next month).
     * Use this as the upper bound for date-range queries (date < end).
     */
    fun gregorianMonthEndExclusive(year: Int, month: Int): Long {
        val cal = Calendar.getInstance(utc)
        cal.clear()
        val nextMonth = if (month == 12) 1 else month + 1
        val nextYear = if (month == 12) year + 1 else year
        cal.set(nextYear, nextMonth - 1, 1, 0, 0, 0)
        return cal.timeInMillis
    }

    /**
     * Start of a Bengali fiscal year (April 1 of the start year, UTC midnight).
     * @param fyStartYear the Gregorian year in which the FY started (e.g. 2026 for FY 2026-27)
     */
    fun bengaliFiscalYearStart(fyStartYear: Int): Long {
        return gregorianMonthStart(fyStartYear, 4) // April = month 4
    }

    /**
     * End of a Bengali fiscal year (exclusive — April 1 of the next year).
     */
    fun bengaliFiscalYearEndExclusive(fyStartYear: Int): Long {
        return gregorianMonthStart(fyStartYear + 1, 4)
    }

    /**
     * The Bengali month index that corresponds to a Gregorian month.
     * April(4)→1 ... March(3)→12.
     */
    fun gregorianToBengaliMonth(gregorianMonth: Int): Int {
        return if (gregorianMonth >= 4) gregorianMonth - 3 else gregorianMonth + 9
    }
}
