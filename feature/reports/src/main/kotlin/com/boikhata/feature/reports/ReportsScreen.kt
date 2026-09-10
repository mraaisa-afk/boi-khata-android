package com.boikhata.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.core.designsystem.format.DigitStyle
import com.boikhata.core.designsystem.format.NumberFormatter
import com.boikhata.core.domain.accounting.BudgetAlertCalculator
import com.boikhata.core.domain.model.BalanceSheetLite
import com.boikhata.core.domain.model.PnLReport
import com.boikhata.core.domain.accounting.ReportDepthCalculator
import com.boikhata.shared.receipt.ReportShareBuilder

/**
 * D37: ReportsScreen — the P3b accounting engine made visible.
 * P&L (dual-calendar month selector) + balance-sheet + period-lock + budget alerts.
 * P6: extended with Trends, Top-10 Rankings, and side-by-side month Comparison.
 */
@Composable
fun ReportsScreen(
    tenantId: String,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    LaunchedEffect(tenantId) {
        viewModel.loadReports(tenantId)
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        R.string.tab_pnl,
        R.string.tab_balance_sheet,
        R.string.tab_period_lock,
        R.string.tab_budget,
        R.string.tab_trends,
        R.string.tab_top_ten,
        R.string.tab_comparison,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        MonthSelector(viewModel = viewModel)
        ScrollableTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, labelRes ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(stringResource(labelRes)) },
                )
            }
        }
        when (selectedTab) {
            0 -> PnLSection(viewModel = viewModel)
            1 -> BalanceSheetSection(viewModel = viewModel)
            2 -> PeriodLockSection(viewModel = viewModel)
            3 -> BudgetSection(viewModel = viewModel)
            4 -> TrendSection(viewModel = viewModel)
            5 -> RankingSection(viewModel = viewModel)
            6 -> ComparisonSection(viewModel = viewModel)
        }
    }
}

@Composable
private fun MonthSelector(viewModel: ReportsViewModel) {
    val monthLabel = viewModel.monthLabel()
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.select_month),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// ── P&L ───────────────────────────────────────────────────────────────────────

@Composable
private fun PnLSection(viewModel: ReportsViewModel) {
    val state by viewModel.pnlState.collectAsState()
    when (val s = state) {
        is PnLState.Loading -> CenterLoading()
        is PnLState.Error -> CenterError(s.message)
        is PnLState.Success -> PnLContent(s.pnl)
    }
}

