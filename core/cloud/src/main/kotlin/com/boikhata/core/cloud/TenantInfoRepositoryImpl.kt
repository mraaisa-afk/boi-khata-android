package com.boikhata.core.cloud

import com.boikhata.core.domain.repository.TenantInfoRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inherited from P4a deferred list: fetch the shop name from the tenants Firestore document.
 * Firebase-Project-Context.md §4: tenants/{tenantId} — "allow read: if isTenantUser(tenantId)".
 * The tenants doc has fields: name, phone, createdAt (per CONVENTIONS §3).
 * The shop name replaces the phone placeholder in MainViewModel.
 *
 * NOTE: Actual Firestore round-trip requires a real device + network.
 */
@Singleton
class TenantInfoRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : TenantInfoRepository {

    override suspend fun fetchShopName(tenantId: String): String? {
        return try {
            val snapshot = firestore.collection("tenants")
                .document(tenantId)
                .get()
                .await()
            if (!snapshot.exists()) return null
            // §6 constraint #7: always check exists()
            snapshot.getString("name")
        } catch (e: Exception) {
            null
        }
    }
}
