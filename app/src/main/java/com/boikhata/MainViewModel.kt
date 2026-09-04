package com.boikhata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.core.domain.enums.Role
import com.boikhata.core.domain.model.CloudUser
import com.boikhata.core.domain.model.LicenseSyncResult
import com.boikhata.core.domain.repository.AuthRepository
import com.boikhata.core.domain.repository.LicenseSyncRepository
import com.boikhata.core.domain.repository.TenantInfoRepository
import com.boikhata.core.domain.repository.TenantRebindRepository
import com.boikhata.core.domain.session.ClaimsSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * D44: MainViewModel — drives the auth state machine.
 * Unauthenticated → LoginScreen | PendingActivation → PendingScreen | Authenticated → Main.
 * On first claims login, runs the one-time tenant rebind BEFORE showing the main screen.
 *
 * P4b inherited: shop name fetched from tenants Firestore doc (replaces phone placeholder).
 * P4b inherited: direct license-refresh from banner button (refreshLicense() method).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tenantRebindRepository: TenantRebindRepository,
    private val licenseSyncRepository: LicenseSyncRepository,
    private val tenantInfoRepository: TenantInfoRepository,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkInitialAuth()
    }

    private fun checkInitialAuth() {
        viewModelScope.launch {
            if (!authRepository.isSignedIn()) {
                _authState.value = AuthState.Unauthenticated
                return@launch
            }
            // Cached Firebase user — check for claims
            val user = authRepository.getCurrentCloudUser()
            resolveAuthState(user)
        }
    }

    /**
     * After OTP verification succeeds, check the cloud user for claims.
     */
    fun onOtpVerified() {
        viewModelScope.launch {
            val user = authRepository.getCurrentCloudUser()
            resolveAuthState(user)
        }
    }

    private suspend fun resolveAuthState(user: CloudUser?) {
        val state = ClaimsSession.deriveState(user)
        when (state) {
            is ClaimsSession.State.Unauthenticated -> {
                _authState.value = AuthState.Unauthenticated
            }
            is ClaimsSession.State.PendingActivation -> {
                _authState.value = AuthState.PendingActivation(state.phone)
            }
            is ClaimsSession.State.AuthenticatedWithClaims -> {
                // D41: One-time tenant rebind — migrate "t_1" rows to claims tenantId
                val oldTenantId = "t_1" // the local seed tenant
                if (ClaimsSession.needsRebind(oldTenantId, state.tenantId)) {
                    tenantRebindRepository.rebind(oldTenantId, state.tenantId)
                }
                // D42: License sync (OWNER only, offline fallback)
                val syncResult = licenseSyncRepository.syncLicense(state.tenantId, state.role)
                // P4b inherited: fetch shop name from tenants Firestore doc
                val shopName = tenantInfoRepository.fetchShopName(state.tenantId) ?: state.phone
                _authState.value = AuthState.Authenticated(
                    tenantId = state.tenantId,
                    role = state.role,
                    phone = state.phone,
                    shopName = shopName,
                    licenseSyncResult = syncResult,
                )
            }
            is ClaimsSession.State.Authenticating -> {
                // Should not happen here — handled by LoginViewModel
            }
        }
    }

    /**
     * P4b inherited: Direct license-refresh call from the banner button.
     * Re-triggers license sync (OWNER only) and updates the auth state.
     */
    fun refreshLicense() {
        viewModelScope.launch {
            val current = _authState.value as? AuthState.Authenticated ?: return@launch
            val syncResult = licenseSyncRepository.syncLicense(current.tenantId, current.role)
            _authState.value = current.copy(licenseSyncResult = syncResult)
        }
    }

    fun retryPendingActivation() {
        viewModelScope.launch {
            val user = authRepository.getCurrentCloudUser()
            resolveAuthState(user)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthState.Unauthenticated
        }
    }
}

sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class PendingActivation(val phone: String) : AuthState()
    data class Authenticated(
        val tenantId: String,
        val role: Role,
        val phone: String,
        val shopName: String,
        val licenseSyncResult: LicenseSyncResult,
    ) : AuthState()
}
