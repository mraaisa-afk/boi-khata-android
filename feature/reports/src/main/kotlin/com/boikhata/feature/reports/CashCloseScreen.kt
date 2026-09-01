package com.boikhata.feature.reports

import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.core.designsystem.format.DigitStyle
import com.boikhata.core.designsystem.format.NumberFormatter
import com.boikhata.core.domain.model.CashCloseReport
import com.boikhata.shared.receipt.CashCloseReportBuilder

/**
 * D36: CashCloseScreen — the daily "আজকের হিসাব".
 * Sales by method + expenses by category + MFS-fee (owner-overridable) + cash variance.
 */
@Composable
fun CashCloseScreen(
    tenantId: String,
    viewModel: CashCloseViewModel = hiltViewModel(),
) {
    LaunchedEffect(tenantId) {
        viewModel.loadClose(tenantId)
    }

    val state by viewModel.closeState.collectAsState()
    val mfsRate by viewModel.mfsFeeRate.collectAsState()
    val countedCash by viewModel.countedCash.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.tab_cash_close),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )
        // Owner-overridable inputs
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = if (mfsRate > 0) mfsRate.toString() else "",
                    onValueChange = { v ->
                        val rate = v.toDoubleOrNull() ?: 0.0
                        viewModel.setMfsFeeRate(rate)
                    },
                    label = { Text(stringResource(R.string.mfs_fee_rate)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = if (countedCash > 0) countedCash.toString() else "",
                    onValueChange = { v ->
                        val cash = v.toDoubleOrNull() ?: 0.0
                        viewModel.setCountedCash(cash)
                    },
                    label = { Text(stringResource(R.string.counted_cash)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        when (val s = state) {
            is CashCloseState.Loading -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }
            is CashCloseState.Error -> Text(
                s.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
            is CashCloseState.Success -> {
                CloseContent(
                    report = s.report,
                    onShare = {
                        val text = CashCloseReportBuilder.buildCloseText(s.report) { amount ->
                            NumberFormatter.formatMoney(amount, DigitStyle.BANGLA)
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "শেয়ার"))
                    },
                )
            }
        }
    }
}

@Composable
private fun CloseContent(report: CashCloseReport, onShare: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Sales by method
        item {
            SectionHeader(stringResource(R.string.sales_by_method))
        }
        item {
            CloseRow(stringResource(R.string.cash_sales), report.salesByMethod.cash)
            CloseRow(stringResource(R.string.bkash_sales), report.salesByMethod.bkash)
            CloseRow(stringResource(R.string.nagad_sales), report.salesByMethod.nagad)
            CloseRow(stringResource(R.string.credit_sales), report.salesByMethod.credit)
            CloseRow(stringResource(R.string.total_sales), report.salesByMethod.total, true)
        }
        // Expenses by category
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(stringResource(R.string.expenses_by_category))
        }
        if (report.expensesByCategory.isEmpty()) {
            item { Text(stringResource(R.string.no_expenses_today), style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(report.expensesByCategory) { cat ->
                CloseRow(cat.categoryNameBn, cat.total)
            }
        }
        item {
            CloseRow(stringResource(R.string.total_expenses), report.totalExpenses, true)
        }
        // MFS fee
        if (report.mfsFeeEstimated > 0.01) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(stringResource(R.string.mfs_fee))
                CloseRow("ফি (${report.mfsFeeRate}%)", report.mfsFeeEstimated)
            }
        }
        // Cash reconciliation
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(stringResource(R.string.cash_reconciliation))
            CloseRow(stringResource(R.string.system_cash), report.systemCashInHand)
            CloseRow(stringResource(R.string.counted_cash), report.countedCash)
            CloseRow("${stringResource(R.string.variance)} (${report.varianceLabelBn})", report.variance, true)
        }
        // Share button
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.share_close))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun CloseRow(label: String, amount: Double, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            text = NumberFormatter.formatMoney(amount, DigitStyle.BANGLA),
            style = if (isBold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
