package com.boikhata.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boikhata.core.designsystem.ColorAccentGold
import com.boikhata.core.designsystem.ColorBrandMaroon
import com.boikhata.core.designsystem.ColorSemanticPositive
import com.boikhata.core.designsystem.ColorSurfaceIvory
import com.boikhata.core.domain.model.HomeAnalyticsPoint
import com.boikhata.core.domain.model.HomeData
import com.boikhata.core.domain.model.LowStockBookSummary
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val ColorHeroBg = ColorSemanticPositive
private val ColorHeroAmount = ColorAccentGold

@Composable
fun HomeScreen(
    tenantId: String,
    shopName: String,
    isLicensed: Boolean = false,
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var amountVisible by remember { mutableStateOf(true) }

    LaunchedEffect(tenantId) { viewModel.loadHome(tenantId) }

    when (val s = uiState) {
        is HomeUiState.Loading -> Box(
            Modifier.fillMaxSize().background(ColorSurfaceIvory),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = ColorBrandMaroon) }

        is HomeUiState.Error -> Box(
            Modifier.fillMaxSize().background(ColorSurfaceIvory),
            contentAlignment = Alignment.Center,
        ) { Text(s.message, color = MaterialTheme.colorScheme.error) }

        is HomeUiState.Success -> HomeContent(
            data = s.data,
            shopName = shopName,
            isLicensed = isLicensed,
            amountVisible = amountVisible,
            onAmountToggle = { amountVisible = !amountVisible },
            onNavigate = onNavigate,
        )
    }
}

private val ScreenPadding = 16.dp
private val SmallTileHeight = 56.dp
private val LargeTileHeight = SmallTileHeight * 2 + 8.dp

@Composable
private fun HomeContent(
    data: HomeData,
    shopName: String,
    isLicensed: Boolean,
    amountVisible: Boolean,
    onAmountToggle: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ColorSurfaceIvory),
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        item(key = "app_bar") { HomeAppBar(shopName = shopName, isLicensed = isLicensed) }
        item(key = "hero_card") {
            HeroCard(
                data = data,
                amountVisible = amountVisible,
                onAmountToggle = onAmountToggle,
                modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 12.dp),
            )
        }
        item(key = "quick_actions") {
            QuickActionGrid(
                todayBillCount = data.todayBillCount,
                pendingKhataCount = data.dueCustomerCount,
                onNavigate = onNavigate,
                modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp),
            )
        }
        item(key = "alerts_section") {
            AlertsSection(
                data = data,
                onNavigate = onNavigate,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }
        item(key = "analytics_sheet") {
            AnalyticsSheet(
                monthNetProfit = data.monthNetProfit,
                points = data.monthAnalytics,
                modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun HomeAppBar(shopName: String, isLicensed: Boolean) {
    Surface(modifier = Modifier.fillMaxWidth(), color = ColorBrandMaroon, tonalElevation = 0.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Book, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_wordmark), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                if (isLicensed) { Spacer(Modifier.width(6.dp)); PremiumBadge() }
                Spacer(Modifier.weight(1f))
                SyncStatusChip()
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.home_notification_cd), tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(2.dp))
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).semantics { contentDescription = shopName },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(shopName.firstOrNull()?.toString() ?: stringResource(R.string.home_avatar_fallback), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { }) {
                Text(shopName, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.80f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun PremiumBadge() {
    Surface(shape = RoundedCornerShape(4.dp), color = ColorAccentGold) {
        Text(stringResource(R.string.home_premium_badge), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ColorBrandMaroon, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun SyncStatusChip() {
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.15f)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ColorSemanticPositive))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.home_sync_label), style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
    }
}

@Composable
private fun HeroCard(data: HomeData, amountVisible: Boolean, onAmountToggle: () -> Unit, modifier: Modifier = Modifier) {
    // D81: Full D79 formula — নিট লাভ = (নগদ বিক্রি + খাতা আদায়) − নগদ ব্যয়
    val todayIncome = data.todaySalesTotal + data.todayKhataCollection
    val netProfit = todayIncome - data.todayExpenseTotal
    val heroText = if (amountVisible) formatBengaliTaka(netProfit) else stringResource(R.string.home_hero_amount_hidden)
    val trendPercent: Float? = if (data.yesterdayNetProfit > 0.01) ((netProfit - data.yesterdayNetProfit) / data.yesterdayNetProfit * 100).toFloat() else null

    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = ColorHeroBg) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.home_hero_title), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.80f), modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.15f), modifier = Modifier.clickable { }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(stringResource(R.string.home_hero_period), style = MaterialTheme.typography.labelMedium, color = Color.White)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onAmountToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (amountVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = stringResource(if (amountVisible) R.string.home_hero_hide_amount else R.string.home_hero_show_amount),
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                AnimatedContent(targetState = heroText, label = "hero_amount") { text ->
                    Text(text, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = ColorHeroAmount)
                }
                if (amountVisible && trendPercent != null) {
                    Spacer(Modifier.width(8.dp))
                    TrendBadge(trendPercent = trendPercent, trendSuffix = stringResource(R.string.home_hero_trend_suffix), modifier = Modifier.padding(bottom = 4.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.20f), thickness = 0.5.dp)
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                // D81: ↑ আয় = নগদ বিক্রি + খাতা আদায় (combined income)
                HeroSubAmount(stringResource(R.string.home_hero_income_label), "↑", todayIncome, amountVisible, Modifier.weight(1f))
                HeroSubAmount(stringResource(R.string.home_hero_expense_label), "↓", data.todayExpenseTotal, amountVisible, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.home_hero_footer, banglaDigit(data.todayBillCount), banglaDigit(data.dueCustomerCount)), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.60f))
        }
    }
}

