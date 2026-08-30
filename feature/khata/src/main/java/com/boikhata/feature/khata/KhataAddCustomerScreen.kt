package com.boikhata.feature.khata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.feature.khata.R

/**
 * P2a: Add customer screen — name+area keyed (phone optional).
 * Blueprint §7.4: নাম+এলাকা-কী (ফোন ঐচ্ছিক) — বাংলাদেশের খাতা-স্মৃতির আসল চাবি।
 * CONVENTIONS §4: khata_customers-তৈরি = OWNER-ONLY (data-layer gate).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataAddCustomerScreen(
    onBack: () -> Unit,
    viewModel: KhataViewModel = hiltViewModel(),
) {
    var nameBn by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var creditLimit by remember { mutableStateOf("5000") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_customer)) },
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
                value = nameBn,
                onValueChange = { nameBn = it },
                label = { Text(stringResource(R.string.customer_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(stringResource(R.string.area_address)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.phone_optional)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            OutlinedTextField(
                value = creditLimit,
                onValueChange = { creditLimit = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(stringResource(R.string.credit_limit)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Button(
                onClick = {
                    viewModel.addCustomer(
                        nameBn = nameBn,
                        phone = phone.ifBlank { null },
                        address = address.ifBlank { null },
                        creditLimit = creditLimit.toDoubleOrNull() ?: 0.0,
                        onDone = onBack,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = nameBn.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
