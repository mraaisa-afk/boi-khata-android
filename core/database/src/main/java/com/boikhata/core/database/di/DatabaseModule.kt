package com.boikhata.core.database.di

import android.content.Context
import androidx.room.Room
import com.boikhata.core.database.BoiKhataDatabase
import com.boikhata.core.database.dao.AuditLogDao
import com.boikhata.core.database.dao.BackupDao
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
import com.boikhata.core.database.dao.MelaDao
import com.boikhata.core.database.dao.OwnerDrawingDao
import com.boikhata.core.database.dao.PeriodLockDao
import com.boikhata.core.database.dao.RecurringExpenseDao
import com.boikhata.core.database.dao.StockLedgerDao
import com.boikhata.core.database.dao.SupplierDao
import com.boikhata.core.database.dao.TenantDao
import com.boikhata.core.database.dao.TenantRebindDao
import com.boikhata.core.database.dao.UserDao
import com.boikhata.core.database.migration.Migration1To2
import com.boikhata.core.database.migration.Migration2To3
import com.boikhata.core.database.migration.Migration3To4
import com.boikhata.core.database.seed.DatabaseSeeder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BoiKhataDatabase {
        // D16: Migration v1→v2 (normalized columns); D32/D35: Migration v2→v3 (accounting tables)
        return Room.databaseBuilder(
            context,
            BoiKhataDatabase::class.java,
            BoiKhataDatabase.DATABASE_NAME
        )
            .addMigrations(Migration1To2, Migration2To3, Migration3To4)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides fun provideTenantDao(db: BoiKhataDatabase): TenantDao = db.tenantDao()
    @Provides fun provideUserDao(db: BoiKhataDatabase): UserDao = db.userDao()
    @Provides fun provideDeviceDao(db: BoiKhataDatabase): DeviceDao = db.deviceDao()
    @Provides fun provideCloudSyncStateDao(db: BoiKhataDatabase): CloudSyncStateDao = db.cloudSyncStateDao()
    @Provides fun provideBookDao(db: BoiKhataDatabase): BookDao = db.bookDao()
    @Provides fun provideStockLedgerDao(db: BoiKhataDatabase): StockLedgerDao = db.stockLedgerDao()
    @Provides fun provideBillDao(db: BoiKhataDatabase): BillDao = db.billDao()
    @Provides fun provideKhataCustomerDao(db: BoiKhataDatabase): KhataCustomerDao = db.khataCustomerDao()
    @Provides fun provideKhataEntryDao(db: BoiKhataDatabase): KhataEntryDao = db.khataEntryDao()
    @Provides fun provideKhataInstallmentDao(db: BoiKhataDatabase): KhataInstallmentDao = db.khataInstallmentDao()
    @Provides fun provideExpenseCategoryDao(db: BoiKhataDatabase): ExpenseCategoryDao = db.expenseCategoryDao()
    @Provides fun provideExpenseDao(db: BoiKhataDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideCashbookDao(db: BoiKhataDatabase): CashbookDao = db.cashbookDao()
    @Provides fun provideOwnerDrawingDao(db: BoiKhataDatabase): OwnerDrawingDao = db.ownerDrawingDao()
    @Provides fun provideAuditLogDao(db: BoiKhataDatabase): AuditLogDao = db.auditLogDao()
    @Provides fun providePeriodLockDao(db: BoiKhataDatabase): PeriodLockDao = db.periodLockDao()
    @Provides fun provideRecurringExpenseDao(db: BoiKhataDatabase): RecurringExpenseDao = db.recurringExpenseDao()
    @Provides fun provideBudgetDao(db: BoiKhataDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideTenantRebindDao(db: BoiKhataDatabase): TenantRebindDao = db.tenantRebindDao()
    @Provides fun provideBackupDao(db: BoiKhataDatabase): BackupDao = db.backupDao()
    @Provides fun provideSupplierDao(db: BoiKhataDatabase): SupplierDao = db.supplierDao()
    @Provides fun provideMelaDao(db: BoiKhataDatabase): MelaDao = db.melaDao()

    @Provides
    @Singleton
    fun provideSeeder(
        tenantDao: TenantDao,
        userDao: UserDao,
        cloudSyncStateDao: CloudSyncStateDao,
        expenseCategoryDao: ExpenseCategoryDao,
    ): DatabaseSeeder = DatabaseSeeder(tenantDao, userDao, cloudSyncStateDao, expenseCategoryDao)
}
