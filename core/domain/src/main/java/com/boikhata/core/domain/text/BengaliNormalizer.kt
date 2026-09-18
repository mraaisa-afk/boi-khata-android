package com.boikhata.core.domain.text

/**
 * D13: Bengali fuzzy search normalizer — strips diacritics to create a
 * consonant-skeleton form that tolerates spelling variations.
 *
 * Strips: vowel signs (া ি ী ু ূ ে ৈ ো ৌ ৃ ৄ ৢ ৣ),
 * chandrabindu/bindu/visarga (ঁ ং ঃ), hasanta (্), nukta (়),
 * and converts Bengali digits to Latin.
 *
 * Pure function — no Android, no Room. Independently unit-testable.
 */
object BengaliNormalizer {

    // Bengali vowel signs (U+09BE–U+09CC + U+09D7 + U+0983 range)
    private val vowelSigns = setOf(
        '\u09BE', // া
        '\u09BF', // ি
        '\u09C0', // ী
        '\u09C1', // ু
        '\u09C2', // ূ
        '\u09C3', // ৃ
        '\u09C4', // ৄ
        '\u09C7', // ে
        '\u09C8', // ৈ
        '\u09CB', // ো
        '\u09CC', // ৌ
        '\u09D7', // ৗ
        '\u09E2', // ৢ
        '\u09E3', // ৣ
    )

    // Signs to strip entirely
    private val stripChars = setOf(
        '\u0981', // ঁ chandrabindu
        '\u0982', // ং bindu
        '\u0983', // ঃ visarga
        '\u09CD', // ্ hasanta
        '\u09BC', // ় nukta
        '\u097F', // ৱ (rare)
    )

    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    /**
     * B-007: Bengali-digit → ASCII-digit conversion for NUMERIC INPUT parsing.
     * The app renders amounts in Bangla digits (DigitStyle.BANGLA) and Bangla
     * keyboards emit ০-৯, but Double.parseDouble only accepts ASCII [0-9] —
     * so an unparsed "৪" silently became a 0 discount. Use before toDoubleOrNull
     * on any user-typed amount. Leaves non-digit characters untouched.
     */
    fun toAsciiDigits(input: String): String {
        if (input.isEmpty()) return input
        return buildString(input.length) {
            for (ch in input) {
                if (ch in banglaDigits) append(ch - '\u09E6') else append(ch)
            }
        }
    }

    /**
     * Normalize a Bengali string to its consonant-skeleton form.
     * Returns lowercase Latin for mixed scripts.
     */
    fun normalize(input: String): String {
        if (input.isEmpty()) return ""
        return buildString(input.length) {
            for (ch in input) {
                when {
                    ch in vowelSigns -> { /* skip */ }
                    ch in stripChars -> { /* skip */ }
                    ch in banglaDigits -> append(ch - '\u09E6') // ০=U+09E6 → '0'
                    ch in '0'..'9' -> append(ch)
                    ch in 'A'..'Z' -> append(ch.lowercaseChar())
                    else -> append(ch)
                }
            }
        }.trim()
    }
}
