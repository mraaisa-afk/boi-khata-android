package com.boikhata.core.domain.accounting

/**
 * D24: Purchase auto-routing rule.
 * Blueprint §7.8: "বই-ক্রয় → ইনভেন্টরি (COGS), খরচ নয়।"
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 */
object PurchaseRouter {

    enum class PurchaseItemType {
        BOOK_PURCHASE,      // → stock_ledger (PURCHASE), NOT an expense
        NON_BOOK_PURCHASE,  // → expense, NOT stock
    }

    /**
     * Determine whether a purchase should route to inventory or expense.
     * Book purchases → inventory (stock_ledger PURCHASE entry).
     * Non-book purchases → expense.
     */
    fun shouldRouteToInventory(itemType: PurchaseItemType): Boolean {
        return itemType == PurchaseItemType.BOOK_PURCHASE
    }

    /**
     * Determine the routing destination.
     * Returns ROUTE_INVENTORY for books, ROUTE_EXPENSE for non-books.
     */
    fun route(itemType: PurchaseItemType): RoutingDestination {
        return if (itemType == PurchaseItemType.BOOK_PURCHASE) {
            RoutingDestination.INVENTORY
        } else {
            RoutingDestination.EXPENSE
        }
    }
}

enum class RoutingDestination {
    INVENTORY,  // stock_ledger PURCHASE entry
    EXPENSE,    // expenses table entry
}
