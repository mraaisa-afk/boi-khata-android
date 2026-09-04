package com.boikhata.core.domain.session

import com.boikhata.core.domain.enums.Role
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionManagerTest {

    private val NOW = 1_700_000_000_000L
    private val SEC = 1_000L
    private val MIN = 60_000L

    @Test
    fun `should not be logged in before session start`() {
        val sm = SessionManager()
        assertThat(sm.isLoggedIn()).isFalse()
        assertThat(sm.isLocked(NOW)).isTrue()
    }

    @Test
    fun `should be logged in and unlocked after session start`() {
        val sm = SessionManager()
        sm.startSession("u1", Role.SALES, "t1", NOW)
        assertThat(sm.isLoggedIn()).isTrue()
        assertThat(sm.isLocked(NOW)).isFalse()
    }

    @Test
    fun `should auto-lock after 2 minutes for non-OWNER`() {
        val sm = SessionManager()
        sm.startSession("u1", Role.SALES, "t1", NOW)
        // 2 min + 1 ms → locked
        assertThat(sm.isLocked(NOW + 2 * MIN + 1)).isTrue()
    }

    @Test
    fun `should NOT auto-lock for OWNER`() {
        val sm = SessionManager()
        sm.startSession("u1", Role.OWNER, "t1", NOW)
        // even after 1 hour, OWNER stays unlocked
        assertThat(sm.isLocked(NOW + 60 * MIN)).isFalse()
    }

    @Test
    fun `should reset idle timer on interaction`() {
        val sm = SessionManager()
        sm.startSession("u1", Role.MANAGER, "t1", NOW)
        // at 1.5 min, interact → clock resets
        sm.recordInteraction(NOW + 90_000)
        // 2.5 min after that (2.5 min total) → still unlocked (1 min since last interaction)
        assertThat(sm.isLocked(NOW + 150_000)).isFalse()
        // 2 min after interaction → locked
        assertThat(sm.isLocked(NOW + 90_000 + 2 * MIN + 1)).isTrue()
    }

    @Test
    fun `should lock manually`() {
        val sm = SessionManager()
        sm.startSession("u1", Role.OWNER, "t1", NOW)
        sm.lock()
        assertThat(sm.isLocked(NOW)).isTrue()
    }

    @Test
    fun `should clear session on endSession`() {
        val sm = SessionManager()
        sm.startSession("u1", Role.SALES, "t1", NOW)
        sm.endSession()
        assertThat(sm.isLoggedIn()).isFalse()
        assertThat(sm.getCurrentUserId()).isNull()
    }

    @Test
    fun `should not lock exactly at 2 minutes (boundary)`() {
        val sm = SessionManager()
        sm.startSession("u1", Role.SALES, "t1", NOW)
        assertThat(sm.isLocked(NOW + 2 * MIN)).isFalse()
    }
}
