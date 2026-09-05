package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.aging.AgingBucket
import com.boikhata.core.domain.enums.SupplierEntryType
import com.boikhata.core.domain.model.Supplier
import com.boikhata.core.domain.model.SupplierEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
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

    // ── P5 exit-gate: consignment settlement coverage (D52) ───────────────

    @Ignore("B2: allocatePayment discards leftover remaining. " +
            "Asserts intended behaviour. Unignore when fixed.")
    @Test
    fun `overpayment should track leftover as credit balance or advance`() {
        val t0 = 1000L
        val entries = listOf(
            entry("c", SupplierEntryType.CONSIGNMENT, 100.0, t0),
            entry("p", SupplierEntryType.PAYMENT, 150.0, t0 + day),
        )
        val result = SupplierAgingCalculator.calculate(entries, t0 + 10 * day)
        // Intended: overpayment of 50 should be tracked as a credit/advance (negative payable)
        assertThat(result.totalPayable).isLessThan(0.0)
    }

    @Ignore("B4: buildStatement ADJUSTMENT branch is dead code — " +
            "if (e.amount >= 0) e.amount else e.amount (both arms identical). " +
            "Asserts intended behaviour: runningBalance should match FIFO totalPayable. " +
            "Unignore when fixed.")
    @Test
    fun `buildStatement runningBalance should reconcile with totalPayable for negative adjustment`() {
        val t0 = 1000L
        val entries = listOf(
            entry("open", SupplierEntryType.OPENING, 100.0, t0),
            entry("pay", SupplierEntryType.PAYMENT, 50.0, t0 + day),
            entry("adj", SupplierEntryType.ADJUSTMENT, -60.0, t0 + 2 * day),
        )
        val stmt = SupplierAgingCalculator.buildStatement("Shop", supplier, entries, null, t0 + 10 * day)
        val lastLine = stmt.entries.last()
        // Intended: runningBalance should reconcile with FIFO totalPayable
        assertThat(lastLine.runningBalance).isEqualTo(stmt.totalPayable)
    }

    @Ignore("B6: parseDays concatenates all digits — '১৫-৩০ দিন' yields 1530 instead of a range. " +
            "Asserts intended behaviour. Unignore when fixed.")
    @Test
    fun `parseDays should not concatenate digits across a range separator`() {
        // "১৫-৩০ দিন" means "15-30 days"; should not yield 1530
        val result = SupplierAgingCalculator.parseDays("১৫-৩০ দিন")
        assertThat(result).isNotEqualTo(1530)
        assertThat(result).isLessThan(366)
    }

    // ── B7: supplierBalance single-entry limitation (passing, documents the trap) ──

    @Test
    fun `supplierBalance takes a single entry and ignores others from the same supplier`() {
        val t0 = 1000L
        val openEntry = entry("open", SupplierEntryType.OPENING, 500.0, t0)
        val consignEntry = entry("consign", SupplierEntryType.CONSIGNMENT, 300.0, t0 + day)
        // supplierBalance only sees the single entry passed to it
        val balance = SupplierAgingCalculator.supplierBalance(supplier, openEntry, t0 + 5 * day)
        assertThat(balance.balance).isEqualTo(500.0) // only the opening, not 800.0
        // calculate sees both entries
        val fullResult = SupplierAgingCalculator.calculate(listOf(openEntry, consignEntry), t0 + 5 * day)
        assertThat(fullResult.totalPayable).isEqualTo(800.0)
        // This documents the limitation: supplierBalance under-reports when multiple entries exist
        assertThat(balance.balance).isNotEqualTo(fullResult.totalPayable)
    }

    // ── buildStatement coverage ──────────────────────────────────────────

    @Test
    fun `buildStatement runningBalance is correct across mixed entry types`() {
        val t0 = 1000L
        val entries = listOf(
            entry("open", SupplierEntryType.OPENING, 100.0, t0),
            entry("consign", SupplierEntryType.CONSIGNMENT, 50.0, t0 + day),
            entry("pay", SupplierEntryType.PAYMENT, 80.0, t0 + 2 * day),
            entry("adj", SupplierEntryType.ADJUSTMENT, 20.0, t0 + 3 * day),
        )
        val stmt = SupplierAgingCalculator.buildStatement("Shop", supplier, entries, null, t0 + 10 * day)
        assertThat(stmt.entries).hasSize(4)
        assertThat(stmt.entries[0].runningBalance).isEqualTo(100.0)
        assertThat(stmt.entries[1].runningBalance).isEqualTo(150.0)
        assertThat(stmt.entries[2].runningBalance).isEqualTo(70.0)
        assertThat(stmt.entries[3].runningBalance).isEqualTo(90.0)
    }

    @Test
    fun `buildStatement with empty entries and null startDate produces no lines`() {
        val stmt = SupplierAgingCalculator.buildStatement("Shop", supplier, emptyList(), null, 1000L)
        assertThat(stmt.entries).isEmpty()
        assertThat(stmt.totalPayable).isEqualTo(0.0)
        assertThat(stmt.bucket).isEqualTo(AgingBucket.NONE)
        assertThat(stmt.startDate).isNull()
    }

    // ── ADJUSTMENT positive through calculate ────────────────────────────

    @Test
    fun `positive adjustment through calculate adds to payable as a new credit`() {
        val t0 = 1000L
        val entries = listOf(
            entry("open", SupplierEntryType.OPENING, 100.0, t0),
            entry("adj", SupplierEntryType.ADJUSTMENT, 50.0, t0 + day),
        )
        val result = SupplierAgingCalculator.calculate(entries, t0 + 5 * day)
        assertThat(result.totalPayable).isEqualTo(150.0)
        assertThat(result.allocation).hasSize(2)
        assertThat(result.allocation[1].entryId).isEqualTo("adj")
        assertThat(result.allocation[1].remainingAfterAllocation).isEqualTo(50.0)
    }

    // ── Bucket boundaries ───────────────────────────────────────────────

    @Test
    fun `bucket boundaries at 14 15 30 31 days`() {
        val t0 = 1000L
        val e = entry("e1", SupplierEntryType.CONSIGNMENT, 10.0, t0)
        assertThat(SupplierAgingCalculator.calculate(listOf(e), t0 + 14 * day).bucket).isEqualTo(AgingBucket.GREEN)
        assertThat(SupplierAgingCalculator.calculate(listOf(e), t0 + 15 * day).bucket).isEqualTo(AgingBucket.YELLOW)
        assertThat(SupplierAgingCalculator.calculate(listOf(e), t0 + 30 * day).bucket).isEqualTo(AgingBucket.YELLOW)
        assertThat(SupplierAgingCalculator.calculate(listOf(e), t0 + 31 * day).bucket).isEqualTo(AgingBucket.RED)
    }

    // ── Sort order ──────────────────────────────────────────────────────

    @Test
    fun `entries supplied out of date order are sorted by date for FIFO`() {
        val t0 = 1000L
        val entries = listOf(
            entry("new", SupplierEntryType.CONSIGNMENT, 50.0, t0 + 5 * day),
            entry("old", SupplierEntryType.OPENING, 100.0, t0),
            entry("pay", SupplierEntryType.PAYMENT, 120.0, t0 + 10 * day),
        )
        val result = SupplierAgingCalculator.calculate(entries, t0 + 20 * day)
        // FIFO: 120 against old (100) + 20 of new → new remaining = 30
        assertThat(result.totalPayable).isEqualTo(30.0)
        assertThat(result.oldestUnpaidDate).isEqualTo(t0 + 5 * day)
    }

    @Test
    fun `two credits sharing the same date preserve insertion order under sortedBy`() {
        val t0 = 1000L
        val entries = listOf(
            entry("first", SupplierEntryType.OPENING, 100.0, t0),
            entry("second", SupplierEntryType.CONSIGNMENT, 50.0, t0),
            entry("pay", SupplierEntryType.PAYMENT, 120.0, t0 + day),
        )
        val result = SupplierAgingCalculator.calculate(entries, t0 + 10 * day)
        // FIFO: 120 against first (100) + 20 of second → second remaining = 30
        assertThat(result.totalPayable).isEqualTo(30.0)
        // sortedBy is stable: equal dates keep insertion order
        assertThat(result.allocation[0].entryId).isEqualTo("first")
        assertThat(result.allocation[0].remainingAfterAllocation).isWithin(0.001).of(0.0)
        assertThat(result.allocation[1].entryId).isEqualTo("second")
        assertThat(result.allocation[1].remainingAfterAllocation).isWithin(0.001).of(30.0)
    }

    // ── now earlier than oldest entry ───────────────────────────────────

    @Test
    fun `now earlier than the oldest entry coerces ageDays to zero`() {
        val t0 = 1000L
        val entries = listOf(entry("e1", SupplierEntryType.CONSIGNMENT, 100.0, t0 + 5 * day))
        val result = SupplierAgingCalculator.calculate(entries, t0)
        assertThat(result.ageDays).isEqualTo(0L)
        assertThat(result.bucket).isEqualTo(AgingBucket.GREEN)
    }

    // ── summarize mixed buckets ─────────────────────────────────────────

    @Test
    fun `summarize with mixed green yellow red and none buckets`() {
        val t0 = 1000L
        val greenBalance = SupplierAgingCalculator.supplierBalance(
            supplier, entry("g", SupplierEntryType.OPENING, 100.0, t0), t0 + 5 * day)
        val yellowBalance = SupplierAgingCalculator.supplierBalance(
            supplier.copy(id = "s2"), entry("y", SupplierEntryType.OPENING, 200.0, t0), t0 + 20 * day)
        val redBalance = SupplierAgingCalculator.supplierBalance(
            supplier.copy(id = "s3"), entry("r", SupplierEntryType.OPENING, 300.0, t0), t0 + 35 * day)
        // NONE bucket: a lone PAYMENT entry leaves credits empty → payable 0
        val noneBalance = SupplierAgingCalculator.supplierBalance(
            supplier.copy(id = "s4"),
            entry("n", SupplierEntryType.PAYMENT, 100.0, t0), t0 + 5 * day)

        val summary = SupplierAgingCalculator.summarize(listOf(greenBalance, yellowBalance, redBalance, noneBalance))
        assertThat(summary.greenBucket).isEqualTo(100.0)
        assertThat(summary.yellowBucket).isEqualTo(200.0)
        assertThat(summary.redBucket).isEqualTo(300.0)
        assertThat(summary.totalPayable).isEqualTo(600.0)
        assertThat(summary.supplierCount).isEqualTo(3) // none excluded
    }

    // ── parseDays edge cases (current behaviour) ────────────────────────

    @Test
    fun `parseDays for empty zero bangla latin and mixed labels`() {
        assertThat(SupplierAgingCalculator.parseDays("")).isEqualTo(30)      // fallback
        assertThat(SupplierAgingCalculator.parseDays("০")).isEqualTo(0)      // single bangla zero
        assertThat(SupplierAgingCalculator.parseDays("2 weeks")).isEqualTo(2) // latin digit only
        assertThat(SupplierAgingCalculator.parseDays("৩০ দিন")).isEqualTo(30) // bangla digits
    }

    // ── B8 observation: epsilon inconsistency between totalPayable and bucket ──

    @Test
    fun `sub-epsilon remaining is treated as none bucket but totalPayable is not zero`() {
        val t0 = 1000L
        val entries = listOf(entry("c", SupplierEntryType.CONSIGNMENT, 0.0009, t0))
        val result = SupplierAgingCalculator.calculate(entries, t0 + 5 * day)
        // totalPayable is a raw sum — 0.0009 is non-zero
        assertThat(result.totalPayable).isEqualTo(0.0009)
        // but bucket = NONE because totalPayable <= 0.001 epsilon threshold
        assertThat(result.bucket).isEqualTo(AgingBucket.NONE)
        // This documents B8: totalPayable and bucket use different epsilon thresholds
    }
}
