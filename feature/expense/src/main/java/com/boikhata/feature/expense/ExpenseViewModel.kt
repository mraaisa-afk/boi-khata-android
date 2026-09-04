package com.boikhata.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.core.domain.accounting.GoriBalanceCalculator
import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.enums.CashbookEntryType
import com.boikhata.core.domain.model.CashbookBalance
import com.boikhata.core.domain.model.Expense
import com.boikhata.core.domain.model.ExpenseCategory
import com.boikhata.core.domain.model.OwnerDrawing
import com.boikhata.core.domain.repository.CashbookRepository
import com.boikhata.core.domain.repository.ExpenseRepository
import com.boikhata.core.domain.repository.OwnerDrawingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val cashbookRepository: CashbookRepository,
    private val ownerDrawingRepository: OwnerDrawingRepository,
) : ViewModel() {

    private val _expenseState = MutableStateFlow<ExpenseUiState>(ExpenseUiState.Loading)
    val expenseState: StateFlow<ExpenseUiState> = _expenseState.asStateFlow()

    private val _cashbookState = MutableStateFlow<CashbookUiState>(CashbookUiState.Loading)
    val cashbookState: StateFlow<CashbookUiState> = _cashbookState.asStateFlow()

    private val _drawingState = MutableStateFlow<DrawingUiState>(DrawingUiState.Loading)
    val drawingState: StateFlow<DrawingUiState> = _drawingState.asStateFlow()

    private var currentTenantId = "t_1"
    private var categoryMap: Map<String, ExpenseCategory> = emptyMap()

    // ── Expense ────────────────────────────────────────────────────────────

    fun loadExpenses(tenantId: String) {
        currentTenantId = tenantId
        viewModelScope.launch {
            _expenseState.value = ExpenseUiState.Loading
            try {
                val categories = expenseRepository.getCategories(tenantId)
                categoryMap = categories.associateBy { it.id }
                val expenses = expenseRepository.getExpenses(tenantId).map { exp ->
                    exp.copy(categoryNameBn = categoryMap[exp.categoryId]?.nameBn ?: "")
                }
                val goriCategoryId = categories.find { it.icon == "advance" }?.id ?: ""
                val goriExpenses = if (goriCategoryId.isNotEmpty()) {
                    expenseRepository.getExpensesByCategory(tenantId, goriCategoryId).map {
                        it.copy(categoryNameBn = categoryMap[it.categoryId]?.nameBn ?: "")
                    }
                } else emptyList()
                val goriBalances = GoriBalanceCalculator.calculatePerUserBalances(goriExpenses)
                _expenseState.value = ExpenseUiState.Success(
                    categories = categories,
                    expenses = expenses,
                    goriBalances = goriBalances,
                )
            } catch (e: Exception) {
                _expenseState.value = ExpenseUiState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    fun addExpense(
        categoryId: String,
        amount: Double,
        description: String,
        cashbookAccount: CashbookAccount,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                expenseRepository.addExpense(
                    tenantId = currentTenantId,
                    categoryId = categoryId,
                    amount = amount,
                    description = description,
                    expenseDate = System.currentTimeMillis(),
                    receiptPhotoPath = null,
                    userId = "u_1",
                    cashbookAccount = cashbookAccount,
                )
                loadExpenses(currentTenantId)
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "সেভ ব্যর্থ")
            }
        }
    }

    fun addBookPurchase(
        bookId: String,
        quantity: Int,
        unitPrice: Double,
        description: String,
        cashbookAccount: CashbookAccount,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                expenseRepository.addBookPurchase(
                    tenantId = currentTenantId,
                    bookId = bookId,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    description = description,
                    userId = "u_1",
                    cashbookAccount = cashbookAccount,
                )
                loadExpenses(currentTenantId)
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "সেভ ব্যর্থ")
            }
        }
    }

    // ── Cashbook ───────────────────────────────────────────────────────────

    fun loadCashbook(tenantId: String) {
        currentTenantId = tenantId
        viewModelScope.launch {
            _cashbookState.value = CashbookUiState.Loading
            try {
                val balances = cashbookRepository.getBalances(tenantId)
                _cashbookState.value = CashbookUiState.Success(balances)
            } catch (e: Exception) {
                _cashbookState.value = CashbookUiState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    fun addManualCashbookEntry(
        account: CashbookAccount,
        type: CashbookEntryType,
        amount: Double,
        description: String,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                cashbookRepository.addManualEntry(
                    tenantId = currentTenantId,
                    account = account,
                    type = type,
                    amount = amount,
                    description = description,
                    userId = "u_1",
                )
                loadCashbook(currentTenantId)
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "সেভ ব্যর্থ")
            }
        }
    }

    // ── Owner Drawing ──────────────────────────────────────────────────────

    fun loadDrawings(tenantId: String) {
        currentTenantId = tenantId
        viewModelScope.launch {
            _drawingState.value = DrawingUiState.Loading
            try {
                val drawings = ownerDrawingRepository.getDrawings(tenantId)
                _drawingState.value = DrawingUiState.Success(drawings)
            } catch (e: Exception) {
                _drawingState.value = DrawingUiState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    fun createDrawing(
        amount: Double,
        description: String,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                ownerDrawingRepository.createDrawing(
                    tenantId = currentTenantId,
                    amount = amount,
                    description = description,
                    userId = "u_1",
                )
                loadDrawings(currentTenantId)
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "সেভ ব্যর্থ")
            }
        }
    }
}

sealed interface ExpenseUiState {
    data object Loading : ExpenseUiState
    data class Success(
        val categories: List<ExpenseCategory>,
        val expenses: List<Expense>,
        val goriBalances: Map<String, Double>,
    ) : ExpenseUiState
    data class Error(val message: String) : ExpenseUiState
}

sealed interface CashbookUiState {
    data object Loading : CashbookUiState
    data class Success(val balances: List<CashbookBalance>) : CashbookUiState
    data class Error(val message: String) : CashbookUiState
}

sealed interface DrawingUiState {
    data object Loading : DrawingUiState
    data class Success(val drawings: List<OwnerDrawing>) : DrawingUiState
    data class Error(val message: String) : DrawingUiState
}
