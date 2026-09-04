package com.boikhata.core.database

import com.boikhata.core.database.seed.DatabaseSeeder
import javax.inject.Inject

class DemoResetter @Inject constructor(
    private val database: BoiKhataDatabase,
    private val seeder: DatabaseSeeder,
) {
    suspend fun reset() {
        database.clearAllTables()
        seeder.seedIfEmpty()
    }
}
