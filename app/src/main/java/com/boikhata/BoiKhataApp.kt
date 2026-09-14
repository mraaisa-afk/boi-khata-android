package com.boikhata

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration as WorkManagerConfiguration
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
import javax.inject.Inject

/**
 * D82: Force Bengali (Bangladesh) locale on app startup.
 *
 * Placed in attachBaseContext() so the Application context is created with Bengali locale
 * — Firebase and Hilt then initialize with Bengali strings from day one. No runtime
 * updateConfiguration() call that can crash Firebase mid-initialization.
 *
 * Android resource resolution follows the device locale — without this, an English device
 * loads values-en/strings.xml and the entire UI renders in English, which the 45-year-old
 * Bangladeshi bookshop owner cannot read.
 *
 * attachBaseContext() is the official Android place for locale overrides (see
 * https://developer.android.com/reference/android/app/Application#attachBaseContext(android.content.Context) ).
 */
@HiltAndroidApp
class BoiKhataApp : Application() {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun attachBaseContext(base: Context) {
        val bnBD = Locale("bn", "BD")
        Locale.setDefault(bnBD)
        val config = Configuration(base.resources.configuration)
        config.setLocale(bnBD)
        val context = base.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    // WorkManager initialization — done lazily to avoid calling Hilt-injected
    // dependencies too early in attachBaseContext.
    private val wmConfig: WorkManagerConfiguration
        get() = WorkManagerConfiguration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    // WorkManagerConfiguration.Provider — delegates to lazy getter
    override val workManagerConfiguration: WorkManagerConfiguration
        get() = wmConfig
}
