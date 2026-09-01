package com.boikhata.core.domain.accounting

import com.google.common.truth.Truth.assertThat
import com.boikhata.core.domain.model.Expense
import org.junit.Test

/**
 * D35: BudgetAlertCalculator unit tests — monthly budget alert threshold.
 */
class BudgetAlertCalculatorTest {

    private fun budget(catId: String, name: String, limit: Double) =
        BudgetAlertCalculator.Budget(catId, name, limit)

    private fun expense(catId: String, amount: Double) = Expense(
        id = "e1", categoryId = catId, categoryNameBn = "", amount = amount,
        description = "", expenseDate = 0L, receiptPhotoPath = null, userId = "u1",
    )

    @Test
    fun `should not alert when actual is below warning threshold`() {
        val budgets = listOf(budget("c1", "ভাড়া", 10000.0))
        val actuals = mapOf("c1" to 5000.0) // 50%
        val alerts = BudgetAlertCalculator.computeAlerts(budgets, actuals, 0.8)
        assertThat(alerts).isEmpty()
    }

    @Test
    fun `should warn when actual reaches 80 percent of budget`() {
        val budgets = listOf(budget("c1", "ভাড়া", 10000.0))
        val actuals = mapOf("c1" to 8000.0) // 80%
        val alerts = BudgetAlertCalculator.computeAlerts(budgets, actuals, 0.8)
        assertThat(alerts).hasSize(1)
        assertThat(alerts[0].severity).isEqualTo(BudgetAlertCalculator.Severity.WARNING)
        assertThat(alerts[0].percentage).isEqualTo(80.0)
    }

    @Test
    fun `should alert OVER when actual equals budget`() {
        val budgets = listOf(budget("c1", "ভাড়া", 10000.0))
        val actuals = mapOf("c1" to 10000.0) // 100%
        val alerts = BudgetAlertCalculator.computeAlerts(budgets, actuals, 0.8)
        assertThat(alerts[0].severity).isEqualTo(BudgetAlertCalculator.Severity.OVER)
    }

    @Test
    fun `should alert OVER when actual exceeds budget`() {
        val budgets = listOf(budget("c1", "ভাড়া", 10000.0))
        val actuals = mapOf("c1" to 12000.0) // 120%
        val alerts = BudgetAlertCalculator.computeAlerts(budgets, actuals, 0.8)
        assertThat(alerts[0].severity).isEqualTo(BudgetAlertCalculator.Severity.OVER)
        assertThat(alerts[0].percentage).isEqualTo(120.0)
    }

    @Test
    fun `should sort alerts by percentage descending`() {
        val budgets = listOf(
            budget("c1", "ভাড়া", 10000.0),
            budget("c2", "বিদ্যুৎ", 5000.0),
        )
        val actuals = mapOf("c1" to 9000.0, "c2" to 5000.0) // 90% and 100%
        val alerts = BudgetAlertCalculator.computeAlerts(budgets, actuals, 0.8)
        assertThat(alerts[0].categoryId).isEqualTo("c2") // 100% first
        assertThat(alerts[1].categoryId).isEqualTo("c1") // 90% second
    }

    @Test
    fun `should skip categories with zero budget limit`() {
        val budgets = listOf(budget("c1", "অন্যান্য", 0.0))
        val actuals = mapOf("c1" to 100.0)
        val alerts = BudgetAlertCalculator.computeAlerts(budgets, actuals, 0.8)
        assertThat(alerts).isEmpty()
    }

    @Test
    fun `should treat missing actual as zero`() {
        val budgets = listOf(budget("c1", "ভাড়া", 10000.0))
        val alerts = BudgetAlertCalculator.computeAlerts(budgets, emptyMap(), 0.8)
        assertThat(alerts).isEmpty()
    }

    @Test
    fun `should aggregate expenses by category`() {
        val expenses = listOf(
            expense("c1", 3000.0),
            expense("c1", 2000.0),
            expense("c2", 1000.0),
        )
        val actuals = BudgetAlertCalculator.aggregateByCategory(expenses)
        assertThat(actuals["c1"]).isEqualTo(5000.0)
        assertThat(actuals["c2"]).isEqualTo(1000.0)
    }

    @Test
    fun `should alert on multiple categories at once`() {
        val budgets = listOf(
            budget("c1", "ভাড়া", 10000.0),
            budget("c2", "বিদ্যুৎ", 5000.0),
            budget("c3", "ইন্টারনেট", 1000.0),
        )
        val actuals = mapOf("c1" to 8500.0, "c2" to 5000.0, "c3" to 500.0)
        val alerts = BudgetAlertCalculator.computeAlerts(budgets, actuals, 0.8)
        assertThat(alerts).hasSize(2) // c1 (85%) and c2 (100%); c3 (50%) below threshold
    }
}
