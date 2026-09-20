package com.boikhata.feature.melamode

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * C-2 (P11) regression tests for the mela stock-move quantity parse. NOTE
 * (disclosed audit correction): `toIntOrNull()` is Unicode-digit-aware on JVM
 * (Integer.parseInt → Character.digit), so the raw parse never actually
 * rejected Bangla digits here — these tests pin the shared production parse
 * contract (ERR-013: drive the real function) and guard against future
 * regressions, exactly like the other C-round parse tests.
 */
class MelaQuantityParseTest {

    @Test
    fun `bangla digits parse to real values`() {
        assertThat(parseMelaQuantity("৫")).isEqualTo(5)
        assertThat(parseMelaQuantity("১২")).isEqualTo(12)
    }

    @Test
    fun `ascii digits unchanged`() {
        assertThat(parseMelaQuantity("12")).isEqualTo(12)
    }

    @Test
    fun `blank and garbage fall to zero`() {
        assertThat(parseMelaQuantity("")).isEqualTo(0)
        assertThat(parseMelaQuantity("x")).isEqualTo(0)
    }
}
