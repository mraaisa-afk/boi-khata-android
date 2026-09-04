package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.accounting.MelaStockCalculator.StockMove
import com.boikhata.core.domain.enums.StockChangeReason
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D56: Mela stock rules — stock cycle (MELA_IN/MELA_OUT), low-stock soft-reserve (≤3),
 * oversell reconciliation.
 */
class MelaStockCalculatorTest {

    @Test
    fun `should compute net stock from moves`() {
        val moves = listOf(
            StockMove(StockChangeReason.PURCHASE, 10),
            StockMove(StockChangeReason.SALE, -3),
            // MELA_IN is a shop→mela transfer: it must NOT change total on-hand stock.
            StockMove(StockChangeReason.MELA_IN, 5),
        )
        assertThat(MelaStockCalculator.netStock(moves)).isEqualTo(7)
    }

    @Test
    fun `should compute atMela as in minus out`() {
        val moves = listOf(
            StockMove(StockChangeReason.MELA_IN, 8),
            StockMove(StockChangeReason.MELA_OUT, -2), // signed per MelaRepositoryImpl (bring back)
        )
        val line = MelaStockCalculator.melaStockLine("b1", "বই", moves)
        assertThat(line.atMela).isEqualTo(6)
        assertThat(line.melaIn).isEqualTo(8)
        assertThat(line.melaOut).isEqualTo(2) // magnitude
    }

    @Test
    fun `should warn low stock at or below soft threshold of three`() {
        val books = listOf("b1" to "বই", "b2" to "বই২")
        val melaStocks = mapOf("b1" to 3, "b2" to 10)
        val shopStocks = mapOf("b1" to 20, "b2" to 0)
        val alerts = MelaStockCalculator.lowStockAlerts(books, melaStocks, shopStocks)
        assertThat(alerts).hasSize(1)
        assertThat(alerts[0].bookId).isEqualTo("b1")
        assertThat(alerts[0].melaStock).isEqualTo(3)
        assertThat(alerts[0].atShop).isEqualTo(20)
    }

    @Test
    fun `should not warn when mela stock above threshold`() {
        val books = listOf("b1" to "বই")
        val alerts = MelaStockCalculator.lowStockAlerts(books, mapOf("b1" to 5), mapOf("b1" to 0))
        assertThat(alerts).isEmpty()
    }

    @Test
    fun `should flag oversell when net stock is negative`() {
        val books = listOf("b1" to "বই")
        val oversell = MelaStockCalculator.oversellAlerts(books, mapOf("b1" to -2))
        assertThat(oversell).hasSize(1)
        assertThat(oversell[0].oversoldBy).isEqualTo(2)
    }

    @Test
    fun `should not flag oversell when net stock non-negative`() {
        val books = listOf("b1" to "বই", "b2" to "বই২")
        val oversell = MelaStockCalculator.oversellAlerts(books, mapOf("b1" to 0, "b2" to 5))
        assertThat(oversell).isEmpty()
    }

    @Test
    fun `should build per-book report across books`() {
        val movesByBook = mapOf(
            ("b1" to "বই") to listOf(
                StockMove(StockChangeReason.PURCHASE, 10),
                StockMove(StockChangeReason.MELA_IN, 5),
                StockMove(StockChangeReason.MELA_OUT, -1), // signed: bring 1 back
            ),
            ("b2" to "বই২") to listOf(
                StockMove(StockChangeReason.PURCHASE, 4),
                StockMove(StockChangeReason.MELA_IN, 2),
            ),
        )
        val report = MelaStockCalculator.buildReport(movesByBook)
        assertThat(report).hasSize(2)
        val b1 = report.first { it.bookId == "b1" }
        assertThat(b1.atMela).isEqualTo(4)        // 5 mela-in − 1 mela-out
        assertThat(b1.netStock).isEqualTo(10)     // purchase only; transfers excluded
        assertThat(b1.melaIn).isEqualTo(5)
        assertThat(b1.melaOut).isEqualTo(1)       // magnitude
        val b2 = report.first { it.bookId == "b2" }
        assertThat(b2.atMela).isEqualTo(2)
        assertThat(b2.netStock).isEqualTo(4)
    }
}
