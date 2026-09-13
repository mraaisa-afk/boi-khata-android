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
 * The override is applied BEFORE super.onCreate() so the Application's resource Configuration
 * is seeded with Bengali from the first LayoutInflator call.
 */
@HiltAndroidApp
class BoiKhataApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        // D82: lock the locale to Bengali (Bangladesh) before any UI is created
        val bnBD = Locale("bn", "BD")
        Locale.setDefault(bnBD)
        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(bnBD)
        resources.updateConfiguration(config, resources.displayMetrics)
        super.onCreate()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
