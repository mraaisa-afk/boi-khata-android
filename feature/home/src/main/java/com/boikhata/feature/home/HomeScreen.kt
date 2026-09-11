package com.boikhata.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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

// D79 §4.5 — Hero card colors
private val ColorHeroBg     = ColorSemanticPositive  // #1B6E3F
private val ColorHeroAmount = ColorAccentGold         // #C9A227 — contrast 2.59:1 vs bg
// ⚠️ WCAG AA FAILURE: contrast ratio = 2.59:1 (required ≥4.5:1 normal, ≥3.0:1 large)
// Owner ruling required before GA (spec §4.5: "WCAG AA পাস করতে হবে — PR B-তে যাচাই করে রিপোর্ট দিতে হবে")

/**
 * HomeScreen v2 — D79 Locked Design Spec implementation.
 * PR B scope: AppBar + HeroCard + QuickActionGrid.
 * Data source: existing HomeViewModel fields (PR C will wire net-profit calculation).
 *
 * @param tenantId   Active tenant identifier (Room isolation).
 * @param shopName   Shop name displayed in AppBar row 2.
 * @param isLicensed §5.1 OPEN ITEM: premium badge state pending owner ruling.
 *                   For PR B: badge shown only when true; hidden otherwise.
 * @param onNavigate Navigation callback for quick-action tiles.
 */
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
            data           = s.data,
            shopName       = shopName,
            isLicensed     = isLicensed,
            amountVisible  = amountVisible,
            onAmountToggle = { amountVisible = !amountVisible },
            onNavigate     = onNavigate,
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────────
private val ScreenPadding   = 16.dp
private val SmallTileHeight = 56.dp  // minTouchTarget token
private val LargeTileHeight = SmallTileHeight * 2 + 8.dp  // = 120dp

@Composable
private fun HomeContent(
    data:           HomeData,
    shopName:       String,
    isLicensed:     Boolean,
    amountVisible:  Boolean,
    onAmountToggle: () -> Unit,
    onNavigate:     (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorSurfaceIvory),
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        // §2.1 App bar (embedded; replaces temporary Scaffold topBar from PR A)
        item(key = "app_bar") {
            HomeAppBar(shopName = shopName, isLicensed = isLicensed)
        }
        // §2.2 Hero card — আজকের নিট লাভ
        item(key = "hero_card") {
            HeroCard(
                data           = data,
                amountVisible  = amountVisible,
                onAmountToggle = onAmountToggle,
                modifier       = Modifier.padding(horizontal = ScreenPadding, vertical = 12.dp),
            )
        }
        // §2.3 Quick actions (5 tiles)
        item(key = "quick_actions") {
            QuickActionGrid(
                todayBillCount    = data.todayBillCount,
                pendingKhataCount = data.dueCustomerCount,
                onNavigate        = onNavigate,
                modifier          = Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp),
            )
        }
        // §2.4 আজকের করণীয় (alert cards) — PR D scope
        // §2.5 বিশ্লেষণ (collapsible sheet) — PR E scope
    }
}

// ─── §2.1 App bar ────────────────────────────────────────────────────────────────────

