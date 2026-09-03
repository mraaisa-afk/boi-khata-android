package com.boikhata.core.cloud.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
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
}
