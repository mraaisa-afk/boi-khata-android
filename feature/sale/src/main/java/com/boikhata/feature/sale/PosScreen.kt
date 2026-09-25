package com.boikhata.feature.sale

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.core.designsystem.format.DigitStyle
import com.boikhata.core.designsystem.format.NumberFormatter
import com.boikhata.core.domain.enums.MfsProvider
import com.boikhata.core.domain.enums.PaymentLineCategory
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
    val customerSearchState by viewModel.customerSearchState.collectAsState()

    var showBookSearch by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showCheckoutConfirm by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // P14 (owner device ruling — «স্টকে পর্যাপ্ত বই নেই»): the cart guards emit a
    // one-shot warning; surface it as a Toast and clear it so the same blocked tap
    // can fire again later.
    val context = LocalContext.current
    LaunchedEffect(cartState.stockWarning) {
        cartState.stockWarning?.let { warning ->
            Toast.makeText(context, warning, Toast.LENGTH_SHORT).show()
            viewModel.dismissStockWarning()
        }
    }

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
                        onValueChange = { input ->
                            // B-007: accept ASCII + Bengali digits (Bangla keyboards emit ০-৯)
                            val filtered = input.filter { it.isDigit() || it in '০'..'৯' || it == '.' }
                            viewModel.setDiscount(filtered, cartState.isPercentageDiscount)
                        },
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

                // P12/D92: multi-line payment entry — নগদ / ব্যাংক / মোবাইল
                // ব্যাংকিং (with provider selector) are combinable in ONE sale;
                // the remainder posts as বাকি (preview below). The single blank
                // নগদ line keeps the legacy “full cash payment” default.
                Text(stringResource(R.string.payment_method), style = MaterialTheme.typography.labelMedium)
                cartState.paymentLines.forEachIndexed { index, line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text(paymentLineCategoryLabel(line.category)) },
                        )
                        OutlinedTextField(
                            value = line.amountInput,
                            onValueChange = { input ->
                                // B-007 pattern: ASCII + Bengali digits both parse in the VM.
                                viewModel.setPaymentLineAmount(index, input.filter { it.isDigit() || it in '০'..'৯' || it == '.' })
                            },
                            label = { Text(stringResource(R.string.paid_amount)) },
                            placeholder = {
                                Text(
                                    if (cartState.paymentLines.size == 1) {
                                        NumberFormatter.formatMoney(cartState.totalAmount, DigitStyle.BANGLA)
                                    } else {
                                        "০"
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        if (cartState.paymentLines.size > 1) {
                            IconButton(onClick = { viewModel.removePaymentLine(index) }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_payment_line))
                            }
                        }
                    }
                    if (line.category == PaymentLineCategory.MOBILE) {
                        // Provider selector (D87 chips pattern — always-visible, tap to pick).
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MfsProvider.entries.forEach { provider ->
                                FilterChip(
                                    selected = line.provider == provider,
                                    onClick = { viewModel.setPaymentLineProvider(index, provider) },
                                    label = { Text(mfsProviderLabel(provider)) },
                                )
                            }
                        }
                    }
                }
                // Add another payment method (one line per category)
                val present = cartState.paymentLines.map { it.category }.toSet()
                val addable = PaymentLineCategory.entries
                    .filter { it != PaymentLineCategory.DUE && it !in present }
                if (addable.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        addable.forEach { category ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.addPaymentLine(category) },
                                label = {
                                    Text("+ " + paymentLineCategoryLabel(category))
                                },
                            )
                        }
                    }
                }
                // P12: বাকি-remainder preview (posts to the selected customer's খাতা)
                if (cartState.dueAmount > 0.01) {
                    Text(
                        text = stringResource(R.string.remaining_due_preview) + " " +
                            NumberFormatter.formatMoney(cartState.dueAmount, DigitStyle.BANGLA),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
                // D93: overpayment preview — named customer → the excess becomes a
                // খাতা জমা (no error); walk-in → meaningful block hint (no khata target).
                if (cartState.overpaymentAmount > 0.01) {
                    if (cartState.selectedCustomer != null) {
                        Text(
                            text = stringResource(R.string.khata_advance_preview) + " " +
                                NumberFormatter.formatMoney(cartState.overpaymentAmount, DigitStyle.BANGLA),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.walk_in_overpay_error),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        )
                    }
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

                // B-008: standard full-width primary button (maroon/white via theme
                // primary) — replaces the old FloatingActionButton whose M3 default
                // primaryContainer token (#FFD7D7) rendered as a pink pill that
                // matched neither the brand nor the app's established button style.
                Button(
                    onClick = { showCheckoutConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.checkout))
                }
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
                state = customerSearchState,
                onSearch = { query -> viewModel.searchCustomers(tenantId, query) },
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
                        tenantId = tenantId, // D86: explicit write tenant
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
    // U-001/D87: search-as-filter — the full book list loads on open (blank query
    // = getBooks per repo contract); typing narrows it. The sheet is never blank.
    LaunchedEffect(Unit) { onSearch("") }
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
        when (val s = state) {
            is BookSearchState.Loading -> {
                Row(
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) { CircularProgressIndicator() }
            }
            is BookSearchState.Error -> {
                Text(
                    s.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp),
                )
            }
            is BookSearchState.Success -> {
                if (s.books.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_books_found),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.height(400.dp)) {
                        items(s.books) { book ->
                            Card(onClick = { onBookSelected(book) }, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(book.titleBn, fontWeight = FontWeight.Bold)
                                        val subtitle = buildString {
                                            append(book.author)
                                            if (book.classLevel.isNotBlank()) append(" · ${book.classLevel}")
                                        }
                                        Text(subtitle, style = MaterialTheme.typography.bodySmall)
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
            }
            BookSearchState.Idle -> {} // unreachable: LaunchedEffect fires onSearch("") on open
        }
    }
}

