package com.boikhata.feature.khata

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.core.designsystem.format.DigitStyle
import com.boikhata.core.designsystem.format.NumberFormatter
import com.boikhata.core.domain.enums.KhataEntryType
import com.boikhata.core.domain.model.KhataInstallment
import com.boikhata.core.domain.model.KhataStatementLine
import com.boikhata.feature.khata.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * P2a: Khata customer detail — entries, aging, credit-limit warning,
 * installment tracking, দেনা-মুন, shareable বাকি হিসাব statement.
 * Blueprint §7.4: the project's heart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataCustomerDetailScreen(
    tenantId: String,
    customerId: String,
    shopName: String,
    onBack: () -> Unit,
    viewModel: KhataViewModel = hiltViewModel(),
) {
    LaunchedEffect(tenantId, customerId) {
        viewModel.loadDetail(tenantId, customerId)
    }

    val uiState by viewModel.detailState.collectAsState()

    var showAddCredit by remember { mutableStateOf(false) }
    var showAddPayment by remember { mutableStateOf(false) }
    var showForgiveDebt by remember { mutableStateOf(false) }
    var showAddInstallment by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val state = uiState
                    Text(if (state is KhataDetailUiState.Success) state.customer.nameBn else "খাতা")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shareStatement(shopName) }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_statement))
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is KhataDetailUiState.Loading -> {
                Row(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) { CircularProgressIndicator() }
            }
            is KhataDetailUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
            is KhataDetailUiState.Success -> {
                DetailContent(
                    state = state,
                    padding = padding,
                    onAddCredit = { showAddCredit = true },
                    onAddPayment = { showAddPayment = true },
                    onForgiveDebt = { showForgiveDebt = true },
                    onAddInstallment = { showAddInstallment = true },
                    onMarkInstallmentPaid = { id -> viewModel.markInstallmentPaid(id) },
                )
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────

    if (showAddCredit) {
        AmountDialog(
            title = stringResource(R.string.add_credit),
            label = stringResource(R.string.amount),
            onConfirm = { amount, desc ->
                viewModel.addCredit(amount, desc) { showAddCredit = false }
            },
            onDismiss = { showAddCredit = false },
        )
    }

    if (showAddPayment) {
        AmountDialog(
            title = stringResource(R.string.add_payment),
            label = stringResource(R.string.amount),
            onConfirm = { amount, desc ->
                viewModel.addPayment(amount, desc) { showAddPayment = false }
            },
            onDismiss = { showAddPayment = false },
        )
    }

    if (showForgiveDebt) {
        val state = uiState
        val dueAmount = if (state is KhataDetailUiState.Success) state.statement.totalDue else 0.0
        AlertDialog(
            onDismissRequest = { showForgiveDebt = false },
            title = { Text(stringResource(R.string.forgive_debt)) },
            text = {
                Text(stringResource(R.string.forgive_debt_confirm,
                    NumberFormatter.formatMoney(dueAmount, DigitStyle.BANGLA)))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.forgiveDebt { showForgiveDebt = false }
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showForgiveDebt = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showAddInstallment) {
        InstallmentDialog(
            onConfirm = { dueDate, amount ->
                viewModel.addInstallment(dueDate, amount) { showAddInstallment = false }
            },
            onDismiss = { showAddInstallment = false },
        )
    }
}

@Composable
private fun DetailContent(
    state: KhataDetailUiState.Success,
    padding: androidx.compose.foundation.layout.PaddingValues,
    onAddCredit: () -> Unit,
    onAddPayment: () -> Unit,
    onForgiveDebt: () -> Unit,
    onAddInstallment: () -> Unit,
    onMarkInstallmentPaid: (String) -> Unit,
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val stmt = state.statement

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Summary card
        item {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stmt.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    stmt.customerArea?.let { if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodySmall) }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.total_due) + ": " +
                            NumberFormatter.formatMoney(stmt.totalDue, DigitStyle.BANGLA),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (stmt.exceedsCreditLimit) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.credit_limit_warning,
                                NumberFormatter.formatMoney(stmt.creditLimit, DigitStyle.BANGLA)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (stmt.totalDue > 0.01 && stmt.aging.ageDays > 0) {
                        Text(
                            text = stringResource(R.string.age_days, stmt.aging.ageDays.toInt()),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        // Action buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onAddCredit, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.add_credit))
                }
                OutlinedButton(onClick = onAddPayment, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.add_payment))
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onAddInstallment, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.add_installment))
                }
                OutlinedButton(onClick = onForgiveDebt, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.forgive_debt))
                }
            }
        }

        // Entry history
        item {
            Text(
                text = stringResource(R.string.entry_history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (state.statement.lines.isEmpty()) {
            item { Text(stringResource(R.string.no_entries), style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(state.statement.lines) { line ->
                StatementLineCard(line, dateFormat)
            }
        }

        // Installments
        if (state.installments.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.installments),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(state.installments) { inst ->
                InstallmentCard(inst, dateFormat, onMarkInstallmentPaid)
            }
        }
    }
}

@Composable
private fun StatementLineCard(line: KhataStatementLine, dateFormat: SimpleDateFormat) {
    val sign = when (line.type) {
        KhataEntryType.CREDIT, KhataEntryType.OPENING -> "+"
        KhataEntryType.PAYMENT -> "-"
        KhataEntryType.ADJUSTMENT -> if (line.amount >= 0) "+" else "-"
    }
    val displayAmount = if (line.amount < 0) -line.amount else line.amount
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("${dateFormat.format(Date(line.date))}  ${line.description}", style = MaterialTheme.typography.bodySmall)
                Text("$sign${NumberFormatter.formatMoney(displayAmount, DigitStyle.BANGLA)}", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = NumberFormatter.formatMoney(line.runningBalance, DigitStyle.BANGLA),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun InstallmentCard(
    inst: KhataInstallment,
    dateFormat: SimpleDateFormat,
    onMarkPaid: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(dateFormat.format(Date(inst.dueDate)), style = MaterialTheme.typography.bodySmall)
                Text(NumberFormatter.formatMoney(inst.amount, DigitStyle.BANGLA), style = MaterialTheme.typography.bodyMedium)
            }
            if (inst.isPaid) {
                Text(stringResource(R.string.paid), color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            } else {
                TextButton(onClick = { onMarkPaid(inst.id) }) { Text(stringResource(R.string.mark_paid)) }
            }
        }
    }
}

@Composable
private fun AmountDialog(
    title: String,
    label: String,
    onConfirm: (Double, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount.toDoubleOrNull() ?: 0.0, description) },
                enabled = amount.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun InstallmentDialog(
    onConfirm: (Long, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var daysFromNow by remember { mutableStateOf("30") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_installment)) },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.amount)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = daysFromNow,
                    onValueChange = { daysFromNow = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.due_after_days)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    val days = daysFromNow.toLongOrNull() ?: 30
                    if (amt > 0) onConfirm(System.currentTimeMillis() + days * 24 * 60 * 60 * 1000, amt)
                },
                enabled = amount.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
