package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.model.Expense
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D26: GoriBalanceCalculator unit tests — ঘরি (staff advance) sub-ledger.
 */
class GoriBalanceCalculatorTest {

    private fun expense(userId: String, amount: Double, description: String) = Expense(
        id = "id",
        categoryId = "ec_ghori",
        categoryNameBn = "ঘরি/অ্যাডভান্স",
        amount = amount,
        description = description,
        expenseDate = System.currentTimeMillis(),
        receiptPhotoPath = null,
        userId = userId,
    )

    @Test
    fun `should return zero balance when no expenses`() {
        assertThat(GoriBalanceCalculator.calculateBalance(emptyList())).isEqualTo(0.0)
    }

    @Test
    fun `should calculate balance for advances only`() {
        val expenses = listOf(
            expense("u1", 500.0, "ঘরি প্রদান"),
            expense("u1", 300.0, "ঘরি প্রদান"),
        )
        assertThat(GoriBalanceCalculator.calculateBalance(expenses)).isWithin(0.01).of(800.0)
    }

    @Test
    fun `should subtract returns from advances`() {
        val expenses = listOf(
            expense("u1", 500.0, "ঘরি প্রদান"),
            expense("u1", 200.0, "ঘরি ফেরত"),
        )
        assertThat(GoriBalanceCalculator.calculateBalance(expenses)).isWithin(0.01).of(300.0)
    }

    @Test
    fun `should return zero when all advances returned`() {
        val expenses = listOf(
            expense("u1", 500.0, "ঘরি প্রদান"),
            expense("u1", 500.0, "ঘরি ফেরত"),
        )
        assertThat(GoriBalanceCalculator.calculateBalance(expenses)).isWithin(0.01).of(0.0)
    }

    @Test
    fun `should calculate per-user balances separately`() {
        val expenses = listOf(
            expense("u1", 500.0, "ঘরি প্রদান"),
            expense("u2", 300.0, "ঘরি প্রদান"),
            expense("u1", 200.0, "ঘরি ফেরত"),
        )
        val balances = GoriBalanceCalculator.calculatePerUserBalances(expenses)
        assertThat(balances["u1"]).isWithin(0.01).of(300.0)
        assertThat(balances["u2"]).isWithin(0.01).of(300.0)
    }
}
