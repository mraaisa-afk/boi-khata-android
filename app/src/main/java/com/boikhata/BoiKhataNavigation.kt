package com.boikhata

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.boikhata.core.designsystem.ColorAccentGold
import com.boikhata.core.designsystem.ColorBrandMaroon
import com.boikhata.core.designsystem.ColorNavUnselected
import com.boikhata.core.designsystem.ColorSurfaceIvory
import com.boikhata.feature.catalog.BookAddEditScreen
import com.boikhata.feature.catalog.CatalogScreen
import com.boikhata.feature.expense.ExpenseScreen
import com.boikhata.feature.home.HomeScreen
import com.boikhata.feature.khata.KhataAddCustomerScreen
import com.boikhata.feature.khata.KhataCustomerDetailScreen
import com.boikhata.feature.khata.KhataCustomerListScreen
import com.boikhata.feature.melamode.MelaScreen
import com.boikhata.feature.reports.CashCloseScreen
import com.boikhata.feature.reports.ReportsScreen
import com.boikhata.feature.sale.BillDetailScreen
import com.boikhata.feature.sale.BillHistoryScreen
import com.boikhata.feature.sale.PosScreen
import com.boikhata.feature.subscription.SubscriptionScreen
import com.boikhata.feature.supplier.SupplierScreen

/**
 * D18: Bottom navigation via Navigation-Compose.
 * D79 (Locked Design Spec — HomeScreen v2): হোম · স্টক · [৳+ FAB] · খাতা · আরও.
 * Blueprint §2 / G22: max 4 tabs — the central FAB is not a tab, so the limit holds.
 * G19: every label comes from strings.xml. G23: no drawer.
 * Locked Design Spec §4.5 (hybrid palette): maroon chrome, ivory surface, gold FAB.
 *
 * PR B: topBar removed — HomeScreen now embeds its own AppBar in LazyColumn (D79 §2.1).
 * Other screens will add their own TopAppBar in future PRs.
 */
