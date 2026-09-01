package com.boikhata.core.domain.accounting

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * D30: BengaliFiscalCalendar unit tests — dual-calendar rollup.
 */
class BengaliFiscalCalendarTest {

    private val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

    private fun millis(year: Int, month: Int, day: Int): Long {
        cal.clear()
        cal.set(year, month - 1, day, 12, 0, 0)
        return cal.timeInMillis
    }

    @Test
    fun `should map April date to FY starting same year`() {
        val date = millis(2026, 4, 15) // April 2026
        assertThat(BengaliFiscalCalendar.toBengaliFiscalYearStart(date)).isEqualTo(2026)
    }

    @Test
    fun `should map December date to FY starting same year`() {
        val date = millis(2026, 12, 20)
        assertThat(BengaliFiscalCalendar.toBengaliFiscalYearStart(date)).isEqualTo(2026)
    }

    @Test
    fun `should map January date to FY starting previous year`() {
        val date = millis(2027, 1, 10) // Jan 2027 → FY 2026-27
        assertThat(BengaliFiscalCalendar.toBengaliFiscalYearStart(date)).isEqualTo(2026)
    }

    @Test
    fun `should map March date to FY starting previous year`() {
        val date = millis(2027, 3, 31) // March 2027 → FY 2026-27
        assertThat(BengaliFiscalCalendar.toBengaliFiscalYearStart(date)).isEqualTo(2026)
    }

    @Test
    fun `should map April to Bengali month 1 Boishakh`() {
        val date = millis(2026, 4, 1)
        assertThat(BengaliFiscalCalendar.toBengaliMonth(date)).isEqualTo(1)
        assertThat(BengaliFiscalCalendar.bengaliMonthNameForDate(date)).isEqualTo("বৈশাখ")
    }

    @Test
    fun `should map May to Bengali month 2 Joishtho`() {
        val date = millis(2026, 5, 15)
        assertThat(BengaliFiscalCalendar.toBengaliMonth(date)).isEqualTo(2)
        assertThat(BengaliFiscalCalendar.bengaliMonthNameForDate(date)).isEqualTo("জ্যৈষ্ঠ")
    }

    @Test
    fun `should map March to Bengali month 12 Choitro`() {
        val date = millis(2027, 3, 31)
        assertThat(BengaliFiscalCalendar.toBengaliMonth(date)).isEqualTo(12)
        assertThat(BengaliFiscalCalendar.bengaliMonthNameForDate(date)).isEqualTo("চৈত্র")
    }

    @Test
    fun `should map January to Bengali month 10 Magh`() {
        // FY Apr2026-Mar2027: Dec=Poush(9), Jan=Magh(10), Feb=Falgun(11), Mar=Choitro(12)
        val date = millis(2027, 1, 5)
        assertThat(BengaliFiscalCalendar.toBengaliMonth(date)).isEqualTo(10)
        assertThat(BengaliFiscalCalendar.bengaliMonthNameForDate(date)).isEqualTo("মাঘ")
    }

    @Test
    fun `should map December to Bengali month 9 Poush`() {
        val date = millis(2026, 12, 15)
        assertThat(BengaliFiscalCalendar.toBengaliMonth(date)).isEqualTo(9)
        assertThat(BengaliFiscalCalendar.bengaliMonthNameForDate(date)).isEqualTo("পৌষ")
    }

    @Test
    fun `should return all 12 Bengali month names`() {
        assertThat(BengaliFiscalCalendar.bengaliMonthName(1)).isEqualTo("বৈশাখ")
        assertThat(BengaliFiscalCalendar.bengaliMonthName(6)).isEqualTo("আশ্বিন")
        assertThat(BengaliFiscalCalendar.bengaliMonthName(12)).isEqualTo("চৈত্র")
    }

    @Test
    fun `should compute gregorian month start as UTC midnight on the 1st`() {
        val start = BengaliFiscalCalendar.gregorianMonthStart(2026, 9) // Sep 2026
        cal.clear()
        cal.timeInMillis = start
        assertThat(cal.get(Calendar.YEAR)).isEqualTo(2026)
        assertThat(cal.get(Calendar.MONTH)).isEqualTo(8) // 0-indexed
        assertThat(cal.get(Calendar.DAY_OF_MONTH)).isEqualTo(1)
        assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(0)
    }

    @Test
    fun `should compute gregorian month end exclusive as first millis of next month`() {
        val end = BengaliFiscalCalendar.gregorianMonthEndExclusive(2026, 9) // end of Sep
        cal.clear()
        cal.timeInMillis = end
        assertThat(cal.get(Calendar.YEAR)).isEqualTo(2026)
        assertThat(cal.get(Calendar.MONTH)).isEqualTo(9) // October (0-indexed)
        assertThat(cal.get(Calendar.DAY_OF_MONTH)).isEqualTo(1)
    }

    @Test
    fun `should compute gregorian month end exclusive across year boundary`() {
        val end = BengaliFiscalCalendar.gregorianMonthEndExclusive(2026, 12) // end of Dec
        cal.clear()
        cal.timeInMillis = end
        assertThat(cal.get(Calendar.YEAR)).isEqualTo(2027)
        assertThat(cal.get(Calendar.MONTH)).isEqualTo(0) // January
    }

    @Test
    fun `should compute bengali fiscal year start as April 1`() {
        val start = BengaliFiscalCalendar.bengaliFiscalYearStart(2026)
        cal.clear()
        cal.timeInMillis = start
        assertThat(cal.get(Calendar.YEAR)).isEqualTo(2026)
        assertThat(cal.get(Calendar.MONTH)).isEqualTo(3) // April (0-indexed)
        assertThat(cal.get(Calendar.DAY_OF_MONTH)).isEqualTo(1)
    }

    @Test
    fun `should compute bengali fiscal year end exclusive as April 1 next year`() {
        val end = BengaliFiscalCalendar.bengaliFiscalYearEndExclusive(2026)
        cal.clear()
        cal.timeInMillis = end
        assertThat(cal.get(Calendar.YEAR)).isEqualTo(2027)
        assertThat(cal.get(Calendar.MONTH)).isEqualTo(3) // April
    }

    @Test
    fun `should map gregorian month to bengali month`() {
        assertThat(BengaliFiscalCalendar.gregorianToBengaliMonth(4)).isEqualTo(1)  // April → Boishakh
        assertThat(BengaliFiscalCalendar.gregorianToBengaliMonth(5)).isEqualTo(2)  // May → Joishtho
        assertThat(BengaliFiscalCalendar.gregorianToBengaliMonth(3)).isEqualTo(12) // March → Choitro
        assertThat(BengaliFiscalCalendar.gregorianToBengaliMonth(1)).isEqualTo(10) // Jan → Poush
    }

    @Test
    fun `should return gregorian month name in Bangla`() {
        assertThat(BengaliFiscalCalendar.gregorianMonthNameBn(1)).isEqualTo("জানুয়ারি")
        assertThat(BengaliFiscalCalendar.gregorianMonthNameBn(9)).isEqualTo("সেপ্টেম্বর")
        assertThat(BengaliFiscalCalendar.gregorianMonthNameBn(12)).isEqualTo("ডিসেম্বর")
    }
}
