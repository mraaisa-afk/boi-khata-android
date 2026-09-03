package com.boikhata.core.domain.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D48: SubscriptionRecord unit tests — Firestore subscription_payments doc construction.
 * Firebase-Project-Context.md §3: client create ONLY with status == 'PENDING'.
 * §5: TrxID is OPTIONAL in-app (never require it).
 */
class SubscriptionRecordTest {

    @Test
    fun `toFirestoreMap should always set status to PENDING`() {
        val map = SubscriptionRecord.toFirestoreMap(
            tenantId = "t1", amount = 250.0, trxId = null, note = null, createdAt = 1000L,
        )
        assertThat(map["status"]).isEqualTo("PENDING")
    }

    @Test
    fun `toFirestoreMap should stamp tenantId from claims`() {
        val map = SubscriptionRecord.toFirestoreMap(
            tenantId = "claims_tenant", amount = 250.0, trxId = null, note = null, createdAt = 1000L,
        )
        assertThat(map["tenantId"]).isEqualTo("claims_tenant")
    }

    @Test
    fun `toFirestoreMap should include trxId when provided`() {
        val map = SubscriptionRecord.toFirestoreMap(
            tenantId = "t1", amount = 250.0, trxId = "8A9B7C12D3", note = null, createdAt = 1000L,
        )
        assertThat(map["trxId"]).isEqualTo("8A9B7C12D3")
    }

    @Test
    fun `toFirestoreMap should set trxId to null when blank`() {
        val map = SubscriptionRecord.toFirestoreMap(
            tenantId = "t1", amount = 250.0, trxId = "", note = "", createdAt = 1000L,
        )
        assertThat(map["trxId"]).isNull()
        assertThat(map["note"]).isNull()
    }

    @Test
    fun `toFirestoreMap should set trxId to null when not provided`() {
        val map = SubscriptionRecord.toFirestoreMap(
            tenantId = "t1", amount = 250.0, trxId = null, note = null, createdAt = 1000L,
        )
        assertThat(map["trxId"]).isNull()
        assertThat(map["note"]).isNull()
    }

    @Test
    fun `toFirestoreMap should include note when provided`() {
        val map = SubscriptionRecord.toFirestoreMap(
            tenantId = "t1", amount = 500.0, trxId = "TRX123", note = "২ মাসের পেমেন্ট", createdAt = 1000L,
        )
        assertThat(map["note"]).isEqualTo("২ মাসের পেমেন্ট")
    }

    @Test
    fun `toFirestoreMap should include amount and createdAt`() {
        val map = SubscriptionRecord.toFirestoreMap(
            tenantId = "t1", amount = 750.0, trxId = null, note = null, createdAt = 5000L,
        )
        assertThat(map["amount"]).isEqualTo(750.0)
        assertThat(map["createdAt"]).isEqualTo(5000L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toFirestoreMap should reject zero amount`() {
        SubscriptionRecord.toFirestoreMap(
            tenantId = "t1", amount = 0.0, trxId = null, note = null, createdAt = 1000L,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toFirestoreMap should reject negative amount`() {
        SubscriptionRecord.toFirestoreMap(
            tenantId = "t1", amount = -100.0, trxId = null, note = null, createdAt = 1000L,
        )
    }

    @Test
    fun `buildFields should construct PaymentFields with PENDING status`() {
        val fields = SubscriptionRecord.buildFields(
            tenantId = "t1", amount = 250.0, trxId = "TRX1", note = "নোট", createdAt = 1000L,
        )
        assertThat(fields.tenantId).isEqualTo("t1")
        assertThat(fields.amount).isEqualTo(250.0)
        assertThat(fields.trxId).isEqualTo("TRX1")
        assertThat(fields.note).isEqualTo("নোট")
        assertThat(fields.status).isEqualTo("PENDING")
        assertThat(fields.createdAt).isEqualTo(1000L)
    }

    @Test
    fun `MONTHLY_FEE should be 250`() {
        assertThat(SubscriptionRecord.MONTHLY_FEE).isEqualTo(250.0)
    }

    @Test
    fun `VENDOR_BKASH should be the vendor phone number`() {
        assertThat(SubscriptionRecord.VENDOR_BKASH).isEqualTo("+8801711468027")
    }

    @Test
    fun `STATUS_PENDING should be PENDING`() {
        assertThat(SubscriptionRecord.STATUS_PENDING).isEqualTo("PENDING")
    }
}
