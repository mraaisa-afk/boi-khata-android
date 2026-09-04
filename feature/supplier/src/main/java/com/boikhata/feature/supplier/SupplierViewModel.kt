package com.boikhata.feature.supplier

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.core.designsystem.format.DigitStyle
import com.boikhata.core.designsystem.format.NumberFormatter
import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.enums.SupplierEntryType
import com.boikhata.core.domain.model.Supplier
import com.boikhata.core.domain.model.SupplierAgingSummary
import com.boikhata.core.domain.model.SupplierBalance
import com.boikhata.core.domain.model.SupplierEntry
import com.boikhata.core.domain.model.SupplierStatement
import com.boikhata.core.domain.repository.SupplierRepository
import com.boikhata.shared.receipt.SupplierStatementBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SupplierViewModel @Inject constructor(
    private val supplierRepository: SupplierRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<SupplierUiState>(SupplierUiState.Loading)
    val state: StateFlow<SupplierUiState> = _state.asStateFlow()

    private val _detailState = MutableStateFlow<SupplierDetailUiState>(SupplierDetailUiState.Idle)
    val detailState: StateFlow<SupplierDetailUiState> = _detailState.asStateFlow()

    private var currentTenantId = "t_1"
    private var currentShopName = "বই খাতা"
    private var currentSupplierId: String? = null

    fun loadSuppliers(tenantId: String, shopName: String) {
        currentTenantId = tenantId
        currentShopName = shopName
        viewModelScope.launch {
            _state.value = SupplierUiState.Loading
            try {
                val suppliers = supplierRepository.getSuppliers(tenantId)
                val now = System.currentTimeMillis()
                val summary = supplierRepository.getSupplierAgingSummary(tenantId, now)
                val withBalance = suppliers.map { s ->
                    supplierRepository.getSupplierBalance(tenantId, s.id, now)
                }
                _state.value = SupplierUiState.Success(
                    suppliers = suppliers,
                    summary = summary,
                    balances = withBalance,
                    reminders = supplierRepository.getSettlementReminders(tenantId, now),
                )
            } catch (e: Exception) {
                _state.value = SupplierUiState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    fun loadSupplierDetail(supplierId: String) {
        currentSupplierId = supplierId
        viewModelScope.launch {
            _detailState.value = SupplierDetailUiState.Loading
            try {
                val supplier = supplierRepository.getSupplier(currentTenantId, supplierId)
                    ?: throw IllegalStateException("সাপ্লায়ার পাওয়া যায়নি")
                val now = System.currentTimeMillis()
                val balance = supplierRepository.getSupplierBalance(currentTenantId, supplierId, now)
                val entries = supplierRepository.getEntries(currentTenantId, supplierId)
                val endOfDay = now + 1
                val statement = supplierRepository.getSettlementStatement(
                    tenantId = currentTenantId,
                    supplierId = supplierId,
                    shopName = currentShopName,
                    startDate = null,
                    endDate = endOfDay,
                )
                _detailState.value = SupplierDetailUiState.Success(
                    supplier = supplier,
                    balance = balance,
                    entries = entries,
                    statement = statement,
                )
            } catch (e: Exception) {
                _detailState.value = SupplierDetailUiState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    fun refreshDetail() {
        currentSupplierId?.let { loadSupplierDetail(it) }
    }

    fun shareStatement() {
        val state = _detailState.value
        if (state !is SupplierDetailUiState.Success) return
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val text = SupplierStatementBuilder.buildStatementText(
            statement = state.statement,
            formatAmount = { amount -> NumberFormatter.formatMoney(amount, DigitStyle.BANGLA) },
            formatDate = { millis -> dateFormat.format(Date(millis)) },
        )
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "সাপ্লায়ার স্টেটমেন্ট শেয়ার করুন")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        appContext.startActivity(shareIntent)
    }

    fun addSupplier(
        nameBn: String,
        phone: String?,
        settlementCycle: String,
        notes: String?,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                supplierRepository.addSupplier(
                    tenantId = currentTenantId,
                    nameBn = nameBn,
                    phone = phone,
                    settlementCycle = settlementCycle,
                    notes = notes,
                )
                loadSuppliers(currentTenantId, currentShopName)
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "সেভ ব্যর্থ")
            }
        }
    }

    fun addEntry(
        type: SupplierEntryType,
        amount: Double,
        description: String,
        trxId: String?,
        cashbookAccount: CashbookAccount,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val supplierId = currentSupplierId ?: return onError("সাপ্লায়ার নির্বাচন করুন")
        viewModelScope.launch {
            try {
                when (type) {
                    SupplierEntryType.PAYMENT -> supplierRepository.addPayment(
                        tenantId = currentTenantId,
                        supplierId = supplierId,
                        amount = amount,
                        description = description,
                        trxId = trxId,
                        userId = "u_1",
                        cashbookAccount = cashbookAccount,
                    )
                    SupplierEntryType.OPENING -> supplierRepository.addOpeningBalance(
                        tenantId = currentTenantId, supplierId = supplierId, amount = amount, userId = "u_1",
                    )
                    SupplierEntryType.CONSIGNMENT -> supplierRepository.addConsignment(
                        tenantId = currentTenantId, supplierId = supplierId, amount = amount,
                        description = description, userId = "u_1",
                    )
                    SupplierEntryType.PURCHASE -> supplierRepository.addPurchase(
                        tenantId = currentTenantId, supplierId = supplierId, amount = amount,
                        description = description, userId = "u_1",
                    )
                    SupplierEntryType.ADJUSTMENT -> supplierRepository.addEntry(
                        tenantId = currentTenantId, supplierId = supplierId, amount = amount,
                        type = SupplierEntryType.ADJUSTMENT, description = description,
                        referenceId = null, date = System.currentTimeMillis(), userId = "u_1",
                    )
                }
                loadSuppliers(currentTenantId, currentShopName)
                refreshDetail()
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "সেভ ব্যর্থ")
            }
        }
    }
}

sealed interface SupplierUiState {
    data object Loading : SupplierUiState
    data class Success(
        val suppliers: List<Supplier>,
        val summary: SupplierAgingSummary,
        val balances: List<SupplierBalance>,
        val reminders: List<SupplierBalance>,
    ) : SupplierUiState
    data class Error(val message: String) : SupplierUiState
}

sealed interface SupplierDetailUiState {
    data object Idle : SupplierDetailUiState
    data object Loading : SupplierDetailUiState
    data class Success(
        val supplier: Supplier,
        val balance: SupplierBalance,
        val entries: List<SupplierEntry>,
        val statement: SupplierStatement,
    ) : SupplierDetailUiState
    data class Error(val message: String) : SupplierDetailUiState
}
