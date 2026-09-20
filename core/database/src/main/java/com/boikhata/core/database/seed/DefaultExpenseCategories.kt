package com.boikhata.core.database.seed

import com.boikhata.core.database.entity.ExpenseCategoryEntity

/**
 * B-013: the Blueprint §7.8 default expense categories as a single business
 * list. Two seeding paths consume the SAME slugs/names so the visible set can
 * never drift apart:
 *  - [DatabaseSeeder] — demo reset only, t_1 rows, legacy-stable ids
 *    ("ec_rent", "ec_ghori", …) — preserved byte-for-byte, guarded by
 *    DatabaseSeederTest's drift check.
 *  - ExpenseRepositoryImpl.seedDefaultCategoriesIfMissing — session bootstrap
 *    for the ACTIVE (claims) tenant, ids "<tenantId>-ec_<slug>".
 *
 * Id strategy: deterministic tenant-prefixed ids (not UUIDs) keep re-seeding
 * idempotent under the DAO's OnConflictStrategy.REPLACE, and cannot collide
 * across tenants because `id` is the PRIMARY KEY (ExpenseCategoryEntity).
 *
 * Contract: the "advance" slug is load-bearing — ExpenseViewModel's ঘরি
 * balance lookup and GoriBalanceCalculator consumers match on
 * `icon == "advance"`, never on id. Renaming it silently breaks the ঘরি feature.
 */
object DefaultExpenseCategories {

    /** slug (icon + id suffix) → Bangla display name, in Blueprint §7.8 order. */
    val ENTRIES: List<Pair<String, String>> = listOf(
        "rent" to "ভাড়া",
        "electricity" to "বিদ্যুৎ",
        "internet" to "ইন্টারনেট",
        "salary" to "বেতন",
        "advance" to "ঘরি/অ্যাডভান্স",
        "transport" to "পরিবহন",
        "mfs_fee" to "MFS-ফি",
        "other" to "অন্যান্য",
    )

    /** Tenant-scoped rows for the session-bootstrap seeding path (B-013). */
    fun entitiesForTenant(tenantId: String): List<ExpenseCategoryEntity> =
        ENTRIES.map { (slug, nameBn) ->
            ExpenseCategoryEntity(
                id = "$tenantId-ec_$slug",
                tenantId = tenantId,
                nameBn = nameBn,
                icon = slug,
                isActive = true,
            )
        }
}
