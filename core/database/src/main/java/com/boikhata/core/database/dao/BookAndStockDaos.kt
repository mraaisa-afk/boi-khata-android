package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.boikhata.core.database.entity.BookEntity
import com.boikhata.core.database.entity.StockLedgerEntity

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntity)

    @Update
    suspend fun update(book: BookEntity)

    @Query("SELECT * FROM books WHERE tenantId = :tenantId AND isActive = 1 ORDER BY titleBn")
    suspend fun getActiveByTenant(tenantId: String): List<BookEntity>

    @Query("SELECT * FROM books WHERE tenantId = :tenantId ORDER BY titleBn")
    suspend fun getAllByTenant(tenantId: String): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: String): BookEntity?

    @Query("""
        SELECT * FROM books
        WHERE tenantId = :tenantId AND isActive = 1
        AND (titleBnNormalized LIKE '%' || :normalizedQuery || '%'
             OR titleEn LIKE '%' || :normalizedQuery || '%'
             OR author LIKE '%' || :normalizedQuery || '%'
             OR isbn LIKE '%' || :normalizedQuery || '%')
        ORDER BY titleBn
    """)
    suspend fun search(tenantId: String, normalizedQuery: String): List<BookEntity>
}

@Dao
interface StockLedgerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: StockLedgerEntity)

    @Query("SELECT * FROM stock_ledger WHERE tenantId = :tenantId AND bookId = :bookId ORDER BY timestamp ASC")
    suspend fun getByBook(tenantId: String, bookId: String): List<StockLedgerEntity>
}
