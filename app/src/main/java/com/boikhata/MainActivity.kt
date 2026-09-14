package com.boikhata

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.boikhata.core.designsystem.theme.BoiKhataTheme
import com.boikhata.core.domain.enums.Role
import com.boikhata.core.domain.license.LicensePolicy
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

/**
 * P4a: MainActivity — routes between Login / PendingActivation / Main based on AuthState.
 * The local PIN session (SessionManager) coexists with the cloud session;
 * cloud provides identity, PIN provides device access.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // D82: Force Bengali locale AFTER super.onCreate() (Firebase/Hilt already initialized)
        // but BEFORE setContent() (so all Compose composables render in Bengali).
        // At this point Application is fully built — Firebase/Hilt are done initializing.
        // updateConfiguration() here only affects this Activity's resource context,
        // not the Application's already-cached resources.
        val bnBD = Locale("bn", "BD")
        Locale.setDefault(bnBD)
        val config = Configuration(resources.configuration)
        config.setLocale(bnBD)
        resources.updateConfiguration(config, resources.displayMetrics)
        
        setContent {
            val preferences = remember { getSharedPreferences("boi_khata_display", MODE_PRIVATE) }
            var liteMode by remember { mutableStateOf(preferences.getBoolean("lite_mode", false)) }
            BoiKhataTheme(liteMode = liteMode) {
                val mainViewModel: MainViewModel = hiltViewModel()
                val trialViewModel: TrialViewModel = hiltViewModel()
                val demoResetViewModel: DemoResetViewModel = hiltViewModel()
                val authState by mainViewModel.authState.collectAsState()

                when (val state = authState) {
                    is AuthState.Loading -> {
                        // Splash / loading
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    is AuthState.Unauthenticated -> {
                        LoginScreen(
                            onOtpVerified = { mainViewModel.onOtpVerified() },
                        )
                    }
                    is AuthState.PendingActivation -> {
                        PendingActivationScreen(
                            phone = state.phone,
                            onRetry = { mainViewModel.retryPendingActivation() },
                            onSignOut = { mainViewModel.signOut() },
                        )
                    }
                    is AuthState.Authenticated -> {
                        LaunchedEffect(state.tenantId, state.phone) {
                            trialViewModel.ensureTrial(state.tenantId, state.phone)
                            MonthlyDataCopyScheduler(this@MainActivity).enqueue(
                                tenantId = state.tenantId,
                                shopName = state.shopName,
                            )
                        }
                        Column(modifier = Modifier.fillMaxSize()) {
                            // D43: License banner (driven by synced state, never blocks reads)
                            val now = remember { System.currentTimeMillis() }
                            val daysUntilSoftLock = remember(state) {
                                // Approximate: 30 days from last sync if in grace
                                30L
                            }
                            LicenseBanner(
                                syncResult = state.licenseSyncResult,
                                daysUntilSoftLock = daysUntilSoftLock,
                                isOwner = state.role == Role.OWNER,
                                onRefresh = {
                                    // P4b inherited: direct license-refresh call from banner button
                                    mainViewModel.refreshLicense()
                                },
                            )
                            // Main app screen with claims tenantId
                            BoiKhataMainScreen(
                                tenantId = state.tenantId,
                                shopName = state.shopName,
                                role = state.role,
                                phone = state.phone,
                                liteMode = liteMode,
                                onLiteModeChange = {
                                    liteMode = it
                                    preferences.edit().putBoolean("lite_mode", it).apply()
                                },
                                onSignOut = { mainViewModel.signOut() },
                                onDemoReset = { demoResetViewModel.reset { } },
                            )
                        }
                    }
                }
            }
        }
    }
}
