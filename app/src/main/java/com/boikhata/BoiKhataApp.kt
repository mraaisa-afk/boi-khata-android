package com.boikhata

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
import javax.inject.Inject

/**
 * D82: Force Bengali locale on app startup so all Bangladeshi shopkeepers see the app in
 * Bengali regardless of their device's system locale (many Samsung/Xiaomi phones ship in English
 * by default).
 *
 * Android resource resolution follows the device locale — without this override, an English device
 * loads values-en/strings.xml and the entire UI renders in English, which the 45-year-old
 * bookshop owner cannot read.
 *
 * IMPORTANT: The locale override is placed AFTER super.onCreate() to avoid breaking Firebase
 * and Hilt resource initialization, which happens during the super call.
 */
@HiltAndroidApp
class BoiKhataApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // D82: lock the locale to Bengali (Bangladesh) AFTER Firebase/Hilt init
        val bnBD = Locale("bn", "BD")
        Locale.setDefault(bnBD)
        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(bnBD)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
