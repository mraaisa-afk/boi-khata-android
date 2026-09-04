package com.boikhata

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.boikhata.core.database.dao.BillDao
import com.boikhata.core.database.dao.BookDao
import com.boikhata.core.domain.pilot.TrialPolicy
import com.boikhata.core.domain.repository.TrialRedemptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrialViewModel @Inject constructor(
    private val billDao: BillDao,
    private val bookDao: BookDao,
    private val redemptionRepository: TrialRedemptionRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {
    var status by mutableStateOf<TrialStatus?>(null)
        private set

    fun ensureTrial(tenantId: String, phone: String) {
        viewModelScope.launch {
            val device = Settings.Secure.getString(
                getApplicationContext().contentResolver,
                Settings.Secure.ANDROID_ID,
            ) ?: "unknown-device"
            if (redemptionRepository.getTrialStartedAt(tenantId) == null) {
                redemptionRepository.redeemIfEligible(tenantId, device, phone, System.currentTimeMillis())
            }
            refresh(tenantId)
        }
    }

    fun refresh(tenantId: String) {
        viewModelScope.launch {
            val started = redemptionRepository.getTrialStartedAt(tenantId) ?: return@launch
            val usage = TrialPolicy.Usage(billDao.countForTenant(tenantId), bookDao.countForTenant(tenantId))
            val decision = TrialPolicy.evaluate(started, System.currentTimeMillis(), usage)
            status = TrialStatus(started, usage, decision)
        }
    }

    private fun getApplicationContext(): Context = appContext
}

data class TrialStatus(
    val startedAt: Long,
    val usage: TrialPolicy.Usage,
    val decision: TrialPolicy.Decision,
)

@Composable
fun SettingsScreen(
    tenantId: String,
    phone: String,
    liteMode: Boolean,
    onLiteModeChange: (Boolean) -> Unit,
    onSpeakSetup: () -> Unit,
    onShareCopy: () -> Unit,
    onMigration: () -> Unit,
    onUpgrade: () -> Unit,
    viewModel: TrialViewModel = hiltViewModel(),
) {
    androidx.compose.runtime.LaunchedEffect(tenantId, phone) { viewModel.ensureTrial(tenantId, phone) }
    val status = viewModel.status
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.settings_title))
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.trial_title))
            if (status == null) Text(stringResource(R.string.trial_loading))
            else {
                Text(stringResource(R.string.trial_usage, status.usage.bills, TrialPolicy.MAX_BILLS, status.usage.books, TrialPolicy.MAX_BOOKS))
                Text(if (status.decision.readOnly) stringResource(R.string.trial_expired) else stringResource(R.string.trial_active))
                Button(onClick = onUpgrade) {
                    Text(stringResource(R.string.trial_upgrade))
                }
            }
        } }
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.lite_ui))
            Switch(checked = liteMode, onCheckedChange = onLiteModeChange)
            Button(onClick = onSpeakSetup) { Text(stringResource(R.string.repeat_voice_setup)) }
            Button(onClick = onShareCopy) { Text(stringResource(R.string.share_monthly_copy)) }
            Button(onClick = onMigration) { Text(stringResource(R.string.number_changed)) }
        } }
    }
}

fun shareMonthlyCopy(context: Context) {
    val path = context.getSharedPreferences(MonthlyDataCopyWorker.PREFS, Context.MODE_PRIVATE)
        .getString(MonthlyDataCopyWorker.LAST_COPY_PATH, null) ?: return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", java.io.File(path))
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, context.getString(R.string.share_monthly_copy)))
}
