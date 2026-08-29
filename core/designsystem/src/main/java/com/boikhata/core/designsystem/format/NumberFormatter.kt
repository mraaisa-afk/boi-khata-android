package com.boikhata.core.designsystem.format

enum class DigitStyle {
    BANGLA,
    LATIN,
}

object NumberFormatter {
    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun formatMoney(amount: Double, digitStyle: DigitStyle): String {
        val normalized = if (amount % 1.0 == 0.0) amount.toLong().toString() else String.format("%.2f", amount)
        return "৳" + applyDigitStyle(normalized, digitStyle)
    }

    fun applyDigitStyle(value: String, digitStyle: DigitStyle): String {
        if (digitStyle == DigitStyle.LATIN) return value
        return buildString(value.length) {
            value.forEach { character ->
                append(character.toBanglaDigit())
            }
        }
    }

    private fun Char.toBanglaDigit(): Char {
        return if (this in '0'..'9') banglaDigits[this - '0'] else this
    }
}
