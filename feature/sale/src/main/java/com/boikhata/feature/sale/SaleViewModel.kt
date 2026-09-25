package com.boikhata.feature.sale

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.core.domain.enums.BookCategory
import com.boikhata.core.domain.enums.MfsProvider
import com.boikhata.core.domain.enums.PaymentLineCategory
import com.boikhata.core.domain.enums.PaymentMethod
import com.boikhata.core.domain.model.Bill
import com.boikhata.core.domain.model.BillLine
import com.boikhata.core.domain.model.Book
import com.boikhata.core.domain.model.KhataCustomer
import com.boikhata.core.domain.repository.BillLineInput
import com.boikhata.core.domain.repository.BillRepository
import com.boikhata.core.domain.repository.BookRepository
import com.boikhata.core.domain.repository.KhataRepository
import com.boikhata.core.domain.repository.PaymentLineSpec
import com.boikhata.core.domain.sale.VatCalculator
import com.boikhata.core.domain.text.BengaliNormalizer
import com.boikhata.shared.receipt.ReceiptBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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

    // U-001/D87: dedicated state for the POS customer picker — the sheet auto-loads
    // the full customer list on open (blank query = full list per repo contract),
    // so the buyer sheet is never blank before the first keystroke.
    private val _customerSearchState = MutableStateFlow<CustomerSearchState>(CustomerSearchState.Idle)
    val customerSearchState: StateFlow<CustomerSearchState> = _customerSearchState.asStateFlow()

    private val _historyState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val historyState: StateFlow<HistoryUiState> = _historyState.asStateFlow()

    private var currentTenantId: String = "t_1"

    // U-001/D87: cancel the previous query when a new keystroke fires — prevents a
    // slow older search from overwriting newer results (out-of-order emission).
    private var bookSearchJob: Job? = null
    private var customerSearchJob: Job? = null

    // ── Book search for cart ───────────────────────────────────────────────

    fun searchBooks(tenantId: String, query: String) {
        currentTenantId = tenantId
        bookSearchJob?.cancel()
        bookSearchJob = viewModelScope.launch {
            _bookSearchState.value = BookSearchState.Loading
            try {
                val normalized = BengaliNormalizer.normalize(query)
                val books = bookRepository.searchBooks(tenantId, normalized)
                _bookSearchState.value = BookSearchState.Success(books)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bookSearchState.value = BookSearchState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    fun addToCart(book: Book) {
        val current = _cartState.value
        val existing = current.items.find { it.bookId == book.id }
        // P14 (owner device ruling — «মাইনাস নয়»): requested (existing + 1) may never
        // exceed the book's LIVE stock. Block the add and surface the owner's exact
        // device message; the D22 repo gate remains the authoritative backstop.
        val requested = (existing?.quantity ?: 0) + 1
        if (requested > book.currentStock) {
            _cartState.value = current.copy(stockWarning = "স্টকে পর্যাপ্ত বই নেই")
            return
        }
        val items = if (existing != null) {
            current.items.map { if (it.bookId == book.id) it.copy(quantity = it.quantity + 1) else it }
        } else {
            current.items + CartItem(
                bookId = book.id,
                bookTitleBn = book.titleBn,
                unitPrice = book.sellingPrice,
                quantity = 1,
                category = book.category,
                availableStock = book.currentStock,
            )
        }
        _cartState.value = current.copy(items = items)
        recalculateTotals()
    }

    fun updateQuantity(bookId: String, quantity: Int) {
        val current = _cartState.value
        if (quantity > 0) {
            val item = current.items.find { it.bookId == bookId }
            // P14: block manual increments beyond the captured live stock.
            if (item != null && quantity > item.availableStock) {
                _cartState.value = current.copy(stockWarning = "স্টকে পর্যাপ্ত বই নেই")
                return
            }
        }
        val items = if (quantity <= 0) {
            current.items.filterNot { it.bookId == bookId }
        } else {
            current.items.map { if (it.bookId == bookId) it.copy(quantity = quantity) else it }
        }
        _cartState.value = current.copy(items = items)
        recalculateTotals()
    }

    /** P14: clears the «স্টকে পর্যাপ্ত বই নেই» warning after the UI has surfaced it. */
    fun dismissStockWarning() {
        if (_cartState.value.stockWarning != null) {
            _cartState.value = _cartState.value.copy(stockWarning = null)
        }
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
        // P12 compatibility setter — the POS now drives multi-line state directly;
        // this rewrites the list as the equivalent single line.
        val line = when (method) {
            PaymentMethod.CASH -> PaymentLineUi(PaymentLineCategory.CASH, null, "")
            PaymentMethod.BKASH -> PaymentLineUi(PaymentLineCategory.MOBILE, MfsProvider.BKASH, "")
            PaymentMethod.NAGAD -> PaymentLineUi(PaymentLineCategory.MOBILE, MfsProvider.NAGAD, "")
            PaymentMethod.CREDIT -> PaymentLineUi(PaymentLineCategory.CASH, null, "0")
            PaymentMethod.BANK -> PaymentLineUi(PaymentLineCategory.BANK, null, "")
            PaymentMethod.MOBILE -> PaymentLineUi(PaymentLineCategory.MOBILE, MfsProvider.OTHER, "")
        }
        _cartState.value = _cartState.value.copy(paymentLines = listOf(line))
        recalculateTotals()
    }

    fun setPaidAmount(amount: String) {
        // P12 compatibility setter — sets the FIRST line's amount (single-line case).
        val lines = _cartState.value.paymentLines
        if (lines.isEmpty()) return
        _cartState.value = _cartState.value.copy(
            paymentLines = lines.mapIndexed { i, l -> if (i == 0) l.copy(amountInput = amount) else l }
        )
        recalculateTotals()
    }

    // ── P12/D92: multi-line payment entry ─────────────────────────────────

    fun addPaymentLine(category: PaymentLineCategory) {
        val lines = _cartState.value.paymentLines
        if (lines.any { it.category == category }) return // one line per category
        _cartState.value = _cartState.value.copy(
            paymentLines = lines + PaymentLineUi(category, null, "")
        )
        recalculateTotals()
    }

    fun removePaymentLine(index: Int) {
        val lines = _cartState.value.paymentLines
        if (index !in lines.indices || lines.size == 1) return // keep ≥ 1 line
        _cartState.value = _cartState.value.copy(paymentLines = lines.filterIndexed { i, _ -> i != index })
        recalculateTotals()
    }

    fun setPaymentLineAmount(index: Int, input: String) {
        val lines = _cartState.value.paymentLines
        if (index !in lines.indices) return
        _cartState.value = _cartState.value.copy(
            paymentLines = lines.mapIndexed { i, l -> if (i == index) l.copy(amountInput = input) else l }
        )
        recalculateTotals()
    }

    fun setPaymentLineProvider(index: Int, provider: MfsProvider) {
        val lines = _cartState.value.paymentLines
        if (index !in lines.indices) return
        _cartState.value = _cartState.value.copy(
            paymentLines = lines.mapIndexed { i, l -> if (i == index) l.copy(provider = provider) else l }
        )
    }

    fun selectCustomer(customer: KhataCustomer?) {
        _cartState.value = _cartState.value.copy(selectedCustomer = customer)
    }

    /**
     * U-001/D87: blank query returns the FULL active customer list (repo contract:
     * searchCustomers with blank → getCustomers). The picker sheet fires this on
     * open so existing customers are visible before any keystroke; typing filters.
     */
    fun searchCustomers(tenantId: String, query: String) {
        customerSearchJob?.cancel()
        customerSearchJob = viewModelScope.launch {
            _customerSearchState.value = CustomerSearchState.Loading
            try {
                val normalized = BengaliNormalizer.normalize(query)
                val customers = khataRepository.searchCustomers(tenantId, normalized)
                _customerSearchState.value = CustomerSearchState.Success(customers)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _customerSearchState.value = CustomerSearchState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    private fun recalculateTotals() {
        val state = _cartState.value
        val items = state.items
        val subtotal = items.sumOf { it.unitPrice * it.quantity }
        val vatAmount = items.sumOf { VatCalculator.calculateLineVat(it.unitPrice, it.quantity, it.category) }

        val discountAmount = if (state.isPercentageDiscount) {
            // B-007: Bangla keyboards emit Bengali digits (০-৯); toDoubleOrNull
            // accepts ASCII only — normalize before parsing so "৪" parses as 4.
            val pct = BengaliNormalizer.toAsciiDigits(state.discountInput).toDoubleOrNull() ?: 0.0
            (subtotal + vatAmount) * (pct / 100.0)
        } else {
            BengaliNormalizer.toAsciiDigits(state.discountInput).toDoubleOrNull() ?: 0.0
        }.coerceAtLeast(0.0)

        val totalAmount = (subtotal + vatAmount - discountAmount).coerceAtLeast(0.0)

        // P12/D92: paid = sum of payment-line amounts. Blank input behaves like
        // the legacy field: a SINGLE blank line = full total (plain cash sale);
        // in a multi-line split every amount must be typed explicitly.
        val sumPaidUncapped = state.paymentLines.sumOf { line ->
            val parsed = BengaliNormalizer.toAsciiDigits(line.amountInput).toDoubleOrNull()
            when {
                parsed != null -> parsed.coerceAtLeast(0.0)
                state.paymentLines.size == 1 && line.amountInput.isBlank() -> totalAmount
                else -> 0.0
            }
        }
        // D93: paid (toward the bill) is capped at the total; the excess is the
        // খাতা জমা preview — valid ONLY when a named customer is selected.
        val paidAmount = sumPaidUncapped.coerceAtMost(totalAmount)
        val dueAmount = (totalAmount - paidAmount).coerceAtLeast(0.0)
        val overpaymentAmount = (sumPaidUncapped - totalAmount).coerceAtLeast(0.0)

        _cartState.value = state.copy(
            subtotal = subtotal,
            vatAmount = vatAmount,
            discountAmount = discountAmount,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            dueAmount = dueAmount,
            overpaymentAmount = overpaymentAmount,
        )
    }

    /**
     * D86 rule 1: the write tenant is threaded explicitly from the PosScreen
     * destination — never read from VM-side mutable state at write time.
     */
    fun checkout(tenantId: String, onDone: (String) -> Unit, onError: (String) -> Unit) {
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
        if (tenantId.isBlank()) {
            // D86 fail-fast: refuse to guess a tenant for a write.
            onError("টেনান্ট শনাক্ত করা যায়নি — অ্যাপ রিস্টার্ট করুন")
            return
        }
        currentTenantId = tenantId

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

                // P12/D92: single-line checkouts keep the legacy createBill entry
                // point (and its exact test contract); splits (বাকি combinations)
                // use the multi-line transaction.
                val activeLines = state.paymentLines.filter { lineInputAmount(it, state) > 0.0 }
                val billId = if (activeLines.size <= 1) {
                    val single = activeLines.firstOrNull()
                        ?: state.paymentLines.firstOrNull()
                        ?: PaymentLineUi(PaymentLineCategory.CASH, null, "")
                    val amount = lineInputAmount(single, state)
                    val legacyMethod = when (single.category) {
                        PaymentLineCategory.CASH -> if (amount <= 0.0) PaymentMethod.CREDIT else PaymentMethod.CASH
                        PaymentLineCategory.BANK -> PaymentMethod.BANK
                        PaymentLineCategory.MOBILE -> when (single.provider) {
                            MfsProvider.BKASH -> PaymentMethod.BKASH
                            MfsProvider.NAGAD -> PaymentMethod.NAGAD
                            else -> PaymentMethod.MOBILE
                        }
                        PaymentLineCategory.DUE -> PaymentMethod.CREDIT
                    }
                    billRepository.createBill(
                        tenantId = currentTenantId,
                        customerId = state.selectedCustomer?.id,
                        customerNameBn = customerName,
                        customerPhone = customerPhone,
                        userId = "u_1", // seed owner
                        lines = lines,
                        discountAmount = state.discountAmount,
                        discountType = discountType,
                        paymentMethod = legacyMethod,
                        paidAmount = amount,
                    )
                } else {
                    billRepository.createBillWithPaymentLines(
                        tenantId = currentTenantId,
                        customerId = state.selectedCustomer?.id,
                        customerNameBn = customerName,
                        customerPhone = customerPhone,
                        userId = "u_1", // seed owner
                        lines = lines,
                        discountAmount = state.discountAmount,
                        discountType = discountType,
                        paidLines = activeLines.map { line ->
                            PaymentLineSpec(
                                category = line.category,
                                provider = line.provider,
                                amount = lineInputAmount(line, state),
                            )
                        },
                    )
                }
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
                val paymentLines = run {
                    val raw = billRepository.getPaymentLines(billId)
                    val sum = raw.sumOf { it.amount }
                    if (sum > bill.totalAmount + 0.01 && raw.none { it.category == PaymentLineCategory.DUE }) {
                        // D93: overpaid bill — cap the bill-facing জমা rows at the bill total
                        // and show the excess as its own খাতা row (the amount went to the
                        // customer's khata as a জমা, not to this bill).
                        var remaining = bill.totalAmount
                        raw.flatMap { pl ->
                            val toBill = minOf(pl.amount, remaining.coerceAtLeast(0.0))
                            remaining -= toBill
                            buildList {
                                add(
                                    ReceiptBuilder.PaymentLineDisplay(
                                        labelBn = ReceiptBuilder.paymentLineLabel(pl.category.name, pl.provider?.name),
                                        amount = toBill,
                                    )
                                )
                                if (pl.amount - toBill > 0.01) {
                                    add(
                                        ReceiptBuilder.PaymentLineDisplay(
                                            labelBn = "জমা (খাতায়)",
                                            amount = pl.amount - toBill,
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        // P12/D92: per-line জমা rows on the receipt (paid + DUE); legacy
                        // bills have no lines → single জমা/মাধ্যম rows as before.
                        raw.map { pl ->
                            ReceiptBuilder.PaymentLineDisplay(
                                labelBn = ReceiptBuilder.paymentLineLabel(pl.category.name, pl.provider?.name),
                                amount = pl.amount,
                            )
                        }
                    }
                }
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

    /** P12: resolved amount of a payment line (blank single line = full total). */
    private fun lineInputAmount(line: PaymentLineUi, state: CartState): Double {
        val parsed = BengaliNormalizer.toAsciiDigits(line.amountInput).toDoubleOrNull()
        return when {
            parsed != null -> parsed.coerceAtLeast(0.0)
            state.paymentLines.size == 1 && line.amountInput.isBlank() -> state.totalAmount
            else -> 0.0
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
    /** P14: live stock captured from Book.currentStock when the item was added. */
    val availableStock: Int = Int.MAX_VALUE,
)

data class CartState(
    val items: List<CartItem> = emptyList(),
    val selectedCustomer: KhataCustomer? = null,
    val discountInput: String = "",
    val isPercentageDiscount: Boolean = true,
    // P12/D92: combinable payment lines (default = one blank নগদ line = full cash).
    val paymentLines: List<PaymentLineUi> = listOf(PaymentLineUi(PaymentLineCategory.CASH, null, "")),
    val subtotal: Double = 0.0,
    val vatAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    // D93: sum(entered জমা) − মোট — valid only for a named customer (khata জমা);
    // a walk-in with an overpayment is blocked at checkout.
    val overpaymentAmount: Double = 0.0,
    // P14: one-shot «স্টকে পর্যাপ্ত বই নেই» warning — set by the cart guards,
    // surfaced by PosScreen (Toast) and cleared via dismissStockWarning().
    val stockWarning: String? = null,
)

/** P12/D92: one editable payment line in the POS checkout editor. */
data class PaymentLineUi(
    val category: PaymentLineCategory,
    val provider: MfsProvider?,
    val amountInput: String,
)

sealed interface BookSearchState {
    data object Idle : BookSearchState
    data object Loading : BookSearchState
    data class Success(val books: List<Book>) : BookSearchState
    data class Error(val message: String) : BookSearchState
}

/** U-001/D87: mirrors BookSearchState for the POS buyer picker sheet. */
sealed interface CustomerSearchState {
    data object Idle : CustomerSearchState
    data object Loading : CustomerSearchState
    data class Success(val customers: List<KhataCustomer>) : CustomerSearchState
    data class Error(val message: String) : CustomerSearchState
}

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Success(val bills: List<com.boikhata.core.domain.model.BillSummary>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}
