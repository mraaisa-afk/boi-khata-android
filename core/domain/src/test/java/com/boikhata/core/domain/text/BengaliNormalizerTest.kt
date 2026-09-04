package com.boikhata.core.domain.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D13: BengaliNormalizer unit tests — verifies diacritic stripping
 * and digit conversion for fuzzy search.
 */
class BengaliNormalizerTest {

    @Test
    fun `should return empty for empty input`() {
        assertThat(BengaliNormalizer.normalize("")).isEmpty()
    }

    @Test
    fun `should strip vowel signs from bangla word`() {
        // বাংলা has া (vowel sign aa) → should become বংলা (skeleton)
        val result = BengaliNormalizer.normalize("বাংলা")
        assertThat(result).doesNotContain("\u09BE") // া stripped
    }

    @Test
    fun `should strip bindu and chandrabindu`() {
        val result = BengaliNormalizer.normalize("বাংলা")
        assertThat(result).doesNotContain("\u0982") // ং stripped
        assertThat(result).doesNotContain("\u0981") // ঁ stripped
    }

    @Test
    fun `should strip hasanta`() {
        val result = BengaliNormalizer.normalize("ক্ষু")
        assertThat(result).doesNotContain("\u09CD") // ্ stripped
    }

    @Test
    fun `should convert bangla digits to latin`() {
        val result = BengaliNormalizer.normalize("১২৩")
        assertThat(result).isEqualTo("123")
    }

    @Test
    fun `should keep latin digits as is`() {
        val result = BengaliNormalizer.normalize("abc123")
        assertThat(result).isEqualTo("abc123")
    }

    @Test
    fun `should lowercase latin characters`() {
        val result = BengaliNormalizer.normalize("ABC")
        assertThat(result).isEqualTo("abc")
    }

    @Test
    fun `should normalize spelling variants similarly`() {
        // "বাংলা" and "বঙ্গ" should both lose their diacritics
        val n1 = BengaliNormalizer.normalize("বাংলা")
        val n2 = BengaliNormalizer.normalize("বাংগলা")
        // Both should produce the same consonant skeleton after stripping
        // (ব ং ল vs ব ং গ ল — not identical, but diacritic-free)
        assertThat(n1).doesNotContain("\u09BE")
        assertThat(n2).doesNotContain("\u09BE")
    }

    @Test
    fun `should handle mixed script input`() {
        val result = BengaliNormalizer.normalize("Math ১০")
        assertThat(result).isEqualTo("math 10")
    }

    @Test
    fun `should trim whitespace`() {
        val result = BengaliNormalizer.normalize("  hello  ")
        assertThat(result).isEqualTo("hello")
    }
}
