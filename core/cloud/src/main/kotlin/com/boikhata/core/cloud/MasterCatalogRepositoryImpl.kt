package com.boikhata.core.cloud

import com.boikhata.core.database.dao.BookDao
import com.boikhata.core.database.dao.CloudSyncStateDao
import com.boikhata.core.database.entity.BookEntity
import com.boikhata.core.domain.cloud.CatalogDeltaDetector
import com.boikhata.core.domain.repository.CatalogRefreshResult
import com.boikhata.core.domain.repository.MasterCatalogRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D49: MasterCatalogRepositoryImpl — reads the shared NCTB master catalog from Firestore.
 *
 * Firebase-Project-Context.md §3: masterCatalog — "read: any authenticated; write: false."
 * The app reads it read-only. The catalog delta detector compares with local books.
 * One-tap "প্রয়োগ করুন" applies the master price to the local book (a local Room write).
 *
 * NOTE: Actual Firestore round-trip requires a real device + network.
 */
@Singleton
class MasterCatalogRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val bookDao: BookDao,
    private val cloudSyncStateDao: CloudSyncStateDao,
) : MasterCatalogRepository {

    override suspend fun refreshCatalog(tenantId: String): CatalogRefreshResult {
        return try {
            // Read all master catalog docs (not tenant-scoped — read: isAuthenticated)
            val snapshot = firestore.collection("masterCatalog").get().await()

            val masterEntries = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                CatalogDeltaDetector.MasterCatalogEntry(
                    id = doc.id,
                    isbn = data["isbn"] as? String,
                    titleBn = (data["titleBn"] as? String) ?: "",
                    titleEn = data["titleEn"] as? String,
                    author = (data["author"] as? String) ?: "",
                    publisher = (data["publisher"] as? String) ?: "",
                    classLevel = (data["classLevel"] as? String) ?: "",
                    subject = (data["subject"] as? String) ?: "",
                    editionYear = ((data["editionYear"] as? Number)?.toInt()) ?: 0,
                    mrp = (data["mrp"] as? Number)?.toDouble() ?: 0.0,
                    isActive = (data["isActive"] as? Boolean) ?: false,
                    lastUpdated = ((data["lastUpdated"] as? Number)?.toLong()) ?: 0L,
                )
            }

            // Read local books for comparison
            val localBooks = bookDao.getAllByTenant(tenantId).map {
                CatalogDeltaDetector.LocalBook(
                    id = it.id, isbn = it.isbn, titleBn = it.titleBn,
                    sellingPrice = it.sellingPrice,
                )
            }

            // Detect delta
            val delta = CatalogDeltaDetector.detectDelta(masterEntries, localBooks)

            // Update lastCatalogSyncAt
            cloudSyncStateDao.updateLastCatalogSyncAt(
                System.currentTimeMillis(), System.currentTimeMillis()
            )

            CatalogRefreshResult.Success(
                newBooks = delta.newBooks,
                priceChanges = delta.priceChanges,
                totalInMaster = masterEntries.size,
            )
        } catch (e: Exception) {
            // Network error / offline
            CatalogRefreshResult.Error(e.message ?: "catalog refresh error")
        }
    }

    override suspend fun applyPriceChange(tenantId: String, bookId: String, newPrice: Double): Boolean {
        return try {
            val book = bookDao.getById(bookId) ?: return false
            // Update local book's sellingPrice (a local Room write — not a Firestore write)
            bookDao.update(book.copy(sellingPrice = newPrice, updatedAt = System.currentTimeMillis()))
            true
        } catch (e: Exception) {
            false
        }
    }
}
