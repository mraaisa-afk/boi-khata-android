package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.enums.CashbookEntryType
import com.boikhata.core.domain.model.CashbookEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * P3a: CashbookBalanceCalculator unit tests.
 */
class CashbookBalanceCalculatorTest {

    private fun entry(account: CashbookAccount, type: CashbookEntryType, amount: Double) =
        CashbookEntry("id", account, type, amount, "test", null, System.currentTimeMillis(), "u1")

    @Test
    fun `should calculate positive balance when income exceeds expense`() {
        val entries = listOf(
            entry(CashbookAccount.CASH, CashbookEntryType.INCOME, 1000.0),
            entry(CashbookAccount.CASH, CashbookEntryType.EXPENSE, 300.0),
        )
        val balance = CashbookBalanceCalculator.calculateBalance(entries, CashbookAccount.CASH)
        assertThat(balance.balance).isWithin(0.01).of(700.0)
    }

    @Test
    fun `should calculate zero balance when income equals expense`() {
        val entries = listOf(
            entry(CashbookAccount.CASH, CashbookEntryType.INCOME, 500.0),
            entry(CashbookAccount.CASH, CashbookEntryType.EXPENSE, 500.0),
        )
        val balance = CashbookBalanceCalculator.calculateBalance(entries, CashbookAccount.CASH)
        assertThat(balance.balance).isWithin(0.01).of(0.0)
    }

    @Test
    fun `should calculate negative balance when expense exceeds income`() {
        val entries = listOf(
            entry(CashbookAccount.CASH, CashbookEntryType.INCOME, 200.0),
            entry(CashbookAccount.CASH, CashbookEntryType.EXPENSE, 500.0),
        )
        val balance = CashbookBalanceCalculator.calculateBalance(entries, CashbookAccount.CASH)
        assertThat(balance.balance).isWithin(0.01).of(-300.0)
    }

    @Test
    fun `should filter by account when computing balance`() {
        val entries = listOf(
            entry(CashbookAccount.CASH, CashbookEntryType.INCOME, 1000.0),
            entry(CashbookAccount.BKASH, CashbookEntryType.INCOME, 500.0),
            entry(CashbookAccount.CASH, CashbookEntryType.EXPENSE, 200.0),
        )
        val cashBalance = CashbookBalanceCalculator.calculateBalance(entries, CashbookAccount.CASH)
        val bkashBalance = CashbookBalanceCalculator.calculateBalance(entries, CashbookAccount.BKASH)
        assertThat(cashBalance.balance).isWithin(0.01).of(800.0)
        assertThat(bkashBalance.balance).isWithin(0.01).of(500.0)
    }

    @Test
    fun `should treat TRANSFER as income for destination account`() {
        val entries = listOf(
            entry(CashbookAccount.CASH, CashbookEntryType.TRANSFER, 300.0),
            entry(CashbookAccount.CASH, CashbookEntryType.EXPENSE, 100.0),
        )
        val balance = CashbookBalanceCalculator.calculateBalance(entries, CashbookAccount.CASH)
        assertThat(balance.balance).isWithin(0.01).of(200.0)
    }

    @Test
    fun `should calculate balances for all three accounts`() {
        val entries = listOf(
            entry(CashbookAccount.CASH, CashbookEntryType.INCOME, 1000.0),
            entry(CashbookAccount.BKASH, CashbookEntryType.INCOME, 500.0),
            entry(CashbookAccount.BANK, CashbookEntryType.INCOME, 2000.0),
        )
        val balances = CashbookBalanceCalculator.calculateAllBalances(entries)
        assertThat(balances).hasSize(3)
        assertThat(balances.find { it.account == CashbookAccount.CASH }?.balance).isWithin(0.01).of(1000.0)
        assertThat(balances.find { it.account == CashbookAccount.BKASH }?.balance).isWithin(0.01).of(500.0)
        assertThat(balances.find { it.account == CashbookAccount.BANK }?.balance).isWithin(0.01).of(2000.0)
    }
}
