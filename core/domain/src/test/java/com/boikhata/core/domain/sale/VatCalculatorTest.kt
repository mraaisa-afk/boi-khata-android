package com.boikhata.core.domain.sale

import com.boikhata.core.domain.enums.BookCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D19: VatCalculator unit tests — per-line, category-based VAT.
 */
class VatCalculatorTest {

    @Test
    fun `should return 0% VAT for textbooks`() {
        assertThat(VatCalculator.vatRateForCategory(BookCategory.TEXTBOOK)).isEqualTo(0.0)
    }

    @Test
    fun `should return 0% VAT for general books`() {
        assertThat(VatCalculator.vatRateForCategory(BookCategory.GENERAL)).isEqualTo(0.0)
    }

    @Test
    fun `should return 15% VAT for stationery`() {
        assertThat(VatCalculator.vatRateForCategory(BookCategory.STATIONERY)).isEqualTo(0.15)
    }

    @Test
    fun `should return 0% VAT for other category`() {
        assertThat(VatCalculator.vatRateForCategory(BookCategory.OTHER)).isEqualTo(0.0)
    }

    @Test
    fun `should calculate 0 VAT for book line`() {
        val vat = VatCalculator.calculateLineVat(100.0, 3, BookCategory.TEXTBOOK)
        assertThat(vat).isEqualTo(0.0)
    }

    @Test
    fun `should calculate 15% VAT for stationery line`() {
        val vat = VatCalculator.calculateLineVat(100.0, 2, BookCategory.STATIONERY)
        assertThat(vat).isWithin(0.01).of(30.0) // 200 × 0.15
    }

    @Test
    fun `should calculate line total including VAT for stationery`() {
        val total = VatCalculator.calculateLineTotalWithVat(100.0, 2, BookCategory.STATIONERY)
        assertThat(total).isWithin(0.01).of(230.0) // 200 + 30
    }

    @Test
    fun `should calculate bill-level VAT from mixed lines`() {
        val lines = listOf(
            LineVatInput(100.0, 2, BookCategory.TEXTBOOK),     // 0 VAT
            LineVatInput(50.0, 3, BookCategory.STATIONERY),     // 22.5 VAT
            LineVatInput(200.0, 1, BookCategory.GENERAL),       // 0 VAT
        )
        val billVat = VatCalculator.calculateBillVat(lines)
        assertThat(billVat).isWithin(0.01).of(22.5) // only stationery
    }

    @Test
    fun `should return 0 bill VAT for all-book cart`() {
        val lines = listOf(
            LineVatInput(100.0, 2, BookCategory.TEXTBOOK),
            LineVatInput(200.0, 1, BookCategory.GENERAL),
        )
        assertThat(VatCalculator.calculateBillVat(lines)).isEqualTo(0.0)
    }
}