@Composable
private fun TrendBadge(trendPercent: Float, trendSuffix: String, modifier: Modifier = Modifier) {
    val symbol = if (trendPercent >= 0f) "▲" else "▼"
    val pct = banglaDigit(abs(trendPercent).roundToInt())
    Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.18f), modifier = modifier) {
        Text("$symbol $pct% $trendSuffix", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
    }
}

@Composable
private fun HeroSubAmount(label: String, directionIcon: String, amount: Double, amountVisible: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("$directionIcon $label", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
        Text(if (amountVisible) formatBengaliTaka(amount) else stringResource(R.string.home_hero_amount_hidden), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun QuickActionGrid(todayBillCount: Int, pendingKhataCount: Int, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val tileGap = 8.dp
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(tileGap)) {
        ActionTile(stringResource(R.string.home_action_new_sale), Icons.Filled.ShoppingCart, { onNavigate("sale") }, Modifier.weight(1f).height(LargeTileHeight), true, stringResource(R.string.home_action_today_count, banglaDigit(todayBillCount)))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tileGap)) {
            Row(horizontalArrangement = Arrangement.spacedBy(tileGap)) {
                ActionTile(stringResource(R.string.home_action_income), Icons.Filled.TrendingUp, { onNavigate("expense") }, Modifier.weight(1f).height(SmallTileHeight))
                ActionTile(stringResource(R.string.home_action_expense), Icons.Filled.TrendingDown, { onNavigate("expense") }, Modifier.weight(1f).height(SmallTileHeight))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(tileGap)) {
                ActionTile(stringResource(R.string.home_action_collection), Icons.Filled.Book, { onNavigate("khata") }, Modifier.weight(1f).height(SmallTileHeight), badge = if (pendingKhataCount > 0) banglaDigit(pendingKhataCount) else null)
                ActionTile(stringResource(R.string.home_action_stock_in), Icons.Filled.Inventory, { onNavigate("catalog") }, Modifier.weight(1f).height(SmallTileHeight))
            }
        }
    }
}

