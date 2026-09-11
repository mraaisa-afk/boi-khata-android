package com.boikhata.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.boikhata.core.designsystem.format.DigitStyle
import com.boikhata.core.designsystem.format.NumberFormatter
import com.boikhata.core.domain.model.HomeData
import com.boikhata.core.domain.model.KhataCustomerDue

// D71 §1 semantic colors — only these roles; do NOT add more without a D-entry.
private val ColorSemanticPositive = Color(0xFF1B6E3F)  // credit amounts, positive balances
private val ColorSemanticCaution  = Color(0xFF9E5C00)  // overdue indicators, debt warnings
private val ColorPrimary          = Color(0xFF800000)  // brand/identity — key actions + highest urgency

/**
 * D67 §2 + D75: Trident dashboard.
 * Three Trident cards (নগদ / গ্রাহক বাকি / সাপ্লায়ার পাওনা) + আজকের বিক্রি + শীর্ষ বাকিদার list.
 *
 * D71 §6: No charts. D2: Trident numbers only — no extra metric, no percentage.
 * Blueprint §2: খাতা-প্রথম হোম = দেনা-তালিকা + Trident metrics.
 *
 * D76: HomeScreen reloads on every ON_RESUME lifecycle event so that
 * entries added in other tabs (Khata, Sale) are immediately visible
 * when the user switches back to Home — without killing the app.
 */
@Composable
fun HomeScreen(
    tenantId: String,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    // D76: Reload on every resume so cross-tab entries show immediately.
    // Repository methods are one-shot suspend funs (not Flow), so we must
    // trigger a fresh load whenever this screen becomes active again.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(tenantId, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadHome(tenantId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is HomeUiState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = ColorPrimary)
            }
        }
        is HomeUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = state.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is HomeUiState.Success -> {
            HomeContent(state.data)
        }
    }
}

@Composable
private fun HomeContent(data: HomeData) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        // ── Trident 1: নগদ ব্যালেন্স ────────────────────────────────────────────
        item {
            TridentCard(
                title = stringResource(R.string.cash_balance),
                value = NumberFormatter.formatMoney(data.cashBalance, DigitStyle.BANGLA),
                subtitle = stringResource(R.string.cash_account),
                amountColor = ColorSemanticPositive,
            )
        }
        // ── Trident 2: গ্রাহক বাকি ───────────────────────────────────────────
        item {
            TridentCard(
                title = stringResource(R.string.customer_dues),
                value = NumberFormatter.formatMoney(data.totalDue, DigitStyle.BANGLA),
                subtitle = stringResource(R.string.due_customers, banglaDigit(data.dueCustomerCount.toLong())),
                amountColor = ColorSemanticCaution,
            )
        }
        // ── Trident 3: সাপ্লায়ার পাওনা ──────────────────────────────────────────
        item {
            TridentCard(
                title = stringResource(R.string.supplier_dues),
                value = NumberFormatter.formatMoney(data.supplierDuesTotal, DigitStyle.BANGLA),
                subtitle = stringResource(R.string.supplier_count, banglaDigit(data.supplierCount.toLong())),
                amountColor = ColorSemanticCaution,
            )
        }
        // ── আজকের বিক্রি ─────────────────────────────────────────────────────
        item {
            TodaySalesCard(
                todaySalesTotal = data.todaySalesTotal,
                todayBillCount = data.todayBillCount,
            )
        }
        // ── Section header: শীর্ষ বাকিদার ───────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.top_due),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
        }
        if (data.topDueCustomers.isEmpty()) {
            item { EmptyDueState() }
        } else {
            items(data.topDueCustomers) { due ->
                DueCustomerCard(due)
            }
        }
    }
}

/**
 * আজকের বিক্রি summary — today's sales total + bill count.
 * Blueprint §2: দেনা-তালিকা + today's number on Home.
 * D71 §3: corner radius 16dp; elevation 2dp.
 */
@Composable
private fun TodaySalesCard(
    todaySalesTotal: Double,
    todayBillCount: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.today_sales),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = NumberFormatter.formatMoney(todaySalesTotal, DigitStyle.BANGLA),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ColorSemanticPositive,
            )
            Text(
                text = stringResource(R.string.today_bills, banglaDigit(todayBillCount.toLong())),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Actionable empty state when no customers have dues.
 * Design KB: "Bengali, actionable, with a next step — never a one-line English sentence."
 */
@Composable
private fun EmptyDueState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.no_due),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.no_due_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One arm of the Trident — a single key metric with a colored amount.
 * D71 §3: corner radius 16dp; elevation 2dp; no border combined with elevation.
 */
@Composable
private fun TridentCard(
    title: String,
    value: String,
    subtitle: String,
    amountColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A single customer-due row. Aging-bucket color uses D71 §1 semantic palette only.
 * D71 §3: corner radius 16dp; elevation 1dp.
 * age rendered via banglaDigit() — no Latin digits in Bengali UI (Design KB digit law).
 */
@Composable
private fun DueCustomerCard(due: KhataCustomerDue) {
    // D71 §1: ONLY the four declared semantic colors; no new colors without a D-entry.
    val amountColor = when (due.agingBucket) {
        "GREEN"  -> ColorSemanticPositive  // recent — within normal range
        "YELLOW" -> ColorSemanticCaution   // moderately overdue
        "RED"    -> ColorPrimary           // severely overdue — maroon for highest urgency
        else     -> MaterialTheme.colorScheme.onSurface
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = due.customer.nameBn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.age_days, banglaDigit(due.ageDays)),
                    style = MaterialTheme.typography.bodySmall,
                    color = amountColor,
                )
            }
            Text(
                text = NumberFormatter.formatMoney(due.dueAmount, DigitStyle.BANGLA),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )
        }
    }
}

/**
 * Converts a non-negative integer to Bengali digit string.
 * Example: 15L → "১৫". Used for counts/ages where formatMoney is inappropriate.
 * Digit law: no Latin digits in Bengali UI paths (Design KB).
 */
private fun banglaDigit(n: Long): String {
    val b = "০১২৩৪৫৬৭৮৯"
    return n.toString().map { c -> if (c.isDigit()) b[c - '0'] else c }.joinToString("")
}
