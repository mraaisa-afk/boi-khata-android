package com.boikhata.shared.receipt

import com.boikhata.core.domain.enums.PaymentMethod
import com.boikhata.core.domain.model.Bill
import com.boikhata.core.domain.model.BillLine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D21: ReceiptBuilder unit tests — Unicode text, dual digits, WhatsApp-shareable.
 */
class ReceiptBuilderTest {

    private val bill = Bill(
        id = "b1",
        billNumber = "INV-20260830-0001",
        customerId = "c1",
        customerNameBn = "রহিম",
        customerPhone = "01711000000",
        userId = "u1",
        subtotal = 500.0,
        discountAmount = 50.0,
        discountType = "FIXED",
        vatAmount = 30.0,
        totalAmount = 480.0,
        paymentMethod = PaymentMethod.CASH,
        paidAmount = 480.0,
        dueAmount = 0.0,
        khataEntryId = null,
        billDate = 1725000000000L,
        status = "COMPLETED",
    )

    private val lines = listOf(
        BillLine("l1", "b1", "bk1", "বাংলা বই", 2, 100.0, 200.0, 0.0),
        BillLine("l2", "b1", "bk2", "খাতা কলম", 2, 100.0, 200.0, 30.0),
    )

    private val formatAmount: (Double) -> String = { "৳${it.toLong()}" }
    private val formatDate: (Long) -> String = { "30/08/2026" }

    @Test
    fun `should include shop name in receipt`() {
        val text = ReceiptBuilder.buildReceiptText(bill, lines, "টেস্ট দোকান", formatAmount, formatDate)
        assertThat(text).contains("টেস্ট দোকান")
    }

    @Test
    fun `should include bill number`() {
        val text = ReceiptBuilder.buildReceiptText(bill, lines, "Shop", formatAmount, formatDate)
        assertThat(text).contains("INV-20260830-0001")
    }

    @Test
    fun `should include date`() {
        val text = ReceiptBuilder.buildReceiptText(bill, lines, "Shop", formatAmount, formatDate)
        assertThat(text).contains("30/08/2026")
    }

    @Test
    fun `should include customer name when not walk-in`() {
        val text = ReceiptBuilder.buildReceiptText(bill, lines, "Shop", formatAmount, formatDate)
        assertThat(text).contains("রহিম")
    }

    @Test
    fun `should not include customer name for walk-in`() {
        val walkInBill = bill.copy(customerNameBn = "হাটি ক্রেতা", customerId = null)
        val text = ReceiptBuilder.buildReceiptText(walkInBill, lines, "Shop", formatAmount, formatDate)
        assertThat(text).doesNotContain("ক্রেতা: হাটি ক্রেতা")
    }

    @Test
    fun `should include line items with quantity and price`() {
        val text = ReceiptBuilder.buildReceiptText(bill, lines, "Shop", formatAmount, formatDate)
        assertThat(text).contains("বাংলা বই")
        assertThat(text).contains("খাতা কলম")
        assertThat(text).contains("2")
    }

    @Test
    fun `should include VAT for taxed lines only`() {
        val text = ReceiptBuilder.buildReceiptText(bill, lines, "Shop", formatAmount, formatDate)
        // Stationery line has VAT, book line does not
        assertThat(text).contains("ভ্যাট")
    }

    @Test
    fun `should include subtotal discount vat and total`() {
        val text = ReceiptBuilder.buildReceiptText(bill, lines, "Shop", formatAmount, formatDate)
        // Part A (PR #71): উপমুট → সর্বমোট — guard against the legacy term returning.
        assertThat(text).contains("সর্বমোট")
        assertThat(text).doesNotContain("উপমুট")
        assertThat(text).contains("ছাড়")
        assertThat(text).contains("ভ্যাট")
        assertThat(text).contains("মোট")
    }

    @Test
    fun `should label percentage discount distinctly from fixed discount`() {
        // A5a: the old code had two IDENTICAL branches ("ছাড়" else "ছাড়") — the
        // discountType was dead. Percentage bills must now read «ছাড় (%):».
        val pctBill = bill.copy(discountType = "PERCENTAGE")
        val pctText = ReceiptBuilder.buildReceiptText(pctBill, lines, "Shop", formatAmount, formatDate)
        assertThat(pctText).contains("ছাড় (%):")
        val fixedText = ReceiptBuilder.buildReceiptText(bill, lines, "Shop", formatAmount, formatDate)
        assertThat(fixedText).contains("ছাড়:")
        assertThat(fixedText).doesNotContain("ছাড় (%)")
    }

    @Test
    fun `should disambiguate Nagad mobile banking from cash`() {
        // A4: PaymentMethod.NAGAD rendered as bare «নগদ» — identical to CASH.
        val nagadBill = bill.copy(paymentMethod = PaymentMethod.NAGAD)
        val text = ReceiptBuilder.buildReceiptText(nagadBill, lines, "Shop", formatAmount, formatDate)
        assertThat(text).contains("নগদ (Nagad)")
    }

    @Test
    fun `should include due amount for partial payment`() {
        val partialBill = bill.copy(
            paymentMethod = PaymentMethod.CREDIT,
            paidAmount = 0.0,
            dueAmount = 480.0,
            status = "PARTIAL",
        )
        val text = ReceiptBuilder.buildReceiptText(partialBill, lines, "Shop", formatAmount, formatDate)
        assertThat(text).contains("বাকি")
        assertThat(text).contains("বাকি (খাতা)")
    }

    @Test
    fun `should end with thank you message`() {
        val text = ReceiptBuilder.buildReceiptText(bill, lines, "Shop", formatAmount, formatDate)
        assertThat(text).contains("ধন্যবাদ")
    }

    @Test
    fun `should use injected formatAmount for all amounts`() {
        val customFormat: (Double) -> String = { amount -> "##${amount.toInt()}##" }
        val text = ReceiptBuilder.buildReceiptText(bill, lines, "Shop", customFormat, formatDate)
        assertThat(text).contains("##500##") // subtotal
        assertThat(text).contains("##480##") // total
    }
}
