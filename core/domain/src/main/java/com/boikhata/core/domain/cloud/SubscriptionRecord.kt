package com.boikhata.core.domain.cloud

/**
 * D48: SubscriptionRecord — pure construction of the Firestore subscription_payments document.
 *
 * Firebase-Project-Context.md §3: subscription_payments — "client create ONLY with status == 'PENDING'".
 * §4 rules: "allow create: if isSameTenantIncoming() && isOwner() && request.resource.data.status == 'PENDING'".
 * §5: "TrxID is OPTIONAL in-app (never require it)."
 *
 * Pure object — no Android, no Firestore. Independently unit-testable.
 */
object SubscriptionRecord {

    /** The subscription payment document fields. */
    data class PaymentFields(
        val tenantId: String,
        val amount: Double,
        val trxId: String?,   // optional — never required per §5
        val note: String?,    // optional
        val status: String,   // always "PENDING" per rules
        val createdAt: Long,
    )

    /** The monthly fee (৳250/month per Firebase-Project-Context.md §5). */
    const val MONTHLY_FEE = 250.0

    /** The vendor's bKash number (§5). */
    const val VENDOR_BKASH = "+8801711468027"

    /** Status is always PENDING — the rules deny any other status on create. */
    const val STATUS_PENDING = "PENDING"

    /**
     * Build the Firestore document map for a subscription payment record.
     * The status is ALWAYS "PENDING" — the vendor runs renew.js to activate.
     * trxId and note are optional (never required per §5).
     */
    fun toFirestoreMap(
        tenantId: String,
        amount: Double,
        trxId: String?,
        note: String?,
        createdAt: Long,
    ): Map<String, Any?> {
        require(amount > 0) { "amount must be > 0 (Firestore rules deny amount <= 0)" }
        return mapOf(
            "tenantId" to tenantId,
            "amount" to amount,
            "trxId" to trxId?.takeIf { it.isNotBlank() },
            "note" to note?.takeIf { it.isNotBlank() },
            "status" to STATUS_PENDING,
            "createdAt" to createdAt,
        )
    }

    /**
     * Build the PaymentFields data class (for unit testing the construction logic).
     */
    fun buildFields(
        tenantId: String,
        amount: Double,
        trxId: String?,
        note: String?,
        createdAt: Long,
    ): PaymentFields {
        require(amount > 0) { "amount must be > 0" }
        return PaymentFields(
            tenantId = tenantId,
            amount = amount,
            trxId = trxId?.takeIf { it.isNotBlank() },
            note = note?.takeIf { it.isNotBlank() },
            status = STATUS_PENDING,
            createdAt = createdAt,
        )
    }
}
