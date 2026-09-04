package com.boikhata.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.core.domain.accounting.BengaliFiscalCalendar
import com.boikhata.core.domain.accounting.BudgetAlertCalculator
import com.boikhata.core.domain.model.BalanceSheetLite
import com.boikhata.core.domain.model.PeriodLock
import com.boikhata.core.domain.accounting.ReportDepthCalculator
import com.boikhata.core.domain.model.PnLReport
import com.boikhata.core.domain.repository.AccountingRepository
import com.boikhata.core.domain.repository.BillRepository
import com.boikhata.core.domain.repository.ExpenseRepository
import com.boikhata.core.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

/**
 * D37: ReportsViewModel — makes the P3b accounting engine visible.
 * P&L (dual-calendar month selector) + balance-sheet + period-lock + budget alerts.
 * Read-only: the engine owns the math; this VM reads it.
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val accountingRepository: AccountingRepository,
    private val budgetRepository: BudgetRepository,
    private val billRepository: BillRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    private val _pnlState = MutableStateFlow<PnLState>(PnLState.Loading)
    val pnlState: StateFlow<PnLState> = _pnlState.asStateFlow()

    private val _balanceSheetState = MutableStateFlow<BalanceSheetState>(BalanceSheetState.Loading)
    val balanceSheetState: StateFlow<BalanceSheetState> = _balanceSheetState.asStateFlow()

    private val _periodLockState = MutableStateFlow<PeriodLockState>(PeriodLockState.Loading)
    val periodLockState: StateFlow<PeriodLockState> = _periodLockState.asStateFlow()

    private val _budgetAlertState = MutableStateFlow<BudgetAlertState>(BudgetAlertState.Loading)
    val budgetAlertState: StateFlow<BudgetAlertState> = _budgetAlertState.asStateFlow()

    private val _trendState = MutableStateFlow<TrendState>(TrendState.Loading)
    val trendState: StateFlow<TrendState> = _trendState.asStateFlow()

    private val _rankingState = MutableStateFlow<RankingState>(RankingState.Loading)
    val rankingState: StateFlow<RankingState> = _rankingState.asStateFlow()

    private var currentTenantId = "t_1"
    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0

    init {
        // Default to current month
        val cal = Calendar.getInstance(TimeZone.getDefault())
        selectedYear = cal.get(Calendar.YEAR)
        selectedMonth = cal.get(Calendar.MONTH) + 1
    }

    fun loadReports(tenantId: String) {
        currentTenantId = tenantId
        loadPnL()
        loadBalanceSheet()
        loadPeriodLocks()
        loadBudgetAlerts()
        loadTrend()
        loadRankings()
    }

    fun selectMonth(year: Int, month: Int) {
        selectedYear = year
        selectedMonth = month
        loadPnL()
        loadBudgetAlerts()
    }

    private fun loadPnL() {
        viewModelScope.launch {
            _pnlState.value = PnLState.Loading
            try {
                val pnl = accountingRepository.getMonthlyPnL(currentTenantId, selectedYear, selectedMonth)
                _pnlState.value = PnLState.Success(pnl)
            } catch (e: Exception) {
                _pnlState.value = PnLState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    private fun loadBalanceSheet() {
        viewModelScope.launch {
            _balanceSheetState.value = BalanceSheetState.Loading
            try {
                val bs = accountingRepository.getBalanceSheet(currentTenantId, System.currentTimeMillis())
                _balanceSheetState.value = BalanceSheetState.Success(bs)
            } catch (e: Exception) {
                _balanceSheetState.value = BalanceSheetState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    private fun loadPeriodLocks() {
        viewModelScope.launch {
            _periodLockState.value = PeriodLockState.Loading
            try {
                val locks = accountingRepository.getLockedPeriods(currentTenantId)
                val isCurrentLocked = accountingRepository.isPeriodLocked(currentTenantId, selectedYear, selectedMonth)
                _periodLockState.value = PeriodLockState.Success(locks, isCurrentLocked)
            } catch (e: Exception) {
                _periodLockState.value = PeriodLockState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    fun lockCurrentPeriod() {
        viewModelScope.launch {
            try {
                accountingRepository.lockPeriod(currentTenantId, selectedYear, selectedMonth, "u_1")
                loadPeriodLocks()
            } catch (e: Exception) {
                _periodLockState.value = PeriodLockState.Error(e.message ?: "তালা ব্যর্থ")
            }
        }
    }

    private fun loadTrend() {
        viewModelScope.launch {
            _trendState.value = TrendState.Loading
            try {
                val months = (0..11).map { offset ->
                    val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        add(Calendar.MONTH, -offset)
                    }
                    accountingRepository.getMonthlyPnL(currentTenantId, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
                }
                _trendState.value = TrendState.Success(ReportDepthCalculator.twelveMonthTrend(months))
            } catch (e: Exception) {
                _trendState.value = TrendState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    private fun loadRankings() {
        viewModelScope.launch {
            _rankingState.value = RankingState.Loading
            try {
                val bills = billRepository.getAllBills(currentTenantId)
                val bookRows = bills.flatMap { bill ->
                    billRepository.getBillLines(bill.id).map { line ->
                        ReportDepthCalculator.RankedItem(line.bookTitleBn, line.quantity, line.lineTotal)
                    }
                }
                val customerRows = bills.map { bill ->
                    ReportDepthCalculator.RankedItem(bill.customerNameBn, 1, bill.totalAmount)
                }
                val categories = expenseRepository.getCategories(currentTenantId)
                val expenseRows = expenseRepository.getExpenses(currentTenantId).groupBy { it.categoryId }.map { (id, rows) ->
                    ReportDepthCalculator.RankedItem(
                        categories.firstOrNull { it.id == id }?.nameBn ?: id,
                        rows.size,
                        rows.sumOf { it.amount },
                    )
                }
                _rankingState.value = RankingState.Success(
                    books = ReportDepthCalculator.topBooks(bookRows),
                    customers = ReportDepthCalculator.topCustomers(customerRows),
                    expenses = ReportDepthCalculator.topExpenseCategories(expenseRows),
                )
            } catch (e: Exception) {
                _rankingState.value = RankingState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    private fun loadBudgetAlerts() {
        viewModelScope.launch {
            _budgetAlertState.value = BudgetAlertState.Loading
            try {
                val alerts = budgetRepository.getMonthlyAlerts(currentTenantId, selectedYear, selectedMonth)
                _budgetAlertState.value = BudgetAlertState.Success(alerts)
            } catch (e: Exception) {
                _budgetAlertState.value = BudgetAlertState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    /** The dual-calendar label for the selected month (Gregorian + Bengali). */
    fun monthLabel(): String {
        val gregName = BengaliFiscalCalendar.gregorianMonthNameBn(selectedMonth)
        val bengaliMonth = BengaliFiscalCalendar.gregorianToBengaliMonth(selectedMonth)
        val bengaliName = BengaliFiscalCalendar.bengaliMonthName(bengaliMonth)
        val fy = if (selectedMonth >= 4) selectedYear else selectedYear - 1
        return "$gregName $selectedYear / $bengaliName (FY $fy-${(fy + 1) % 100})"
    }

    val currentYear get() = selectedYear
    val currentMonth get() = selectedMonth
}

