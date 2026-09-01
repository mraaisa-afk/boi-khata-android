package com.boikhata.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.boikhata.core.database.dao.AuditLogDao
import com.boikhata.core.database.dao.BillDao
import com.boikhata.core.database.dao.BookDao
import com.boikhata.core.database.dao.BudgetDao
import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.database.dao.CloudSyncStateDao
import com.boikhata.core.database.dao.DeviceDao
import com.boikhata.core.database.dao.ExpenseCategoryDao
import com.boikhata.core.database.dao.ExpenseDao
import com.boikhata.core.database.dao.KhataCustomerDao
import com.boikhata.core.database.dao.KhataEntryDao
import com.boikhata.core.database.dao.KhataInstallmentDao
import com.boikhata.core.database.dao.OwnerDrawingDao
import com.boikhata.core.database.dao.PeriodLockDao
import com.boikhata.core.database.dao.RecurringExpenseDao
import com.boikhata.core.database.dao.StockLedgerDao
import com.boikhata.core.database.dao.TenantDao
import com.boikhata.core.database.dao.UserDao
import com.boikhata.core.database.entity.AuditLogEntity
import com.boikhata.core.database.entity.BillEntity
import com.boikhata.core.database.entity.BillLineEntity
import com.boikhata.core.database.entity.BookEntity
import com.boikhata.core.database.entity.BudgetEntity
import com.boikhata.core.database.entity.CashbookEntryEntity
import com.boikhata.core.database.entity.CloudSyncStateEntity
import com.boikhata.core.database.entity.DeviceEntity
import com.boikhata.core.database.entity.ExpenseCategoryEntity
import com.boikhata.core.database.entity.ExpenseEntity
import com.boikhata.core.database.entity.KhataCustomerEntity
import com.boikhata.core.database.entity.KhataEntryEntity
import com.boikhata.core.database.entity.KhataInstallmentEntity
import com.boikhata.core.database.entity.MasterCatalogEntity
import com.boikhata.core.database.entity.OwnerDrawingEntity
import com.boikhata.core.database.entity.PeriodLockEntity
import com.boikhata.core.database.entity.RecurringExpenseEntity
import com.boikhata.core.database.entity.StockLedgerEntity
import com.boikhata.core.database.entity.SupplierEntity
import com.boikhata.core.database.entity.SupplierEntryEntity
import com.boikhata.core.database.entity.TenantEntity
import com.boikhata.core.database.entity.UserEntity

/**
 * CONVENTIONS §1: ডিভাইস-সত্য = Room; বিল্ডারে `.addMigrations(...)` বাধ্যতামূলক.
 * All 19 tables from CONVENTIONS §3 + 3 P3b tables (period_locks, recurring_expenses, budgets).
 * v1 = initial schema; v2 = D16 normalized columns; v3 = D32/D35 accounting tables.
 */
@Database(
    entities = [
        TenantEntity::class,
        UserEntity::class,
        DeviceEntity::class,
        CloudSyncStateEntity::class,
        BookEntity::class,
        StockLedgerEntity::class,
        BillEntity::class,
        BillLineEntity::class,
        KhataCustomerEntity::class,
        KhataEntryEntity::class,
        KhataInstallmentEntity::class,
        ExpenseCategoryEntity::class,
        ExpenseEntity::class,
        CashbookEntryEntity::class,
        OwnerDrawingEntity::class,
        SupplierEntity::class,
        SupplierEntryEntity::class,
        MasterCatalogEntity::class,
        AuditLogEntity::class,
        PeriodLockEntity::class,
        RecurringExpenseEntity::class,
        BudgetEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class BoiKhataDatabase : RoomDatabase() {
    abstract fun tenantDao(): TenantDao
    abstract fun userDao(): UserDao
    abstract fun deviceDao(): DeviceDao
    abstract fun cloudSyncStateDao(): CloudSyncStateDao
    abstract fun bookDao(): BookDao
    abstract fun stockLedgerDao(): StockLedgerDao
    abstract fun billDao(): BillDao
    abstract fun khataCustomerDao(): KhataCustomerDao
    abstract fun khataEntryDao(): KhataEntryDao
    abstract fun khataInstallmentDao(): KhataInstallmentDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun cashbookDao(): CashbookDao
    abstract fun ownerDrawingDao(): OwnerDrawingDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun periodLockDao(): PeriodLockDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        const val DATABASE_NAME = "boi-khata.db"
    }
}
