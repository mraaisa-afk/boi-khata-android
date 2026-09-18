package com.boikhata.feature.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.core.designsystem.ColorBrandMaroon
import com.boikhata.core.designsystem.format.DigitStyle
import com.boikhata.core.designsystem.format.NumberFormatter
import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.enums.CashbookEntryType
import com.boikhata.core.domain.model.ExpenseCategory
import com.boikhata.core.domain.text.BengaliNormalizer
import com.boikhata.feature.expense.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * P3a: Expense screen with 3 tabs — খরচ (expense), ক্যাশবুক (cashbook), মালিকের তোলা (owner drawing).
 * B-010/B-012: the add sheets were un-usable on Bangla keyboards — the category
 * dropdown could never open (dead state) and every amount parse was ASCII-only
 * (B-007 class). Sheets now use always-visible category chips (D87 §1: bounded
 * local dataset renders the FULL list) and BengaliNormalizer-normalized parsing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    tenantId: String,
    viewModel: ExpenseViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddExpense by remember { mutableStateOf(false) }
    var showAddDrawing by remember { mutableStateOf(false) }
    var showAddCashbookEntry by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tenantId) {
        viewModel.loadExpenses(tenantId)
        viewModel.loadCashbook(tenantId)
        viewModel.loadDrawings(tenantId)
    }

    val tabs = listOf(
        stringResource(R.string.tab_expense),
        stringResource(R.string.tab_cashbook),
        stringResource(R.string.tab_drawing),
    )

    Scaffold(
        floatingActionButton = {
            // D71 §1 (B-008 precedent): FABs use the brand palette, never the M3
            // default primaryContainer (pink) — same fix as the khata add FAB.
            when (selectedTab) {
                0 -> FloatingActionButton(
                    onClick = { showAddExpense = true },
                    containerColor = ColorBrandMaroon,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_expense))
                }
                1 -> FloatingActionButton(
                    onClick = { showAddCashbookEntry = true },
                    containerColor = ColorBrandMaroon,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_entry))
                }
                2 -> FloatingActionButton(
                    onClick = { showAddDrawing = true },
                    containerColor = ColorBrandMaroon,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_drawing))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            when (selectedTab) {
                0 -> ExpenseTab(viewModel, tenantId)
                1 -> CashbookTab(viewModel)
                2 -> DrawingTab(viewModel)
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

    if (showAddExpense) {
        val sheetState = rememberModalBottomSheetState()
        val state = viewModel.expenseState.collectAsState().value
        val categories = (state as? ExpenseUiState.Success)?.categories ?: emptyList()
        ModalBottomSheet(onDismissRequest = { showAddExpense = false }, sheetState = sheetState) {
            AddExpenseSheet(
                categories = categories,
                onConfirm = { categoryId, amount, desc, account ->
                    viewModel.addExpense(
                        categoryId = categoryId,
                        amount = amount,
                        description = desc,
                        cashbookAccount = account,
                        onDone = { showAddExpense = false },
                        onError = { msg -> errorMessage = msg; showAddExpense = false },
                    )
                },
                onCancel = { showAddExpense = false },
            )
        }
    }

    if (showAddCashbookEntry) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showAddCashbookEntry = false }, sheetState = sheetState) {
            AddCashbookEntrySheet(
                onConfirm = { account, type, amount, desc ->
                    viewModel.addManualCashbookEntry(
                        account = account,
                        type = type,
                        amount = amount,
                        description = desc,
                        onDone = { showAddCashbookEntry = false },
                        onError = { msg -> errorMessage = msg; showAddCashbookEntry = false },
                    )
                },
                onCancel = { showAddCashbookEntry = false },
            )
        }
    }

    if (showAddDrawing) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showAddDrawing = false }, sheetState = sheetState) {
            AddDrawingSheet(
                onConfirm = { amount, desc ->
                    viewModel.createDrawing(
                        amount = amount,
                        description = desc,
                        onDone = { showAddDrawing = false },
                        onError = { msg -> errorMessage = msg; showAddDrawing = false },
                    )
                },
                onCancel = { showAddDrawing = false },
            )
        }
    }
}

