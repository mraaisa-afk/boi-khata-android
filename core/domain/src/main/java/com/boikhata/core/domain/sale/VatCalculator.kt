package com.boikhata.core.domain.sale

import com.boikhata.core.domain.enums.BookCategory

/**
 * D19: VAT calculator — per-line, category-based.
 * Blueprint §7.3/§8: বই ০% / স্টেশনারি ১৫%.
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 */
object VatCalculator {

    private const val STATIONERY_VAT_RATE = 0.15 // 15%

    /**
     * Calculate the VAT rate for a book category.
     * Books (TEXTBOOK, GENERAL) = 0%. Stationery = 15%. OTHER = 0%.
     */
    fun vatRateForCategory(category: BookCategory): Double = when (category) {
        BookCategory.STATIONERY -> STATIONERY_VAT_RATE
        BookCategory.TEXTBOOK, BookCategory.GENERAL, BookCategory.OTHER -> 0.0
    }

    /**
     * Calculate VAT for a single line.
     * Line VAT = unitPrice × quantity × vatRate.
     */
    fun calculateLineVat(unitPrice: Double, quantity: Int, category: BookCategory): Double {
        val lineTotal = unitPrice * quantity
        return lineTotal * vatRateForCategory(category)
    }

    /**
     * Calculate the line total INCLUDING VAT.
     * lineTotalWithVat = unitPrice × quantity + lineVat
     */
    fun calculateLineTotalWithVat(unitPrice: Double, quantity: Int, category: BookCategory): Double {
        val base = unitPrice * quantity
        val vat = base * vatRateForCategory(category)
        return base + vat
    }

    /**
     * Sum line VATs into a bill-level VAT total.
     */
    fun calculateBillVat(lines: List<LineVatInput>): Double {
        return lines.sumOf { calculateLineVat(it.unitPrice, it.quantity, it.category) }
    }
}

data class LineVatInput(
    val unitPrice: Double,
    val quantity: Int,
    val category: BookCategory,
)
