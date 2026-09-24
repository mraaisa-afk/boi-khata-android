plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

// P12/D92: export Room schema JSONs (v6/v7) for the MigrationTestHelper test.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.boikhata.core.database"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // P12/D92: export Room schema JSONs (v6/v7) for the MigrationTestHelper test;
    // the same dir is bundled as unit-test assets (Robolectric provides the
    // Instrumentation that MigrationTestHelper needs).
    sourceSets {
        getByName("test") {
            assets.srcDir("$projectDir/schemas")
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk) // B-005/B-006: mock Room withTransaction for createBill tests
    // P12/D92: MigrationTestHelper runs under Robolectric (JVM — no emulator needed).
    // Explicit coordinates: the P0 version-catalog is deliberately untouched.
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
}
