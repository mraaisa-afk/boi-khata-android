package com.boikhata.core.database.repository

import com.boikhata.core.database.BoiKhataDatabase
import com.boikhata.core.database.dao.CashbookDao
import com.boikhata.core.database.dao.ExpenseCategoryDao
import com.boikhata.core.database.dao.ExpenseDao
import com.boikhata.core.database.dao.StockLedgerDao
import com.boikhata.core.database.entity.CashbookEntryEntity
import com.boikhata.core.database.entity.ExpenseCategoryEntity
import com.boikhata.core.database.entity.ExpenseEntity
import com.boikhata.core.database.entity.StockLedgerEntity
import com.boikhata.core.database.entity.BookStockDelta
import com.boikhata.core.domain.accounting.PeriodLockChecker
import com.boikhata.core.domain.accounting.PeriodLockGuard
import com.boikhata.core.domain.license.LicenseWriteGuard
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * B-013 regression: «কোনো খরচের খাত নেই» on the owner's PR #68 device-test
 * build — the B-010 chips + Save gate were CORRECT; the data beneath them was
 * missing because `DatabaseSeeder.seedIfEmpty()` had exactly one call site
 * (DemoResetter — the destructive demo reset), so `expense_categories` was
 * empty on every install. These tests drive the REAL repository (in-memory
 * fakes, same template as KhataRepositoryImplTest) against the new session
 * bootstrap: seed the Blueprint §7.8 defaults when the tenant has none,
 * idempotently, and fail fast on a blank tenant (D86).
 */
class ExpenseRepositoryImplTest {

    private class FakeExpenseCategoryDao : ExpenseCategoryDao {
        val rows = mutableListOf<ExpenseCategoryEntity>()
        override suspend fun insert(category: ExpenseCategoryEntity) {
            // Emulate the production OnConflictStrategy.REPLACE semantics.
            rows.removeAll { it.id == category.id }
            rows.add(category)
        }
        override suspend fun getActiveByTenant(tenantId: String) =
            rows.filter { it.tenantId == tenantId && it.isActive }.sortedBy { it.nameBn }
        override suspend fun getById(id: String) = rows.firstOrNull { it.id == id }
    }

    private class FakeExpenseDao : ExpenseDao {
        val rows = mutableListOf<ExpenseEntity>()
        override suspend fun insert(expense: ExpenseEntity) { rows.add(expense) }
        override suspend fun getByTenant(tenantId: String) = rows.filter { it.tenantId == tenantId }
        override suspend fun getByDateRange(tenantId: String, start: Long, end: Long) =
            rows.filter { it.tenantId == tenantId && it.expenseDate in start until end }
        override suspend fun getByCategory(tenantId: String, categoryId: String) =
            rows.filter { it.tenantId == tenantId && it.categoryId == categoryId }
        override suspend fun getByCategoryAndUser(tenantId: String, categoryId: String, userId: String) =
            rows.filter { it.tenantId == tenantId && it.categoryId == categoryId && it.userId == userId }
    }

    private class FakeStockLedgerDao : StockLedgerDao {
        override suspend fun insert(entry: StockLedgerEntity) { /* unused */ }
        override suspend fun getByBook(tenantId: String, bookId: String) = emptyList<StockLedgerEntity>()
        override suspend fun getStockQuantityForBook(bookId: String) = 0
        override suspend fun getDeltasByTenant(tenantId: String) = emptyList<BookStockDelta>()
    }

    private class FakeCashbookDao : CashbookDao {
        val entries = mutableListOf<CashbookEntryEntity>()
        override suspend fun insert(entry: CashbookEntryEntity) { entries.add(entry) }
        override suspend fun getByTenant(tenantId: String) = entries.filter { it.tenantId == tenantId }
        override suspend fun getByAccount(tenantId: String, account: String) =
            entries.filter { it.tenantId == tenantId && it.account == account }
        override suspend fun getByDateRange(tenantId: String, start: Long, end: Long) =
            entries.filter { it.tenantId == tenantId && it.date in start until end }
    }

    private object NoLock : PeriodLockChecker {
        override suspend fun getLockedPeriods(tenantId: String): Set<PeriodLockGuard.LockedPeriod> = emptySet()
        override suspend fun assertNotLocked(tenantId: String, date: Long) { /* no-op */ }
    }

    private lateinit var categoryDao: FakeExpenseCategoryDao
    private lateinit var repo: ExpenseRepositoryImpl

    @Before
    fun setup() {
        categoryDao = FakeExpenseCategoryDao()
        repo = ExpenseRepositoryImpl(
            db = mockk<BoiKhataDatabase>(relaxed = true),
            expenseDao = FakeExpenseDao(),
            expenseCategoryDao = categoryDao,
            stockLedgerDao = FakeStockLedgerDao(),
            cashbookDao = FakeCashbookDao(),
            writeGuard = LicenseWriteGuard(), // default GRACE → reads/writes allowed
            periodLockChecker = NoLock,
        )
    }

    @Test
    fun seedsEightBlueprintDefaults_whenTenantHasNoCategories() = runTest {
        val inserted = repo.seedDefaultCategoriesIfMissing("tn_owner")

        assertThat(inserted).isEqualTo(8)
        val categories = repo.getCategories("tn_owner")
        assertThat(categories).hasSize(8)
        // Gori contract: the ঘরি/অ্যাডভান্স lookup matches on icon, never id.
        assertThat(categories.map { it.icon }).contains("advance")
        // Deterministic tenant-scoped ids (PK-safe across tenants).
        assertThat(categories.first().id).startsWith("tn_owner-ec_")
    }

    @Test
    fun isIdempotent_secondCallInsertsNothing() = runTest {
        repo.seedDefaultCategoriesIfMissing("tn_owner")
        val second = repo.seedDefaultCategoriesIfMissing("tn_owner")

        assertThat(second).isEqualTo(0)
        assertThat(repo.getCategories("tn_owner")).hasSize(8)
    }

    @Test
    fun noOp_whenTenantAlreadyHasAnyActiveCategory() = runTest {
        categoryDao.insert(
            ExpenseCategoryEntity(
                id = "custom-1",
                tenantId = "tn_owner",
                nameBn = "চা-নাস্তা",
                icon = "tea",
                isActive = true,
            )
        )

        val inserted = repo.seedDefaultCategoriesIfMissing("tn_owner")

        assertThat(inserted).isEqualTo(0)
        val categories = repo.getCategories("tn_owner")
        assertThat(categories).hasSize(1) // the custom row survives, defaults never duplicate it
        assertThat(categories.single().id).isEqualTo("custom-1")
    }

    @Test
    fun tenantScoping_seedsIndependentlyPerTenant() = runTest {
        repo.seedDefaultCategoriesIfMissing("tn_a")

        // tenant B also gets its own 8 rows; tenant A's rows are untouched
        val b = repo.seedDefaultCategoriesIfMissing("tn_b")
        assertThat(b).isEqualTo(8)
        assertThat(repo.getCategories("tn_a")).hasSize(8)
        assertThat(repo.getCategories("tn_b")).hasSize(8)
    }

    @Test
    fun failsFast_onBlankTenantId() = runTest {
        var thrown: IllegalStateException? = null
        try {
            repo.seedDefaultCategoriesIfMissing("")
        } catch (e: IllegalStateException) {
            thrown = e
        }
        assertThat(thrown).isNotNull()
        assertThat(categoryDao.rows).isEmpty()
    }
}
