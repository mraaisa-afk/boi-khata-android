package com.boikhata.core.cloud.di

import com.boikhata.core.cloud.AuthRepositoryImpl
import com.boikhata.core.cloud.LicenseSyncRepositoryImpl
import com.boikhata.core.domain.repository.AuthRepository
import com.boikhata.core.domain.repository.LicenseSyncRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * P4a: Hilt module for the cloud layer — Firebase + repository bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CloudBindingsModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindLicenseSyncRepository(impl: LicenseSyncRepositoryImpl): LicenseSyncRepository
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
