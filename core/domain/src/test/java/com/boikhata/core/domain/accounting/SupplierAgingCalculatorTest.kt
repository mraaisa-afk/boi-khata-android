package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.aging.AgingBucket
import com.boikhata.core.domain.enums.SupplierEntryType
import com.boikhata.core.domain.model.Supplier
import com.boikhata.core.domain.model.SupplierEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D51/D52: Supplier payable aging + consignment-settlement E2E (the P5 exit-gate test).
 */
class SupplierAgingCalculatorTest {

    private val day = 24L * 60 * 60 * 1000
    private val supplier = Supplier(
        id = "s1", tenantId = "t_1", nameBn = "রাইসা প্রকাশনী",
        phone = "017...", settlementCycle = "30", notes = null,
    )

    private fun entry(id: String, type: SupplierEntryType, amount: Double, date: Long, desc: String = "") =
        SupplierEntry(id, "t_1", "s1", amount, type, desc, null, date)

    @Test
    fun `should return zero when no entries`() {
        val result = SupplierAgingCalculator.calculate(emptyList(), 0L)
        assertThat(result.totalPayable).isEqualTo(0.0)
        assertThat(result.bucket).isEqualTo(AgingBucket.NONE)
    }

    @Test
    fun `should sum opening and consignment as payable`() {
        val now = 1000L + 5 * day
        val entries = listOf(
            entry("e1", SupplierEntryType.OPENING, 100.0, 1000L),
            entry("e2", SupplierEntryType.CONSIGNMENT, 50.0, 1000L + 2 * day),
        )
        val result = SupplierAgingCalculator.calculate(entries, now)
        assertThat(result.totalPayable).isEqualTo(150.0)
    }

    @Test
    fun `should reduce payable by payment and allocate FIFO to oldest first`() {
        val t0 = 1000L
        val entries = listOf(
            entry("old", SupplierEntryType.OPENING, 100.0, t0),
            entry("new", SupplierEntryType.CONSIGNMENT, 50.0, t0 + 5 * day),
            entry("pay", SupplierEntryType.PAYMENT, 120.0, t0 + 10 * day),
        )
        val result = SupplierAgingCalculator.calculate(entries, t0 + 20 * day)
        // FIFO: 120 paid against 100 (old) + 20 (new) → remaining new = 30
        assertThat(result.totalPayable).isEqualTo(30.0)
    }

    @Test
    fun `should bucket red when oldest unpaid is over 30 days`() {
        val t0 = 1000L
        val entries = listOf(
            entry("e1", SupplierEntryType.PURCHASE, 200.0, t0),
        )
        val result = SupplierAgingCalculator.calculate(entries, t0 + 35 * day)
        assertThat(result.bucket).isEqualTo(AgingBucket.RED)
        assertThat(result.ageDays).isEqualTo(35L)
    }

    @Test
    fun `should bucket green under 15 days`() {
        val t0 = 1000L
        val entries = listOf(entry("e1", SupplierEntryType.CONSIGNMENT, 10.0, t0))
        val result = SupplierAgingCalculator.calculate(entries, t0 + 5 * day)
        assertThat(result.bucket).isEqualTo(AgingBucket.GREEN)
    }

    @Test
    fun `should flag settlement reminder only when over cycle`() {
        val t0 = 1000L
        val entries = listOf(entry("e1", SupplierEntryType.OPENING, 500.0, t0))
        val within = SupplierAgingCalculator.calculate(entries, t0 + 25 * day)
        val overdue = SupplierAgingCalculator.calculate(entries, t0 + 35 * day)
        assertThat(within.ageDays).isEqualTo(25L)
        assertThat(overdue.ageDays).isEqualTo(35L)
        val withinBalance = SupplierAgingCalculator.supplierBalance(supplier, entries.first(), t0 + 25 * day)
        val overdueBalance = SupplierAgingCalculator.supplierBalance(supplier, entries.first(), t0 + 35 * day)
        assertThat(withinBalance.reminderDue).isFalse()
        assertThat(overdueBalance.reminderDue).isTrue()
    }

    @Test
    fun `should settle fully when payment covers all payable`() {
        val t0 = 1000L
        val entries = listOf(
            entry("c", SupplierEntryType.CONSIGNMENT, 100.0, t0),
            entry("p", SupplierEntryType.PAYMENT, 100.0, t0 + day),
        )
        val result = SupplierAgingCalculator.calculate(entries, t0 + 10 * day)
        assertThat(result.totalPayable).isEqualTo(0.0)
        assertThat(result.bucket).isEqualTo(AgingBucket.NONE)
    }

    @Test
    fun `consignment settlement E2E - opening plus consignment then partial and full settlement`() {
        val t0 = 1000L
        val entries = mutableListOf(
            entry("open", SupplierEntryType.OPENING, 400.0, t0),
            entry("consign", SupplierEntryType.CONSIGNMENT, 600.0, t0 + 3 * day),
            entry("purchase", SupplierEntryType.PURCHASE, 200.0, t0 + 6 * day),
        )
        // First payment 500 → FIFO against opening (400) + 100 of consignment
        entries.add(entry("pay1", SupplierEntryType.PAYMENT, 500.0, t0 + 10 * day))
        val afterPay1 = SupplierAgingCalculator.calculate(entries, t0 + 12 * day)
        assertThat(afterPay1.totalPayable).isEqualTo(700.0) // 600+200 - 100 = 700

        // Full settlement 700
        entries.add(entry("pay2", SupplierEntryType.PAYMENT, 700.0, t0 + 20 * day))
        val afterPay2 = SupplierAgingCalculator.calculate(entries, t0 + 22 * day)
        assertThat(afterPay2.totalPayable).isEqualTo(0.0)
        assertThat(afterPay2.bucket).isEqualTo(AgingBucket.NONE)
    }

    @Test
    fun `should parse bangla cycle label into days`() {
        assertThat(SupplierAgingCalculator.parseDays("৩০ দিন")).isEqualTo(30)
        assertThat(SupplierAgingCalculator.parseDays("30 days")).isEqualTo(30)
        assertThat(SupplierAgingCalculator.parseDays("সাপ্তাহিক")).isEqualTo(30) // fallback
    }

    @Test
    fun `should summarize across suppliers`() {
        val t0 = 1000L
        val b1 = SupplierAgingCalculator.supplierBalance(supplier, entry("a", SupplierEntryType.OPENING, 100.0, t0), t0 + 5 * day)
        val b2 = SupplierAgingCalculator.supplierBalance(supplier.copy(id = "s2").copy(settlementCycle = "30"),
            entry("b", SupplierEntryType.CONSIGNMENT, 50.0, t0), t0 + 4 * day)
        val summary = SupplierAgingCalculator.summarize(listOf(b1, b2))
        assertThat(summary.totalPayable).isEqualTo(150.0)
        assertThat(summary.greenBucket).isEqualTo(150.0)
        assertThat(summary.supplierCount).isEqualTo(2)
    }
}
