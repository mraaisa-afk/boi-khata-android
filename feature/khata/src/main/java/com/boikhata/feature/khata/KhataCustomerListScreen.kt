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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.boikhata.core.domain.model.KhataCustomer
import com.boikhata.core.domain.model.KhataCustomerDue
import com.boikhata.feature.khata.R

/**
 * P2a: Khata customer list — name+area keyed, with Bengali search.
 * Blueprint §7.4: নাম+এলাকা-কী (ফোন ঐচ্ছিক).
 */
@Composable
fun KhataCustomerListScreen(
    tenantId: String,
    onAddCustomer: () -> Unit,
    onCustomerClick: (String) -> Unit,
    viewModel: KhataViewModel = hiltViewModel(),
) {
    LaunchedEffect(tenantId) {
        viewModel.loadCustomers(tenantId)
    }

    val uiState by viewModel.listState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCustomer) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_customer))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                placeholder = { Text(stringResource(R.string.search_customers)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (val state = uiState) {
                is KhataListUiState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) { CircularProgressIndicator() }
                }
                is KhataListUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                is KhataListUiState.Success -> {
                    if (state.customers.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_customers),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.customers) { customer ->
                                val due = state.dueList.find { it.customer.id == customer.id }
                                CustomerCard(customer, due) { onCustomerClick(customer.id) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerCard(customer: KhataCustomer, due: KhataCustomerDue?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.nameBn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                customer.address?.let { if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodySmall) }
                customer.phone?.let { if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            if (due != null) {
                val bucketColor = when (due.agingBucket) {
                    "GREEN" -> Color(0xFF2E7D32)
                    "YELLOW" -> Color(0xFFF57F17)
                    "RED" -> Color(0xFFC62828)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = NumberFormatter.formatMoney(due.dueAmount, DigitStyle.BANGLA),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = bucketColor,
                    )
                    if (due.ageDays > 0) {
                        Text(
                            text = stringResource(R.string.age_days, due.ageDays.toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = bucketColor,
                        )
                    }
                }
            }
        }
    }
}
