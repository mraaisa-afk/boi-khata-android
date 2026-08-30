package com.boikhata.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.core.domain.enums.BookCategory
import com.boikhata.core.domain.enums.BookCondition
import com.boikhata.feature.catalog.R

/**
 * P2a: Add/Edit book screen — fully offline local entry.
 * Blueprint §7.2: ISBN, class, subject, edition, publisher, MRP + condition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAddEditScreen(
    tenantId: String,
    bookId: String?,
    onBack: () -> Unit,
    viewModel: CatalogViewModel = hiltViewModel(),
) {
    val isEdit = bookId != null

    var isbn by remember { mutableStateOf("") }
    var titleBn by remember { mutableStateOf("") }
    var titleEn by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var classLevel by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var editionYear by remember { mutableStateOf("2026") }
    var purchasePrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var initialStock by remember { mutableStateOf("0") }
    var lowStockThreshold by remember { mutableStateOf("5") }
    var category by remember { mutableStateOf(BookCategory.TEXTBOOK) }
    var condition by remember { mutableStateOf(BookCondition.NEW) }
    var isActive by remember { mutableStateOf(true) }

    // Load existing book for edit
    LaunchedEffect(bookId) {
        if (bookId != null) {
            // The ViewModel loads from the book repository
            // We read from the current success state
            viewModel.uiState.collect { state ->
                if (state is CatalogUiState.Success) {
                    val book = state.books.find { it.id == bookId }
                    if (book != null) {
                        isbn = book.isbn ?: ""
                        titleBn = book.titleBn
                        titleEn = book.titleEn ?: ""
                        author = book.author
                        publisher = book.publisher
                        classLevel = book.classLevel
                        subject = book.subject
                        editionYear = book.editionYear.toString()
                        purchasePrice = book.purchasePrice.toString()
                        sellingPrice = book.sellingPrice.toString()
                        initialStock = book.initialStock.toString()
                        lowStockThreshold = book.lowStockThreshold.toString()
                        category = book.category
                        condition = book.condition
                        isActive = book.isActive
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) stringResource(R.string.edit_book) else stringResource(R.string.add_book)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = titleBn,
                onValueChange = { titleBn = it },
                label = { Text(stringResource(R.string.title_bn)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = titleEn,
                onValueChange = { titleEn = it },
                label = { Text(stringResource(R.string.title_en)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text(stringResource(R.string.author)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = publisher,
                onValueChange = { publisher = it },
                label = { Text(stringResource(R.string.publisher)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = isbn,
                onValueChange = { isbn = it },
                label = { Text(stringResource(R.string.isbn)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = classLevel,
                    onValueChange = { classLevel = it },
                    label = { Text(stringResource(R.string.class_level)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(stringResource(R.string.subject)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editionYear,
                    onValueChange = { editionYear = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.edition_year)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                CategoryDropdown(category) { category = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = purchasePrice,
                    onValueChange = { purchasePrice = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.purchase_price)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { sellingPrice = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.selling_price)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = initialStock,
                    onValueChange = { initialStock = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.initial_stock)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = lowStockThreshold,
                    onValueChange = { lowStockThreshold = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.low_stock_threshold)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            ConditionDropdown(condition) { condition = it }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val parsedEdition = editionYear.toIntOrNull() ?: 2026
                    val parsedPurchase = purchasePrice.toDoubleOrNull() ?: 0.0
                    val parsedSelling = sellingPrice.toDoubleOrNull() ?: 0.0
                    val parsedStock = initialStock.toIntOrNull() ?: 0
                    val parsedLowStock = lowStockThreshold.toIntOrNull() ?: 5

                    if (isEdit && bookId != null) {
                        viewModel.updateBook(
                            id = bookId,
                            isbn = isbn.ifBlank { null },
                            titleBn = titleBn,
                            titleEn = titleEn.ifBlank { null },
                            author = author,
                            publisher = publisher,
                            classLevel = classLevel,
                            subject = subject,
                            editionYear = parsedEdition,
                            category = category,
                            condition = condition,
                            purchasePrice = parsedPurchase,
                            sellingPrice = parsedSelling,
                            lowStockThreshold = parsedLowStock,
                            isActive = isActive,
                            onDone = onBack,
                        )
                    } else {
                        viewModel.addBook(
                            isbn = isbn.ifBlank { null },
                            titleBn = titleBn,
                            titleEn = titleEn.ifBlank { null },
                            author = author,
                            publisher = publisher,
                            classLevel = classLevel,
                            subject = subject,
                            editionYear = parsedEdition,
                            category = category,
                            condition = condition,
                            purchasePrice = parsedPurchase,
                            sellingPrice = parsedSelling,
                            initialStock = parsedStock,
                            lowStockThreshold = parsedLowStock,
                            onDone = onBack,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = titleBn.isNotBlank() && author.isNotBlank(),
            ) {
                Text(if (isEdit) stringResource(R.string.save) else stringResource(R.string.add_book))
            }
        }
    }
}

@Composable
private fun CategoryDropdown(selected: BookCategory, onSelect: (BookCategory) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = categoryLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.category)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            interactionSource = remember { MutableInteractionSource() }
                .also { source ->
                    LaunchedEffect(source) {
                        source.interactions.collect { interaction ->
                            if (interaction is PressInteraction.Release) expanded = true
                        }
                    }
                },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BookCategory.entries.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(categoryLabel(cat)) },
                    onClick = { onSelect(cat); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun ConditionDropdown(selected: BookCondition, onSelect: (BookCondition) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = conditionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.condition)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            interactionSource = remember { MutableInteractionSource() }
                .also { source ->
                    LaunchedEffect(source) {
                        source.interactions.collect { interaction ->
                            if (interaction is PressInteraction.Release) expanded = true
                        }
                    }
                },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            BookCondition.entries.forEach { cond ->
                DropdownMenuItem(
                    text = { Text(conditionLabel(cond)) },
                    onClick = { onSelect(cond); expanded = false },
                )
            }
        }
    }
}

private fun categoryLabel(cat: BookCategory): String = when (cat) {
    BookCategory.TEXTBOOK -> "পাঠ্যবই"
    BookCategory.GENERAL -> "সাধারণ"
    BookCategory.STATIONERY -> "স্টেশনারি"
    BookCategory.OTHER -> "অন্যান্য"
}

private fun conditionLabel(cond: BookCondition): String = when (cond) {
    BookCondition.NEW -> "নতুন"
    BookCondition.USED -> "পুরনো"
    BookCondition.DAMAGED -> "ক্ষতিগ্রস্ত"
}
