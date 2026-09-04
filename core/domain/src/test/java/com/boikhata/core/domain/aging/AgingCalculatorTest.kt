package com.boikhata.core.domain.aging

import com.boikhata.core.domain.enums.KhataEntryType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AgingCalculatorTest {

    private val DAY = 24L * 60 * 60 * 1000
    private val NOW = 1_700_000_000_000L

    // ── FIFO allocation ──────────────────────────────────────────────────────

    @Test
    fun `should return zero due when no entries`() {
        val result = AgingCalculator.calculate(emptyList(), NOW)
        assertThat(result.totalDue).isEqualTo(0.0)
        assertThat(result.bucket).isEqualTo(AgingBucket.NONE)
    }

    @Test
    fun `should return total due for a single unpaid credit`() {
        val entries = listOf(
            KhataEntry("e1", KhataEntryType.CREDIT, 1000.0, NOW - 5 * DAY),
        )
        val result = AgingCalculator.calculate(entries, NOW)
        assertThat(result.totalDue).isEqualTo(1000.0)
        assertThat(result.ageDays).isEqualTo(5)
        assertThat(result.bucket).isEqualTo(AgingBucket.GREEN)
    }

    @Test
    fun `should reduce oldest credit first (FIFO)`() {
        val entries = listOf(
            KhataEntry("e1", KhataEntryType.CREDIT, 500.0, NOW - 20 * DAY),
            KhataEntry("e2", KhataEntryType.CREDIT, 500.0, NOW - 5 * DAY),
            KhataEntry("p1", KhataEntryType.PAYMENT, 300.0, NOW - 1 * DAY),
        )
        val result = AgingCalculator.calculate(entries, NOW)
        // 300 reduces e1 first → e1 remaining 200, e2 remaining 500 → total 700
        assertThat(result.totalDue).isEqualTo(700.0)
        // oldest unpaid is e1 (20 days ago) → YELLOW
        assertThat(result.ageDays).isEqualTo(20)
        assertThat(result.bucket).isEqualTo(AgingBucket.YELLOW)
    }

    @Test
    fun `should fully pay oldest credit and move aging to next`() {
        val entries = listOf(
            KhataEntry("e1", KhataEntryType.CREDIT, 300.0, NOW - 40 * DAY),
            KhataEntry("e2", KhataEntryType.CREDIT, 500.0, NOW - 5 * DAY),
            KhataEntry("p1", KhataEntryType.PAYMENT, 300.0, NOW - 1 * DAY),
        )
        val result = AgingCalculator.calculate(entries, NOW)
        // e1 fully paid → oldest unpaid is e2 (5 days ago) → GREEN
        assertThat(result.totalDue).isEqualTo(500.0)
        assertThat(result.ageDays).isEqualTo(5)
        assertThat(result.bucket).isEqualTo(AgingBucket.GREEN)
    }

    // ── Aging buckets ─────────────────────────────────────────────────────────

    @Test
    fun `should return GREEN when age under 15 days`() {
        val entries = listOf(KhataEntry("e1", KhataEntryType.CREDIT, 100.0, NOW - 10 * DAY))
        val result = AgingCalculator.calculate(entries, NOW)
        assertThat(result.bucket).isEqualTo(AgingBucket.GREEN)
    }

    @Test
    fun `should return YELLOW when age between 15 and 30 days`() {
        val entries = listOf(KhataEntry("e1", KhataEntryType.CREDIT, 100.0, NOW - 25 * DAY))
        val result = AgingCalculator.calculate(entries, NOW)
        assertThat(result.bucket).isEqualTo(AgingBucket.YELLOW)
    }

    @Test
    fun `should return RED when age over 30 days`() {
        val entries = listOf(KhataEntry("e1", KhataEntryType.CREDIT, 100.0, NOW - 45 * DAY))
        val result = AgingCalculator.calculate(entries, NOW)
        assertThat(result.bucket).isEqualTo(AgingBucket.RED)
    }

    // ── OPENING and ADJUSTMENT ────────────────────────────────────────────────

    @Test
    fun `should treat OPENING as an initial credit`() {
        val entries = listOf(
            KhataEntry("e1", KhataEntryType.OPENING, 500.0, NOW - 50 * DAY),
        )
        val result = AgingCalculator.calculate(entries, NOW)
        assertThat(result.totalDue).isEqualTo(500.0)
        assertThat(result.bucket).isEqualTo(AgingBucket.RED)
    }

    @Test
    fun `should handle positive ADJUSTMENT as additional credit`() {
        val entries = listOf(
            KhataEntry("e1", KhataEntryType.CREDIT, 500.0, NOW - 10 * DAY),
            KhataEntry("a1", KhataEntryType.ADJUSTMENT, 200.0, NOW - 5 * DAY),
        )
        val result = AgingCalculator.calculate(entries, NOW)
        assertThat(result.totalDue).isEqualTo(700.0)
    }

    @Test
    fun `should handle negative ADJUSTMENT as a payment (reduces oldest first)`() {
        val entries = listOf(
            KhataEntry("e1", KhataEntryType.CREDIT, 500.0, NOW - 20 * DAY),
            KhataEntry("e2", KhataEntryType.CREDIT, 500.0, NOW - 5 * DAY),
            KhataEntry("a1", KhataEntryType.ADJUSTMENT, -200.0, NOW - 1 * DAY),
        )
        val result = AgingCalculator.calculate(entries, NOW)
        // -200 reduces e1 → e1 remaining 300, e2 remaining 500 → total 800
        assertThat(result.totalDue).isEqualTo(800.0)
        assertThat(result.ageDays).isEqualTo(20)
    }

    // ── fully paid ────────────────────────────────────────────────────────────

    @Test
    fun `should return NONE when all credits are fully paid`() {
        val entries = listOf(
            KhataEntry("e1", KhataEntryType.CREDIT, 500.0, NOW - 40 * DAY),
            KhataEntry("p1", KhataEntryType.PAYMENT, 500.0, NOW - 1 * DAY),
        )
        val result = AgingCalculator.calculate(entries, NOW)
        assertThat(result.totalDue).isEqualTo(0.0)
        assertThat(result.bucket).isEqualTo(AgingBucket.NONE)
        assertThat(result.oldestUnpaidDate).isNull()
    }

    @Test
    fun `should handle overpayment (payment exceeds total credit)`() {
        val entries = listOf(
            KhataEntry("e1", KhataEntryType.CREDIT, 500.0, NOW - 10 * DAY),
            KhataEntry("p1", KhataEntryType.PAYMENT, 700.0, NOW - 1 * DAY),
        )
        val result = AgingCalculator.calculate(entries, NOW)
        assertThat(result.totalDue).isEqualTo(0.0)
        assertThat(result.bucket).isEqualTo(AgingBucket.NONE)
    }
}
