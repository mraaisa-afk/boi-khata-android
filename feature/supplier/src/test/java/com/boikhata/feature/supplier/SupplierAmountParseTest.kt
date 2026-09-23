package com.boikhata.feature.supplier

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * C-1 (P11) regression: the supplier entry sheet's amount field accepts
 * Bangla-keyboard digits (০-৯) — `Char.isDigit()` is Unicode-aware — but the
 * Save gate + onClick used a raw ASCII-only `toDoubleOrNull()`, so a Bangla
 * amount kept সেভ gray (and would have written ৳0.00 if it ever ran).
 * Drives the REAL production parse (ERR-013 lesson): if the normalizer is
 * ever dropped from parseSupplierAmount, these fail.
 */
class SupplierAmountParseTest {

    @Test
    fun `bangla digits parse to real values`() {
        assertThat(parseSupplierAmount("২০০")).isEqualTo(200.0)
    }

    @Test
    fun `bangla decimal amount parses`() {
        assertThat(parseSupplierAmount("৫০.২৫")).isEqualTo(50.25)
    }

    @Test
    fun `ascii digits unchanged`() {
        assertThat(parseSupplierAmount("200.50")).isEqualTo(200.5)
    }

    @Test
    fun `blank and garbage fall to zero`() {
        assertThat(parseSupplierAmount("")).isEqualTo(0.0)
        assertThat(parseSupplierAmount("abc")).isEqualTo(0.0)
    }
}
