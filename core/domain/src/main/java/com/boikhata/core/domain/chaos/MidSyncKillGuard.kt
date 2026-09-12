package com.boikhata.core.domain.chaos

/**
 * D80: MidSyncKillGuard — evaluates whether a backup can safely resume after
 * a mid-backup process kill (airplane-mode cut, ANR, OOM kill, etc.).
 *
 * Safety contract:
 *  1. Append-only tables with idempotencyKey: a re-upload of the same row is a
 *     no-op in Firestore (create-only rules + same document ID = no-op on collision).
 *  2. [lastBackupAt] timestamp acts as the resume filter: only rows with
 *     updatedAt/createdAt > lastBackupAt are re-uploaded after a kill.
 *  3. A fully committed backup updates [lastBackupAt] atomically AFTER all batches
 *     succeed. A mid-kill leaves [lastBackupAt] at the pre-run value, so the next
 *     run safely re-uploads the incomplete collections.
 *
 * This is a PURE domain service — no Room/Firebase dependency.
 */
object MidSyncKillGuard {

    data class BackupCheckpoint(
        /** Epoch-millis of the last fully committed batch, or 0 if never backed up. */
        val lastBackupAt: Long,
        /** Number of collections fully committed in the interrupted run (0–10). */
        val collectionsCompleted: Int,
        /** Total collections in the backup scope (10 per CONVENTIONS §5). */
        val totalCollections: Int = 10,
    )

    data class ResumeAssessment(
        val isSafeToResume: Boolean,
        val pendingCollections: Int,
        val risk: ResumeRisk,
        val message: String,
    )

    enum class ResumeRisk {
        /** Clean state or fully committed — no risk. */
        NONE,
        /** Mid-kill detected; idempotencyKey prevents duplicates. */
        LOW,
        /** lastBackupAt == 0 but collectionsCompleted > 0 — state inconsistency. */
        HIGH,
    }

    /**
     * Assesses whether resuming from [checkpoint] is safe.
     *
     * - completed == total → previous run fully committed; no resume needed.
     * - 0 < completed < total → mid-kill; safe to resume (idempotencyKey guarantees).
     * - completed > 0 && lastBackupAt == 0 → HIGH risk; manual inspection needed.
     * - completed == 0 && lastBackupAt == 0 → fresh first backup; safe.
     */
    fun assess(checkpoint: BackupCheckpoint): ResumeAssessment {
        val pending = checkpoint.totalCollections - checkpoint.collectionsCompleted

        return when {
            checkpoint.collectionsCompleted == checkpoint.totalCollections -> ResumeAssessment(
                isSafeToResume = true,
                pendingCollections = 0,
                risk = ResumeRisk.NONE,
                message = "Previous backup fully committed. No resume needed.",
            )
            checkpoint.collectionsCompleted > 0 && checkpoint.lastBackupAt == 0L -> ResumeAssessment(
                isSafeToResume = false,
                pendingCollections = pending,
                risk = ResumeRisk.HIGH,
                message = "State inconsistency: collectionsCompleted=${checkpoint.collectionsCompleted} " +
                    "but lastBackupAt=0. Manual inspection required before resuming.",
            )
            checkpoint.collectionsCompleted in 1 until checkpoint.totalCollections -> ResumeAssessment(
                isSafeToResume = true,
                pendingCollections = pending,
                risk = ResumeRisk.LOW,
                message = "Mid-kill detected: ${checkpoint.collectionsCompleted}/${checkpoint.totalCollections} " +
                    "collections committed. Safe to resume — idempotencyKey prevents duplicates " +
                    "(ARCHITECTURE C1/C2, D46).",
            )
            else -> ResumeAssessment(
                isSafeToResume = true,
                pendingCollections = checkpoint.totalCollections,
                risk = ResumeRisk.NONE,
                message = "No prior backup found. Fresh full backup will run.",
            )
        }
    }

    /**
     * Returns true if all [idempotencyKeys] are unique.
     * A duplicate key in the upload set indicates a contract violation (D46/D70).
     */
    fun verifyIdempotencyKeys(idempotencyKeys: List<String>): Boolean =
        idempotencyKeys.size == idempotencyKeys.toSet().size
}
