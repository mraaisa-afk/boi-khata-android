package com.boikhata.core.domain.khata

import com.boikhata.core.domain.aging.KhataEntry
import com.boikhata.core.domain.enums.KhataEntryType
import com.boikhata.core.domain.model.KhataCustomer
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D14: KhataStatementBuilder unit tests — verifies statement generation
 * with running balance, aging, and credit-limit warning.
 */
class KhataStatementBuilderTest {

    private val now = 1724956800000L // fixed timestamp for deterministic tests
    private val dayMs = 24L * 60 * 60 * 1000

    private val customer = KhataCustomer(
        id = "c1",
        nameBn = "রহিম চৌধুরী",
        phone = "01711000000",
        address = "খুলনা",
        creditLimit = 5000.0,
        isActive = true,
    )

    @Test
    fun `should build statement with zero entries`() {
        val stmt = KhataStatementBuilder.buildStatement(customer, emptyList(), now)
        assertThat(stmt.lines).isEmpty()
        assertThat(stmt.totalDue).isWithin(0.01).of(0.0)
        assertThat(stmt.exceedsCreditLimit).isFalse()
    }

    @Test
    fun `should calculate running balance correctly`() {
        val entries = listOf(
            KhataEntry("e1", KhataEntryType.CREDIT, 1000.0, now - 30 * dayMs),
            KhataEntry("e2", KhataEntryType.PAYMENT, 300.0, now - 20 * dayMs),
            KhataEntry("e3", KhataEntryType.CREDIT, 500.0, now - 10 * dayMs),
        )
        val stmt = KhataStatementBuilder.buildStatement(customer, entries, now)
        assertThat(stmt.lines).hasSize(3)
        assertThat(stmt.lines[0].runningBalance).isWithin(0.01).of(1000.0)
        assertThat(stmt.lines[1].runningBalance).isWithin(0.01).of(700.0)
        assertThat(stmt.lines[2].runningBalance).isWithin(0.01).of(1200.0)
        assertThat(stmt.totalDue).isWithin(0.01).of(1200.0)
    }

    @Test
    fun `should flag credit limit exceeded`() {
        val entries = listOf(
            KhataEntry("e1", KhataEntryType.CREDIT, 6000.0, now - 5 * dayMs),
        )
        val stmt = KhataStatementBuilder.buildStatement(customer, entries, now)
        assertThat(stmt.totalDue).isWithin(0.01).of(6000.0)
        assertThat(stmt.exceedsCreditLimit).isTrue()
    }

    @Test
    fun `should not flag credit limit when within bounds`() {
        val entries = listOf(
            KhataEntry("e1", KhataEntryType.CREDIT, 3000.0, now - 5 * dayMs),
        )
        val stmt = KhataStatementBuilder.buildStatement(customer, entries, now)
        assertThat(stmt.exceedsCreditLimit).isFalse()
    }

    @Test
    fun `should handle debt forgiveness as negative adjustment`() {
        val entries = listOf(
            KhataEntry("e1", KhataEntryType.CREDIT, 1000.0, now - 20 * dayMs),
            KhataEntry("e2", KhataEntryType.ADJUSTMENT, -1000.0, now - 1 * dayMs, description = "দেনা মুন"),
        )
        val stmt = KhataStatementBuilder.buildStatement(customer, entries, now)
        assertThat(stmt.totalDue).isWithin(0.01).of(0.0)
        assertThat(stmt.lines).hasSize(2)
        assertThat(stmt.lines[1].runningBalance).isWithin(0.01).of(0.0)
    }

    @Test
    fun `should generate text with shop name and customer info`() {
        val entries = listOf(
            KhataEntry("e1", KhataEntryType.CREDIT, 500.0, now - 10 * dayMs),
        )
        val stmt = KhataStatementBuilder.buildStatement(customer, entries, now)
        val text = KhataStatementBuilder.toText(
            statement = stmt,
            shopName = "টেস্ট দোকান",
            formatAmount = { "৳${it.toLong()}" },
            formatDate = { "01/01/2026" },
        )
        assertThat(text).contains("টেস্ট দোকান")
        assertThat(text).contains("রহিম চৌধুরী")
        assertThat(text).contains("খুলনা")
        assertThat(text).contains("মোট বাকি")
    }

    @Test
    fun `should calculate aging from oldest unpaid entry`() {
        val entries = listOf(
            KhataEntry("e1", KhataEntryType.CREDIT, 1000.0, now - 40 * dayMs),
            KhataEntry("e2", KhataEntryType.PAYMENT, 500.0, now - 5 * dayMs),
        )
        val stmt = KhataStatementBuilder.buildStatement(customer, entries, now)
        // Oldest unpaid = e1 (40 days ago), 500 still due
        assertThat(stmt.aging.ageDays).isAtLeast(35L)
        assertThat(stmt.aging.bucket.name).isEqualTo("RED")
    }
}
