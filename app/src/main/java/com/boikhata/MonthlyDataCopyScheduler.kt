package com.boikhata

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.boikhata.core.domain.trust.MonthlyCopySchedule
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class MonthlyDataCopyScheduler(private val context: Context) {
    fun enqueue(tenantId: String, shopName: String, nowMillis: Long = System.currentTimeMillis()) {
        val next = MonthlyCopySchedule.nextFirstDayAt(nowMillis, ZoneId.systemDefault())
        val delay = (next - nowMillis).coerceAtLeast(0L)
        val input = Data.Builder()
            .putString(MonthlyDataCopyWorker.KEY_TENANT_ID, tenantId)
            .putString(MonthlyDataCopyWorker.KEY_SHOP_NAME, shopName)
            .build()
        val request = OneTimeWorkRequestBuilder<MonthlyDataCopyWorker>()
            .setInputData(input)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    companion object { const val WORK_NAME = "monthly_data_copy" }
}
