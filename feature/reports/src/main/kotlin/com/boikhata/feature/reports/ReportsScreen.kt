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
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Month selector
        MonthSelector(viewModel = viewModel)
        // Top tab row
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
}

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
