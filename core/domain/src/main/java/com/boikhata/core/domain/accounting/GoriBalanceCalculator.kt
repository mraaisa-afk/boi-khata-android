package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.model.Expense

/**
 * D26: ঘরি (staff advance) sub-ledger balance calculator.
 * ঘরি = money advanced to staff (an asset, recoverable).
 * Advance given = expense with ঘরি category.
 * Advance returned = expense with ঘরি category + description containing "ঘরি ফেরত".
 *
 * Balance = SUM(advances) − SUM(returns).
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 */
object GoriBalanceCalculator {

    private const val RETURN_MARKER = "ঘরি ফেরত"

    /**
     * Compute the ঘরি balance for a single user from their expense list.
     * @param expenses all expenses with the ঘরি category for this user
     * @return net advance balance (advances given minus returns)
     */
    fun calculateBalance(expenses: List<Expense>): Double {
        val advances = expenses.filter { !it.description.contains(RETURN_MARKER) }.sumOf { it.amount }
        val returns = expenses.filter { it.description.contains(RETURN_MARKER) }.sumOf { it.amount }
        return advances - returns
    }

    /**
     * Compute per-user ঘরি balances from a list of expenses.
     * @param expenses all expenses with the ঘরি category
     * @return map of userId → net advance balance
     */
    fun calculatePerUserBalances(expenses: List<Expense>): Map<String, Double> {
        return expenses.groupBy { it.userId }.mapValues { (_, userExpenses) ->
            calculateBalance(userExpenses)
        }
    }
}
