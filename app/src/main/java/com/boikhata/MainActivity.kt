package com.boikhata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
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

/**
 * P4a: MainActivity — routes between Login / PendingActivation / Main based on AuthState.
 * The local PIN session (SessionManager) coexists with the cloud session;
 * cloud provides identity, PIN provides device access.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BoiKhataTheme {
                val mainViewModel: MainViewModel = hiltViewModel()
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
                                    // Re-trigger license sync via the ViewModel
                                    // (the ViewModel handles it on next resolveAuthState)
                                },
                            )
                            // Main app screen with claims tenantId
                            BoiKhataMainScreen(
                                tenantId = state.tenantId,
                                shopName = state.shopName,
                            )
                        }
                    }
                }
            }
        }
    }
}