@Composable
private fun PnLContent(pnl: PnLReport) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(pnl.toLines()) { line ->
            PnLLineRow(line.labelBn, line.amount, line.labelEn == "Net Profit" || line.labelEn == "Gross Profit")
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PnLLineRow(label: String, amount: Double, isBold: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            text = NumberFormatter.formatMoney(amount, DigitStyle.BANGLA),
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// ── Balance Sheet ─────────────────────────────────────────────────────────────

@Composable
private fun BalanceSheetSection(viewModel: ReportsViewModel) {
    val state by viewModel.balanceSheetState.collectAsState()
    when (val s = state) {
        is BalanceSheetState.Loading -> CenterLoading()
        is BalanceSheetState.Error -> CenterError(s.message)
        is BalanceSheetState.Success -> BalanceSheetContent(s.balanceSheet)
    }
}

@Composable
private fun BalanceSheetContent(bs: BalanceSheetLite) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(stringResource(R.string.assets), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(bs.assetLines()) { line ->
            BalanceRow(line.labelBn, line.amount)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.liabilities), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(bs.liabilityLines()) { line ->
            BalanceRow(line.labelBn, line.amount)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.equity), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(bs.equityLines()) { line ->
            BalanceRow(line.labelBn, line.amount)
        }
    }
}

@Composable
private fun BalanceRow(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = NumberFormatter.formatMoney(amount, DigitStyle.BANGLA),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

// ── Period Lock ───────────────────────────────────────────────────────────────

@Composable
private fun PeriodLockSection(viewModel: ReportsViewModel) {
    val state by viewModel.periodLockState.collectAsState()
    when (val s = state) {
        is PeriodLockState.Loading -> CenterLoading()
        is PeriodLockState.Error -> CenterError(s.message)
        is PeriodLockState.Success -> PeriodLockContent(s, viewModel)
    }
}

@Composable
private fun PeriodLockContent(state: PeriodLockState.Success, viewModel: ReportsViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (state.isCurrentLocked)
                    MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = viewModel.monthLabel(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (state.isCurrentLocked) stringResource(R.string.locked)
                           else stringResource(R.string.not_locked),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (!state.isCurrentLocked) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.lockCurrentPeriod() }) {
                        Text(stringResource(R.string.lock_button))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.locked_periods),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (state.locks.isEmpty()) {
            Text(stringResource(R.string.no_locked_periods), style = MaterialTheme.typography.bodyMedium)
        } else {
            state.locks.forEach { lock ->
                Text(
                    text = "${lock.periodYear}-${lock.periodMonth}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}

// ── Budget ────────────────────────────────────────────────────────────────────

@Composable
private fun BudgetSection(viewModel: ReportsViewModel) {
    val state by viewModel.budgetAlertState.collectAsState()
    when (val s = state) {
        is BudgetAlertState.Loading -> CenterLoading()
        is BudgetAlertState.Error -> CenterError(s.message)
        is BudgetAlertState.Success -> BudgetContent(s.alerts)
    }
}

@Composable
private fun BudgetContent(alerts: List<BudgetAlertCalculator.BudgetAlert>) {
    if (alerts.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.no_budget_alerts), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(alerts) { alert ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (alert.severity == BudgetAlertCalculator.Severity.OVER)
                        MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.tertiaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = alert.categoryNameBn,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${NumberFormatter.formatMoney(alert.budget, DigitStyle.BANGLA)} → ${NumberFormatter.formatMoney(alert.actual, DigitStyle.BANGLA)} (${String.format("%.0f", alert.percentage)}%)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = if (alert.severity == BudgetAlertCalculator.Severity.OVER)
                            stringResource(R.string.over)
                        else stringResource(R.string.warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (alert.severity == BudgetAlertCalculator.Severity.OVER)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

// ── 12-Month Trend ────────────────────────────────────────────────────────────

@Composable
private fun TrendSection(viewModel: ReportsViewModel) {
    val state by viewModel.trendState.collectAsState()
    when (val value = state) {
        TrendState.Loading -> CenterLoading()
        is TrendState.Error -> CenterError(value.message)
        is TrendState.Success -> {
            val context = LocalContext.current
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    TextButton(onClick = {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, ReportShareBuilder.buildTrend(value.points))
                        }, null))
                    }) { Text(stringResource(R.string.share_report)) }
                }
                items(value.points) { point ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(point.labelBn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.trend_sales) + ": " + NumberFormatter.formatMoney(point.sales, DigitStyle.BANGLA))
                            Text(stringResource(R.string.trend_profit) + ": " + NumberFormatter.formatMoney(point.profit, DigitStyle.BANGLA))
                            Text(stringResource(R.string.trend_expenses) + ": " + NumberFormatter.formatMoney(point.expenses, DigitStyle.BANGLA))
                            if (point.year != value.points.first().year || point.month != value.points.first().month) {
                                Text(stringResource(R.string.mom_change, point.salesChangePercent))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Top-10 Rankings ───────────────────────────────────────────────────────────

@Composable
private fun RankingSection(viewModel: ReportsViewModel) {
    val state by viewModel.rankingState.collectAsState()
    when (val value = state) {
        RankingState.Loading -> CenterLoading()
        is RankingState.Error -> CenterError(value.message)
        is RankingState.Success -> LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item { RankingGroup(stringResource(R.string.top_books), value.books) }
            item { RankingGroup(stringResource(R.string.top_customers), value.customers) }
            item { RankingGroup(stringResource(R.string.top_expenses), value.expenses) }
        }
    }
}

@Composable
private fun RankingGroup(title: String, rows: List<ReportDepthCalculator.RankedItem>) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
    rows.forEachIndexed { index, row ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${index + 1}. ${row.label}")
            Text("${row.quantity} · ${NumberFormatter.formatMoney(row.amount, DigitStyle.BANGLA)}")
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

// ── P6: Side-by-side Month Comparison ────────────────────────────────────────

@Composable
private fun ComparisonSection(viewModel: ReportsViewModel) {
    val state by viewModel.comparisonState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Month A / Month B selector buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.comparison_month_a), style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = {
                    // Step back one month from A
                    val (y, m) = prevMonth(viewModel.compareYearA, viewModel.compareMonthA)
                    viewModel.selectCompareMonthA(y, m)
                }) { Text("◄ ${stringResource(R.string.comparison_prev_month)}") }
                Text(
                    text = monthLabel(viewModel.compareYearA, viewModel.compareMonthA),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = {
                    val (y, m) = nextMonth(viewModel.compareYearA, viewModel.compareMonthA)
                    viewModel.selectCompareMonthA(y, m)
                }) { Text("${stringResource(R.string.comparison_next_month)} ►") }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.comparison_month_b), style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = {
                    val (y, m) = prevMonth(viewModel.compareYearB, viewModel.compareMonthB)
                    viewModel.selectCompareMonthB(y, m)
                }) { Text("◄ ${stringResource(R.string.comparison_prev_month)}") }
                Text(
                    text = monthLabel(viewModel.compareYearB, viewModel.compareMonthB),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = {
                    val (y, m) = nextMonth(viewModel.compareYearB, viewModel.compareMonthB)
                    viewModel.selectCompareMonthB(y, m)
                }) { Text("${stringResource(R.string.comparison_next_month)} ►") }
            }
        }

        when (val s = state) {
            ComparisonState.Idle, ComparisonState.Loading -> CenterLoading()
            is ComparisonState.Error -> CenterError(s.message)
            is ComparisonState.Success -> {
                val cmp = s.comparison
                // Share button
                TextButton(
                    onClick = {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, ReportShareBuilder.buildComparison(cmp))
                        }, null))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) { Text(stringResource(R.string.share_comparison)) }

                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.comparison_metric), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.4f))
                    Text(cmp.labelA, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(cmp.labelB, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(stringResource(R.string.comparison_change), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.8f))
                }

                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(cmp.rows) { row ->
                        ComparisonRow(row)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonRow(row: ReportDepthCalculator.ComparisonRow) {
    val deltaColor = when {
        row.deltaPercent > 0 -> MaterialTheme.colorScheme.tertiary
        row.deltaPercent < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val deltaSign = when {
        row.deltaPercent > 0 -> "+"
        row.deltaPercent < 0 -> ""
        else -> ""
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(row.labelBn, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.4f))
        Text(NumberFormatter.formatMoney(row.valueA, DigitStyle.BANGLA), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(NumberFormatter.formatMoney(row.valueB, DigitStyle.BANGLA), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(
            text = if (row.deltaPercent == 0.0) "=" else "$deltaSign${"%.1f".format(row.deltaPercent)}%",
            style = MaterialTheme.typography.bodySmall,
            color = deltaColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.8f),
        )
    }
}

private fun prevMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 1) Pair(year - 1, 12) else Pair(year, month - 1)

private fun nextMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 12) Pair(year + 1, 1) else Pair(year, month + 1)

private fun monthLabel(year: Int, month: Int): String {
    val names = listOf("জান", "ফেব", "মার", "এপ্র", "মে", "জুন", "জুল", "আগ", "সেপ", "অক্ট", "নভ", "ডিস")
    return "${names.getOrElse(month - 1) { month.toString() }} $year"
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun CenterLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenterError(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
    }
}
