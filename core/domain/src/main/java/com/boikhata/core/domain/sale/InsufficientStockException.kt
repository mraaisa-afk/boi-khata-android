package com.boikhata.core.domain.sale

/**
 * P14 (owner device ruling — «মাইনাস নয়»): thrown inside the D22 checkout
 * transaction when a bill line's quantity exceeds the book's LIVE stock
 * (books.initialStock + stock_ledger delta — the B-005 derivation). Throwing
 * aborts the whole atomic transaction: no bill, no bill lines, no stock-ledger
 * rows, no khata/cashbook entries.
 *
 * [message] deliberately STARTS with the owner's exact device-string
 * «স্টকে পর্যাপ্ত বই নেই» so the POS error surface renders it verbatim.
 */
class InsufficientStockException(
    val bookTitleBn: String,
    val availableStock: Int,
    val requestedQuantity: Int,
) : Exception() {
    override val message: String
        get() = "স্টকে পর্যাপ্ত বই নেই: $bookTitleBn (স্টকে আছে ${availableStock}, চাওয়া হয়েছে ${requestedQuantity})"
}