@Composable
private fun HomeAppBar(
    shopName:   String,
    isLicensed: Boolean,
) {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        color          = ColorBrandMaroon,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            // Row 1: Logo | wordmark | premium badge ┃ sync | notifications | avatar
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // বৃত্তাকার logo circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Book,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Wordmark
                Text(
                    text       = stringResource(R.string.app_name),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
                // §5.1 OPEN ITEM: badge behavior for FREE/GRACE/SOFT_LOCKED pending owner ruling
                if (isLicensed) {
                    Spacer(Modifier.width(6.dp))
                    PremiumBadge()
                }
                Spacer(Modifier.weight(1f))
                // Sync chip (offline-first — Room-backed, always synced)
                SyncStatusChip()
                Spacer(Modifier.width(4.dp))
                // Notification bell
                IconButton(
                    onClick  = { /* TODO: notification screen — future PR */ },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Notifications,
                        contentDescription = stringResource(R.string.home_notification_cd),
                        tint               = Color.White,
                        modifier           = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(2.dp))
                // User avatar — first letter of shop name
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .semantics { contentDescription = shopName },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = shopName.firstOrNull()?.toString()
                                     ?: stringResource(R.string.home_avatar_fallback),
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            // Row 2: shop name + ▾ switcher
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.clickable(
                    onClick = { /* TODO: shop switcher — multi-shop, future PR */ },
                ),
            ) {
                Text(
                    text     = shopName,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = Color.White.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    imageVector        = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint               = Color.White.copy(alpha = 0.6f),
                    modifier           = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun PremiumBadge() {
    // §5.1 OPEN ITEM: exact text/color per license state pending owner ruling.
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = ColorAccentGold,
    ) {
        Text(
            text       = stringResource(R.string.home_premium_badge),
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color      = ColorBrandMaroon,
            modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SyncStatusChip() {
    // Offline-first: Room-backed, never network-dependent — always "সিংক্‌ড".
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.15f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(ColorSemanticPositive),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text  = stringResource(R.string.home_sync_label),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

// ─── §2.2 Hero card ────────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(
    data:           HomeData,
    amountVisible:  Boolean,
    onAmountToggle: () -> Unit,
    modifier:       Modifier = Modifier,
) {
    // PR B proxy: todaySalesTotal used as নিট লাভ.
    // PR C will wire: netProfit = todaySalesTotal - todayExpenseTotal per spec §5.2 ruling.
    val heroAmount = data.todaySalesTotal
    val heroText   = if (amountVisible) formatBengaliAmount(heroAmount)
                     else stringResource(R.string.home_hero_amount_hidden)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = ColorHeroBg,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Title row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text     = stringResource(R.string.home_hero_title),
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = Color.White.copy(alpha = 0.80f),
                    modifier = Modifier.weight(1f),
                )
                // Period chip: "আজ ▾" (PR C will wire period selector)
                Surface(
                    shape    = RoundedCornerShape(8.dp),
                    color    = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.clickable { /* TODO PR C: period selector */ },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text  = stringResource(R.string.home_hero_period),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                        )
                        Icon(
                            imageVector        = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                // Eye icon: amount hide / show
                IconButton(
                    onClick  = onAmountToggle,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (amountVisible) Icons.Filled.Visibility
                                      else Icons.Filled.VisibilityOff,
                        contentDescription = stringResource(
                            if (amountVisible) R.string.home_hero_hide_amount
                            else R.string.home_hero_show_amount,
                        ),
                        tint     = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            // Hero amount
            AnimatedContent(targetState = heroText, label = "hero_amount") { text ->
                Text(
                    text       = text,
                    style      = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color      = ColorHeroAmount,
                )
            }
            // Delta pill: ▲/▼ — no yesterday data in PR B; PR C will add
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.20f), thickness = 0.5.dp)
            Spacer(Modifier.height(14.dp))
            // আয় | ব্যয় two-column breakdown
            Row(modifier = Modifier.fillMaxWidth()) {
                HeroSubAmount(
                    label         = stringResource(R.string.home_hero_income_label),
                    directionIcon = "↑",
                    amount        = data.todaySalesTotal,  // proxy until PR C
                    amountVisible = amountVisible,
                    modifier      = Modifier.weight(1f),
                )
                HeroSubAmount(
                    label         = stringResource(R.string.home_hero_expense_label),
                    directionIcon = "↓",
                    amount        = 0L,  // PR C: wire todayExpenseTotal
                    amountVisible = amountVisible,
                    modifier      = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            // Footer: "৫টি বিক্রি · ৩ কাস্টমার"
            Text(
                text  = stringResource(
                    R.string.home_hero_footer,
                    banglaDigit(data.todayBillCount),
                    banglaDigit(data.dueCustomerCount),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.60f),
            )
        }
    }
}

@Composable
private fun HeroSubAmount(
    label:         String,
    directionIcon: String,
    amount:        Long,
    amountVisible: Boolean,
    modifier:      Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text  = "$directionIcon $label",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.65f),
        )
        Text(
            text       = if (amountVisible) formatBengaliAmount(amount)
                         else stringResource(R.string.home_hero_amount_hidden),
            style      = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color      = Color.White,
        )
    }
}

// ─── §2.3 Quick action grid ──────────────────────────────────────────────────────────

@Composable
private fun QuickActionGrid(
    todayBillCount:    Int,
    pendingKhataCount: Int,
    onNavigate:        (String) -> Unit,
    modifier:          Modifier = Modifier,
) {
    val tileGap = 8.dp
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tileGap),
    ) {
        // বড় primary tile: নতুন বিক্রি / POS
        ActionTile(
            label     = stringResource(R.string.home_action_new_sale),
            icon      = Icons.Filled.ShoppingCart,
            isPrimary = true,
            badge     = stringResource(R.string.home_action_today_count, banglaDigit(todayBillCount)),
            onClick   = { onNavigate("sale") },
            modifier  = Modifier
                .weight(1f)
                .height(LargeTileHeight),
        )
        // ছোট ৪ tiles: 2×2 grid
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(tileGap),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(tileGap)) {
                ActionTile(
                    label    = stringResource(R.string.home_action_income),
                    icon     = Icons.Filled.TrendingUp,
                    onClick  = { onNavigate("expense") },
                    modifier = Modifier.weight(1f).height(SmallTileHeight),
                )
                ActionTile(
                    label    = stringResource(R.string.home_action_expense),
                    icon     = Icons.Filled.TrendingDown,
                    onClick  = { onNavigate("expense") },
                    modifier = Modifier.weight(1f).height(SmallTileHeight),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(tileGap)) {
                ActionTile(
                    label    = stringResource(R.string.home_action_collection),
                    icon     = Icons.Filled.Book,
                    badge    = if (pendingKhataCount > 0) banglaDigit(pendingKhataCount) else null,
                    onClick  = { onNavigate("khata") },
                    modifier = Modifier.weight(1f).height(SmallTileHeight),
                )
                ActionTile(
                    label    = stringResource(R.string.home_action_stock_in),
                    icon     = Icons.Filled.Inventory,
                    onClick  = { onNavigate("catalog") },
                    modifier = Modifier.weight(1f).height(SmallTileHeight),
                )
            }
        }
    }
}

