package com.boikhata.core.domain.model

import com.boikhata.core.domain.enums.BookCategory
import com.boikhata.core.domain.enums.BookCondition

/**
 * P2a: Domain model for a book in the local catalog.
 * No Room dependency — used by repository interfaces + ViewModels.
 */
data class Book(
    val id: String,
    val isbn: String?,
    val titleBn: String,
    val titleEn: String?,
    val author: String,
    val publisher: String,
    val classLevel: String,
    val subject: String,
    val editionYear: Int,
    val category: BookCategory,
    val condition: BookCondition,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val initialStock: Int,
    val lowStockThreshold: Int,
    val isActive: Boolean,
)
