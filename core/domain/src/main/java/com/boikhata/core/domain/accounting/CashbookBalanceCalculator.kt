package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.enums.CashbookEntryType
import com.boikhata.core.domain.model.CashbookBalance
import com.boikhata.core.domain.model.CashbookEntry

/**
 * P3a: Cashbook balance calculator.
 * D25: Balances are derived (ARCHITECTURE §4).
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 */
object CashbookBalanceCalculator {

    /**
     * Compute the balance for a single account from a list of entries.
     * Balance = INCOME − EXPENSE + TRANSFER (transfers in are income-like, out are expense-like).
     * For TRANSFER: positive amount = transfer INTO this account (adds to balance);
     * negative amount = transfer OUT (reduces balance). Since amount is always > 0
     * per Firestore rules, TRANSFER entries with this account = money in, TRANSFER entries
     * from this account = money out. We treat TRANSFER as INCOME for the destination account.
     */
    fun calculateBalance(entries: List<CashbookEntry>, account: CashbookAccount): CashbookBalance {
        val accountEntries = entries.filter { it.account == account }
        val income = accountEntries.filter {
            it.type == CashbookEntryType.INCOME || it.type == CashbookEntryType.TRANSFER
        }.sumOf { it.amount }
        val expense = accountEntries.filter {
            it.type == CashbookEntryType.EXPENSE
        }.sumOf { it.amount }
        return CashbookBalance(
            account = account,
            income = income,
            expense = expense,
            balance = income - expense,
        )
    }

    /**
     * Compute balances for all three accounts.
     */
    fun calculateAllBalances(entries: List<CashbookEntry>): List<CashbookBalance> {
        return CashbookAccount.entries.map { account ->
            calculateBalance(entries, account)
        }
    }
}
