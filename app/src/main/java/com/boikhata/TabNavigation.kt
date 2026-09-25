package com.boikhata

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.People
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/**
 * Bottom-tab model + navigation primitive.
 *
 * The tab table and [navigateToTab] live here (not inside the composable) so
 * TabNavigationTest can drive the EXACT production semantics under Robolectric.
 */
internal data class NavTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    // B-009: the আরও hub must always land on its own menu root. Restoring the
    // tab's saved back-stack unit resurrects child screens (রিপোর্ট, খরচ, …)
    // that were pushed on top of the menu from ANY entry point (menu rows AND
    // the POS top-bar actions — same flat routes), so the tab tap showed the
    // stale child instead of the menu. false = always a fresh root.
    val restoresState: Boolean = true,
)

// D79: POS left the tab row and became the central FAB, so "sale" is not in this list.
// B-009: "more" is the only tab with restoresState = false.
internal val LEADING_TABS = listOf(
    NavTab("home", R.string.nav_home, Icons.Default.Home),
    NavTab("catalog", R.string.nav_stock, Icons.Default.Book),
)

internal val TRAILING_TABS = listOf(
    NavTab("khata", R.string.nav_khata, Icons.Default.People),
    NavTab("more", R.string.nav_more, Icons.Default.List, restoresState = false),
)

/**
 * Standard Navigation-Compose bottom-bar pattern: pop everything above the start
 * destination, saving the popped stack as the tab's restorable unit; single-top;
 * optionally restore that tab's previously saved unit.
 *
 * B-009: [restoreState] defaults to true (খাতা keeps its list/detail state across
 * tab switches — intended). The আরও hub passes false — see [NavTab.restoresState].
 */
internal fun NavHostController.navigateToTab(route: String, restoreState: Boolean = true) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        this.restoreState = restoreState
    }
}

/**
 * P14 (owner device finding — "tab tap does nothing until back is pressed"): a tab
 * whose restored unit tops out on a CHILD re-landed on that child on every tap, so
 * the tab appeared dead and only BACK "un-stuck" it. This primitive is the re-tap
 * path: pop the tab's stack WITHOUT saving (the stale unit is discarded, so the
 * child can never resurrect) and land on a FRESH tab root, single-top.
 */
internal fun NavHostController.navigateToTabRoot(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}
