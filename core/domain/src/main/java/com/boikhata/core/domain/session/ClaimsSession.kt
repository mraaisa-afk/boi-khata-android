package com.boikhata.core.domain.session

import com.boikhata.core.domain.enums.Role
import com.boikhata.core.domain.model.CloudUser

/**
 * D40: ClaimsSession — the cloud session state machine.
 * Firebase-Project-Context.md §2: Phone OTP → ID token claims {tenantId, role}.
 * The app NEVER writes claims — they are vendor-side via Admin SDK.
 *
 * States:
 * - Unauthenticated: no Firebase user
 * - Authenticating: OTP verification in progress
 * - PendingActivation: authenticated but no claims (vendor hasn't provisioned yet)
 * - AuthenticatedWithClaims: has tenantId + role — cloud session active
 *
 * Pure function — no Android, no Firebase. Independently unit-testable.
 */
object ClaimsSession {

    sealed class State {
        data object Unauthenticated : State()
        data object Authenticating : State()
        data class PendingActivation(val phone: String) : State()
        data class AuthenticatedWithClaims(
            val uid: String,
            val phone: String,
            val tenantId: String,
            val role: Role,
        ) : State()
    }

    /**
     * Derive the session state from a CloudUser (or null).
     * @param user the current Firebase user, or null if signed out
     * @param isAuthenticating true if OTP verification is in progress
     */
    fun deriveState(user: CloudUser?, isAuthenticating: Boolean = false): State {
        if (isAuthenticating) return State.Authenticating
        if (user == null) return State.Unauthenticated
        if (!user.hasClaims) return State.PendingActivation(user.phone)
        return State.AuthenticatedWithClaims(
            uid = user.uid,
            phone = user.phone,
            tenantId = user.tenantId!!,
            role = user.role!!,
        )
    }

    /**
     * Check if a rebind is needed: the local seed tenantId "t_1" differs from the claims tenantId.
     * @param localTenantId the current local tenantId (from cloud_sync_state)
     * @param claimsTenantId the tenantId from the ID token claims
     * @return true if rebind is needed (local != claims and local is the seed "t_1")
     */
    fun needsRebind(localTenantId: String, claimsTenantId: String): Boolean {
        return localTenantId != claimsTenantId
    }
}
