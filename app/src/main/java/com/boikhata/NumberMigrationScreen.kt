package com.boikhata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boikhata.core.domain.pilot.NumberMigrationPolicy

@Composable
fun NumberMigrationScreen(onSignOut: () -> Unit) {
    var number by remember { mutableStateOf("") }
    var state by remember { mutableStateOf(NumberMigrationPolicy.State.IDLE) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.number_migration_title))
        OutlinedTextField(
            value = number,
            onValueChange = { number = it },
            label = { Text(stringResource(R.string.new_phone_number)) },
            enabled = state == NumberMigrationPolicy.State.IDLE,
        )
        when (state) {
            NumberMigrationPolicy.State.IDLE -> Button(
                onClick = { if (number.isNotBlank()) state = NumberMigrationPolicy.State.OTP_VERIFIED },
            ) { Text(stringResource(R.string.send_otp)) }
            NumberMigrationPolicy.State.OTP_VERIFIED -> {
                Text(stringResource(R.string.migration_otp_sent))
                Button(onClick = { state = NumberMigrationPolicy.State.REBOUND }) {
                    Text(stringResource(R.string.confirm_number))
                }
            }
            NumberMigrationPolicy.State.REBOUND -> {
                Text(stringResource(R.string.migration_vendor_approval))
                Button(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
            }
            NumberMigrationPolicy.State.CLAIMS_TRANSFERRED -> Text(stringResource(R.string.migration_complete))
        }
    }
}
