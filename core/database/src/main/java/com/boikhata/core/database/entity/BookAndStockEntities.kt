package com.boikhata.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** CONVENTIONS §3: books(id PK, tenantId, isbn?, titleBn, titleEn?, author, publisher, classLevel,
 *  subject, editionYear, category, condition, purchasePrice, sellingPrice, initialStock,
 *  lowStockThreshold, isActive, createdAt, updatedAt) */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val isbn: String?,
    val titleBn: String,
    val titleEn: String?,
    val author: String,
    val publisher: String,
    val classLevel: String,
    val subject: String,
    val editionYear: Int,
    val category: String,
    val condition: String,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val initialStock: Int,
    val lowStockThreshold: Int,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

/** CONVENTIONS §3 🔒: stock_ledger(id PK, tenantId, bookId, changeQuantity, reason, referenceId?,
 *  userId, timestamp, idempotencyKey) — append-only money table */
@Entity(tableName = "stock_ledger")
data class StockLedgerEntity(
    @PrimaryKey val id: String,
    val tenantId: String,
    val bookId: String,
    val changeQuantity: Int,
    val reason: String,
    val referenceId: String?,
    val userId: String,
    val timestamp: Long,
    val idempotencyKey: String,
)
