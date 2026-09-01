package com.boikhata.core.domain.session

import com.boikhata.core.domain.enums.Role
import com.boikhata.core.domain.model.CloudUser
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D40: ClaimsSession state machine unit tests.
 */
class ClaimsSessionTest {

    @Test
    fun `should return Unauthenticated when user is null`() {
        val state = ClaimsSession.deriveState(null)
        assertThat(state).isEqualTo(ClaimsSession.State.Unauthenticated)
    }

    @Test
    fun `should return Authenticating when flag is true regardless of user`() {
        val state = ClaimsSession.deriveState(null, isAuthenticating = true)
        assertThat(state).isEqualTo(ClaimsSession.State.Authenticating)
    }

    @Test
    fun `should return Authenticating even with a user present`() {
        val user = CloudUser("uid1", "+8801711468027", null, null)
        val state = ClaimsSession.deriveState(user, isAuthenticating = true)
        assertThat(state).isEqualTo(ClaimsSession.State.Authenticating)
    }

    @Test
    fun `should return PendingActivation when user has no claims`() {
        val user = CloudUser("uid1", "+8801711468027", tenantId = null, role = null)
        val state = ClaimsSession.deriveState(user)
        assertThat(state).isInstanceOf(ClaimsSession.State.PendingActivation::class.java)
        val pending = state as ClaimsSession.State.PendingActivation
        assertThat(pending.phone).isEqualTo("+8801711468027")
    }

    @Test
    fun `should return AuthenticatedWithClaims when user has tenantId and role`() {
        val user = CloudUser("uid1", "+8801711468027", "tenant_abc", Role.OWNER)
        val state = ClaimsSession.deriveState(user)
        assertThat(state).isInstanceOf(ClaimsSession.State.AuthenticatedWithClaims::class.java)
        val authed = state as ClaimsSession.State.AuthenticatedWithClaims
        assertThat(authed.tenantId).isEqualTo("tenant_abc")
        assertThat(authed.role).isEqualTo(Role.OWNER)
        assertThat(authed.uid).isEqualTo("uid1")
    }

    @Test
    fun `should return PendingActivation when tenantId present but role missing`() {
        val user = CloudUser("uid1", "+8801711468027", "tenant_abc", null)
        val state = ClaimsSession.deriveState(user)
        assertThat(state).isInstanceOf(ClaimsSession.State.PendingActivation::class.java)
    }

    @Test
    fun `should return PendingActivation when role present but tenantId missing`() {
        val user = CloudUser("uid1", "+8801711468027", null, Role.OWNER)
        val state = ClaimsSession.deriveState(user)
        assertThat(state).isInstanceOf(ClaimsSession.State.PendingActivation::class.java)
    }

    @Test
    fun `needsRebind should be true when local differs from claims`() {
        assertThat(ClaimsSession.needsRebind("t_1", "tenant_abc")).isTrue()
    }

    @Test
    fun `needsRebind should be false when local equals claims`() {
        assertThat(ClaimsSession.needsRebind("tenant_abc", "tenant_abc")).isFalse()
    }

    @Test
    fun `hasClaims should be false when both null`() {
        val user = CloudUser("uid1", "+880", null, null)
        assertThat(user.hasClaims).isFalse()
    }

    @Test
    fun `hasClaims should be true when both present`() {
        val user = CloudUser("uid1", "+880", "tenant_abc", Role.OWNER)
        assertThat(user.hasClaims).isTrue()
    }
}
