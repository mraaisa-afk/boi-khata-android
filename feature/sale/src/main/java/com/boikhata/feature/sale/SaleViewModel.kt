package com.boikhata.feature.sale

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.core.domain.enums.BookCategory
import com.boikhata.core.domain.enums.PaymentMethod
import com.boikhata.core.domain.model.Bill
import com.boikhata.core.domain.model.BillLine
import com.boikhata.core.domain.model.Book
import com.boikhata.core.domain.model.KhataCustomer
import com.boikhata.core.domain.repository.BillLineInput
import com.boikhata.core.domain.repository.BillRepository
import com.boikhata.core.domain.repository.BookRepository
import com.boikhata.core.domain.repository.KhataRepository
import com.boikhata.core.domain.sale.VatCalculator
import com.boikhata.core.domain.text.BengaliNormalizer
import com.boikhata.shared.receipt.ReceiptBuilder
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
class SaleViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val billRepository: BillRepository,
    private val khataRepository: KhataRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    private val _bookSearchState = MutableStateFlow<BookSearchState>(BookSearchState.Idle)
    val bookSearchState: StateFlow<BookSearchState> = _bookSearchState.asStateFlow()

    private val _historyState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val historyState: StateFlow<HistoryUiState> = _historyState.asStateFlow()

    private var currentTenantId: String = "t_1"

    // ── Book search for cart ───────────────────────────────────────────────

    fun searchBooks(tenantId: String, query: String) {
        currentTenantId = tenantId
        viewModelScope.launch {
            _bookSearchState.value = BookSearchState.Loading
            try {
                val normalized = BengaliNormalizer.normalize(query)
                val books = bookRepository.searchBooks(tenantId, normalized)
                _bookSearchState.value = BookSearchState.Success(books)
            } catch (e: Exception) {
                _bookSearchState.value = BookSearchState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    fun addToCart(book: Book) {
        val current = _cartState.value
        val existing = current.items.find { it.bookId == book.id }
        val items = if (existing != null) {
            current.items.map { if (it.bookId == book.id) it.copy(quantity = it.quantity + 1) else it }
        } else {
            current.items + CartItem(
                bookId = book.id,
                bookTitleBn = book.titleBn,
                unitPrice = book.sellingPrice,
                quantity = 1,
                category = book.category,
            )
        }
        _cartState.value = current.copy(items = items)
        recalculateTotals()
    }

    fun updateQuantity(bookId: String, quantity: Int) {
        val current = _cartState.value
        val items = if (quantity <= 0) {
            current.items.filterNot { it.bookId == bookId }
        } else {
            current.items.map { if (it.bookId == bookId) it.copy(quantity = quantity) else it }
        }
        _cartState.value = current.copy(items = items)
        recalculateTotals()
    }

    fun setDiscount(discountInput: String, isPercentage: Boolean) {
        val current = _cartState.value
        _cartState.value = current.copy(
            discountInput = discountInput,
            isPercentageDiscount = isPercentage,
        )
        recalculateTotals()
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _cartState.value = _cartState.value.copy(paymentMethod = method)
        recalculateTotals()
    }

    fun setPaidAmount(amount: String) {
        _cartState.value = _cartState.value.copy(paidAmountInput = amount)
        recalculateTotals()
    }

    fun selectCustomer(customer: KhataCustomer?) {
        _cartState.value = _cartState.value.copy(selectedCustomer = customer)
    }

    fun searchCustomers(tenantId: String, query: String) {
        viewModelScope.launch {
            try {
                val normalized = BengaliNormalizer.normalize(query)
                val customers = khataRepository.searchCustomers(tenantId, normalized)
                _cartState.value = _cartState.value.copy(customerSearchResults = customers)
            } catch (_: Exception) { }
        }
    }

    private fun recalculateTotals() {
        val state = _cartState.value
        val items = state.items
        val subtotal = items.sumOf { it.unitPrice * it.quantity }
        val vatAmount = items.sumOf { VatCalculator.calculateLineVat(it.unitPrice, it.quantity, it.category) }

        val discountAmount = if (state.isPercentageDiscount) {
            val pct = state.discountInput.toDoubleOrNull() ?: 0.0
            (subtotal + vatAmount) * (pct / 100.0)
        } else {
            state.discountInput.toDoubleOrNull() ?: 0.0
        }.coerceAtLeast(0.0)

        val totalAmount = (subtotal + vatAmount - discountAmount).coerceAtLeast(0.0)

        val paidAmount = when (state.paymentMethod) {
            PaymentMethod.CREDIT -> 0.0
            else -> {
                val inputPaid = state.paidAmountInput.toDoubleOrNull() ?: totalAmount
                inputPaid.coerceAtMost(totalAmount)
            }
        }
        val dueAmount = (totalAmount - paidAmount).coerceAtLeast(0.0)

        _cartState.value = state.copy(
            subtotal = subtotal,
            vatAmount = vatAmount,
            discountAmount = discountAmount,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            dueAmount = dueAmount,
        )
    }

    fun checkout(onDone: (String) -> Unit, onError: (String) -> Unit) {
        val state = _cartState.value
        if (state.items.isEmpty()) {
            onError("কার্ট খালি")
            return
        }
        // If due > 0, customer must be selected
        if (state.dueAmount > 0.01 && state.selectedCustomer == null) {
            onError("বাকি থাকলে ক্রেতা নির্বাচন করুন")
            return
        }

        viewModelScope.launch {
            try {
                val lines = state.items.map { item ->
                    BillLineInput(
                        bookId = item.bookId,
                        bookTitleBn = item.bookTitleBn,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        category = item.category,
                    )
                }
                val discountType = if (state.isPercentageDiscount) "PERCENTAGE" else "FIXED"
                val customerName = state.selectedCustomer?.nameBn ?: "হাটি ক্রেতা"
                val customerPhone = state.selectedCustomer?.phone

                val billId = billRepository.createBill(
                    tenantId = currentTenantId,
                    customerId = state.selectedCustomer?.id,
                    customerNameBn = customerName,
                    customerPhone = customerPhone,
                    userId = "u_1", // seed owner
                    lines = lines,
                    discountAmount = state.discountAmount,
                    discountType = discountType,
                    paymentMethod = state.paymentMethod,
                    paidAmount = state.paidAmount,
                )
                _cartState.value = CartState() // reset cart
                onDone(billId)
            } catch (e: Exception) {
                onError(e.message ?: "বিল তৈরি ব্যর্থ")
            }
        }
    }

    fun shareReceipt(billId: String, shopName: String) {
        viewModelScope.launch {
            try {
                val bill = billRepository.getBill(currentTenantId, billId) ?: return@launch
                val lines = billRepository.getBillLines(billId)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val text = ReceiptBuilder.buildReceiptText(
                    bill = bill,
                    lines = lines,
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
                val shareIntent = Intent.createChooser(sendIntent, "রসিদ শেয়ার করুন")
                shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                appContext.startActivity(shareIntent)
            } catch (_: Exception) { }
        }
    }

    // ── Bill history ───────────────────────────────────────────────────────

    fun loadHistory(tenantId: String) {
        currentTenantId = tenantId
        viewModelScope.launch {
            _historyState.value = HistoryUiState.Loading
            try {
                val bills = billRepository.getAllBills(tenantId)
                _historyState.value = HistoryUiState.Success(bills)
            } catch (e: Exception) {
                _historyState.value = HistoryUiState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    fun clearCart() {
        _cartState.value = CartState()
    }

    // ── Bill detail ────────────────────────────────────────────────────────

    suspend fun getBillForDetail(tenantId: String, billId: String): Bill? {
        currentTenantId = tenantId
        return billRepository.getBill(tenantId, billId)
    }

    suspend fun getBillLinesForDetail(billId: String): List<BillLine> {
        return billRepository.getBillLines(billId)
    }
}

data class CartItem(
    val bookId: String,
    val bookTitleBn: String,
    val unitPrice: Double,
    val quantity: Int,
    val category: BookCategory,
)

data class CartState(
    val items: List<CartItem> = emptyList(),
    val selectedCustomer: KhataCustomer? = null,
    val customerSearchResults: List<KhataCustomer> = emptyList(),
    val discountInput: String = "",
    val isPercentageDiscount: Boolean = true,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val paidAmountInput: String = "",
    val subtotal: Double = 0.0,
    val vatAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
)

sealed interface BookSearchState {
    data object Idle : BookSearchState
    data object Loading : BookSearchState
    data class Success(val books: List<Book>) : BookSearchState
    data class Error(val message: String) : BookSearchState
}

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Success(val bills: List<com.boikhata.core.domain.model.BillSummary>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}
