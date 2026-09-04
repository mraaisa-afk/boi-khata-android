package com.boikhata.feature.melamode

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.boikhata.core.domain.enums.StockChangeReason
import com.boikhata.core.domain.model.Book
import com.boikhata.feature.melamode.R

/**
 * P5: Mela mode screen — seasonal/book-fair session lifecycle + stock-cycle (MELA_IN/MELA_OUT),
 * low-stock soft-reservation warnings, oversell-reconciliation alerts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MelaScreen(
    tenantId: String,
    viewModel: MelaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    var showStart by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tenantId) { viewModel.load(tenantId) }

    Scaffold(
        floatingActionButton = {
            val s = state
            if (s is MelaUiState.Success && s.session != null) {
                FloatingActionButton(onClick = { showMove = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.move_stock))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is MelaUiState.Loading -> Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) { CircularProgressIndicator() }

                is MelaUiState.Error -> Text(
                    s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)
                )

                is MelaUiState.Success -> MelaContent(
                    state = s,
                    onStart = { showStart = true },
                    onPause = { reason -> viewModel.pauseSession(reason, {}, onError = { errorMessage = it }) },
                    onResume = { viewModel.resumeSession({}, onError = { errorMessage = it }) },
                    onEnd = { viewModel.endSession({}, onError = { errorMessage = it }) },
                )
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

    if (showStart) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showStart = false }, sheetState = sheetState) {
            StartSessionSheet(
                onConfirm = { name, location ->
                    val now = System.currentTimeMillis()
                    viewModel.startSession(name, location, now, now + 21L * 24 * 60 * 60 * 1000,
                        onDone = { showStart = false }, onError = { errorMessage = it })
                },
                onCancel = { showStart = false },
            )
        }
    }

    if (showMove) {
        val sheetState = rememberModalBottomSheetState()
        val s = state
        val books = (s as? MelaUiState.Success)?.books ?: emptyList()
        ModalBottomSheet(onDismissRequest = { showMove = false }, sheetState = sheetState) {
            MoveStockSheet(
                books = books,
                onConfirm = { bookId, qty, direction ->
                    viewModel.moveStock(bookId, qty, direction,
                        onDone = { showMove = false }, onError = { errorMessage = it })
                },
                onCancel = { showMove = false },
            )
        }
    }
}

@Composable
private fun MelaContent(
    state: MelaUiState.Success,
    onStart: () -> Unit,
    onPause: (String?) -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        val session = state.session
        if (session == null) {
            Text(stringResource(R.string.no_active_session), modifier = Modifier.padding(16.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_session)) }
        } else {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(session.nameBn, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(session.location, style = MaterialTheme.typography.bodyMedium)
                    if (session.isPaused) {
                        Text(stringResource(R.string.paused), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (session.isPaused) {
                            Button(onClick = onResume) { Text(stringResource(R.string.resume_session)) }
                        } else {
                            Button(onClick = { onPause(null) }) { Text(stringResource(R.string.pause_session)) }
                        }
                        TextButton(onClick = onEnd) { Text(stringResource(R.string.end_session)) }
                    }
                }
            }
        }

        // Low-stock alerts (soft-reservation warnings)
        Text(stringResource(R.string.low_stock_alerts), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
        if (state.lowStock.isEmpty()) {
            Text(stringResource(R.string.no_low_stock), modifier = Modifier.padding(4.dp))
        } else {
            state.lowStock.forEach { alert ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(
                        "${alert.bookTitleBn} — মেলায় ${alert.melaStock} (≤${alert.softThreshold}) · দোকানে ${alert.atShop}",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        // Oversell reconciliation alerts
        Text(stringResource(R.string.oversell_alerts), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
        if (state.oversell.isEmpty()) {
            Text(stringResource(R.string.no_oversell), modifier = Modifier.padding(4.dp))
        } else {
            state.oversell.forEach { alert ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        "${alert.bookTitleBn} — ${stringResource(R.string.oversell_alerts)} ${alert.oversoldBy}",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        // Mela stock report
        Text(stringResource(R.string.mela_stock_report), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
        if (state.report.isEmpty()) {
            Text(stringResource(R.string.no_stock_moves), modifier = Modifier.padding(4.dp))
        } else {
            state.report.forEach { line ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(line.bookTitleBn, fontWeight = FontWeight.Bold)
                            Text("IN ${line.melaIn} · OUT ${line.melaOut} · নেট ${line.netStock}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${stringResource(R.string.at_mela)}: ${line.atMela}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun StartSessionSheet(
    onConfirm: (name: String, location: String) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.start_session), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.session_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text(stringResource(R.string.location)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            Button(onClick = { onConfirm(name, location) }, enabled = name.isNotBlank(), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun MoveStockSheet(
    books: List<Book>,
    onConfirm: (bookId: String, quantity: Int, direction: StockChangeReason) -> Unit,
    onCancel: () -> Unit,
) {
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var quantity by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(StockChangeReason.MELA_IN) }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.move_stock), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        // Book picker via chips (first few books) — a simple selector for the demo.
        if (books.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(200.dp)) {
                items(books.take(15)) { book ->
                    Card(onClick = { selectedBook = book }, modifier = Modifier.fillMaxWidth()) {
                        Text(book.titleBn, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Text(stringResource(R.string.no_stock_moves), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = direction == StockChangeReason.MELA_IN, onClick = { direction = StockChangeReason.MELA_IN }, label = { Text(stringResource(R.string.move_in)) })
            FilterChip(selected = direction == StockChangeReason.MELA_OUT, onClick = { direction = StockChangeReason.MELA_OUT }, label = { Text(stringResource(R.string.move_out)) })
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = quantity, onValueChange = { quantity = it.filter { c -> c.isDigit() } },
            label = { Text(stringResource(R.string.quantity)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            Button(
                onClick = { selectedBook?.let { onConfirm(it.id, quantity.toIntOrNull() ?: 0, direction) } },
                enabled = selectedBook != null && (quantity.toIntOrNull() ?: 0) > 0,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.save)) }
        }
    }
}
