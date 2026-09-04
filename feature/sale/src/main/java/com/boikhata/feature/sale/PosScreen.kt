package com.boikhata.feature.sale

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.boikhata.core.domain.enums.PaymentMethod
import com.boikhata.core.domain.model.Book
import com.boikhata.core.domain.model.KhataCustomer
import com.boikhata.feature.sale.R

/**
 * P2b: POS sale screen — cart, book search, discount, VAT, payment, checkout.
 * Blueprint §7.3: fast POS; partial → auto-khata; one-transaction sale+দাক-রসিদ.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    tenantId: String,
    onCheckoutComplete: (String) -> Unit,
    onExpenseClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onCashCloseClick: () -> Unit = {},
    onSubscriptionClick: () -> Unit = {},
    onSupplierClick: () -> Unit = {},
    onMelaClick: () -> Unit = {},
    viewModel: SaleViewModel = hiltViewModel(),
) {
    val cartState by viewModel.cartState.collectAsState()
    val bookSearchState by viewModel.bookSearchState.collectAsState()

    var showBookSearch by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showCheckoutConfirm by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.pos_title)) },
                actions = {
                    TextButton(onClick = onCashCloseClick) {
                        Text(stringResource(R.string.cash_close_button))
                    }
                    TextButton(onClick = onReportsClick) {
                        Text(stringResource(R.string.reports_button))
                    }
                    TextButton(onClick = onExpenseClick) {
                        Text(stringResource(R.string.expense_button))
                    }
                    TextButton(onClick = onSubscriptionClick) {
                        Text(stringResource(R.string.subscription_button))
                    }
                    TextButton(onClick = onSupplierClick) {
                        Text(stringResource(R.string.supplier_button))
                    }
                    TextButton(onClick = onMelaClick) {
                        Text(stringResource(R.string.mela_button))
                    }
                },
            )
        },
        floatingActionButton = {
            if (cartState.items.isNotEmpty()) {
                FloatingActionButton(onClick = { showCheckoutConfirm = true }) {
                    Text(stringResource(R.string.checkout))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Customer selection
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(stringResource(R.string.customer), style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = cartState.selectedCustomer?.nameBn ?: stringResource(R.string.walk_in_customer),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    TextButton(onClick = { showCustomerPicker = true }) {
                        Text(stringResource(R.string.select))
                    }
                }
            }

            // Cart items
            Text(
                stringResource(R.string.cart_items, cartState.items.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (cartState.items.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.cart_empty), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { showBookSearch = true }) {
                            Text(stringResource(R.string.add_books))
                        }
                    }
                }
            } else {
                cartState.items.forEach { item ->
                    CartItemCard(
                        title = item.bookTitleBn,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        lineTotal = item.unitPrice * item.quantity,
                        onIncrease = { viewModel.updateQuantity(item.bookId, item.quantity + 1) },
                        onDecrease = { viewModel.updateQuantity(item.bookId, item.quantity - 1) },
                        onRemove = { viewModel.updateQuantity(item.bookId, 0) },
                    )
                }

                Button(onClick = { showBookSearch = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.add_more_books))
                }

                // Discount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = cartState.discountInput,
                        onValueChange = { viewModel.setDiscount(it, cartState.isPercentageDiscount) },
                        label = { Text(stringResource(R.string.discount)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    TextButton(
                        onClick = { viewModel.setDiscount(cartState.discountInput, !cartState.isPercentageDiscount) }
                    ) {
                        Text(if (cartState.isPercentageDiscount) "%" else "৳")
                    }
                }

                // Payment method
                Text(stringResource(R.string.payment_method), style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PaymentMethod.entries.filter { it != PaymentMethod.NAGAD }.forEach { method ->
                        val selected = cartState.paymentMethod == method
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setPaymentMethod(method) },
                            label = { Text(paymentLabel(method)) },
                        )
                    }
                }

                // Paid amount (for partial payment)
                if (cartState.paymentMethod != PaymentMethod.CREDIT) {
                    OutlinedTextField(
                        value = cartState.paidAmountInput,
                        onValueChange = { viewModel.setPaidAmount(it.filter { c -> c.isDigit() || c == '.' }) },
                        label = { Text(stringResource(R.string.paid_amount)) },
                        placeholder = { Text(NumberFormatter.formatMoney(cartState.totalAmount, DigitStyle.BANGLA)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                // Totals summary
                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SummaryRow(stringResource(R.string.subtotal), cartState.subtotal)
                        if (cartState.discountAmount > 0.01)
                            SummaryRow(stringResource(R.string.discount), -cartState.discountAmount)
                        if (cartState.vatAmount > 0.01)
                            SummaryRow(stringResource(R.string.vat), cartState.vatAmount)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.total) + ": " +
                                NumberFormatter.formatMoney(cartState.totalAmount, DigitStyle.BANGLA),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (cartState.dueAmount > 0.01) {
                            Text(
                                text = stringResource(R.string.due) + ": " +
                                    NumberFormatter.formatMoney(cartState.dueAmount, DigitStyle.BANGLA),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(80.dp)) // FAB space
            }

            // Error message
            errorMessage?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            }
        }
    }

    // Book search sheet
    if (showBookSearch) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showBookSearch = false }, sheetState = sheetState) {
            BookSearchSheet(
                state = bookSearchState,
                onSearch = { query -> viewModel.searchBooks(tenantId, query) },
                onBookSelected = { book ->
                    viewModel.addToCart(book)
                    showBookSearch = false
                },
            )
        }
    }

    // Customer picker sheet
    if (showCustomerPicker) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showCustomerPicker = false }, sheetState = sheetState) {
            CustomerPickerSheet(
                onSearch = { query -> viewModel.searchCustomers(tenantId, query) },
                results = cartState.customerSearchResults,
                onCustomerSelected = { customer ->
                    viewModel.selectCustomer(customer)
                    showCustomerPicker = false
                },
                onWalkInSelected = {
                    viewModel.selectCustomer(null)
                    showCustomerPicker = false
                },
            )
        }
    }

    // Checkout confirmation
    if (showCheckoutConfirm) {
        AlertDialog(
            onDismissRequest = { showCheckoutConfirm = false },
            title = { Text(stringResource(R.string.confirm_checkout)) },
            text = {
                Text(stringResource(R.string.confirm_checkout_msg,
                    NumberFormatter.formatMoney(cartState.totalAmount, DigitStyle.BANGLA),
                    NumberFormatter.formatMoney(cartState.dueAmount, DigitStyle.BANGLA)))
            },
            confirmButton = {
                TextButton(onClick = {
                    showCheckoutConfirm = false
                    viewModel.checkout(
                        onDone = { billId -> onCheckoutComplete(billId) },
                        onError = { msg -> errorMessage = msg },
                    )
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutConfirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun CartItemCard(
    title: String,
    quantity: Int,
    unitPrice: Double,
    lineTotal: Double,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${NumberFormatter.formatMoney(unitPrice, DigitStyle.BANGLA)} × $quantity",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDecrease) { Text("−") }
                Text("$quantity", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onIncrease) { Text("+") }
                Spacer(Modifier.height(4.dp))
                Text(
                    NumberFormatter.formatMoney(lineTotal, DigitStyle.BANGLA),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
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
        Text(
            NumberFormatter.formatMoney(if (amount < 0) -amount else amount, DigitStyle.BANGLA),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun BookSearchSheet(
    state: BookSearchState,
    onSearch: (String) -> Unit,
    onBookSelected: (Book) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; onSearch(it) },
            label = { Text(stringResource(R.string.search_books)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        when (state) {
            is BookSearchState.Success -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.height(400.dp)) {
                    items(state.books) { book ->
                        Card(onClick = { onBookSelected(book) }, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(book.titleBn, fontWeight = FontWeight.Bold)
                                    Text(book.author, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    NumberFormatter.formatMoney(book.sellingPrice, DigitStyle.BANGLA),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun CustomerPickerSheet(
    onSearch: (String) -> Unit,
    results: List<KhataCustomer>,
    onCustomerSelected: (KhataCustomer) -> Unit,
    onWalkInSelected: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(16.dp)) {
        Text(stringResource(R.string.select_customer), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; onSearch(it) },
            label = { Text(stringResource(R.string.search_customers)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onWalkInSelected, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.walk_in_customer))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.height(300.dp)) {
            items(results) { customer ->
                Card(onClick = { onCustomerSelected(customer) }, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(customer.nameBn, fontWeight = FontWeight.Bold)
                        customer.address?.let { if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
    )
}

private fun paymentLabel(method: PaymentMethod): String = when (method) {
    PaymentMethod.CASH -> "নগদ"
    PaymentMethod.BKASH -> "বিকাশ"
    PaymentMethod.NAGAD -> "নগদ"
    PaymentMethod.CREDIT -> "বাকি"
}
