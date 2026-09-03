package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.boikhata.core.database.entity.CloudSyncStateEntity

@Dao
interface CloudSyncStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: CloudSyncStateEntity)

    @Update
    suspend fun update(state: CloudSyncStateEntity)

    @Query("SELECT * FROM cloud_sync_state WHERE id = 'primary'")
    suspend fun get(): CloudSyncStateEntity?

    @Query("UPDATE cloud_sync_state SET licenseState = :state, updatedAt = :now WHERE id = 'primary'")
    suspend fun updateLicenseState(state: String, now: Long)

    @Query("UPDATE cloud_sync_state SET licenseExpiresAt = :expiresAt, updatedAt = :now WHERE id = 'primary'")
    suspend fun updateLicenseExpiry(expiresAt: Long?, now: Long)

    @Query("UPDATE cloud_sync_state SET wifiOnlySync = :enabled, updatedAt = :now WHERE id = 'primary'")
    suspend fun updateWifiOnlySync(enabled: Boolean, now: Long)

    @Query("UPDATE cloud_sync_state SET lastBackupAt = :lastBackupAt, updatedAt = :now WHERE id = 'primary'")
    suspend fun updateLastBackupAt(lastBackupAt: Long, now: Long)

    @Query("UPDATE cloud_sync_state SET lastRestoreAt = :lastRestoreAt, updatedAt = :now WHERE id = 'primary'")
    suspend fun updateLastRestoreAt(lastRestoreAt: Long, now: Long)

    @Query("UPDATE cloud_sync_state SET lastCatalogSyncAt = :lastCatalogSyncAt, updatedAt = :now WHERE id = 'primary'")
    suspend fun updateLastCatalogSyncAt(lastCatalogSyncAt: Long, now: Long)
}
