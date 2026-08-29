package com.boikhata.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** CONVENTIONS §3: khata_customers(id PK, tenantId, nameBn, phone?, address?, creditLimit,
 *  isActive, createdAt, updatedAt) */
@Entity(tableName = "khata_customers")
data class KhataCustomerEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val nameBn: String,
    val phone: String?,
    val address: String?,
    val creditLimit: Double,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

/** CONVENTIONS §3 🔒: khata_entries(id PK, tenantId, customerId, amount, type, description,
 *  referenceBillId?, collectedByUserId, date, idempotencyKey) — append-only */
@Entity(tableName = "khata_entries")
data class KhataEntryEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val customerId: String,
    val amount: Double,
    val type: String,
    val description: String,
    val referenceBillId: String?,
    val collectedByUserId: String,
    val date: Long,
    val idempotencyKey: String,
)

/** CONVENTIONS §3: khata_installments(id PK, tenantId, customerId, khataEntryId, dueDate,
 *  amount, isPaid) */
@Entity(tableName = "khata_installments")
data class KhataInstallmentEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val customerId: String,
    val khataEntryId: String,
    val dueDate: Long,
    val amount: Double,
    val isPaid: Boolean,
)
