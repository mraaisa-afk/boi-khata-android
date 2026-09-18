package com.boikhata.feature.sale

import com.boikhata.core.domain.enums.BookCategory
import com.boikhata.core.domain.enums.PaymentMethod
import com.boikhata.core.domain.model.Book
import com.boikhata.core.domain.model.KhataCustomer
import com.boikhata.core.domain.repository.BillRepository
import com.boikhata.core.domain.repository.BookRepository
import com.boikhata.core.domain.repository.KhataRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import android.content.Context

/**
 * B-007 regression: the ছাড় (discount) field IS wired — setDiscount →
 * recalculateTotals — but the parser accepted ASCII digits only. Bangla keyboards
 * emit ০-৯ which `toDoubleOrNull()` silently rejected, so a Bengali digit produced
 * a 0 discount and the total never moved (device evidence 2026-09-18).
 *
 * Also locks in: the partial-payment math (জমা → বাকি remainder) and the D86
 * blank-tenant checkout fail-fast.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SaleViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var bookRepository: BookRepository
    private lateinit var billRepository: BillRepository
    private lateinit var khataRepository: KhataRepository
    private lateinit var viewModel: SaleViewModel

    private val misirAli = Book(
        id = "misir-ali", isbn = null, titleBn = "মিসির আলী", titleEn = null, author = "হুমায়ূন আহমেদ",
        publisher = "", classLevel = "", subject = "", editionYear = 2024,
        category = BookCategory.GENERAL, condition = com.boikhata.core.domain.enums.BookCondition.NEW,
        purchasePrice = 200.0, sellingPrice = 350.0, initialStock = 40, lowStockThreshold = 5, isActive = true,
    )
    private val himu = misirAli.copy(id = "himu", titleBn = "হিমু", sellingPrice = 300.0, initialStock = 30)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        bookRepository = mockk()
        billRepository = mockk(relaxed = true)
        khataRepository = mockk()
        coEvery { bookRepository.searchBooks(any(), any()) } returns listOf(misirAli, himu)
        viewModel = SaleViewModel(bookRepository, billRepository, khataRepository, mockk<Context>(relaxed = true))
    }

    @After
    fun teardown() { Dispatchers.resetMain() }

    private fun fillCartThreeEach() {
        viewModel.addToCart(misirAli)
        viewModel.addToCart(himu)
        viewModel.updateQuantity("misir-ali", 3)
        viewModel.updateQuantity("himu", 3)
    }

    @Test
    fun `ascii discount 8 percent recalculates the total`() = runTest {
        fillCartThreeEach() // subtotal 1050 + 900 = 1950, books 0% VAT
        viewModel.setDiscount("8", isPercentage = true)
        val s = viewModel.cartState.value
        assertThat(s.subtotal).isEqualTo(1950.0)
        assertThat(s.discountAmount).isEqualTo(156.0)
        assertThat(s.totalAmount).isEqualTo(1794.0)
    }

    @Test
    fun `bengali digit discount parses - the owner's device case`() = runTest {
        fillCartThreeEach()
        viewModel.setDiscount("৮", isPercentage = true) // B-007: was silently 0 before
        val s = viewModel.cartState.value
        assertThat(s.discountAmount).isEqualTo(156.0)
        assertThat(s.totalAmount).isEqualTo(1794.0)
    }

    @Test
    fun `partial payment - paid 500 leaves 1450 due`() = runTest {
        fillCartThreeEach()
        viewModel.setPaymentMethod(PaymentMethod.CASH)
        viewModel.setPaidAmount("৫০০") // Bengali digits from a Bangla keyboard
        val s = viewModel.cartState.value
        assertThat(s.paidAmount).isEqualTo(500.0)
        assertThat(s.dueAmount).isEqualTo(1450.0)
    }

    @Test
    fun `checkout with blank tenant fails fast per D86`() = runTest {
        fillCartThreeEach()
        var error: String? = null
        viewModel.checkout(tenantId = "", onDone = {}, onError = { error = it })
        assertThat(error).isEqualTo("টেনান্ট শনাক্ত করা যায়নি — অ্যাপ রিস্টার্ট করুন")
    }

    @Test
    fun `checkout full-cash passes the discounted total as paid`() = runTest {
        fillCartThreeEach()
        viewModel.setDiscount("8", isPercentage = true) // total 1794
        coEvery { billRepository.createBill(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns "bill-1"
        var doneBill: String? = null
        // pre-launch math: discount applied → paid = discounted total, due 0
        val pre = viewModel.cartState.value
        assertThat(pre.paidAmount).isEqualTo(1794.0)
        assertThat(pre.dueAmount).isEqualTo(0.0)
        viewModel.checkout(tenantId = "t_1", onDone = { doneBill = it }, onError = {})
        dispatcher.scheduler.advanceUntilIdle() // checkout runs on viewModelScope (Main = test dispatcher)
        assertThat(doneBill).isEqualTo("bill-1")
        assertThat(viewModel.cartState.value.items).isEmpty() // cart reset on success
    }

    @Test
    fun `checkout with due and no customer is refused`() = runTest {
        fillCartThreeEach()
        viewModel.setPaidAmount("0")
        var error: String? = null
        viewModel.checkout(tenantId = "t_1", onDone = {}, onError = { error = it })
        assertThat(error).isEqualTo("বাকি থাকলে ক্রেতা নির্বাচন করুন")
    }

    @Test
    fun `selecting a customer satisfies the due guard`() = runTest {
        fillCartThreeEach()
        viewModel.setPaidAmount("100") // due 1850
        viewModel.selectCustomer(KhataCustomer(id = "karim", nameBn = "করিম", phone = null, address = null, creditLimit = 0.0, isActive = true))
        coEvery { billRepository.createBill(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns "bill-2"
        var doneBill: String? = null
        viewModel.checkout(tenantId = "t_1", onDone = { doneBill = it }, onError = {})
        dispatcher.scheduler.advanceUntilIdle() // let the launched checkout coroutine run
        assertThat(doneBill).isEqualTo("bill-2")
    }
}
