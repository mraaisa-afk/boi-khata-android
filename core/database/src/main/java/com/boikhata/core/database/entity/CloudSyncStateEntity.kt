package com.boikhata.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * CONVENTIONS §3: cloud_sync_state(id PK="primary", tenantId, cloudPhone, cloudRole,
 * isPendingActivation, lastBackupAt, lastRestoreAt, lastCatalogSyncAt,
 * licenseExpiresAt, licenseState=GRACE, updatedAt)
 * এক-সারি-টেবিল; আপসার্ট-বাধ্য (C9: licenseState from Room, never hardcoded; GRACE default).
 * wifiOnlySync added per D11 (amends CONVENTIONS §3 — ALTER-ADD column, migration-safe).
 */
@Entity(tableName = "cloud_sync_state")
data class CloudSyncStateEntity(
    @PrimaryKey val id: String = "primary",
    val tenantId: String,
    val cloudPhone: String?,
    val cloudRole: String?,
    val isPendingActivation: Boolean,
    val lastBackupAt: Long?,
    val lastRestoreAt: Long?,
    val lastCatalogSyncAt: Long?,
    val licenseExpiresAt: Long?,
    val licenseState: String = "GRACE",
    val wifiOnlySync: Boolean = true,
    val updatedAt: Long,
)
