package com.boikhata.feature.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.core.designsystem.format.DigitStyle
import com.boikhata.core.designsystem.format.NumberFormatter
import com.boikhata.core.domain.model.BillSummary
import com.boikhata.feature.sale.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * P2b: Bill history — the shopkeeper's record of what was sold.
 */
@Composable
fun BillHistoryScreen(
    tenantId: String,
    onBillClick: (String) -> Unit,
    viewModel: SaleViewModel = hiltViewModel(),
) {
    LaunchedEffect(tenantId) {
        viewModel.loadHistory(tenantId)
    }

    val uiState by viewModel.historyState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    when (val state = uiState) {
        is HistoryUiState.Loading -> {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) { CircularProgressIndicator() }
        }
        is HistoryUiState.Error -> {
            Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp),
            )
        }
        is HistoryUiState.Success -> {
            if (state.bills.isEmpty()) {
                Text(
                    stringResource(R.string.no_bills),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.bills) { bill ->
                        BillCard(bill, dateFormat) { onBillClick(bill.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BillCard(bill: BillSummary, dateFormat: SimpleDateFormat, onClick: () -> Unit) {
    val statusColor = if (bill.status == "PARTIAL") Color(0xFFC62828) else Color(0xFF2E7D32)
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(bill.billNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(bill.customerNameBn, style = MaterialTheme.typography.bodySmall)
                Text(dateFormat.format(Date(bill.billDate)), style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    NumberFormatter.formatMoney(bill.totalAmount, DigitStyle.BANGLA),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (bill.dueAmount > 0.01) {
                    Text(
                        stringResource(R.string.due_label, NumberFormatter.formatMoney(bill.dueAmount, DigitStyle.BANGLA)),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                    )
                } else {
                    Text(
                        stringResource(R.string.paid_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> remember(calculation: () -> T): T = androidx.compose.runtime.remember(calculation = calculation)
