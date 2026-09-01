package com.boikhata.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.core.domain.model.CashCloseReport
import com.boikhata.core.domain.repository.CashCloseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

/**
 * D36: CashCloseViewModel — the daily "আজকের হিসাব".
 * Loads today's bills + expenses + cashbook, computes the close with the
 * owner-overridable MFS fee rate and counted cash.
 */
@HiltViewModel
class CashCloseViewModel @Inject constructor(
    private val cashCloseRepository: CashCloseRepository,
) : ViewModel() {

    private val _closeState = MutableStateFlow<CashCloseState>(CashCloseState.Loading)
    val closeState: StateFlow<CashCloseState> = _closeState.asStateFlow()

    // Owner-overridable inputs
    private val _mfsFeeRate = MutableStateFlow(0.0)
    val mfsFeeRate: StateFlow<Double> = _mfsFeeRate.asStateFlow()

    private val _countedCash = MutableStateFlow(0.0)
    val countedCash: StateFlow<Double> = _countedCash.asStateFlow()

    private var currentTenantId = "t_1"

    fun loadClose(tenantId: String) {
        currentTenantId = tenantId
        computeClose()
    }

    fun setMfsFeeRate(rate: Double) {
        _mfsFeeRate.value = rate
        computeClose()
    }

    fun setCountedCash(amount: Double) {
        _countedCash.value = amount
        computeClose()
    }

    private fun computeClose() {
        viewModelScope.launch {
            _closeState.value = CashCloseState.Loading
            try {
                val cal = Calendar.getInstance(TimeZone.getDefault())
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfDay = cal.timeInMillis
                val endOfDay = startOfDay + 24L * 60 * 60 * 1000

                val report = cashCloseRepository.getDailyClose(
                    tenantId = currentTenantId,
                    startOfDay = startOfDay,
                    endOfDay = endOfDay,
                    mfsFeeRate = _mfsFeeRate.value,
                    countedCash = _countedCash.value,
                )
                _closeState.value = CashCloseState.Success(report)
            } catch (e: Exception) {
                _closeState.value = CashCloseState.Error(e.message ?: "ত্রুটি")
            }
        }
    }
}

sealed interface CashCloseState {
    data object Loading : CashCloseState
    data class Success(val report: CashCloseReport) : CashCloseState
    data class Error(val message: String) : CashCloseState
}
