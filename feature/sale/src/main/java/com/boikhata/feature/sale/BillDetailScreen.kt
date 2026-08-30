package com.boikhata.feature.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.core.designsystem.format.DigitStyle
import com.boikhata.core.designsystem.format.NumberFormatter
import com.boikhata.feature.sale.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * P2b: Bill detail — shows full bill with lines, allows receipt sharing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(
    tenantId: String,
    billId: String,
    shopName: String,
    onBack: () -> Unit,
    onNewSale: () -> Unit,
    viewModel: SaleViewModel = hiltViewModel(),
) {
    var bill by remember { mutableStateOf<com.boikhata.core.domain.model.Bill?>(null) }
    var lines by remember { mutableStateOf<List<com.boikhata.core.domain.model.BillLine>>(emptyList()) }

    LaunchedEffect(tenantId, billId) {
        bill = viewModel.getBillForDetail(tenantId, billId)
        lines = viewModel.getBillLinesForDetail(billId)
    }

    val configuration = LocalConfiguration.current
    val dateFormat = remember(configuration) {
        val locale = if (configuration.locales.isEmpty) Locale.getDefault() else configuration.locales[0]
        SimpleDateFormat("dd/MM/yyyy HH:mm", locale)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bill?.billNumber ?: stringResource(R.string.bill_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    bill?.let { b ->
                        IconButton(onClick = { viewModel.shareReceipt(b.id, shopName) }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_receipt))
                        }
                    }
                },
            )
        }
    ) { padding ->
        val b = bill
        if (b == null) {
            Row(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) { Text(stringResource(R.string.loading)) }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Bill info card
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(b.billNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(dateFormat.format(Date(b.billDate)), style = MaterialTheme.typography.bodySmall)
                        Text(b.customerNameBn, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Line items
                Text(stringResource(R.string.cart_items, lines.size), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                lines.forEach { line ->
                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(line.bookTitleBn, fontWeight = FontWeight.Bold)
                                Text("${line.quantity} × ${NumberFormatter.formatMoney(line.unitPrice, DigitStyle.BANGLA)}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Text(NumberFormatter.formatMoney(line.lineTotal, DigitStyle.BANGLA),
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Totals
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SummaryRow(stringResource(R.string.subtotal), b.subtotal)
                        if (b.discountAmount > 0.01) SummaryRow(stringResource(R.string.discount), -b.discountAmount)
                        if (b.vatAmount > 0.01) SummaryRow(stringResource(R.string.vat), b.vatAmount)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.total) + ": " + NumberFormatter.formatMoney(b.totalAmount, DigitStyle.BANGLA),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(stringResource(R.string.paid_amount) + ": " + NumberFormatter.formatMoney(b.paidAmount, DigitStyle.BANGLA))
                        if (b.dueAmount > 0.01) {
                            Text(
                                stringResource(R.string.due) + ": " + NumberFormatter.formatMoney(b.dueAmount, DigitStyle.BANGLA),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                OutlinedButton(onClick = onNewSale, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.new_sale))
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(NumberFormatter.formatMoney(if (amount < 0) -amount else amount, DigitStyle.BANGLA),
            style = MaterialTheme.typography.bodyMedium)
    }
}
