package com.boikhata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * D79 (Locked Design Spec — HomeScreen v2, §2.6): the "আরও" tab is a hub for every
 * destination that left the bottom bar when POS became the central FAB.
 * G19: all labels from strings.xml. G23: this is a normal screen, not a drawer.
 */
private val ColorPrimaryMaroon = Color(0xFF800000)
private val ColorSurfaceIvory = Color(0xFFFDFAF6)

private data class MoreEntry(val labelRes: Int, val route: String)

@Composable
fun MoreScreen(onEntryClick: (String) -> Unit) {
    val entries = listOf(
        MoreEntry(R.string.more_reports, "reports"),
        MoreEntry(R.string.more_cash_close, "cash_close"),
        MoreEntry(R.string.more_expense, "expense"),
        MoreEntry(R.string.more_supplier, "supplier"),
        MoreEntry(R.string.more_bill_history, "bill_history"),
        MoreEntry(R.string.more_mela, "mela"),
        MoreEntry(R.string.more_subscription, "subscription"),
        MoreEntry(R.string.more_settings, "settings"),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp), // screenPadding token
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.more_title),
                style = MaterialTheme.typography.headlineSmall,
                color = ColorPrimaryMaroon,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(entries) { entry ->
            Card(
                onClick = { onEntryClick(entry.route) },
                shape = RoundedCornerShape(16.dp), // D71 §3 shape token
                colors = CardDefaults.cardColors(containerColor = ColorSurfaceIvory),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp), // primaryTouchTarget token
            ) {
                Text(
                    text = stringResource(entry.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                )
            }
        }
    }
}