sealed interface PnLState {
    data object Loading : PnLState
    data class Success(val pnl: PnLReport) : PnLState
    data class Error(val message: String) : PnLState
}

sealed interface BalanceSheetState {
    data object Loading : BalanceSheetState
    data class Success(val balanceSheet: BalanceSheetLite) : BalanceSheetState
    data class Error(val message: String) : BalanceSheetState
}

sealed interface PeriodLockState {
    data object Loading : PeriodLockState
    data class Success(val locks: List<PeriodLock>, val isCurrentLocked: Boolean) : PeriodLockState
    data class Error(val message: String) : PeriodLockState
}

sealed interface BudgetAlertState {
    data object Loading : BudgetAlertState
    data class Success(val alerts: List<BudgetAlertCalculator.BudgetAlert>) : BudgetAlertState
    data class Error(val message: String) : BudgetAlertState
}

sealed interface TrendState {
    data object Loading : TrendState
    data class Success(val points: List<ReportDepthCalculator.MonthPoint>) : TrendState
    data class Error(val message: String) : TrendState
}

sealed interface RankingState {
    data object Loading : RankingState
    data class Success(
        val books: List<ReportDepthCalculator.RankedItem>,
        val customers: List<ReportDepthCalculator.RankedItem>,
        val expenses: List<ReportDepthCalculator.RankedItem>,
    ) : RankingState
    data class Error(val message: String) : RankingState
}
