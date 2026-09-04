package com.boikhata.core.domain.accounting

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D24: PurchaseRouter unit tests — auto-routing rules.
 */
class PurchaseRouterTest {

    @Test
    fun `should route book purchase to inventory`() {
        assertThat(PurchaseRouter.shouldRouteToInventory(PurchaseRouter.PurchaseItemType.BOOK_PURCHASE)).isTrue()
    }

    @Test
    fun `should not route non-book purchase to inventory`() {
        assertThat(PurchaseRouter.shouldRouteToInventory(PurchaseRouter.PurchaseItemType.NON_BOOK_PURCHASE)).isFalse()
    }

    @Test
    fun `should route book purchase to INVENTORY destination`() {
        assertThat(PurchaseRouter.route(PurchaseRouter.PurchaseItemType.BOOK_PURCHASE)).isEqualTo(RoutingDestination.INVENTORY)
    }

    @Test
    fun `should route non-book purchase to EXPENSE destination`() {
        assertThat(PurchaseRouter.route(PurchaseRouter.PurchaseItemType.NON_BOOK_PURCHASE)).isEqualTo(RoutingDestination.EXPENSE)
    }
}
