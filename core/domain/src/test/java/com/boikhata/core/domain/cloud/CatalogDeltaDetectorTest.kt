package com.boikhata.core.domain.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D49: CatalogDeltaDetector unit tests — new-book detection + price-change detection.
 * Firebase-Project-Context.md §3: masterCatalog read-only.
 */
class CatalogDeltaDetectorTest {

    private fun masterEntry(
        id: String, isbn: String? = null, titleBn: String = "বই",
        author: String = "লেখক", mrp: Double = 100.0, isActive: Boolean = true,
    ) = CatalogDeltaDetector.MasterCatalogEntry(
        id = id, isbn = isbn, titleBn = titleBn, titleEn = null, author = author,
        publisher = "প্রকাশক", classLevel = "৯", subject = "বিষয়",
        editionYear = 2024, mrp = mrp, isActive = isActive, lastUpdated = 1000L,
    )

    private fun localBook(
        id: String, isbn: String? = null, titleBn: String = "বই",
        sellingPrice: Double = 100.0,
    ) = CatalogDeltaDetector.LocalBook(id = id, isbn = isbn, titleBn = titleBn, sellingPrice = sellingPrice)

    @Test
    fun `detectDelta should return empty delta when master and local match`() {
        val master = listOf(masterEntry("m1", isbn = "isbn1", mrp = 100.0))
        val local = listOf(localBook("b1", isbn = "isbn1", sellingPrice = 100.0))
        val delta = CatalogDeltaDetector.detectDelta(master, local)
        assertThat(delta.hasChanges).isFalse()
        assertThat(delta.newBooksCount).isEqualTo(0)
        assertThat(delta.priceChangesCount).isEqualTo(0)
    }

    @Test
    fun `detectDelta should detect new book when master has no local match`() {
        val master = listOf(masterEntry("m1", isbn = "isbn1", titleBn = "নতুন বই"))
        val local = listOf(localBook("b1", isbn = "different_isbn", titleBn = "পুরোনো বই"))
        val delta = CatalogDeltaDetector.detectDelta(master, local)
        assertThat(delta.newBooksCount).isEqualTo(1)
        assertThat(delta.newBooks[0].masterEntry.id).isEqualTo("m1")
        assertThat(delta.priceChangesCount).isEqualTo(0)
    }

    @Test
    fun `detectDelta should detect price change when local price differs from master mrp`() {
        val master = listOf(masterEntry("m1", isbn = "isbn1", mrp = 120.0))
        val local = listOf(localBook("b1", isbn = "isbn1", sellingPrice = 100.0))
        val delta = CatalogDeltaDetector.detectDelta(master, local)
        assertThat(delta.priceChangesCount).isEqualTo(1)
        assertThat(delta.priceChanges[0].localPrice).isEqualTo(100.0)
        assertThat(delta.priceChanges[0].masterPrice).isEqualTo(120.0)
        assertThat(delta.priceChanges[0].localBookId).isEqualTo("b1")
        assertThat(delta.newBooksCount).isEqualTo(0)
    }

    @Test
    fun `detectDelta should match by titleBn when ISBN is null`() {
        val master = listOf(masterEntry("m1", isbn = null, titleBn = "পদার্থবিজ্ঞান", mrp = 150.0))
        val local = listOf(localBook("b1", isbn = null, titleBn = "পদার্থবিজ্ঞান", sellingPrice = 100.0))
        val delta = CatalogDeltaDetector.detectDelta(master, local)
        // Matched by titleBn → price change (100 vs 150)
        assertThat(delta.priceChangesCount).isEqualTo(1)
        assertThat(delta.newBooksCount).isEqualTo(0)
    }

    @Test
    fun `detectDelta should skip inactive master entries`() {
        val master = listOf(masterEntry("m1", isbn = "isbn1", isActive = false))
        val local = listOf(localBook("b1", isbn = "different"))
        val delta = CatalogDeltaDetector.detectDelta(master, local)
        // Inactive master entry is skipped → no new book, no price change
        assertThat(delta.hasChanges).isFalse()
    }

    @Test
    fun `detectDelta should handle multiple new books and price changes`() {
        val master = listOf(
            masterEntry("m1", isbn = "isbn1", titleBn = "বই১", mrp = 100.0),   // matches local, same price
            masterEntry("m2", isbn = "isbn2", titleBn = "বই২", mrp = 120.0),   // matches local, price change
            masterEntry("m3", isbn = "isbn3", titleBn = "বই৩", mrp = 90.0),    // no local match → new book
        )
        val local = listOf(
            localBook("b1", isbn = "isbn1", titleBn = "বই১", sellingPrice = 100.0),
            localBook("b2", isbn = "isbn2", titleBn = "বই২", sellingPrice = 100.0),
        )
        val delta = CatalogDeltaDetector.detectDelta(master, local)
        assertThat(delta.newBooksCount).isEqualTo(1)
        assertThat(delta.priceChangesCount).isEqualTo(1)
        assertThat(delta.newBooks[0].masterEntry.id).isEqualTo("m3")
        assertThat(delta.priceChanges[0].masterEntry.id).isEqualTo("m2")
    }

    @Test
    fun `detectDelta should match case-insensitively by titleBn`() {
        val master = listOf(masterEntry("m1", isbn = null, titleBn = "পদার্থ"))
        val local = listOf(localBook("b1", isbn = null, titleBn = "পদার্থ", sellingPrice = 100.0))
        val delta = CatalogDeltaDetector.detectDelta(master, local)
        // Same title, same price → no change
        assertThat(delta.hasChanges).isFalse()
    }

    @Test
    fun `detectDelta should return empty when master catalog is empty`() {
        val delta = CatalogDeltaDetector.detectDelta(emptyList(), listOf(localBook("b1")))
        assertThat(delta.hasChanges).isFalse()
    }

    @Test
    fun `detectDelta should return all as new books when local is empty`() {
        val master = listOf(
            masterEntry("m1", isbn = "isbn1"),
            masterEntry("m2", isbn = "isbn2"),
        )
        val delta = CatalogDeltaDetector.detectDelta(master, emptyList())
        assertThat(delta.newBooksCount).isEqualTo(2)
        assertThat(delta.priceChangesCount).isEqualTo(0)
    }
}
