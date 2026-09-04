package com.boikhata.feature.melamode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.core.domain.enums.StockChangeReason
import com.boikhata.core.domain.model.Book
import com.boikhata.core.domain.model.LowStockAlert
import com.boikhata.core.domain.model.MelaSession
import com.boikhata.core.domain.model.MelaStockLine
import com.boikhata.core.domain.model.OversellAlert
import com.boikhata.core.domain.repository.BookRepository
import com.boikhata.core.domain.repository.MelaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MelaViewModel @Inject constructor(
    private val melaRepository: MelaRepository,
    private val bookRepository: BookRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<MelaUiState>(MelaUiState.Loading)
    val state: StateFlow<MelaUiState> = _state.asStateFlow()

    private var currentTenantId = "t_1"

    fun load(tenantId: String) {
        currentTenantId = tenantId
        viewModelScope.launch {
            _state.value = MelaUiState.Loading
            try {
                val session = melaRepository.getCurrentSession(tenantId)
                val lowStock = melaRepository.getLowStockAlerts(tenantId)
                val oversell = melaRepository.getOversellAlerts(tenantId)
                val report = melaRepository.getMelaStockReport(tenantId)
                val books = bookRepository.getBooks(tenantId)
                _state.value = MelaUiState.Success(
                    session = session,
                    lowStock = lowStock,
                    oversell = oversell,
                    report = report,
                    books = books,
                )
            } catch (e: Exception) {
                _state.value = MelaUiState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    fun startSession(nameBn: String, location: String, startDate: Long, endDate: Long, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                melaRepository.startSession(
                    tenantId = currentTenantId, nameBn = nameBn, location = location,
                    startDate = startDate, endDate = endDate, userId = "u_1",
                )
                load(currentTenantId)
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "শুরু ব্যর্থ")
            }
        }
    }

    fun pauseSession(reason: String?, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                melaRepository.pauseSession(currentTenantId, "u_1", reason)
                load(currentTenantId)
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "পজ ব্যর্থ")
            }
        }
    }

    fun resumeSession(onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                melaRepository.resumeSession(currentTenantId, "u_1")
                load(currentTenantId)
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "পুনরায় শুরু ব্যর্থ")
            }
        }
    }

    fun endSession(onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                melaRepository.endSession(currentTenantId, "u_1")
                load(currentTenantId)
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "শেষ ব্যর্থ")
            }
        }
    }

    fun moveStock(bookId: String, quantity: Int, direction: StockChangeReason, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                melaRepository.moveStock(
                    tenantId = currentTenantId, bookId = bookId, quantity = quantity,
                    direction = direction, userId = "u_1",
                )
                load(currentTenantId)
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "স্থানান্তর ব্যর্থ")
            }
        }
    }
}

sealed interface MelaUiState {
    data object Loading : MelaUiState
    data class Success(
        val session: MelaSession?,
        val lowStock: List<LowStockAlert>,
        val oversell: List<OversellAlert>,
        val report: List<MelaStockLine>,
        val books: List<Book>,
    ) : MelaUiState
    data class Error(val message: String) : MelaUiState
}
