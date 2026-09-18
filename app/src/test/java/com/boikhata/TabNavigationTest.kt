package com.boikhata

import android.app.Application
import androidx.compose.material3.Text
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * B-009 regression tests — the bottom-tab state machine.
 *
 * Root cause (empirically proven — see the captured pre-fix failure in the PR):
 * `navigateToTab(..., restoreState = true)` restores the tab's SAVED back-stack
 * unit. That unit accumulates every child pushed on top of the tab root by plain
 * `navigate()` calls — from the আরও menu rows AND the POS top-bar actions (same
 * flat routes). After আরও → রিপোর্ট → tab-away, the saved unit was [more, reports]
 * and the next আরও tap restored "reports" (Truth output: `expected: more but was:
 * reports`) — the owner's device finding.
 *
 * Production semantics are driven through the REAL tab table (LEADING_TABS /
 * TRAILING_TABS in TabNavigation.kt): flipping `NavTab.restoresState` back to
 * true for আরও makes moreTab_alwaysLandsOnMenuRoot* fail immediately.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class TabNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: NavHostController

    /** The exact production tab objects — no hardcoded booleans in these tests. */
    private val tabs = LEADING_TABS + TRAILING_TABS
    private val moreTab = tabs.first { it.route == "more" }
    private val khataTab = tabs.first { it.route == "khata" }
    private val homeTab = tabs.first { it.route == "home" }

    private fun setUpGraph() {
        composeRule.setContent {
            navController = rememberNavController()
            NavHost(navController = navController, startDestination = "home") {
                composable("home") { Text("home") }
                composable("catalog") { Text("catalog") }
                composable("khata") { Text("khata") }
                composable("khata_detail/{customerId}") { Text("khata detail") }
                composable("more") { Text("more") }
                composable("sale") { Text("sale") }
                composable("reports") { Text("reports") }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun moreTab_showsMenuRoot_onFirstTap() {
        setUpGraph()
        composeRule.runOnIdle {
            navController.navigateToTab(moreTab.route, restoreState = moreTab.restoresState)
            assertThat(navController.currentDestination?.route).isEqualTo("more")
        }
    }

    @Test
    fun moreTab_alwaysLandsOnMenuRoot_evenAfterChildWasSavedIntoItsUnit() {
        // THE owner-reported bug: আরও → menu child (রিপোর্ট) → switch tab → আরও.
        // Pre-fix (restoresState = true) this restored the polluted [more, reports]
        // unit and landed on "reports".
        setUpGraph()
        composeRule.runOnIdle {
            navController.navigateToTab(moreTab.route, restoreState = moreTab.restoresState)
            assertThat(navController.currentDestination?.route).isEqualTo("more")
            navController.navigate("reports") // মেনু রো / POS action → child of the hub
            assertThat(navController.currentDestination?.route).isEqualTo("reports")
            navController.navigateToTab(homeTab.route) // switch away → saves [more, reports]
            navController.navigateToTab(moreTab.route, restoreState = moreTab.restoresState)
            assertThat(navController.currentDestination?.route).isEqualTo("more")
        }
    }

    @Test
    fun moreTab_landsOnMenuRoot_afterPosSubtabNavigation() {
        // Owner's literal repro path: POS first (central FAB), in-screen action →
        // রিপোর্ট, then the আরও bottom tab. Must always render the menu root.
        setUpGraph()
        composeRule.runOnIdle {
            navController.navigateToTab("sale") // central FAB semantics (unchanged)
            assertThat(navController.currentDestination?.route).isEqualTo("sale")
            navController.navigate("reports") // POS top-bar action
            assertThat(navController.currentDestination?.route).isEqualTo("reports")
            navController.navigateToTab(moreTab.route, restoreState = moreTab.restoresState)
            assertThat(navController.currentDestination?.route).isEqualTo("more")
        }
    }

    @Test
    fun khataTab_keepsRestoreState_detailSurvivesTabSwitch() {
        // Intended behavior that must NOT regress: খাতা list → customer detail →
        // switch away → return → detail is restored (restoreState = true stays).
        setUpGraph()
        composeRule.runOnIdle {
            navController.navigateToTab(khataTab.route, restoreState = khataTab.restoresState)
            navController.navigate("khata_detail/cust-1")
            assertThat(navController.currentDestination?.route)
                .isEqualTo("khata_detail/{customerId}")
            navController.navigateToTab(homeTab.route)
            navController.navigateToTab(khataTab.route, restoreState = khataTab.restoresState)
            assertThat(navController.currentDestination?.route)
                .isEqualTo("khata_detail/{customerId}")
        }
    }

    @Test
    fun moreTab_fromInsideOwnChild_popsBackToMenuRoot() {
        // Tapping আরও while ALREADY inside a menu child must still land on the
        // menu root (children are popped and saved, never re-shown by the tap).
        setUpGraph()
        composeRule.runOnIdle {
            navController.navigateToTab(moreTab.route, restoreState = moreTab.restoresState)
            navController.navigate("reports")
            navController.navigateToTab(moreTab.route, restoreState = moreTab.restoresState)
            assertThat(navController.currentDestination?.route).isEqualTo("more")
        }
    }
}
