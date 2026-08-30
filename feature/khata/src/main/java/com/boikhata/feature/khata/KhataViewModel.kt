package com.boikhata.feature.khata

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.core.domain.aging.AgingCalculator
import com.boikhata.core.domain.aging.KhataEntry
import com.boikhata.core.domain.enums.KhataEntryType
import com.boikhata.core.domain.khata.KhataStatementBuilder
import com.boikhata.core.domain.model.KhataCustomer
import com.boikhata.core.domain.model.KhataCustomerDue
import com.boikhata.core.domain.model.KhataInstallment
import com.boikhata.core.domain.model.KhataStatement
import com.boikhata.core.domain.repository.KhataRepository
import com.boikhata.core.domain.repository.LicenseRepository
import com.boikhata.core.domain.text.BengaliNormalizer
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
class KhataViewModel @Inject constructor(
    private val khataRepository: KhataRepository,
    private val licenseRepository: LicenseRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _listState = MutableStateFlow<KhataListUiState>(KhataListUiState.Loading)
    val listState: StateFlow<KhataListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow<KhataDetailUiState>(KhataDetailUiState.Loading)
    val detailState: StateFlow<KhataDetailUiState> = _detailState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var currentTenantId: String = "t_1"

    // ── Customer list ──────────────────────────────────────────────────────

    fun loadCustomers(tenantId: String) {
        currentTenantId = tenantId
        viewModelScope.launch {
            _listState.value = KhataListUiState.Loading
            try {
                val customers = khataRepository.getCustomers(tenantId)
                val dueList = mutableListOf<KhataCustomerDue>()
                val now = System.currentTimeMillis()
                for (customer in customers) {
                    val entries = khataRepository.getEntries(tenantId, customer.id)
                    if (entries.isNotEmpty()) {
                        val aging = AgingCalculator.calculate(entries, now)
                        if (aging.totalDue > 0.01) {
                            dueList.add(
                                KhataCustomerDue(
                                    customer = customer,
                                    dueAmount = aging.totalDue,
                                    ageDays = aging.ageDays,
                                    agingBucket = aging.bucket.name,
                                )
                            )
                        }
                    }
                }
                _listState.value = KhataListUiState.Success(customers, dueList, _searchQuery.value)
            } catch (e: Exception) {
                _listState.value = KhataListUiState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        val current = _listState.value
        if (current is KhataListUiState.Success) {
            if (query.isBlank()) {
                _listState.value = current.copy(searchQuery = "")
            } else {
                val normalized = BengaliNormalizer.normalize(query)
                val filtered = current.customers.filter { c ->
                    BengaliNormalizer.normalize(c.nameBn).contains(normalized, ignoreCase = true) ||
                    (c.address?.contains(query, ignoreCase = true) == true) ||
                    (c.phone?.contains(query) == true)
                }
                _listState.value = current.copy(customers = filtered, searchQuery = query)
            }
        }
    }

    fun addCustomer(
        nameBn: String,
        phone: String?,
        address: String?,
        creditLimit: Double,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                khataRepository.addCustomer(currentTenantId, nameBn, phone, address, creditLimit)
                loadCustomers(currentTenantId)
                onDone()
            } catch (e: Exception) {
                _listState.value = KhataListUiState.Error(e.message ?: "সেভ ব্যর্থ")
            }
        }
    }

    // ── Customer detail ────────────────────────────────────────────────────

    fun loadDetail(tenantId: String, customerId: String) {
        currentTenantId = tenantId
        viewModelScope.launch {
            _detailState.value = KhataDetailUiState.Loading
            try {
                val customer = khataRepository.getCustomer(tenantId, customerId)
                if (customer == null) {
                    _detailState.value = KhataDetailUiState.Error("কাস্টমার পাওয়া যায়নি")
                    return@launch
                }
                val entries = khataRepository.getEntries(tenantId, customerId)
                val installments = khataRepository.getInstallments(tenantId, customerId)
                val now = System.currentTimeMillis()
                val statement = KhataStatementBuilder.buildStatement(customer, entries, now)
                _detailState.value = KhataDetailUiState.Success(
                    customer = customer,
                    entries = entries,
                    installments = installments,
                    statement = statement,
                )
            } catch (e: Exception) {
                _detailState.value = KhataDetailUiState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    fun addCredit(amount: Double, description: String, onDone: () -> Unit) {
        val state = _detailState.value
        if (state !is KhataDetailUiState.Success) return
        val customerId = state.customer.id
        viewModelScope.launch {
            try {
                khataRepository.addEntry(
                    currentTenantId, customerId, amount,
                    KhataEntryType.CREDIT, description.ifBlank { "বাকি" },
                    referenceBillId = null,
                    collectedByUserId = "u_1", // seed owner
                )
                loadDetail(currentTenantId, customerId)
                onDone()
            } catch (e: Exception) {
                _detailState.value = KhataDetailUiState.Error(e.message ?: "সেভ ব্যর্থ")
            }
        }
    }

    fun addPayment(amount: Double, description: String, onDone: () -> Unit) {
        val state = _detailState.value
        if (state !is KhataDetailUiState.Success) return
        val customerId = state.customer.id
        viewModelScope.launch {
            try {
                khataRepository.addEntry(
                    currentTenantId, customerId, amount,
                    KhataEntryType.PAYMENT, description.ifBlank { "জমা" },
                    referenceBillId = null,
                    collectedByUserId = "u_1",
                )
                loadDetail(currentTenantId, customerId)
                onDone()
            } catch (e: Exception) {
                _detailState.value = KhataDetailUiState.Error(e.message ?: "সেভ ব্যর্থ")
            }
        }
    }

    fun forgiveDebt(onDone: () -> Unit) {
        val state = _detailState.value
        if (state !is KhataDetailUiState.Success) return
        val customerId = state.customer.id
        viewModelScope.launch {
            try {
                khataRepository.forgiveDebt(currentTenantId, customerId, "u_1")
                loadDetail(currentTenantId, customerId)
                onDone()
            } catch (e: Exception) {
                _detailState.value = KhataDetailUiState.Error(e.message ?: "দেনা মুন ব্যর্থ")
            }
        }
    }

    fun addInstallment(dueDate: Long, amount: Double, onDone: () -> Unit) {
        val state = _detailState.value
        if (state !is KhataDetailUiState.Success) return
        val customerId = state.customer.id
        val entryId = state.entries.lastOrNull { it.type == KhataEntryType.CREDIT }?.id ?: ""
        viewModelScope.launch {
            try {
                khataRepository.addInstallment(currentTenantId, customerId, entryId, dueDate, amount)
                loadDetail(currentTenantId, customerId)
                onDone()
            } catch (e: Exception) {
                _detailState.value = KhataDetailUiState.Error(e.message ?: "কিস্তি যোগ ব্যর্থ")
            }
        }
    }

    fun markInstallmentPaid(installmentId: String) {
        val state = _detailState.value
        if (state !is KhataDetailUiState.Success) return
        val customerId = state.customer.id
        viewModelScope.launch {
            try {
                khataRepository.markInstallmentPaid(installmentId)
                loadDetail(currentTenantId, customerId)
            } catch (e: Exception) {
                _detailState.value = KhataDetailUiState.Error(e.message ?: "আপডেট ব্যর্থ")
            }
        }
    }

    fun shareStatement(shopName: String) {
        val state = _detailState.value
        if (state !is KhataDetailUiState.Success) return
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val text = KhataStatementBuilder.toText(
            statement = state.statement,
            shopName = shopName,
            formatAmount = { amount ->
                com.boikhata.core.designsystem.format.NumberFormatter.formatMoney(
                    amount, com.boikhata.core.designsystem.format.DigitStyle.BANGLA
                )
            },
            formatDate = { millis -> dateFormat.format(Date(millis)) },
        )
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "বাকি হিসাব শেয়ার করুন")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        appContext.startActivity(shareIntent)
    }
}

sealed interface KhataListUiState {
    data object Loading : KhataListUiState
    data class Success(
        val customers: List<KhataCustomer>,
        val dueList: List<KhataCustomerDue>,
        val searchQuery: String,
    ) : KhataListUiState
    data class Error(val message: String) : KhataListUiState
}

sealed interface KhataDetailUiState {
    data object Loading : KhataDetailUiState
    data class Success(
        val customer: KhataCustomer,
        val entries: List<KhataEntry>,
        val installments: List<KhataInstallment>,
        val statement: KhataStatement,
    ) : KhataDetailUiState
    data class Error(val message: String) : KhataDetailUiState
}