@Composable
private fun ActionTile(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, isPrimary: Boolean = false, badge: String? = null) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = if (isPrimary) ColorBrandMaroon else MaterialTheme.colorScheme.surfaceVariant, onClick = onClick) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.align(Alignment.Center).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(icon, contentDescription = null, tint = if (isPrimary) ColorAccentGold else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(if (isPrimary) 28.dp else 22.dp))
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (isPrimary) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (badge != null) {
                Surface(modifier = Modifier.align(Alignment.TopEnd), shape = RoundedCornerShape(topEnd = 12.dp, bottomStart = 8.dp), color = if (isPrimary) ColorAccentGold else MaterialTheme.colorScheme.primary) {
                    Text(badge, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isPrimary) ColorBrandMaroon else MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun AlertsSection(data: HomeData, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val hasLowStock = data.lowStockAlerts.isNotEmpty()
    val hasDue = data.dueCustomerCount > 0
    if (!hasLowStock && !hasDue) return
    val cardCount = data.lowStockAlerts.size + if (hasDue) 1 else 0
    val alertCount = banglaDigit(cardCount)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenPadding, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_alerts_header), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.home_alerts_count, alertCount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.home_alerts_all), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = ColorBrandMaroon, modifier = Modifier.clickable { onNavigate("alerts") })
        }
        LazyRow(contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(data.lowStockAlerts) { _, alert ->
                LowStockAlertCard(alert, { onNavigate("catalog/order/${alert.bookId}") }, { }, Modifier.width(260.dp))
            }
            if (hasDue) {
                item { DueCollectionAlertCard(data.dueCustomerCount, data.totalDue, { onNavigate("khata") }, Modifier.width(260.dp)) }
            }
        }
        if (cardCount > 1) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                repeat(cardCount) { i ->
                    Box(modifier = Modifier.padding(horizontal = 3.dp).size(if (i == 0) 6.dp else 4.dp).clip(CircleShape).background(if (i == 0) ColorBrandMaroon else MaterialTheme.colorScheme.outlineVariant))
                }
            }
        }
    }
}

@Composable
private fun LowStockAlertCard(alert: LowStockBookSummary, onOrder: () -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.home_alert_low_stock_title), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(stringResource(R.string.home_alert_urgent_badge), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("${alert.bookTitleBn} — ${alert.classLevel}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.home_alert_low_stock_copy, banglaDigit(alert.currentStock)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOrder, colors = ButtonDefaults.buttonColors(containerColor = ColorBrandMaroon), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), modifier = Modifier.height(32.dp)) { Text(stringResource(R.string.home_alert_low_stock_order), style = MaterialTheme.typography.labelSmall, color = Color.White) }
                OutlinedButton(onClick = onLater, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), modifier = Modifier.height(32.dp)) { Text(stringResource(R.string.home_alert_low_stock_later), style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@Composable
private fun DueCollectionAlertCard(dueCustomerCount: Int, totalDue: Double, onCollect: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = ColorBrandMaroon, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.home_alert_due_title), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.home_alert_due_customers, banglaDigit(dueCustomerCount)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text("${stringResource(R.string.home_alert_due_total)} ${formatBengaliTaka(totalDue)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = ColorBrandMaroon)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onCollect, colors = ButtonDefaults.buttonColors(containerColor = ColorBrandMaroon), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), modifier = Modifier.height(32.dp)) { Text(stringResource(R.string.home_alert_due_action), style = MaterialTheme.typography.labelSmall, color = Color.White) }
        }
    }
}

@Composable
private fun AnalyticsSheet(monthNetProfit: Double, points: List<HomeAnalyticsPoint>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(42.dp).height(4.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.outlineVariant))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_analytics_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.home_analytics_month_total, formatBengaliTaka(monthNetProfit)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = stringResource(if (expanded) R.string.home_analytics_collapse else R.string.home_analytics_expand), tint = ColorBrandMaroon)
            }
            if (expanded) {
                Spacer(Modifier.height(14.dp))
                MiniBarSparkline(points = points)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.home_analytics_sparkline_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MiniBarSparkline(points: List<HomeAnalyticsPoint>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) {
        Text(stringResource(R.string.home_analytics_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val maxAbs = max(1.0, points.maxOf { abs(it.netProfit) })
    Row(
        modifier = modifier.fillMaxWidth().height(72.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        points.forEach { point ->
            val barHeight = ((abs(point.netProfit) / maxAbs) * 56).roundToInt().coerceAtLeast(4)
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(barHeight.dp).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(if (point.netProfit >= 0.0) ColorSemanticPositive else ColorBrandMaroon),
                )
            }
        }
    }
}

private fun banglaDigit(n: Int): String {
    val map = "০১২৩৪৫৬৭৮৯"
    return n.toString().map { c -> if (c.isDigit()) map[c - '0'] else c }.joinToString("")
}

private fun formatBengaliTaka(taka: Double): String {
    val rounded = taka.toLong()
    val formatted = String.format("%,d", rounded)
    val map = "০১২৩৪৫৬৭৮৯"
    val bangla = formatted.map { c -> if (c.isDigit()) map[c - '0'] else c }.joinToString("")
    return "৳ $bangla"
}
