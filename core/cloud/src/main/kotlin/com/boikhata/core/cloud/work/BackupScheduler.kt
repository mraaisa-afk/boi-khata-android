package com.boikhata.core.cloud.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * D50: BackupScheduler — enqueues the DailyBackupWorker as a periodic work.
 *
 * Schedule: daily (24-hour repeat interval).
 * Constraints: network connected (backup needs Firestore).
 * NOT requiring battery-not-low (the shopkeeper may plug in overnight).
 * Uses KEEP policy — if already scheduled, does not replace.
 */
@Singleton
class BackupScheduler @Inject constructor() {

    fun scheduleDailyBackup(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<DailyBackupWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DailyBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest,
        )
    }

    /**
     * P14 (owner device ruling — «ম্যানুয়ালি সিঙ্ক করুন» button): explicit one-time
     * run of the SAME OWNER-gated DailyBackupWorker the daily job uses — no separate
     * sync path. Unique-name + REPLACE so repeated taps coalesce instead of stacking.
     *
     * Forensics note: before P14, [scheduleDailyBackup] had NO call site on main —
     * the D50 periodic backup was never armed. The manual-sync entry point also
     * re-arms it (KEEP = no-op when already scheduled), healing the gap on devices
     * that visit the settings screen.
     */
    fun syncNow(context: Context) {
        scheduleDailyBackup(context) // self-heal: KEEP policy, no-op if already armed

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<DailyBackupWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            MANUAL_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    companion object {
        const val MANUAL_SYNC_WORK_NAME = "manual_backup"
    }
}
