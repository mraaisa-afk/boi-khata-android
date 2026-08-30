package com.boikhata.core.domain.accounting

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * D27: RecurringExpenseCalculator unit tests — next-due logic.
 */
class RecurringExpenseCalculatorTest {

    private val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    private val dayMs = 24L * 60 * 60 * 1000

    private fun millis(year: Int, month: Int, day: Int): Long {
        calendar.clear()
        calendar.set(year, month - 1, day, 12, 0, 0)
        return calendar.timeInMillis
    }

    @Test
    fun `should compute daily next due`() {
        val base = millis(2026, 8, 30)
        val next = RecurringExpenseCalculator.nextDueDate(RecurringExpenseCalculator.Frequency.DAILY, base)
        assertThat(next).isEqualTo(base + 1 * dayMs)
    }

    @Test
    fun `should compute weekly next due`() {
        val base = millis(2026, 8, 30)
        val next = RecurringExpenseCalculator.nextDueDate(RecurringExpenseCalculator.Frequency.WEEKLY, base)
        assertThat(next).isEqualTo(base + 7 * dayMs)
    }

    @Test
    fun `should compute monthly next due`() {
        val base = millis(2026, 8, 30)
        val next = RecurringExpenseCalculator.nextDueDate(RecurringExpenseCalculator.Frequency.MONTHLY, base)
        assertThat(next).isEqualTo(base + 30 * dayMs)
    }

    @Test
    fun `should compute quarterly next due`() {
        val base = millis(2026, 8, 30)
        val next = RecurringExpenseCalculator.nextDueDate(RecurringExpenseCalculator.Frequency.QUARTERLY, base)
        assertThat(next).isEqualTo(base + 90 * dayMs)
    }

    @Test
    fun `should compute yearly next due`() {
        val base = millis(2026, 8, 30)
        val next = RecurringExpenseCalculator.nextDueDate(RecurringExpenseCalculator.Frequency.YEARLY, base)
        assertThat(next).isEqualTo(base + 365 * dayMs)
    }

    @Test
    fun `should report due when next due date has passed`() {
        val base = millis(2026, 8, 1)
        val now = millis(2026, 8, 31)
        assertThat(RecurringExpenseCalculator.isDue(RecurringExpenseCalculator.Frequency.MONTHLY, base, now)).isTrue()
    }

    @Test
    fun `should report not due when next due date has not passed`() {
        val base = millis(2026, 8, 30)
        val now = millis(2026, 8, 30)
        assertThat(RecurringExpenseCalculator.isDue(RecurringExpenseCalculator.Frequency.MONTHLY, base, now)).isFalse()
    }
}
