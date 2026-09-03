package com.boikhata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boikhata.core.domain.enums.LicenseState
import com.boikhata.core.domain.model.LicenseSyncResult

/**
 * D43: LicenseBanner — reflects the synced license state.
 * Never blocks reads/exports (never-lock rule). Write-gating happens at the repo layer.
 */
@Composable
fun LicenseBanner(
    syncResult: LicenseSyncResult?,
    daysUntilSoftLock: Long,
    isOwner: Boolean,
    onRefresh: () -> Unit,
) {
    if (syncResult == null) return

    val (bgColor, contentColor, text) = when (syncResult) {
        is LicenseSyncResult.Synced -> when (syncResult.state) {
            LicenseState.FULL -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), stringResource(R.string.license_active))
            LicenseState.PAID_UNVERIFIED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), stringResource(R.string.license_active))
            LicenseState.GRACE -> Triple(Color(0xFFFFF9C4), Color(0xFFF57F17), stringResource(R.string.license_grace, daysUntilSoftLock))
            LicenseState.SOFT_LOCKED -> Triple(Color(0xFFFFF3E0), Color(0xE65100), stringResource(R.string.license_soft_locked))
            LicenseState.SUSPENDED -> Triple(Color(0xFFFFCDD2), Color(0xC62828), stringResource(R.string.license_suspended))
        }
        is LicenseSyncResult.Offline -> Triple(Color(0xFFECEFF1), Color(0xFF546E7A), stringResource(R.string.license_offline))
        is LicenseSyncResult.NotOwner -> Triple(Color.Transparent, Color.Transparent, "")
        is LicenseSyncResult.MissingDoc -> Triple(Color(0xFFFFF9C4), Color(0xFFF57F17), stringResource(R.string.license_grace, daysUntilSoftLock))
        is LicenseSyncResult.Error -> Triple(Color(0xFFECEFF1), Color(0xFF546E7A), stringResource(R.string.license_offline))
    }

    if (text.isBlank()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
        )
        if (isOwner) {
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onRefresh) {
                Text(stringResource(R.string.license_refresh), color = contentColor)
            }
        }
    }
}
