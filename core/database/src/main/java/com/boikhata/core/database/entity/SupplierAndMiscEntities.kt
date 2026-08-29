package com.boikhata.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** CONVENTIONS §3: suppliers(id PK, tenantId, nameBn, phone?, settlementCycle, notes?) */
@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val nameBn: String,
    val phone: String?,
    val settlementCycle: String,
    val notes: String?,
)

/** CONVENTIONS §3 🔒: supplier_entries(id PK, tenantId, supplierId, amount, type, description,
 *  referenceId?, date, idempotencyKey) — append-only */
@Entity(tableName = "supplier_entries")
data class SupplierEntryEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val supplierId: String,
    val amount: Double,
    val type: String,
    val description: String,
    val referenceId: String?,
    val date: Long,
    val idempotencyKey: String,
)

/** CONVENTIONS §3: master_catalog(id PK, isbn?, titleBn, titleEn?, author, publisher, classLevel,
 *  subject, editionYear, mrp, isActive, lastUpdated) */
@Entity(tableName = "master_catalog")
data class MasterCatalogEntity(
    @PrimaryKey val id: String,
    val isbn: String?,
    val titleBn: String,
    val titleEn: String?,
    val author: String,
    val publisher: String,
    val classLevel: String,
    val subject: String,
    val editionYear: Int,
    val mrp: Double,
    val isActive: Boolean,
    val lastUpdated: Long,
)

/** CONVENTIONS §3 🔒: audit_logs(id PK, tenantId, userId, action, detail, timestamp)
 *  LOCAL-ONLY, কখনো আপলোড নয় (never uploaded to cloud) */
@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val userId: String,
    val action: String,
    val detail: String,
    val timestamp: Long,
)
