package com.boikhata.feature.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boikhata.core.domain.cloud.SubscriptionRecord
import com.boikhata.core.domain.enums.Role

/**
 * D48: SubscriptionScreen — manual bKash payment record.
 * Firebase-Project-Context.md §5: customer pays ৳250/month via bKash to vendor's personal number.
 * TrxID is optional. Status is always PENDING (vendor runs renew.js to activate).
 * OWNER-only (rules deny non-OWNER create).
 */
@Composable
fun SubscriptionScreen(
    tenantId: String,
    role: Role,
) {
    val viewModel: SubscriptionViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var amountText by remember { mutableStateOf(SubscriptionRecord.MONTHLY_FEE.toString()) }
    var trxId by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.subscription_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        // Vendor bKash info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.subscription_bkash_info),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = SubscriptionRecord.VENDOR_BKASH,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.subscription_amount_info, SubscriptionRecord.MONTHLY_FEE.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Text(
            text = stringResource(R.string.subscription_cooling_off),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )

        // Amount field
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text(stringResource(R.string.subscription_amount_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )

        // TrxId field (optional)
        OutlinedTextField(
            value = trxId,
            onValueChange = { trxId = it },
            label = { Text(stringResource(R.string.subscription_trxid_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Note field (optional)
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(stringResource(R.string.subscription_note_label)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
        )

        // Error message
        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // Success message
        if (uiState.success) {
            Text(
                text = stringResource(R.string.subscription_success),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Submit button
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        } else {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        viewModel.recordPayment(tenantId, role, amount, trxId, note)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = role == Role.OWNER,
            ) {
                Text(stringResource(R.string.subscription_submit))
            }
            if (role != Role.OWNER) {
                Text(
                    text = stringResource(R.string.subscription_owner_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