@Composable
private fun CustomerPickerSheet(
    state: CustomerSearchState,
    onSearch: (String) -> Unit,
    onCustomerSelected: (KhataCustomer) -> Unit,
    onWalkInSelected: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    // U-001/D87: search-as-filter — the full active-customer list loads on open
    // (blank query = getCustomers per repo contract); typing narrows it.
    LaunchedEffect(Unit) { onSearch("") }
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
        when (val s = state) {
            is CustomerSearchState.Loading -> {
                Row(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) { CircularProgressIndicator() }
            }
            is CustomerSearchState.Error -> {
                Text(
                    s.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp),
                )
            }
            is CustomerSearchState.Success -> {
                if (s.customers.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_customers_found),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.height(300.dp)) {
                        items(s.customers) { customer ->
                            Card(onClick = { onCustomerSelected(customer) }, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(customer.nameBn, fontWeight = FontWeight.Bold)
                                    customer.address?.let { if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodySmall) }
                                    customer.phone?.let { if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                        }
                    }
                }
            }
            CustomerSearchState.Idle -> {} // unreachable: LaunchedEffect fires onSearch("") on open
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
    PaymentMethod.NAGAD -> "নগদ (Nagad)"
    PaymentMethod.CREDIT -> "বাকি"
    // P12/D92: summary values for multi-line bills
    PaymentMethod.BANK -> "ব্যাংক"
    PaymentMethod.MOBILE -> "মোবাইল ব্যাংকিং"
}

/** P12/D92: POS editor labels for the payment-line categories. */
private fun paymentLineCategoryLabel(category: PaymentLineCategory): String = when (category) {
    PaymentLineCategory.CASH -> "নগদ"
    PaymentLineCategory.BANK -> "ব্যাংক"
    PaymentLineCategory.MOBILE -> "মোবাইল ব্যাংকিং"
    PaymentLineCategory.DUE -> "বাকি"
}

/** P12/D92: mobile-banking provider labels (নগদ disambiguated from cash). */
private fun mfsProviderLabel(provider: MfsProvider): String = when (provider) {
    MfsProvider.BKASH -> "বিকাশ"
    MfsProvider.NAGAD -> "নগদ (Nagad)"
    MfsProvider.ROCKET -> "রকেট"
    MfsProvider.UPAY -> "উপায়"
    MfsProvider.OTHER -> "অন্যান্য"
}
