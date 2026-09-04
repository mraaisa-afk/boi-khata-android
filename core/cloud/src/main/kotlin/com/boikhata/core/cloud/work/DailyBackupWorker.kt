package com.boikhata.core.cloud.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.boikhata.core.cloud.BackupRepositoryImpl
import com.boikhata.core.database.dao.CloudSyncStateDao
import com.boikhata.core.domain.enums.Role
import com.boikhata.core.domain.repository.BackupResult
import com.boikhata.core.domain.repository.BackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * D50: DailyBackupWorker — background scheduled backup.
 *
 * ARCHITECTURE §1: "ব্যাকগ্রাউন্ড: WorkManager."
 * Gate: OWNER-only (rules deny non-OWNER writes on most collections).
 * The worker reads cloud_sync_state.cloudRole — if not OWNER, no-ops (success, not failure).
 * If BackupResult.Error or Partial → Result.retry() (transient failure with backoff).
 *
 * NOTE: Actual WorkManager scheduling + Firestore round-trip requires a real device.
 * The worker logic is testable via androidx-work-testing with a fake BackupRepository.
 */
@HiltWorker
class DailyBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val cloudSyncStateDao: CloudSyncStateDao,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "daily_backup"
    }

    override suspend fun doWork(): Result {
        val syncState = cloudSyncStateDao.get() ?: return Result.success()

        // Gate: OWNER-only (rules deny non-OWNER writes)
        val roleStr = syncState.cloudRole
        val role = try { Role.valueOf(roleStr ?: "") } catch (e: Exception) { return Result.success() }
        if (role != Role.OWNER) return Result.success()

        // Gate: rebind must be done first
        if (syncState.isPendingActivation) return Result.success()

        return when (val result = backupRepository.backup(syncState.tenantId, role)) {
            is BackupResult.Success -> Result.success()
            is BackupResult.Partial -> Result.retry() // some collections failed — retry
            is BackupResult.Error -> Result.retry()
            is BackupResult.NotOwner -> Result.success() // expected condition, not a failure
            is BackupResult.RebindNeeded -> Result.success() // expected condition
        }
    }
}
