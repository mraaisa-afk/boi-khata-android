package com.boikhata.feature.catalog

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * B-014 regression: the «বই যোগ করুন» form accepted Bangla-keyboard digits
 * (০-৯) in every numeric field — `Char.isDigit()` is Unicode-aware, so the
 * fields filled with ১০০/১২০/০/৫ — but the production parse was raw
 * `toIntOrNull()/toDoubleOrNull()`, which only understands ASCII [0-9].
 * Result: the values silently fell back to 2026/0.0/0.0/0/5 and books were
 * saved with ৳0.00 prices. Drives the REAL production parse (no test-local
 * reimplementation — ERR-013 lesson): if the normalizer is ever dropped from
 * parseBookFormNumbers, `bangla digits parse to real values` fails.
 */
class BookAddEditParseTest {

    @Test
    fun `bangla digits parse to real values`() {
        val n = parseBookFormNumbers(
            editionYear = "২০২৬",
            purchasePrice = "১০০",
            sellingPrice = "১২০",
            initialStock = "০",
            lowStockThreshold = "৫",
        )
        assertThat(n.editionYear).isEqualTo(2026)
        assertThat(n.purchasePrice).isEqualTo(100.0)
        assertThat(n.sellingPrice).isEqualTo(120.0)
        assertThat(n.initialStock).isEqualTo(0)
        assertThat(n.lowStockThreshold).isEqualTo(5)
    }

    @Test
    fun `bangla decimal amount parses`() {
        val n = parseBookFormNumbers(
            editionYear = "2026",
            purchasePrice = "৫০.২৫",
            sellingPrice = "১২০.৫০",
            initialStock = "10",
            lowStockThreshold = "5",
        )
        assertThat(n.purchasePrice).isEqualTo(50.25)
        assertThat(n.sellingPrice).isEqualTo(120.50)
        assertThat(n.initialStock).isEqualTo(10)
    }

    @Test
    fun `ascii digits unchanged`() {
        val n = parseBookFormNumbers("2026", "100", "120", "12", "5")
        assertThat(n.editionYear).isEqualTo(2026)
        assertThat(n.purchasePrice).isEqualTo(100.0)
        assertThat(n.sellingPrice).isEqualTo(120.0)
        assertThat(n.initialStock).isEqualTo(12)
        assertThat(n.lowStockThreshold).isEqualTo(5)
    }

    @Test
    fun `blank fields fall back to the documented defaults`() {
        val n = parseBookFormNumbers("", "", "", "", "")
        assertThat(n.editionYear).isEqualTo(2026)
        assertThat(n.purchasePrice).isEqualTo(0.0)
        assertThat(n.sellingPrice).isEqualTo(0.0)
        assertThat(n.initialStock).isEqualTo(0)
        assertThat(n.lowStockThreshold).isEqualTo(5)
    }

    @Test
    fun `garbage never crashes the form`() {
        val n = parseBookFormNumbers("abc", "--", "…", "x", "y")
        assertThat(n.editionYear).isEqualTo(2026)
        assertThat(n.purchasePrice).isEqualTo(0.0)
        assertThat(n.sellingPrice).isEqualTo(0.0)
        assertThat(n.initialStock).isEqualTo(0)
        assertThat(n.lowStockThreshold).isEqualTo(5)
    }
}
