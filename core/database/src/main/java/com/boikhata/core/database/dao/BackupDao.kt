package com.boikhata.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.boikhata.core.database.entity.BillEntity
import com.boikhata.core.database.entity.BillLineEntity
import com.boikhata.core.database.entity.BookEntity
import com.boikhata.core.database.entity.CashbookEntryEntity
import com.boikhata.core.database.entity.ExpenseCategoryEntity
import com.boikhata.core.database.entity.ExpenseEntity
import com.boikhata.core.database.entity.KhataCustomerEntity
import com.boikhata.core.database.entity.KhataEntryEntity
import com.boikhata.core.database.entity.OwnerDrawingEntity
import com.boikhata.core.database.entity.StockLedgerEntity

/**
 * D46: BackupDao — reads all rows per collection for a tenant (for incremental backup).
 * The backup repository reads all rows, then filters via BackupMapper.filterNewRows.
 * Constraint #5: per-collection reads (not one giant query).
 */
@Dao
interface BackupDao {
    @Query("SELECT * FROM books WHERE tenantId = :tenantId")
    suspend fun getBooksForBackup(tenantId: String): List<BookEntity>

    @Query("SELECT * FROM stock_ledger WHERE tenantId = :tenantId")
    suspend fun getStockLedgerForBackup(tenantId: String): List<StockLedgerEntity>

    @Query("SELECT * FROM bills WHERE tenantId = :tenantId")
    suspend fun getBillsForBackup(tenantId: String): List<BillEntity>

    @Query("SELECT * FROM bill_lines WHERE tenantId = :tenantId")
    suspend fun getBillLinesForBackup(tenantId: String): List<BillLineEntity>

    @Query("SELECT * FROM khata_customers WHERE tenantId = :tenantId")
    suspend fun getKhataCustomersForBackup(tenantId: String): List<KhataCustomerEntity>

    @Query("SELECT * FROM khata_entries WHERE tenantId = :tenantId")
    suspend fun getKhataEntriesForBackup(tenantId: String): List<KhataEntryEntity>

    @Query("SELECT * FROM expenses WHERE tenantId = :tenantId")
    suspend fun getExpensesForBackup(tenantId: String): List<ExpenseEntity>

    @Query("SELECT * FROM cashbook_entries WHERE tenantId = :tenantId")
    suspend fun getCashbookEntriesForBackup(tenantId: String): List<CashbookEntryEntity>

    @Query("SELECT * FROM expense_categories WHERE tenantId = :tenantId")
    suspend fun getExpenseCategoriesForBackup(tenantId: String): List<ExpenseCategoryEntity>

    @Query("SELECT * FROM owner_drawings WHERE tenantId = :tenantId")
    suspend fun getOwnerDrawingsForBackup(tenantId: String): List<OwnerDrawingEntity>

    // ── Restore: count rows to detect both-sides-have-data ────────────────────
    @Query("SELECT COUNT(*) FROM books WHERE tenantId = :tenantId")
    suspend fun countBooks(tenantId: String): Int

    @Query("SELECT COUNT(*) FROM bills WHERE tenantId = :tenantId")
    suspend fun countBills(tenantId: String): Int

    @Query("SELECT COUNT(*) FROM khata_entries WHERE tenantId = :tenantId")
    suspend fun countKhataEntries(tenantId: String): Int

    @Query("SELECT COUNT(*) FROM cashbook_entries WHERE tenantId = :tenantId")
    suspend fun countCashbookEntries(tenantId: String): Int
}
