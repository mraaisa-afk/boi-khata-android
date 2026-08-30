package com.boikhata.core.domain.accounting

/**
 * D27: Recurring expense next-due calculator.
 * Pure-logic piece for computing when a recurring expense is next due.
 * Persistence + auto-trigger = P3b scope.
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 */
object RecurringExpenseCalculator {

    enum class Frequency {
        DAILY,
        WEEKLY,
        MONTHLY,
        QUARTERLY,
        YEARLY,
    }

    private const val DAY_MS = 24L * 60 * 60 * 1000

    /**
     * Compute the next due date for a recurring expense.
     * @param frequency the recurrence pattern
     * @param lastAppliedDate the date it was last applied (epoch-millis)
     * @return the next due date (epoch-millis)
     */
    fun nextDueDate(frequency: Frequency, lastAppliedDate: Long): Long {
        return when (frequency) {
            Frequency.DAILY -> lastAppliedDate + 1 * DAY_MS
            Frequency.WEEKLY -> lastAppliedDate + 7 * DAY_MS
            Frequency.MONTHLY -> lastAppliedDate + 30 * DAY_MS
            Frequency.QUARTERLY -> lastAppliedDate + 90 * DAY_MS
            Frequency.YEARLY -> lastAppliedDate + 365 * DAY_MS
        }
    }

    /**
     * Check if a recurring expense is due as of now.
     * @param frequency the recurrence pattern
     * @param lastAppliedDate the date it was last applied
     * @param now the current time
     * @return true if the next due date has passed
     */
    fun isDue(frequency: Frequency, lastAppliedDate: Long, now: Long): Boolean {
        return nextDueDate(frequency, lastAppliedDate) <= now
    }
}
