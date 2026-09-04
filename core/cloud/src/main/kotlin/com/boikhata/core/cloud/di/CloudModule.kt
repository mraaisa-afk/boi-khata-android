package com.boikhata.core.cloud.di

import com.boikhata.core.cloud.AuthRepositoryImpl
import com.boikhata.core.cloud.BackupRepositoryImpl
import com.boikhata.core.cloud.LicenseSyncRepositoryImpl
import com.boikhata.core.cloud.MasterCatalogRepositoryImpl
import com.boikhata.core.cloud.RestoreRepositoryImpl
import com.boikhata.core.cloud.SubscriptionRepositoryImpl
import com.boikhata.core.cloud.TenantInfoRepositoryImpl
import com.boikhata.core.domain.repository.AuthRepository
import com.boikhata.core.domain.repository.BackupRepository
import com.boikhata.core.domain.repository.LicenseSyncRepository
import com.boikhata.core.domain.repository.MasterCatalogRepository
import com.boikhata.core.domain.repository.RestoreRepository
import com.boikhata.core.domain.repository.SubscriptionRepository
import com.boikhata.core.domain.repository.TenantInfoRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * P4a/P4b: Hilt module for the cloud layer — Firebase + repository bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CloudBindingsModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindLicenseSyncRepository(impl: LicenseSyncRepositoryImpl): LicenseSyncRepository

    // P4b bindings
    @Binds @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds @Singleton
    abstract fun bindRestoreRepository(impl: RestoreRepositoryImpl): RestoreRepository

    @Binds @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

    @Binds @Singleton
    abstract fun bindMasterCatalogRepository(impl: MasterCatalogRepositoryImpl): MasterCatalogRepository

    @Binds @Singleton
    abstract fun bindTenantInfoRepository(impl: TenantInfoRepositoryImpl): TenantInfoRepository
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
