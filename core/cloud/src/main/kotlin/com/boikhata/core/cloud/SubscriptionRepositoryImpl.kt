package com.boikhata.core.cloud

import com.boikhata.core.domain.cloud.SubscriptionRecord
import com.boikhata.core.domain.enums.Role
import com.boikhata.core.domain.repository.SubscriptionResult
import com.boikhata.core.domain.repository.SubscriptionRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D48: SubscriptionRepositoryImpl — records a manual bKash payment to Firestore.
 *
 * Firebase-Project-Context.md §3: subscription_payments — "client create ONLY with status == 'PENDING'".
 * §4 rules: "allow create: if isSameTenantIncoming() && isOwner() && request.resource.data.status == 'PENDING'".
 * §5: "TrxID is OPTIONAL in-app (never require it)."
 *
 * Gate: role == OWNER (rules deny non-OWNER create).
 * The status is ALWAYS "PENDING" — the vendor runs renew.js to activate.
 *
 * NOTE: Actual Firestore round-trip requires a real device + network.
 */
@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : SubscriptionRepository {

    override suspend fun recordPayment(
        tenantId: String,
        role: Role,
        amount: Double,
        trxId: String?,
        note: String?,
    ): SubscriptionResult {
        if (role != Role.OWNER) return SubscriptionResult.NotOwner

        return try {
            val paymentId = UUID.randomUUID().toString()
            val data = SubscriptionRecord.toFirestoreMap(
                tenantId = tenantId,
                amount = amount,
                trxId = trxId,
                note = note,
                createdAt = System.currentTimeMillis(),
            )
            firestore.collection("subscription_payments")
                .document(paymentId)
                .set(data)
                .await()
            SubscriptionResult.Success(paymentId)
        } catch (e: Exception) {
            SubscriptionResult.Error(e.message ?: "subscription error")
        }
    }
}
