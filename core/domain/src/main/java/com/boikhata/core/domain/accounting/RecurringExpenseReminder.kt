package com.boikhata.core.domain.accounting

/**
 * D35: Recurring-expense due-reminder.
 * D27 implemented the next-due calculator; this service uses it to find due templates.
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 */
object RecurringExpenseReminder {

    /** A recurring-expense template for the reminder. */
    data class RecurringTemplate(
        val id: String,
        val categoryId: String,
        val categoryNameBn: String,
        val amount: Double,
        val description: String,
        val frequency: RecurringExpenseCalculator.Frequency,
        val lastAppliedDate: Long,
        val nextDueDate: Long,
    )

    /**
     * Find templates that are due as of `now`.
     * @param templates all active recurring templates
     * @param now the current time (epoch-millis)
     * @return templates whose nextDueDate has passed, sorted by nextDueDate asc
     */
    fun findDue(templates: List<RecurringTemplate>, now: Long): List<RecurringTemplate> {
        return templates.filter { it.nextDueDate <= now }
            .sortedBy { it.nextDueDate }
    }

    /**
     * Count how many templates are due.
     */
    fun countDue(templates: List<RecurringTemplate>, now: Long): Int {
        return templates.count { it.nextDueDate <= now }
    }

    /**
     * Compute the next due date for a template after an application, using the calculator.
     */
    fun nextDueAfterApply(
        frequency: RecurringExpenseCalculator.Frequency,
        appliedDate: Long,
    ): Long {
        return RecurringExpenseCalculator.nextDueDate(frequency, appliedDate)
    }
}
