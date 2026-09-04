package com.boikhata

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.boikhata.core.database.dao.BackupDao
import com.boikhata.shared.receipt.MonthlyDataCopyBuilder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.time.LocalDate

@HiltWorker
class MonthlyDataCopyWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupDao: BackupDao,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val tenantId = inputData.getString(KEY_TENANT_ID) ?: return Result.failure()
        val shopName = inputData.getString(KEY_SHOP_NAME) ?: return Result.failure()
        val today = LocalDate.now()
        val bills = backupDao.getBillsForBackup(tenantId)
        val books = backupDao.getBooksForBackup(tenantId)
        val khata = backupDao.getKhataEntriesForBackup(tenantId)
        val stock = backupDao.getStockLedgerForBackup(tenantId)
        val rows = listOf(
            MonthlyDataCopyBuilder.Row("বিক্রি", "বিল", bills.size, bills.sumOf { it.totalAmount }),
            MonthlyDataCopyBuilder.Row("বই", "ক্যাটালগ", books.size, books.sumOf { it.sellingPrice }),
            MonthlyDataCopyBuilder.Row("খাতা", "এন্ট্রি", khata.size, khata.sumOf { it.amount }),
            MonthlyDataCopyBuilder.Row("স্টক", "চলাচল", stock.size, stock.sumOf { it.changeQuantity }.toDouble()),
        )
        val csv = MonthlyDataCopyBuilder.build(shopName, today.year, today.monthValue, rows)
        val dir = File(applicationContext.filesDir, "monthly-copies").apply { mkdirs() }
        val output = File(dir, "boi-khata-${today.year}-${today.monthValue}.csv")
        output.writeText(csv, Charsets.UTF_8)
        applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(LAST_COPY_PATH, output.absolutePath)
            .apply()
        return Result.success()
    }

    companion object {
        const val KEY_TENANT_ID = "tenant_id"
        const val KEY_SHOP_NAME = "shop_name"
        const val PREFS = "boi_khata_trust"
        const val LAST_COPY_PATH = "last_monthly_copy_path"
    }
}
