package com.boikhata.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.core.domain.aging.AgingBucket
import com.boikhata.core.domain.aging.AgingCalculator
import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.model.HomeAnalyticsPoint
import com.boikhata.core.domain.model.HomeData
import com.boikhata.core.domain.model.KhataCustomerDue
import com.boikhata.core.domain.repository.BillRepository
import com.boikhata.core.domain.repository.BookRepository
import com.boikhata.core.domain.repository.CashbookRepository
import com.boikhata.core.domain.repository.ExpenseRepository
import com.boikhata.core.domain.repository.KhataRepository
import com.boikhata.core.domain.repository.SupplierRepository
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
    private val cashbookRepository: CashbookRepository,
    private val supplierRepository: SupplierRepository,
    private val expenseRepository: ExpenseRepository,
    private val bookRepository: BookRepository,
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
                val startOfToday = cal.timeInMillis
                val endOfToday = now
                val startOfYesterday = startOfToday - 24L * 60 * 60 * 1000
                val endOfYesterday = startOfToday - 1L

                val customers = khataRepository.getCustomers(tenantId)
                val todayBills = billRepository.getBillsByDate(tenantId, startOfToday, endOfToday)

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

                val todayExpenses = expenseRepository.getExpensesByDateRange(
                    tenantId, startOfToday, endOfToday,
                )
                val todayExpenseTotal = todayExpenses.sumOf { it.amount }

                val yesterdayBills = billRepository.getBillsByDate(
                    tenantId, startOfYesterday, endOfYesterday,
                )
                val yesterdayExpenses = expenseRepository.getExpensesByDateRange(
                    tenantId, startOfYesterday, endOfYesterday,
                )
                val yesterdayNetProfit = yesterdayBills.sumOf { it.totalAmount } -
                    yesterdayExpenses.sumOf { it.amount }

                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val monthAnalytics = mutableListOf<HomeAnalyticsPoint>()
                val dayCursor = monthStart.clone() as Calendar
                while (dayCursor.timeInMillis <= startOfToday) {
                    val dayStart = dayCursor.timeInMillis
                    val dayEnd = if (dayStart == startOfToday) endOfToday else dayStart + 24L * 60 * 60 * 1000 - 1L
                    val dayBills = billRepository.getBillsByDate(tenantId, dayStart, dayEnd)
                    val dayExpenses = expenseRepository.getExpensesByDateRange(tenantId, dayStart, dayEnd)
                    monthAnalytics.add(
                        HomeAnalyticsPoint(
                            dayOfMonth = dayCursor.get(Calendar.DAY_OF_MONTH),
                            netProfit = dayBills.sumOf { it.totalAmount } - dayExpenses.sumOf { it.amount },
                        )
                    )
                    dayCursor.add(Calendar.DAY_OF_MONTH, 1)
                }
                val monthNetProfit = monthAnalytics.sumOf { it.netProfit }

                val balances = cashbookRepository.getBalances(tenantId)
                val cashBalance = balances
                    .firstOrNull { it.account == CashbookAccount.CASH }
                    ?.balance ?: 0.0

                val supplierSummary = supplierRepository.getSupplierAgingSummary(tenantId, now)
                val lowStockAlerts = bookRepository.getLowStockBookSummaries(tenantId).take(3)

                _uiState.value = HomeUiState.Success(
                    HomeData(
                        totalDue = totalDue,
                        dueCustomerCount = dueList.size,
                        todaySalesTotal = todaySalesTotal,
                        todayExpenseTotal = todayExpenseTotal,
                        todayBillCount = todayBillCount,
                        topDueCustomers = topDue,
                        cashBalance = cashBalance,
                        supplierDuesTotal = supplierSummary.totalPayable,
                        supplierCount = supplierSummary.supplierCount,
                        yesterdayNetProfit = yesterdayNetProfit,
                        lowStockAlerts = lowStockAlerts,
                        monthNetProfit = monthNetProfit,
                        monthAnalytics = monthAnalytics,
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