@Composable
fun BoiKhataMainScreen(
    tenantId: String,
    shopName: String,
    role: com.boikhata.core.domain.enums.Role,
    phone: String,
    liteMode: Boolean,
    onLiteModeChange: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onDemoReset: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // G19 + a11y: stringResource is @Composable, so the FAB's spoken label is read
    // here and captured by the semantics lambda below.
    val newSaleLabel = stringResource(R.string.fab_new_sale)

    // D79: POS left the tab row and became the central FAB, so "sale" is not in this list.
    // Tab table + B-009 restore semantics live in TabNavigation.kt (test-shared).
    val leadingTabs = LEADING_TABS
    val trailingTabs = TRAILING_TABS

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        // topBar removed in PR B — HomeScreen embeds its own AppBar (D79 §2.1).
        // Other screens will add their own TopAppBar in future PRs.
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                NavigationBar(containerColor = ColorSurfaceIvory) {
                    leadingTabs.forEach { tab ->
                        NavBarTab(tab = tab, currentRoute = currentRoute, navController = navController)
                    }
                    // Reserved gap for the central FAB
                    Spacer(modifier = Modifier.weight(1f))
                    trailingTabs.forEach { tab ->
                        NavBarTab(tab = tab, currentRoute = currentRoute, navController = navController)
                    }
                }
                FloatingActionButton(
                    onClick = { navController.navigateToTab("sale") },
                    shape = CircleShape,
                    containerColor = ColorAccentGold,
                    contentColor = ColorBrandMaroon,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-20).dp)
                        .size(64.dp)
                        .semantics { contentDescription = newSaleLabel },
                ) {
                    Text(
                        text = stringResource(R.string.fab_new_sale_symbol),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable("home") {
                HomeScreen(
                    tenantId   = tenantId,
                    shopName   = shopName,
                    phone      = phone, // P14: dashboard header shows the shop phone line
                    isLicensed = false, // §5.1 OPEN ITEM: pending Sakira ruling on license states
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable("catalog") {
                CatalogScreen(
                    tenantId = tenantId,
                    // D85 (fixes B-003): never put a literal "null" path segment in a route —
                    // navigation 2.8.x NavType.StringType.parseValue deserializes "null" into an
                    // actual null, which then fails NavArgument verification for the declared
                    // argument ("Wrong argument type for 'bookId' ... string expected").
                    // Optional route args use the query-param form; absent arg → add mode.
                    onAddBook = { navController.navigate("book_add_edit") },
                    onEditBook = { bookId -> navController.navigate("book_add_edit?bookId=$bookId") },
                )
            }
            // D85 (fixes B-003): bookId is an OPTIONAL query argument with a null default.
            // The previous path route "book_add_edit/{bookId}" + navigate("book_add_edit/null")
            // crashed on navigation 2.8.5: parseValue("null") → actual null in the argument
            // bundle → NavArgument.verify rejects null for the non-nullable declared argument
            // at NavDestination.addInDefaultArgs → IllegalArgumentException on the "+" tap.
            composable(
                route = "book_add_edit?bookId={bookId}",
                arguments = listOf(
                    navArgument("bookId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val bookId = entry.arguments?.getString("bookId")
                BookAddEditScreen(
                    tenantId = tenantId,
                    bookId = bookId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("khata") {
                KhataCustomerListScreen(
                    tenantId = tenantId,
                    onAddCustomer = { navController.navigate("khata_add_customer") },
                    onCustomerClick = { customerId -> navController.navigate("khata_detail/$customerId") },
                )
            }
            composable("khata_add_customer") {
                KhataAddCustomerScreen(
                    // B-004: thread the claims tenant through navigation — the add
                    // screen's own VM instance has no tenant until told explicitly.
                    tenantId = tenantId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "khata_detail/{customerId}",
                arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
            ) { entry ->
                val customerId = entry.arguments?.getString("customerId") ?: return@composable
                KhataCustomerDetailScreen(
                    tenantId = tenantId,
                    customerId = customerId,
                    shopName = shopName,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("more") {
                MoreScreen(onEntryClick = { route -> navController.navigate(route) })
            }
            composable("sale") {
                PosScreen(
                    tenantId = tenantId,
                    onCheckoutComplete = { billId -> navController.navigate("bill_detail/$billId") },
                    onExpenseClick = { navController.navigate("expense") },
                    onReportsClick = { navController.navigate("reports") },
                    onCashCloseClick = { navController.navigate("cash_close") },
                    onSubscriptionClick = { navController.navigate("subscription") },
                    onSupplierClick = { navController.navigate("supplier") },
                    onMelaClick = { navController.navigate("mela") },
                )
            }
            composable("expense") {
                ExpenseScreen(tenantId = tenantId)
            }
            composable("reports") {
                ReportsScreen(tenantId = tenantId)
            }
            composable("cash_close") {
                CashCloseScreen(tenantId = tenantId)
            }
            composable("subscription") {
                SubscriptionScreen(tenantId = tenantId, role = role)
            }
            composable("supplier") {
                SupplierScreen(tenantId = tenantId, shopName = shopName)
            }
            composable("mela") {
                MelaScreen(tenantId = tenantId)
            }
            composable("settings") {
                SettingsScreen(
                    tenantId = tenantId,
                    phone = phone,
                    liteMode = liteMode,
                    onLiteModeChange = onLiteModeChange,
                    onSpeakSetup = { VoiceSetupSpeaker(context).speakSetup() },
                    onShareCopy = { shareMonthlyCopy(context) },
                    onMigration = { navController.navigate("number_migration") },
                    onUpgrade = { navController.navigate("subscription") },
                    onDemoReset = onDemoReset,
                    isOwner = role == com.boikhata.core.domain.enums.Role.OWNER,
                )
            }
            composable("number_migration") {
                NumberMigrationScreen(onSignOut = onSignOut)
            }
            composable("bill_history") {
                BillHistoryScreen(
                    tenantId = tenantId,
                    onBillClick = { billId -> navController.navigate("bill_detail/$billId") },
                )
            }
            composable(
                route = "bill_detail/{billId}",
                arguments = listOf(navArgument("billId") { type = NavType.StringType }),
            ) { entry ->
                val billId = entry.arguments?.getString("billId") ?: return@composable
                BillDetailScreen(
                    tenantId = tenantId,
                    billId = billId,
                    shopName = shopName,
                    onBack = { navController.popBackStack() },
                    onNewSale = { navController.navigate("sale") },
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavBarTab(
    tab: NavTab,
    currentRoute: String?,
    navController: NavHostController,
) {
    val selected = currentRoute?.startsWith(tab.route) == true ||
        (tab.route == "home" && currentRoute == null)
    val label = stringResource(tab.labelRes)
    NavigationBarItem(
        selected = selected,
        onClick = {
            // P14 (owner device finding): when the tab is ALREADY selected but its
            // restored unit tops out on a child screen (e.g. খাতা → detail), the
            // canonical restore-tap re-lands on the child — the tap looks dead.
            // A re-tap on the active tab must always respond: land on the tab ROOT
            // (children popped and DISCARDED so they cannot resurrect).
            val onOwnChild = selected && currentRoute != tab.route
            if (onOwnChild) {
                navController.navigateToTabRoot(tab.route)
            } else {
                navController.navigateToTab(tab.route, restoreState = tab.restoresState)
            }
        },
        icon = { Icon(tab.icon, contentDescription = label) },
        label = { Text(label) },
        alwaysShowLabel = true,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = ColorBrandMaroon,
            selectedTextColor = ColorBrandMaroon,
            indicatorColor = ColorAccentGold.copy(alpha = 0.20f),
            unselectedIconColor = ColorNavUnselected,
            unselectedTextColor = ColorNavUnselected,
        ),
    )
}
