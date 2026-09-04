package com.boikhata.core.domain.session

import com.boikhata.core.domain.enums.Role
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D10: timestamp-checked auto-lock, not a background timer.
 * ARCH §6: 2-minute auto-lock for non-OWNER; OWNER exempt.
 * Records lastInteractionAt on each UI touch; isLocked() checks now - last > 2 min.
 */
@Singleton
class SessionManager @Inject constructor() {

    private var currentUserId: String? = null
    private var currentRole: Role? = null
    private var currentTenantId: String? = null
    private var lastInteractionAt: Long = 0L
    private var lockedManually: Boolean = false

    fun startSession(userId: String, role: Role, tenantId: String, now: Long = System.currentTimeMillis()) {
        currentUserId = userId
        currentRole = role
        currentTenantId = tenantId
        lastInteractionAt = now
        lockedManually = false
    }

    fun recordInteraction(now: Long = System.currentTimeMillis()) {
        lastInteractionAt = now
    }

    fun lock() {
        lockedManually = true
    }

    fun endSession() {
        currentUserId = null
        currentRole = null
        currentTenantId = null
        lastInteractionAt = 0L
        lockedManually = false
    }

    fun isLocked(now: Long = System.currentTimeMillis()): Boolean {
        if (lockedManually) return true
        if (currentUserId == null) return true
        val role = currentRole ?: return true
        // OWNER exempt from auto-lock (Blueprint §7.1)
        if (role == Role.OWNER) return false
        val idleMs = now - lastInteractionAt
        return idleMs > AUTO_LOCK_MS
    }

    fun getCurrentUserId(): String? = currentUserId
    fun getCurrentRole(): Role? = currentRole
    fun getCurrentTenantId(): String? = currentTenantId
    fun isLoggedIn(): Boolean = currentUserId != null

    companion object {
        const val AUTO_LOCK_MS: Long = 2 * 60 * 1000 // 2 minutes
    }
}
