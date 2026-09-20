package com.boikhata.feature.subscription

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * C-4 (P11) regression: the manual bKash subscription payment amount used a
 * raw ASCII-only `toDoubleOrNull() ?: 0.0` with no gate — Bangla-digit input
 * silently produced ৳0, the submit no-op'd without any error, and the owner
 * believed the payment was recorded. Drives the REAL production parse
 * (ERR-013 lesson).
 */
class SubscriptionAmountParseTest {

    @Test
    fun `bangla amount parses`() {
        assertThat(parseSubscriptionAmount("১০০০")).isEqualTo(1000.0)
    }

    @Test
    fun `bangla decimal amount parses`() {
        assertThat(parseSubscriptionAmount("৫০০.৫০")).isEqualTo(500.50)
    }

    @Test
    fun `ascii digits unchanged`() {
        assertThat(parseSubscriptionAmount("1000")).isEqualTo(1000.0)
    }

    @Test
    fun `blank and garbage fall to zero`() {
        assertThat(parseSubscriptionAmount("")).isEqualTo(0.0)
        assertThat(parseSubscriptionAmount("abc")).isEqualTo(0.0)
    }
}
