package com.boikhata

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
 */
private val ColorPrimaryMaroon = Color(0xFF800000)
private val ColorSurfaceIvory = Color(0xFFFDFAF6)
private val ColorAccentGold = Color(0xFFC9A227)
private val ColorNavUnselected = Color(0xFF6B6B6B)

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

    // D79: POS left the tab row and became the central FAB, so "sale" is not in this list.
    val leadingTabs = listOf(
        NavTab("home", R.string.nav_home, Icons.Default.Home),
        NavTab("catalog", R.string.nav_stock, Icons.Default.Book),
    )
    val trailingTabs = listOf(
        NavTab("khata", R.string.nav_khata, Icons.Default.People),
        NavTab("more", R.string.nav_more, Icons.Default.List),
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        topBar = {
            IconButton(onClick = { navController.navigate("settings") }) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
            }
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                NavigationBar(containerColor = ColorSurfaceIvory) {
                    leadingTabs.forEach { tab ->
                        NavBarTab(tab = tab, currentRoute = currentRoute, navController = navController)
                    }
                    // Reserved gap for the central FAB — keeps the tab row balanced.
                    Spacer(modifier = Modifier.weight(1f))
                    trailingTabs.forEach { tab ->
                        NavBarTab(tab = tab, currentRoute = currentRoute, navController = navController)
                    }
                }
                FloatingActionButton(
                    onClick = { navController.navigateToTab("sale") },
                    shape = CircleShape,
                    containerColor = ColorAccentGold,
                    contentColor = ColorPrimaryMaroon,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-20).dp)
                        .size(64.dp) // primaryTouchTarget token
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
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable("home") {
                HomeScreen(tenantId = tenantId)
            }
            composable("catalog") {
                CatalogScreen(
                    tenantId = tenantId,
                    onAddBook = { navController.navigate("book_add_edit/null") },
                    onEditBook = { bookId -> navController.navigate("book_add_edit/$bookId") },
                )
            }
            composable(
                route = "book_add_edit/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
            ) { entry ->
                val bookIdArg = entry.arguments?.getString("bookId")
                val bookId = if (bookIdArg == "null") null else bookIdArg
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
            // D79: "আরও" hub — everything that left the tab row lives here.
            composable("more") {
                MoreScreen(onEntryClick = { route -> navController.navigate(route) })
            }
            // P2b: POS sale screen — now reached from the central FAB.
            composable("sale") {
                PosScreen(
                    tenantId = tenantId,
                    onCheckoutComplete = { billId ->
                        navController.navigate("bill_detail/$billId")
                    },
                    onExpenseClick = { navController.navigate("expense") },
                    onReportsClick = { navController.navigate("reports") },
                    onCashCloseClick = { navController.navigate("cash_close") },
                    onSubscriptionClick = { navController.navigate("subscription") },
                    onSupplierClick = { navController.navigate("supplier") },
                    onMelaClick = { navController.navigate("mela") },
                )
            }
            // P3a: Expense + Cashbook + Owner Drawing
            composable("expense") {
                ExpenseScreen(tenantId = tenantId)
            }
            // P3c: Accounting reports (P&L + balance-sheet + period-lock + budget)
            composable("reports") {
                ReportsScreen(tenantId = tenantId)
            }
            // P3c: Daily cash-close "আজকের হিসাব"
            composable("cash_close") {
                CashCloseScreen(tenantId = tenantId)
            }
            // P4b: Subscription screen (manual bKash, OWNER-gated)
            composable("subscription") {
                SubscriptionScreen(tenantId = tenantId, role = role)
            }
            // P5: Supplier/publisher payable ledger (দেনা-খাতা)
            composable("supplier") {
                SupplierScreen(tenantId = tenantId, shopName = shopName)
            }
            // P5: Mela mode (book fair / seasonal)
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
            // P2b: Bill history
            composable("bill_history") {
                BillHistoryScreen(
                    tenantId = tenantId,
                    onBillClick = { billId -> navController.navigate("bill_detail/$billId") },
                )
            }
            // P2b: Bill detail
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
        onClick = { navController.navigateToTab(tab.route) },
        icon = { Icon(tab.icon, contentDescription = label) },
        label = { Text(label) },
        alwaysShowLabel = true,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = ColorPrimaryMaroon,
            selectedTextColor = ColorPrimaryMaroon,
            indicatorColor = ColorAccentGold.copy(alpha = 0.20f),
            unselectedIconColor = ColorNavUnselected,
            unselectedTextColor = ColorNavUnselected,
        ),
    )
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private data class NavTab(val route: String, val labelRes: Int, val icon: ImageVector)
