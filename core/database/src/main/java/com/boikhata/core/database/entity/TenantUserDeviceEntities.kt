package com.boikhata.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** CONVENTIONS §3: tenants(id PK, name, phone, createdAt) */
@Entity(tableName = "tenants")
data class TenantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val createdAt: Long,
)

/** CONVENTIONS §3: users(id PK, tenantId, name, role, pinHash, salt, isActive, createdAt, updatedAt) */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val name: String,
    val role: String,
    val pinHash: String,
    val salt: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

/** CONVENTIONS §3: devices(id PK, tenantId, label, isPrimary, isActive, boundAt) */
@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val label: String,
    val isPrimary: Boolean,
    val isActive: Boolean,
    val boundAt: Long,
)
