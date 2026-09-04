package com.boikhata.core.database.di

import com.boikhata.core.database.repository.AccountingRepositoryImpl
import com.boikhata.core.database.repository.BookRepositoryImpl
import com.boikhata.core.database.repository.BudgetRepositoryImpl
import com.boikhata.core.database.repository.CashCloseRepositoryImpl
import com.boikhata.core.database.repository.CashbookRepositoryImpl
import com.boikhata.core.database.repository.ExpenseRepositoryImpl
import com.boikhata.core.database.repository.KhataRepositoryImpl
import com.boikhata.core.database.repository.LicenseRepositoryImpl
import com.boikhata.core.database.repository.MelaRepositoryImpl
import com.boikhata.core.database.repository.OwnerDrawingRepositoryImpl
import com.boikhata.core.database.repository.PeriodLockCheckerImpl
import com.boikhata.core.database.repository.RecurringExpenseRepositoryImpl
import com.boikhata.core.database.repository.SaleRepositoryImpl
import com.boikhata.core.database.repository.SupplierRepositoryImpl
import com.boikhata.core.database.repository.TenantRebindRepositoryImpl
import com.boikhata.core.database.repository.TrialRedemptionRepositoryImpl
import com.boikhata.core.database.repository.UserRepositoryImpl
import com.boikhata.core.domain.accounting.PeriodLockChecker
import com.boikhata.core.domain.repository.AccountingRepository
import com.boikhata.core.domain.repository.BillRepository
import com.boikhata.core.domain.repository.BookRepository
import com.boikhata.core.domain.repository.BudgetRepository
import com.boikhata.core.domain.repository.CashCloseRepository
import com.boikhata.core.domain.repository.CashbookRepository
import com.boikhata.core.domain.repository.ExpenseRepository
import com.boikhata.core.domain.repository.KhataRepository
import com.boikhata.core.domain.repository.LicenseRepository
import com.boikhata.core.domain.repository.MelaRepository
import com.boikhata.core.domain.repository.OwnerDrawingRepository
import com.boikhata.core.domain.repository.RecurringExpenseRepository
import com.boikhata.core.domain.repository.SupplierRepository
import com.boikhata.core.domain.repository.TenantRebindRepository
import com.boikhata.core.domain.repository.TrialRedemptionRepository
import com.boikhata.core.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository

    @Binds @Singleton
    abstract fun bindKhataRepository(impl: KhataRepositoryImpl): KhataRepository

    @Binds @Singleton
    abstract fun bindBillRepository(impl: SaleRepositoryImpl): BillRepository

    @Binds @Singleton
    abstract fun bindLicenseRepository(impl: LicenseRepositoryImpl): LicenseRepository

    @Binds @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds @Singleton
    abstract fun bindCashbookRepository(impl: CashbookRepositoryImpl): CashbookRepository

    @Binds @Singleton
    abstract fun bindOwnerDrawingRepository(impl: OwnerDrawingRepositoryImpl): OwnerDrawingRepository

    // P3b bindings
    @Binds @Singleton
    abstract fun bindPeriodLockChecker(impl: PeriodLockCheckerImpl): PeriodLockChecker

    @Binds @Singleton
    abstract fun bindAccountingRepository(impl: AccountingRepositoryImpl): AccountingRepository

    @Binds @Singleton
    abstract fun bindRecurringExpenseRepository(impl: RecurringExpenseRepositoryImpl): RecurringExpenseRepository

    @Binds @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds @Singleton
    abstract fun bindCashCloseRepository(impl: CashCloseRepositoryImpl): CashCloseRepository

    @Binds @Singleton
    abstract fun bindTenantRebindRepository(impl: TenantRebindRepositoryImpl): TenantRebindRepository

    // P5: Supplier + Mela
    @Binds @Singleton
    abstract fun bindSupplierRepository(impl: SupplierRepositoryImpl): SupplierRepository

    @Binds @Singleton
    abstract fun bindMelaRepository(impl: MelaRepositoryImpl): MelaRepository

    @Binds @Singleton
    abstract fun bindTrialRedemptionRepository(impl: TrialRedemptionRepositoryImpl): TrialRedemptionRepository
}
