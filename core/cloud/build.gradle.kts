plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.boikhata.core.cloud"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}
