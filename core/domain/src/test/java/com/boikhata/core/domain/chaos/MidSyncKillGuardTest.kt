package com.boikhata.core.domain.chaos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** D80 — JUnit 4 (D69). */
class MidSyncKillGuardTest {

    @Test
    fun `should return NONE risk when previous backup was fully committed`() {
        val checkpoint = MidSyncKillGuard.BackupCheckpoint(
            lastBackupAt = System.currentTimeMillis(),
            collectionsCompleted = 10,
            totalCollections = 10,
        )
        val result = MidSyncKillGuard.assess(checkpoint)

        assertTrue(result.isSafeToResume)
        assertEquals(0, result.pendingCollections)
        assertEquals(MidSyncKillGuard.ResumeRisk.NONE, result.risk)
    }

    @Test
    fun `should return LOW risk and safe to resume when mid-kill detected`() {
        val checkpoint = MidSyncKillGuard.BackupCheckpoint(
            lastBackupAt = System.currentTimeMillis() - 60_000L,
            collectionsCompleted = 5,
            totalCollections = 10,
        )
        val result = MidSyncKillGuard.assess(checkpoint)

        assertTrue(result.isSafeToResume)
        assertEquals(5, result.pendingCollections)
        assertEquals(MidSyncKillGuard.ResumeRisk.LOW, result.risk)
    }

    @Test
    fun `should return HIGH risk when collectionsCompleted is nonzero but lastBackupAt is zero`() {
        val checkpoint = MidSyncKillGuard.BackupCheckpoint(
            lastBackupAt = 0L,
            collectionsCompleted = 3,
            totalCollections = 10,
        )
        val result = MidSyncKillGuard.assess(checkpoint)

        assertFalse(result.isSafeToResume)
        assertEquals(MidSyncKillGuard.ResumeRisk.HIGH, result.risk)
    }

    @Test
    fun `should return NONE risk for fresh first backup with no prior run`() {
        val checkpoint = MidSyncKillGuard.BackupCheckpoint(
            lastBackupAt = 0L,
            collectionsCompleted = 0,
            totalCollections = 10,
        )
        val result = MidSyncKillGuard.assess(checkpoint)

        assertTrue(result.isSafeToResume)
        assertEquals(10, result.pendingCollections)
        assertEquals(MidSyncKillGuard.ResumeRisk.NONE, result.risk)
    }

    @Test
    fun `should pass idempotency key verification when all keys are unique`() {
        val keys = listOf("t1_b1_PURCHASE", "t1_b2_PURCHASE", "t1_b3_PURCHASE")
        assertTrue(MidSyncKillGuard.verifyIdempotencyKeys(keys))
    }

    @Test
    fun `should fail idempotency key verification when duplicates exist`() {
        val keysWithDuplicate = listOf("t1_b1_PURCHASE", "t1_b2_PURCHASE", "t1_b1_PURCHASE")
        assertFalse(MidSyncKillGuard.verifyIdempotencyKeys(keysWithDuplicate))
    }

    @Test
    fun `should pass idempotency key check for empty list`() {
        assertTrue(MidSyncKillGuard.verifyIdempotencyKeys(emptyList()))
    }
}
