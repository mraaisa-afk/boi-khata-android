package com.boikhata.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.core.designsystem.format.DigitStyle
import com.boikhata.core.designsystem.format.NumberFormatter
import com.boikhata.core.domain.model.HomeData
import com.boikhata.core.domain.model.KhataCustomerDue
import com.boikhata.feature.home.R

/**
 * P1 item 4: খাতা-প্রথম হোম (দেনা-তালিকা + আজকের বিক্রি + top-৫).
 * Blueprint §2: খাতা-প্রথম হোম = আজ কার দেনা বাকি + আজকের বিক্রি — ড্যাশবোর্ড-মেট্রিক নয়।
 * Room-driven, zero mock.
 */
@Composable
fun HomeScreen(
    tenantId: String,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    LaunchedEffect(tenantId) {
        viewModel.loadHome(tenantId)
    }

    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is HomeUiState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        is HomeUiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = state.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is HomeUiState.Success -> {
            HomeContent(state.data)
        }
    }
}

@Composable
private fun HomeContent(data: HomeData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TridentCard(
                title = stringResource(R.string.total_due),
                value = NumberFormatter.formatMoney(data.totalDue, DigitStyle.BANGLA),
                subtitle = stringResource(R.string.due_customers, data.dueCustomerCount),
            )
        }
        item {
            TridentCard(
                title = stringResource(R.string.today_sales),
                value = NumberFormatter.formatMoney(data.todaySalesTotal, DigitStyle.BANGLA),
                subtitle = stringResource(R.string.today_bills, data.todayBillCount),
            )
        }
        item {
            Text(
                text = stringResource(R.string.top_due),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
        }
        if (data.topDueCustomers.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_due),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            items(data.topDueCustomers) { due ->
                DueCustomerCard(due)
            }
        }
    }
}

@Composable
private fun TridentCard(title: String, value: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DueCustomerCard(due: KhataCustomerDue) {
    val bucketColor = when (due.agingBucket) {
        "GREEN" -> 0xFF2E7D32
        "YELLOW" -> 0xFFF57F17
        "RED" -> 0xFFC62828
        else -> 0xFF757575
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = due.customer.nameBn, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(R.string.age_days, due.ageDays.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color(bucketColor),
                )
            }
            Text(
                text = NumberFormatter.formatMoney(due.dueAmount, DigitStyle.BANGLA),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color(bucketColor),
            )
        }
    }
}
