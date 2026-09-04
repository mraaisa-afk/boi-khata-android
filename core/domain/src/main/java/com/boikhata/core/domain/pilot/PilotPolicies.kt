package com.boikhata.core.domain.pilot

import java.util.concurrent.TimeUnit

/** P7 trial rules; this service never hides or mutates business data. */
object TrialPolicy {
    const val MAX_BILLS = 100
    const val MAX_BOOKS = 200
    val DURATION_MILLIS: Long = TimeUnit.DAYS.toMillis(14)

    data class Usage(val bills: Int, val books: Int)
    data class Redemption(val deviceFingerprint: String, val phoneE164: String)
    data class KnownRedemption(val deviceFingerprint: String, val phoneE164: String)
    data class Decision(
        val canWrite: Boolean,
        val canBackup: Boolean,
        val readOnly: Boolean,
        val reason: Reason,
    )

    enum class Reason { ACTIVE, EXPIRED, BILL_CAP, BOOK_CAP, ALREADY_REDEEMED }

    class CapExceededException(val reason: Reason) : IllegalStateException(reason.name)

    fun assertCanAddBill(usage: Usage) {
        if (usage.bills >= MAX_BILLS) throw CapExceededException(Reason.BILL_CAP)
    }

    fun assertCanAddBook(usage: Usage) {
        if (usage.books >= MAX_BOOKS) throw CapExceededException(Reason.BOOK_CAP)
    }

    fun evaluate(startedAt: Long, now: Long, usage: Usage): Decision {
        require(startedAt > 0)
        require(now >= startedAt)
        return when {
            now - startedAt >= DURATION_MILLIS -> Decision(false, false, true, Reason.EXPIRED)
            usage.bills >= MAX_BILLS -> Decision(false, false, true, Reason.BILL_CAP)
            usage.books >= MAX_BOOKS -> Decision(false, false, true, Reason.BOOK_CAP)
            else -> Decision(true, false, false, Reason.ACTIVE)
        }
    }

    fun canRedeem(redemption: Redemption, known: List<KnownRedemption>): Boolean =
        known.none {
            it.deviceFingerprint == redemption.deviceFingerprint || it.phoneE164 == redemption.phoneE164
        }
}

/** Explicit migration hand-off; claims are always issued by the vendor, never by the app. */
object NumberMigrationPolicy {
    enum class State { IDLE, OTP_VERIFIED, REBOUND, CLAIMS_TRANSFERRED }

    fun advance(state: State, otpVerified: Boolean, tenantRebound: Boolean, claimsTransferred: Boolean): State {
        require(!(claimsTransferred && !tenantRebound))
        require(!(tenantRebound && !otpVerified))
        return when {
            claimsTransferred -> State.CLAIMS_TRANSFERRED
            tenantRebound -> State.REBOUND
            otpVerified -> State.OTP_VERIFIED
            else -> state
        }
    }

    fun canUseNewNumber(state: State): Boolean = state == State.CLAIMS_TRANSFERRED
}
