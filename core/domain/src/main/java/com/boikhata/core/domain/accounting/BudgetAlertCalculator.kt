package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.model.Expense

/**
 * D35: Monthly budget alert calculator.
 * Blueprint §7.8: "মাসিক বাজেট-অ্যালার্ট।"
 *
 * Compares a month's actual expenses (by category) against budgets and returns
 * alerts where actual spending has reached or exceeded the threshold fraction
 * of the budget limit.
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 */
object BudgetAlertCalculator {

    /** A budget limit for one expense category. */
    data class Budget(
        val categoryId: String,
        val categoryNameBn: String,
        val monthlyLimit: Double,
    )

    /** Severity of a budget alert. */
    enum class Severity { WARNING, OVER }

    /** A triggered alert. */
    data class BudgetAlert(
        val categoryId: String,
        val categoryNameBn: String,
        val budget: Double,
        val actual: Double,
        val percentage: Double, // actual / budget * 100
        val severity: Severity,
    )

    /**
     * Compute budget alerts for a set of categories.
     *
     * @param budgets the monthly budget limits per category
     * @param actuals map of categoryId → actual spend this month
     * @param warningThreshold the fraction of budget at which a WARNING fires (default 0.8 = 80%)
     * @return alerts for categories that crossed the warning threshold, sorted by percentage desc
     */
    fun computeAlerts(
        budgets: List<Budget>,
        actuals: Map<String, Double>,
        warningThreshold: Double = 0.8,
    ): List<BudgetAlert> {
        return budgets.mapNotNull { budget ->
            val actual = actuals[budget.categoryId] ?: 0.0
            if (budget.monthlyLimit <= 0.01) return@mapNotNull null
            val fraction = actual / budget.monthlyLimit
            if (fraction < warningThreshold) return@mapNotNull null
            val pct = fraction * 100.0
            val severity = if (fraction >= 1.0) Severity.OVER else Severity.WARNING
            BudgetAlert(
                categoryId = budget.categoryId,
                categoryNameBn = budget.categoryNameBn,
                budget = budget.monthlyLimit,
                actual = actual,
                percentage = pct,
                severity = severity,
            )
        }.sortedByDescending { it.percentage }
    }

    /**
     * Aggregate actual expenses by category for a month.
     * @param expenses all expenses in the month
     * @return map of categoryId → total amount
     */
    fun aggregateByCategory(expenses: List<Expense>): Map<String, Double> {
        return expenses.groupBy { it.categoryId }.mapValues { (_, list) ->
            list.sumOf { it.amount }
        }
    }
}
