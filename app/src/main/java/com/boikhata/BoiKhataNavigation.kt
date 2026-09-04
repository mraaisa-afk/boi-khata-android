package com.boikhata

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
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
 * D18: Bottom navigation (Home/Catalog/Khata/Sale) via Navigation-Compose.
 * Blueprint §2: দৃশ্যমান Bottom Navigation Bar (সর্বোচ্চ ৪ ট্যাব) — no hamburger.
 * P2b: 4th tab (বিক্রয়) added.
 */
@Composable
fun BoiKhataMainScreen(tenantId: String, shopName: String, role: com.boikhata.core.domain.enums.Role) {
    val navController = rememberNavController()

    val tabs = listOf(
        NavTab("home", R.string.nav_home, Icons.Default.Home),
        NavTab("catalog", R.string.nav_catalog, Icons.Default.Book),
        NavTab("khata", R.string.nav_khata, Icons.Default.People),
        NavTab("sale", R.string.nav_sale, Icons.Default.PointOfSale),
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val selected = currentRoute?.startsWith(tab.route) == true ||
                        (tab.route == "home" && currentRoute == null)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes)) },
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
            // P2b: POS sale screen
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

private data class NavTab(val route: String, val labelRes: Int, val icon: ImageVector)
