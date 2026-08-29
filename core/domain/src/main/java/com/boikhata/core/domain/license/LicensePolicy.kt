package com.boikhata.core.domain.license

import com.boikhata.core.domain.enums.LicenseState

/**
 * ARCH §5 — লাইসেন্সিং (লোকাল-ইঞ্জিন, ৩টি স্বাধীন-পরীক্ষাযোগ্য ফাংশন).
 * These are PURE functions — no Android, no Room, no Hilt. Independently unit-testable.
 *
 * স্টেট: FULL / PAID_UNVERIFIED / GRACE / SOFT_LOCKED(write-only-blocked) / SUSPENDED
 * প্রদত্ত-টেন্যান্ট কখনো READONLY নয়; পড়া/এক্সপোর্ট সর্বদা খোলা (never-lock).
 */

/** A festival window: [startMs, endMs] in epoch-millis. */
data class FestivalWindow(val startMs: Long, val endMs: Long)

/** Output of evaluateGrace — the computed state + metadata for UX. */
data class GraceState(
    val state: LicenseState,
    val daysUntilSoftLock: Long,
    val isWriteBlocked: Boolean,
    val isReadBlocked: Boolean,
)

object LicensePolicy {

    private const val GRACE_DAYS = 14L
    private const val SOFT_LOCK_DAYS = 30L
    private const val SUSPEND_DAYS = 60L
    private const val CONTACT_DEGRADE_DAYS = 35L
    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    /**
     * Law 1: isInsideFestivalWindow — true if `now` falls inside any window.
     * During a festival, paid tenants are never locked (Eid rule, Blueprint §5.4).
     */
    fun isInsideFestivalWindow(windows: List<FestivalWindow>, now: Long): Boolean {
        return windows.any { now >= it.startMs && now <= it.endMs }
    }

    /**
     * Law 2: evaluateGrace — grace = max(lastVerified, lastPayment) + 14 days.
     * A paid tenant never goes read-only in a connectivity dead zone (never-lock).
     * lastVerified/lastPayment = epoch-millis; null = never (treated as 0).
     */
    fun evaluateGrace(
        lastVerified: Long?,
        lastPayment: Long?,
        now: Long,
        isInsideFestival: Boolean = false,
    ): GraceState {
        val anchor = maxOf(lastVerified ?: 0L, lastPayment ?: 0L)
        val graceEnd = anchor + GRACE_DAYS * MILLIS_PER_DAY
        val softLockEnd = anchor + SOFT_LOCK_DAYS * MILLIS_PER_DAY
        val suspendEnd = anchor + SUSPEND_DAYS * MILLIS_PER_DAY

        val daysUntilSoftLock = ((softLockEnd - now) / MILLIS_PER_DAY).coerceAtLeast(0)

        return when {
            now < graceEnd -> GraceState(
                state = LicenseState.GRACE,
                daysUntilSoftLock = daysUntilSoftLock,
                isWriteBlocked = false,
                isReadBlocked = false,
            )
            isInsideFestival -> GraceState(
                // Festival protection: paid tenant stays GRACE during the window
                state = LicenseState.GRACE,
                daysUntilSoftLock = daysUntilSoftLock,
                isWriteBlocked = false,
                isReadBlocked = false,
            )
            now < softLockEnd -> GraceState(
                state = LicenseState.SOFT_LOCKED,
                daysUntilSoftLock = daysUntilSoftLock,
                isWriteBlocked = true,
                isReadBlocked = false, // never-lock: reads always open
            )
            now < suspendEnd -> GraceState(
                state = LicenseState.SUSPENDED,
                daysUntilSoftLock = 0,
                isWriteBlocked = true,
                isReadBlocked = false, // never-lock: reads always open
            )
            else -> GraceState(
                state = LicenseState.SUSPENDED,
                daysUntilSoftLock = 0,
                isWriteBlocked = true,
                isReadBlocked = false, // never-lock: even suspended, reads/exports open
            )
        }
    }

    /**
     * Law 3: evaluateContactDegrade — >35d no server contact → WARN (read stays open).
     * Returns true if the tenant should see a "stale data" warning.
     */
    fun evaluateContactDegrade(lastServerContact: Long?, now: Long): Boolean {
        if (lastServerContact == null) return true
        val daysSinceContact = (now - lastServerContact) / MILLIS_PER_DAY
        return daysSinceContact > CONTACT_DEGRADE_DAYS
    }
}
