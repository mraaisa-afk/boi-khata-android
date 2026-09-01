package com.boikhata.core.domain.accounting

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D35: RecurringExpenseReminder unit tests — due-reminder mechanism.
 */
class RecurringExpenseReminderTest {

    private fun template(
        id: String,
        nextDue: Long,
        frequency: RecurringExpenseCalculator.Frequency = RecurringExpenseCalculator.Frequency.MONTHLY,
    ) = RecurringExpenseReminder.RecurringTemplate(
        id = id,
        categoryId = "c1",
        categoryNameBn = "ভাড়া",
        amount = 5000.0,
        description = "মাসিক ভাড়া",
        frequency = frequency,
        lastAppliedDate = nextDue - 30L * 24 * 60 * 60 * 1000,
        nextDueDate = nextDue,
    )

    private val dayMs = 24L * 60 * 60 * 1000

    @Test
    fun `should find templates whose nextDueDate has passed`() {
        val now = 1000000L
        val due = template("t1", nextDue = now - dayMs) // due yesterday
        val notDue = template("t2", nextDue = now + dayMs) // due tomorrow
        val result = RecurringExpenseReminder.findDue(listOf(due, notDue), now)
        assertThat(result).containsExactly(due)
    }

    @Test
    fun `should find no due templates when all are future`() {
        val now = 1000000L
        val future1 = template("t1", nextDue = now + dayMs)
        val future2 = template("t2", nextDue = now + 7 * dayMs)
        val result = RecurringExpenseReminder.findDue(listOf(future1, future2), now)
        assertThat(result).isEmpty()
    }

    @Test
    fun `should sort due templates by nextDueDate ascending`() {
        val now = 1000000L
        val later = template("t1", nextDue = now - dayMs)
        val earlier = template("t2", nextDue = now - 5 * dayMs)
        val result = RecurringExpenseReminder.findDue(listOf(later, earlier), now)
        assertThat(result[0].id).isEqualTo("t2") // earlier first
        assertThat(result[1].id).isEqualTo("t1")
    }

    @Test
    fun `should count due templates`() {
        val now = 1000000L
        val templates = listOf(
            template("t1", nextDue = now - dayMs),
            template("t2", nextDue = now - 2 * dayMs),
            template("t3", nextDue = now + dayMs),
        )
        assertThat(RecurringExpenseReminder.countDue(templates, now)).isEqualTo(2)
    }

    @Test
    fun `should compute next due after apply using the calculator`() {
        val appliedDate = 1000000L
        val next = RecurringExpenseReminder.nextDueAfterApply(
            RecurringExpenseCalculator.Frequency.MONTHLY, appliedDate,
        )
        assertThat(next).isEqualTo(appliedDate + 30 * dayMs)
    }

    @Test
    fun `should compute next due for weekly frequency after apply`() {
        val appliedDate = 1000000L
        val next = RecurringExpenseReminder.nextDueAfterApply(
            RecurringExpenseCalculator.Frequency.WEEKLY, appliedDate,
        )
        assertThat(next).isEqualTo(appliedDate + 7 * dayMs)
    }

    @Test
    fun `should include template due exactly now`() {
        val now = 1000000L
        val dueNow = template("t1", nextDue = now)
        val result = RecurringExpenseReminder.findDue(listOf(dueNow), now)
        assertThat(result).containsExactly(dueNow)
    }
}
