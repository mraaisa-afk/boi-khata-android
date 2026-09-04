package com.boikhata.feature.supplier

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.core.designsystem.format.DigitStyle
import com.boikhata.core.designsystem.format.NumberFormatter
import com.boikhata.core.domain.aging.AgingBucket
import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.enums.SupplierEntryType
import com.boikhata.core.domain.model.SupplierBalance
import com.boikhata.feature.supplier.R

/**
 * P5: Supplier/publisher payable ledger (দেনা-খাতা) screen.
 * Shows the per-supplier payable ledger, aging summary, settlement reminders, and the
 * shareable settlement statement.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierScreen(
    tenantId: String,
    shopName: String,
    viewModel: SupplierViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val detail by viewModel.detailState.collectAsState()

    var showAddSupplier by remember { mutableStateOf(false) }
    var showAddEntry by remember { mutableStateOf(false) }
    var showDetailFor by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tenantId) {
        viewModel.loadSuppliers(tenantId, shopName)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSupplier = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_supplier))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is SupplierUiState.Loading -> Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) { CircularProgressIndicator() }

                is SupplierUiState.Error -> Text(
                    s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)
                )

                is SupplierUiState.Success -> SupplierListContent(
                    state = s,
                    onSupplierClick = { id -> viewModel.loadSupplierDetail(id); showDetailFor = id },
                )
            }

            // Detail sheet
            if (showDetailFor != null) {
                val sheetState = rememberModalBottomSheetState()
                ModalBottomSheet(onDismissRequest = { showDetailFor = null }, sheetState = sheetState) {
                    val d = detail
                    when (d) {
                        is SupplierDetailUiState.Success -> SupplierDetailContent(
                            balance = d.balance,
                            entries = d.entries,
                            shopName = shopName,
                            onAddEntry = { showAddEntry = true },
                            onShare = { viewModel.shareStatement() },
                        )
                        else -> Text(stringResource(R.string.loading), modifier = Modifier.padding(16.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text(stringResource(R.string.ok)) } },
            text = { Text(msg) },
        )
    }

    if (showAddSupplier) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showAddSupplier = false }, sheetState = sheetState) {
            AddSupplierSheet(
                onConfirm = { name, phone, cycle, notes ->
                    viewModel.addSupplier(
                        nameBn = name,
                        phone = phone,
                        settlementCycle = cycle,
                        notes = notes,
                        onDone = { showAddSupplier = false },
                        onError = { msg -> errorMessage = msg; showAddSupplier = false },
                    )
                },
                onCancel = { showAddSupplier = false },
            )
        }
    }

    if (showAddEntry) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showAddEntry = false }, sheetState = sheetState) {
            AddEntrySheet(
                onConfirm = { type, amount, desc, trxId, account ->
                    viewModel.addEntry(
                        type = type,
                        amount = amount,
                        description = desc,
                        trxId = trxId,
                        cashbookAccount = account,
                        onDone = { showAddEntry = false },
                        onError = { msg -> errorMessage = msg; showAddEntry = false },
                    )
                },
                onCancel = { showAddEntry = false },
            )
        }
    }
}

@Composable
private fun SupplierListContent(
    state: SupplierUiState.Success,
    onSupplierClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Total payable summary
        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.total_payable), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    NumberFormatter.formatMoney(state.summary.totalPayable, DigitStyle.BANGLA),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text("🟢 ${NumberFormatter.formatMoney(state.summary.greenBucket, DigitStyle.BANGLA)}   " +
                    "🟡 ${NumberFormatter.formatMoney(state.summary.yellowBucket, DigitStyle.BANGLA)}   " +
                    "🔴 ${NumberFormatter.formatMoney(state.summary.redBucket, DigitStyle.BANGLA)}",
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        // Settlement reminders
        if (state.reminders.isNotEmpty()) {
            Text(
                stringResource(R.string.settlement_reminders),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
            state.reminders.forEach { reminder ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )) {
                    Text(
                        "${reminder.supplier.nameBn} — ${stringResource(R.string.payable)} " +
                            NumberFormatter.formatMoney(reminder.balance, DigitStyle.BANGLA) +
                            " (${reminder.overdueForDays} ${stringResource(R.string.settlement_cycle)})",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Text(
            "সাপ্লায়ার তালিকা",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )

        if (state.suppliers.isEmpty()) {
            Text(stringResource(R.string.no_suppliers), modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.suppliers) { supplier ->
                    val balance = state.balances.firstOrNull { it.supplier.id == supplier.id }
                    SupplierCard(balance, onClick = { onSupplierClick(supplier.id) })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SupplierCard(balance: SupplierBalance?, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(balance?.supplier?.nameBn ?: "", fontWeight = FontWeight.Bold)
                balance?.supplier?.phone?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                Text(stringResource(R.string.settlement_cycle_hint) + ": " +
                    (balance?.supplier?.settlementCycle ?: ""), style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    NumberFormatter.formatMoney(balance?.balance ?: 0.0, DigitStyle.BANGLA),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(bucketLabel(balance?.bucket ?: AgingBucket.NONE), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SupplierDetailContent(
    balance: SupplierBalance,
    entries: List<com.boikhata.core.domain.model.SupplierEntry>,
    shopName: String,
    onAddEntry: () -> Unit,
    onShare: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(balance.supplier.nameBn, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        balance.supplier.phone?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        Text("${stringResource(R.string.settlement_cycle_hint)}: ${balance.supplier.settlementCycle}",
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.total_payable), fontWeight = FontWeight.Bold)
                    Text(
                        NumberFormatter.formatMoney(balance.balance, DigitStyle.BANGLA),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.aging))
                    Text("${bucketLabel(balance.bucket)} (${balance.ageDays} ${stringResource(R.string.settlement_cycle)})")
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddEntry, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.transactions))
            }
            Button(onClick = onShare, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.share_statement))
            }
        }

        Text("লেনদেন", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        if (entries.isEmpty()) {
            Text(stringResource(R.string.no_entries), modifier = Modifier.padding(16.dp))
        } else {
            entries.forEach { e ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(entryTypeLabel(e.type), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        NumberFormatter.formatMoney(e.amount, DigitStyle.BANGLA),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSupplierSheet(
    onConfirm: (name: String, phone: String?, cycle: String, notes: String?) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf("30") }
    var notes by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.add_supplier), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.supplier_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(stringResource(R.string.phone)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = cycle, onValueChange = { cycle = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.settlement_cycle)) }, placeholder = { Text(stringResource(R.string.settlement_cycle_hint)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.notes)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            Button(onClick = { onConfirm(name, phone.ifBlank { null }, cycle.ifBlank { "30" }, notes.ifBlank { null }) },
                enabled = name.isNotBlank(), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.save)) }
        }
    }
}

@Composable
private fun AddEntrySheet(
    onConfirm: (type: SupplierEntryType, amount: Double, desc: String, trxId: String?, account: CashbookAccount) -> Unit,
    onCancel: () -> Unit,
) {
    var type by remember { mutableStateOf(SupplierEntryType.CONSIGNMENT) }
    var amount by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var trxId by remember { mutableStateOf("") }
    var account by remember { mutableStateOf(CashbookAccount.CASH) }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.transactions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SupplierEntryType.entries.filter { it != SupplierEntryType.ADJUSTMENT }.forEach { t ->
                FilterChip(
                    selected = type == t,
                    onClick = { type = t },
                    label = { Text(entryTypeLabel(t)) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text(stringResource(R.string.entry_amount)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(stringResource(R.string.entry_description)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        if (type == SupplierEntryType.PAYMENT) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = trxId, onValueChange = { trxId = it }, label = { Text(stringResource(R.string.trx_id)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CashbookAccount.entries.forEach { acc ->
                    FilterChip(selected = account == acc, onClick = { account = acc }, label = { Text(accountLabel(acc)) })
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            Button(
                onClick = { onConfirm(type, amount.toDoubleOrNull() ?: 0.0, desc, trxId.ifBlank { null }, account) },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.save)) }
        }
    }
}

private fun bucketLabel(bucket: AgingBucket): String = when (bucket) {
    AgingBucket.GREEN -> "🟢 <১৫দি"
    AgingBucket.YELLOW -> "🟡 ১৫–৩০দি"
    AgingBucket.RED -> "🔴 >৩০দি"
    AgingBucket.NONE -> "—"
}

private fun entryTypeLabel(type: SupplierEntryType): String = when (type) {
    SupplierEntryType.OPENING -> "উদ্বোধনী দেনা"
    SupplierEntryType.CONSIGNMENT -> "কনসাইনমেন্ট"
    SupplierEntryType.PURCHASE -> "ক্রয় (বাকি)"
    SupplierEntryType.PAYMENT -> "পেমেন্ট"
    SupplierEntryType.ADJUSTMENT -> "সমন্বয়"
}

private fun accountLabel(account: CashbookAccount): String = when (account) {
    CashbookAccount.CASH -> "নগদ"
    CashbookAccount.BKASH -> "বিকাশ"
    CashbookAccount.BANK -> "ব্যাংক"
}
