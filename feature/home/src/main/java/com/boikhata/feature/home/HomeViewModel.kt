package com.boikhata.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.core.domain.aging.AgingBucket
import com.boikhata.core.domain.aging.AgingCalculator
import com.boikhata.core.domain.aging.KhataEntry
import com.boikhata.core.domain.model.HomeData
import com.boikhata.core.domain.model.KhataCustomerDue
import com.boikhata.core.domain.repository.BillRepository
import com.boikhata.core.domain.repository.KhataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val khataRepository: KhataRepository,
    private val billRepository: BillRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadHome(tenantId: String) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = cal.timeInMillis
                val endOfDay = now

                val customers = khataRepository.getCustomers(tenantId)
                val todayBills = billRepository.getBillsByDate(tenantId, startOfDay, endOfDay)

                val dueList = mutableListOf<KhataCustomerDue>()
                var totalDue = 0.0

                for (customer in customers) {
                    val entries = khataRepository.getEntries(tenantId, customer.id)
                    if (entries.isEmpty()) continue
                    val aging = AgingCalculator.calculate(entries, now)
                    if (aging.totalDue > 0.01) {
                        totalDue += aging.totalDue
                        dueList.add(
                            KhataCustomerDue(
                                customer = customer,
                                dueAmount = aging.totalDue,
                                ageDays = aging.ageDays,
                                agingBucket = when (aging.bucket) {
                                    AgingBucket.GREEN -> "GREEN"
                                    AgingBucket.YELLOW -> "YELLOW"
                                    AgingBucket.RED -> "RED"
                                    AgingBucket.NONE -> "NONE"
                                },
                            )
                        )
                    }
                }

                dueList.sortByDescending { it.dueAmount }
                val topDue = dueList.take(5)

                val todaySalesTotal = todayBills.sumOf { it.totalAmount }
                val todayBillCount = todayBills.size

                _uiState.value = HomeUiState.Success(
                    HomeData(
                        totalDue = totalDue,
                        dueCustomerCount = dueList.size,
                        todaySalesTotal = todaySalesTotal,
                        todayBillCount = todayBillCount,
                        topDueCustomers = topDue,
                    )
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "ত্রুটি")
            }
        }
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val data: HomeData) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
