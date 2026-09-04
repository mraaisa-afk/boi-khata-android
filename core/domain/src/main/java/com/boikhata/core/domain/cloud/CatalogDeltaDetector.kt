package com.boikhata.core.domain.cloud

/**
 * D49: CatalogDeltaDetector — pure service that compares the shared NCTB master catalog
 * (read from Firestore, read-only) with the local books to detect new books and price changes.
 *
 * Firebase-Project-Context.md §3: masterCatalog — "read: any authenticated; write: false."
 * The app reads the master catalog and compares with local books.
 * "নতুন দাম" badge = a price change (local sellingPrice != master mrp).
 * One-tap "প্রয়োগ করুন" = apply the master price to the local book (a local Room write).
 *
 * Pure object — no Android, no Firestore. Independently unit-testable.
 */
object CatalogDeltaDetector {

    /** A master catalog entry (from Firestore masterCatalog collection). */
    data class MasterCatalogEntry(
        val id: String,
        val isbn: String?,
        val titleBn: String,
        val titleEn: String?,
        val author: String,
        val publisher: String,
        val classLevel: String,
        val subject: String,
        val editionYear: Int,
        val mrp: Double,
        val isActive: Boolean,
        val lastUpdated: Long,
    )

    /** A local book (from Room books table). */
    data class LocalBook(
        val id: String,
        val isbn: String?,
        val titleBn: String,
        val sellingPrice: Double,
    )

    /** A new book: in master but not in local (matched by ISBN, or by titleBn+author if ISBN missing). */
    data class NewBook(
        val masterEntry: MasterCatalogEntry,
    )

    /** A price change: local book exists but sellingPrice != master mrp. */
    data class PriceChange(
        val localBookId: String,
        val localPrice: Double,
        val masterPrice: Double,
        val masterEntry: MasterCatalogEntry,
    )

    /** The delta: new books + price changes. */
    data class CatalogDelta(
        val newBooks: List<NewBook>,
        val priceChanges: List<PriceChange>,
    ) {
        val hasChanges: Boolean get() = newBooks.isNotEmpty() || priceChanges.isNotEmpty()
        val newBooksCount: Int get() = newBooks.size
        val priceChangesCount: Int get() = priceChanges.size
    }

    /**
     * Detect the delta between the master catalog and local books.
     *
     * Matching logic:
     * 1. If a local book has the same ISBN as a master entry → match.
     * 2. If no ISBN on either, match by titleBn (case-insensitive trimmed) + author.
     * 3. A master entry with no local match → new book.
     * 4. A matched entry where local sellingPrice != master mrp → price change.
     *
     * @param masterCatalog all entries from the Firestore masterCatalog collection
     * @param localBooks all active books from the local Room books table
     */
    fun detectDelta(
        masterCatalog: List<MasterCatalogEntry>,
        localBooks: List<LocalBook>,
    ): CatalogDelta {
        // Build a lookup of local books by ISBN (non-null only)
        val localByIsbn: Map<String, LocalBook> = localBooks
            .filter { !it.isbn.isNullOrBlank() }
            .associateBy { it.isbn!! }

        // Build a lookup of local books by normalized titleBn + author
        val localByTitleAuthor: Map<String, LocalBook> = localBooks.associateBy {
            normalizeKey(it.titleBn)
        }

        val newBooks = mutableListOf<NewBook>()
        val priceChanges = mutableListOf<PriceChange>()

        for (master in masterCatalog) {
            if (!master.isActive) continue // skip inactive master entries

            val localMatch = if (!master.isbn.isNullOrBlank()) {
                localByIsbn[master.isbn]
            } else {
                null
            } ?: localByTitleAuthor[normalizeKey(master.titleBn)]

            if (localMatch == null) {
                newBooks.add(NewBook(master))
            } else {
                // Check for price change (local sellingPrice != master mrp)
                if (localMatch.sellingPrice != master.mrp) {
                    priceChanges.add(
                        PriceChange(
                            localBookId = localMatch.id,
                            localPrice = localMatch.sellingPrice,
                            masterPrice = master.mrp,
                            masterEntry = master,
                        )
                    )
                }
            }
        }

        return CatalogDelta(newBooks, priceChanges)
    }

    /** Normalize a key for case-insensitive, whitespace-trimmed matching. */
    private fun normalizeKey(s: String): String = s.trim().lowercase()
}
