package com.boikhata.core.domain.di

import com.boikhata.core.domain.license.LicenseWriteGuard
import com.boikhata.core.domain.session.BiometricAuthenticator
import com.boikhata.core.domain.session.NoOpBiometricAuthenticator
import com.boikhata.core.domain.session.SessionManager
import com.boikhata.core.domain.datameter.DataMeter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides @Singleton
    fun provideLicenseWriteGuard(): LicenseWriteGuard = LicenseWriteGuard()

    @Provides @Singleton
    fun provideSessionManager(): SessionManager = SessionManager()

    @Provides @Singleton
    fun provideDataMeter(): DataMeter = DataMeter()

    @Provides @Singleton
    fun provideBiometricAuthenticator(): BiometricAuthenticator = NoOpBiometricAuthenticator
}
