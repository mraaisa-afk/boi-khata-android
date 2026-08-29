package com.boikhata.core.database.di

import com.boikhata.core.database.repository.BillRepositoryImpl
import com.boikhata.core.database.repository.KhataRepositoryImpl
import com.boikhata.core.database.repository.LicenseRepositoryImpl
import com.boikhata.core.database.repository.UserRepositoryImpl
import com.boikhata.core.domain.repository.BillRepository
import com.boikhata.core.domain.repository.KhataRepository
import com.boikhata.core.domain.repository.LicenseRepository
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
    abstract fun bindKhataRepository(impl: KhataRepositoryImpl): KhataRepository

    @Binds @Singleton
    abstract fun bindBillRepository(impl: BillRepositoryImpl): BillRepository

    @Binds @Singleton
    abstract fun bindLicenseRepository(impl: LicenseRepositoryImpl): LicenseRepository
}
