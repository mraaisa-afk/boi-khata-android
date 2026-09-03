package com.boikhata.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.core.domain.cloud.SubscriptionRecord
import com.boikhata.core.domain.enums.Role
import com.boikhata.core.domain.repository.SubscriptionResult
import com.boikhata.core.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * D48: SubscriptionViewModel — drives the subscription screen.
 * Manual bKash payment record (PENDING-only, OWNER-gated).
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    fun recordPayment(tenantId: String, role: Role, amount: Double, trxId: String, note: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, success = false)
            val result = subscriptionRepository.recordPayment(tenantId, role, amount, trxId, note)
            when (result) {
                is SubscriptionResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, success = true, error = null,
                    )
                }
                is SubscriptionResult.NotOwner -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, error = "শুধুমাত্র OWNER পেমেন্ট রেকর্ড করতে পারবেন",
                    )
                }
                is SubscriptionResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, error = result.message,
                    )
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = SubscriptionUiState()
    }
}

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
)