@Composable
private fun ActionTile(
    label:     String,
    icon:      ImageVector,
    onClick:   () -> Unit,
    modifier:  Modifier = Modifier,
    isPrimary: Boolean  = false,
    badge:     String?  = null,
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        color    = if (isPrimary) ColorBrandMaroon else MaterialTheme.colorScheme.surfaceVariant,
        onClick  = onClick,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier            = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = if (isPrimary) ColorAccentGold
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(if (isPrimary) 28.dp else 22.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text      = label,
                    style     = MaterialTheme.typography.labelSmall,
                    color     = if (isPrimary) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                )
            }
            // Badge — top-right corner (spec §2.3)
            if (badge != null) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape    = RoundedCornerShape(topEnd = 12.dp, bottomStart = 8.dp),
                    color    = if (isPrimary) ColorAccentGold
                               else MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text       = badge,
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = if (isPrimary) ColorBrandMaroon
                                     else MaterialTheme.colorScheme.onPrimary,
                        modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────────────────

private fun banglaDigit(n: Int): String {
    val map = "০১২৩৪৫৬৭৮৯"
    return n.toString().map { c -> if (c.isDigit()) map[c - '0'] else c }.joinToString("")
}

private fun formatBengaliAmount(paise: Long): String {
    val taka = paise / 100
    val formatted = String.format("%,d", taka)
    val bangla = formatted.map { c ->
        if (c.isDigit()) "০১২৩৪৫৬৭৮৯"[c - '0'] else c
    }.joinToString("")
    return "\u09f3\u00a0$bangla"
}