@Composable
private fun ExpenseTab(viewModel: ExpenseViewModel, tenantId: String) {
    LaunchedEffect(tenantId) { viewModel.loadExpenses(tenantId) }
    val state by viewModel.expenseState.collectAsState()
    val configuration = LocalConfiguration.current
    val dateFormat = remember(configuration) {
        val locale = if (configuration.locales.isEmpty) Locale.getDefault() else configuration.locales[0]
        SimpleDateFormat("dd/MM/yyyy", locale)
    }

    when (val s = state) {
        is ExpenseUiState.Loading -> Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) { CircularProgressIndicator() }

        is ExpenseUiState.Error -> Text(
            s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)
        )

        is ExpenseUiState.Success -> {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                if (s.goriBalances.isNotEmpty()) {
                    Text(
                        stringResource(R.string.ghori_balance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    s.goriBalances.forEach { (userId, balance) ->
                        if (balance > 0.01) {
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(userId, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        NumberFormatter.formatMoney(balance, DigitStyle.BANGLA),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    stringResource(R.string.expense_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )

                if (s.expenses.isEmpty()) {
                    Text(stringResource(R.string.no_expenses), modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(s.expenses) { expense ->
                            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(expense.categoryNameBn, fontWeight = FontWeight.Bold)
                                        Text(expense.description, style = MaterialTheme.typography.bodySmall)
                                        Text(dateFormat.format(Date(expense.expenseDate)), style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        NumberFormatter.formatMoney(expense.amount, DigitStyle.BANGLA),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CashbookTab(viewModel: ExpenseViewModel) {
    val state by viewModel.cashbookState.collectAsState()

    when (val s = state) {
        is CashbookUiState.Loading -> Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) { CircularProgressIndicator() }

        is CashbookUiState.Error -> Text(
            s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)
        )

        is CashbookUiState.Success -> {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                s.balances.forEach { balance ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(2.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                accountLabel(balance.account),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.income_label), style = MaterialTheme.typography.bodyMedium)
                                Text(NumberFormatter.formatMoney(balance.income, DigitStyle.BANGLA))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.expense_label), style = MaterialTheme.typography.bodyMedium)
                                Text(NumberFormatter.formatMoney(balance.expense, DigitStyle.BANGLA))
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.balance), fontWeight = FontWeight.Bold)
                                Text(
                                    NumberFormatter.formatMoney(balance.balance, DigitStyle.BANGLA),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawingTab(viewModel: ExpenseViewModel) {
    val state by viewModel.drawingState.collectAsState()
    val configuration = LocalConfiguration.current
    val dateFormat = remember(configuration) {
        val locale = if (configuration.locales.isEmpty) Locale.getDefault() else configuration.locales[0]
        SimpleDateFormat("dd/MM/yyyy", locale)
    }

    when (val s = state) {
        is DrawingUiState.Loading -> Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) { CircularProgressIndicator() }

        is DrawingUiState.Error -> Text(
            s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)
        )

        is DrawingUiState.Success -> {
            if (s.drawings.isEmpty()) {
                Text(stringResource(R.string.no_drawings), modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(s.drawings) { drawing ->
                        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(drawing.description, fontWeight = FontWeight.Bold)
                                    Text(dateFormat.format(Date(drawing.drawingDate)), style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    NumberFormatter.formatMoney(drawing.amount, DigitStyle.BANGLA),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * B-007/B-010: the single parse every amount field in this file uses —
 * Bangla-keyboard digits (০-৯) are normalized to ASCII before toDoubleOrNull;
 * a raw parse silently returned null and permanently disabled সেভ.
 */
internal fun parseAmountInput(raw: String): Double? =
    BengaliNormalizer.toAsciiDigits(raw).toDoubleOrNull()

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddExpenseSheet(
    categories: List<ExpenseCategory>,
    onConfirm: (categoryId: String, amount: Double, description: String, account: CashbookAccount) -> Unit,
    onCancel: () -> Unit,
) {
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf(CashbookAccount.CASH) }

    // B-010: Save required a category that was UNREACHABLE — the old readOnly
    // text field fronted a DropdownMenu whose expanded state was never set true,
    // so nothing could ever be selected and সেভ stayed gray forever. Replaced
    // with always-visible chips (D87 §1: bounded local dataset = full list
    // rendered; §3: explicit empty state when the list is empty).
    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.add_expense), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.category), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        if (categories.isEmpty()) {
            Text(
                stringResource(R.string.no_categories),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    androidx.compose.material3.FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat.nameBn) },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text(stringResource(R.string.amount)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.description)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CashbookAccount.entries.forEach { account ->
                androidx.compose.material3.FilterChip(
                    selected = selectedAccount == account,
                    onClick = { selectedAccount = account },
                    label = { Text(accountLabel(account)) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = {
                    val cat = selectedCategory
                    val amt = parseAmountInput(amount) ?: 0.0
                    if (cat != null && amt > 0) {
                        onConfirm(cat.id, amt, description, selectedAccount)
                    }
                },
                enabled = selectedCategory != null && (parseAmountInput(amount) ?: 0.0) > 0,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.save)) }
        }
    }
}

@Composable
internal fun AddCashbookEntrySheet(
    onConfirm: (account: CashbookAccount, type: CashbookEntryType, amount: Double, description: String) -> Unit,
    onCancel: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf(CashbookAccount.CASH) }
    var selectedType by remember { mutableStateOf(CashbookEntryType.INCOME) }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.add_entry), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CashbookEntryType.entries.forEach { type ->
                androidx.compose.material3.FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(cashbookTypeLabel(type)) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CashbookAccount.entries.forEach { account ->
                androidx.compose.material3.FilterChip(
                    selected = selectedAccount == account,
                    onClick = { selectedAccount = account },
                    label = { Text(accountLabel(account)) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text(stringResource(R.string.amount)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.description)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            Button(
                onClick = {
                    val amt = parseAmountInput(amount) ?: 0.0
                    if (amt > 0) onConfirm(selectedAccount, selectedType, amt, description)
                },
                enabled = (parseAmountInput(amount) ?: 0.0) > 0,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.save)) }
        }
    }
}

@Composable
internal fun AddDrawingSheet(
    onConfirm: (amount: Double, description: String) -> Unit,
    onCancel: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.add_drawing), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text(stringResource(R.string.amount)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.description)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            Button(
                onClick = {
                    val amt = parseAmountInput(amount) ?: 0.0
                    if (amt > 0) onConfirm(amt, description)
                },
                enabled = (parseAmountInput(amount) ?: 0.0) > 0,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.save)) }
        }
    }
}

private fun accountLabel(account: CashbookAccount): String = when (account) {
    CashbookAccount.CASH -> "নগদ"
    CashbookAccount.BKASH -> "বিকাশ"
    CashbookAccount.BANK -> "ব্যাংক"
}

private fun cashbookTypeLabel(type: CashbookEntryType): String = when (type) {
    CashbookEntryType.INCOME -> "আয়"
    CashbookEntryType.EXPENSE -> "খরচ"
    CashbookEntryType.TRANSFER -> "স্থানান্তর"
}
