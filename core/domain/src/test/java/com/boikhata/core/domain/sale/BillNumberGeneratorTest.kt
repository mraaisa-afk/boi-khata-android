package com.boikhata.core.domain.sale

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * D20: BillNumberGenerator unit tests — INV-YYYYMMDD-NNNN format.
 */
class BillNumberGeneratorTest {

    private val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

    private fun millis(year: Int, month: Int, day: Int): Long {
        calendar.clear()
        calendar.set(year, month - 1, day, 12, 0, 0)
        return calendar.timeInMillis
    }

    @Test
    fun `should generate first bill number for a new date`() {
        val dateMillis = millis(2026, 8, 30)
        val result = BillNumberGenerator.generate(dateMillis, 0)
        assertThat(result).isEqualTo("INV-20260830-0001")
    }

    @Test
    fun `should increment sequence for same date`() {
        val dateMillis = millis(2026, 8, 30)
        val result = BillNumberGenerator.generate(dateMillis, 5)
        assertThat(result).isEqualTo("INV-20260830-0006")
    }

    @Test
    fun `should reset sequence for different date`() {
        val dateMillis1 = millis(2026, 8, 30)
        val dateMillis2 = millis(2026, 8, 31)
        val result1 = BillNumberGenerator.generate(dateMillis1, 10)
        val result2 = BillNumberGenerator.generate(dateMillis2, 0)
        assertThat(result1).isEqualTo("INV-20260830-0011")
        assertThat(result2).isEqualTo("INV-20260831-0001")
    }

    @Test
    fun `should zero-pad sequence to 4 digits`() {
        val dateMillis = millis(2026, 1, 1)
        assertThat(BillNumberGenerator.generate(dateMillis, 0)).contains("-0001")
        assertThat(BillNumberGenerator.generate(dateMillis, 99)).contains("-0100")
        assertThat(BillNumberGenerator.generate(dateMillis, 999)).contains("-1000")
    }

    @Test
    fun `should extract sequence from valid bill number`() {
        assertThat(BillNumberGenerator.extractSequence("INV-20260830-0007")).isEqualTo(7)
        assertThat(BillNumberGenerator.extractSequence("INV-20260830-0001")).isEqualTo(1)
    }

    @Test
    fun `should return 0 for invalid bill number format`() {
        assertThat(BillNumberGenerator.extractSequence("invalid")).isEqualTo(0)
        assertThat(BillNumberGenerator.extractSequence("INV-20260830")).isEqualTo(0)
    }

    @Test
    fun `should generate correct date pattern for LIKE query`() {
        val dateMillis = millis(2026, 8, 30)
        val pattern = BillNumberGenerator.datePattern(dateMillis)
        assertThat(pattern).isEqualTo("INV-20260830-%")
    }
}
