package com.boikhata.core.domain.accounting

import com.boikhata.core.domain.enums.StockChangeReason
import com.boikhata.core.domain.model.LowStockAlert
import com.boikhata.core.domain.model.MelaStockLine
import com.boikhata.core.domain.model.OversellAlert

/**
 * D56: Mela stock rules — stock cycle (MELA_IN/MELA_OUT), low-stock soft-reservation
 * warning (≤3/own threshold), and oversell-reconciliation alerts.
 *
 * Stock is DERIVED from the stock_ledger (ARCH §4 "Balances = derived"), never stored.
 * Pure (no Room/Android) + independently unit-testable.
 */
object MelaStockCalculator {

    /** Default soft-reservation threshold per Blueprint §8 "≤৩ পরিমাণে". */
    const val SOFT_RESERVE_THRESHOLD = 3

    /** A stock ledger row reduced to the fields MelaStockCalculator needs. */
    data class StockMove(
        val reason: StockChangeReason,
        val changeQuantity: Int,
    )

    /** Current net stock = SUM of all changeQuantity. */
    fun netStock(moves: List<StockMove>): Int = moves.sumOf { it.changeQuantity }

    /** Aggregate mela in/out for a book from its ledger moves. */
    fun melaStockLine(bookId: String, bookTitleBn: String, moves: List<StockMove>): MelaStockLine {
        val melaIn = moves.filter { it.reason == StockChangeReason.MELA_IN }.sumOf { it.changeQuantity }
        val melaOut = moves.filter { it.reason == StockChangeReason.MELA_OUT }.sumOf { it.changeQuantity }
        return MelaStockLine(
            bookId = bookId,
            bookTitleBn = bookTitleBn,
            netStock = netStock(moves),
            melaIn = melaIn,
            melaOut = melaOut,
            atMela = melaIn - melaOut,
        )
    }

    /**
     * Low-stock soft-reservation warnings for the mela stall. A book alerts when its stock
     * at the mela stall (`atMela`) is at/below the soft threshold (default 3, per Blueprint §8
     * "স্টক-সতর্কতা ≤৩ পরিমাণে") — meaning the stall is running low and the shopkeeper can
     * re-supply from the shop. Soft-reservation is a WARNING only — never a freeze.
     *
     * @param books (bookId, titleBn) pairs
     * @param melaStocks bookId → stock at the mela stall (atMela)
     * @param shopStocks bookId → stock still at the shop (net − atMela)
     */
    fun lowStockAlerts(
        books: List<Pair<String, String>>,
        melaStocks: Map<String, Int>,
        shopStocks: Map<String, Int>,
        softThreshold: Int = SOFT_RESERVE_THRESHOLD,
    ): List<LowStockAlert> {
        return books.mapNotNull { (bookId, titleBn) ->
            val melaStock = melaStocks[bookId] ?: 0
            if (melaStock <= softThreshold) {
                LowStockAlert(
                    bookId = bookId,
                    bookTitleBn = titleBn,
                    melaStock = melaStock,
                    softThreshold = softThreshold,
                    atShop = shopStocks[bookId] ?: 0,
                )
            } else {
                null
            }
        }
    }

    /**
     * Oversell / reconciliation alerts — a book's stock went NEGATIVE (sold more than
     * available). This is the oversell-reconciliation signal the shopkeeper must fix.
     */
    fun oversellAlerts(
        books: List<Pair<String, String>>,
        netStocks: Map<String, Int>,
    ): List<OversellAlert> {
        return books.mapNotNull { (bookId, titleBn) ->
            val stock = netStocks[bookId] ?: 0
            if (stock < 0) {
                OversellAlert(
                    bookId = bookId,
                    bookTitleBn = titleBn,
                    currentStock = stock,
                    oversoldBy = -stock,
                )
            } else {
                null
            }
        }
    }

    /**
     * Split a cargo of MELA_IN/MELA_OUT moves into per-book lines for reporting.
     * @param movesByBook (bookId, titleBn) → list of that book's ledger moves
     */
    fun buildReport(
        movesByBook: Map<Pair<String, String>, List<StockMove>>,
    ): List<MelaStockLine> {
        return movesByBook.mapNotNull { (key, moves) ->
            melaStockLine(key.first, key.second, moves)
        }
    }
}
