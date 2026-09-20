package com.boikhata.feature.reports

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * C-3 (P11) regression: the cash-close screen's owner inputs (MFS fee rate,
 * counted cash) had NO gate and a raw ASCII-only `toDoubleOrNull() ?: 0.0` —
 * Bangla-digit input was silently zeroed, corrupting the MFS fee estimate and
 * the নগদ মিলান variance in the day-close record (D36 report shares it to
 * WhatsApp). Drives the REAL production parse (ERR-013 lesson).
 */
class CashCloseParseTest {

    @Test
    fun `bangla mfs rate parses`() {
        assertThat(parseCashCloseNumber("২.৫")).isEqualTo(2.5)
    }

    @Test
    fun `bangla counted cash parses`() {
        assertThat(parseCashCloseNumber("৫০০০")).isEqualTo(5000.0)
    }

    @Test
    fun `ascii digits unchanged`() {
        assertThat(parseCashCloseNumber("2.5")).isEqualTo(2.5)
        assertThat(parseCashCloseNumber("5000")).isEqualTo(5000.0)
    }

    @Test
    fun `blank and garbage fall to zero`() {
        assertThat(parseCashCloseNumber("")).isEqualTo(0.0)
        assertThat(parseCashCloseNumber("abc")).isEqualTo(0.0)
    }
}
