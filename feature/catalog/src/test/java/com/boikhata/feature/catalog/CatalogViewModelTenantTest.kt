package com.boikhata.feature.catalog

import com.boikhata.core.domain.enums.BookCategory
import com.boikhata.core.domain.enums.BookCondition
import com.boikhata.core.domain.repository.BookRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * C-5 (P11): D86 rule 1 for the catalog write path. CatalogViewModel used to
 * keep a `currentTenantId = ""` field and addBook/updateBook wrote through it
 * with no blank-tenant check — if the write ever ran before loadCatalog, the
 * book was created with an EMPTY tenantId (unreachable by every tenant-scoped
 * query — the same lost-row class as B-004).
 *
 * The fail-fast contract: a blank-tenant write is refused BEFORE the
 * repository is touched, and the UI state turns Error. These tests drive the
 * real ViewModel (no test-local reimplementation — ERR-013 lesson).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTenantTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var bookRepository: BookRepository
    private lateinit var viewModel: CatalogViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        bookRepository = mockk(relaxed = true)
        viewModel = CatalogViewModel(bookRepository = bookRepository)
    }

    @After
    fun teardown() { Dispatchers.resetMain() }

    private fun callAddBook(tenantId: String) {
        viewModel.addBook(
            tenantId = tenantId,
            isbn = null,
            titleBn = "দ্বিতীয় ভাষা",
            titleEn = null,
            author = "লেখক",
            publisher = "প্রকাশক",
            classLevel = "ষষ্ঠ",
            subject = "বাংলা",
            editionYear = 2026,
            category = BookCategory.TEXTBOOK,
            condition = BookCondition.NEW,
            purchasePrice = 100.0,
            sellingPrice = 120.0,
            initialStock = 3,
            lowStockThreshold = 5,
            onDone = { },
        )
    }

    @Test
    fun `addBook with blank tenantId is refused before the repository`() = runTest {
        callAddBook(tenantId = "")

        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(CatalogUiState.Error::class.java)
        coVerify(exactly = 0) { bookRepository.addBook(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `addBook threads the explicit tenantId into the repository`() = runTest {
        callAddBook(tenantId = "t_shop1")

        advanceUntilIdle()

        coVerify(exactly = 1) {
            bookRepository.addBook(
                eq("t_shop1"), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(),
            )
        }
    }

    @Test
    fun `updateBook with blank tenantId is refused before the repository`() = runTest {
        viewModel.updateBook(
            tenantId = "",
            id = "b_1",
            isbn = null,
            titleBn = "দ্বিতীয় ভাষা",
            titleEn = null,
            author = "লেখক",
            publisher = "প্রকাশক",
            classLevel = "ষষ্ঠ",
            subject = "বাংলা",
            editionYear = 2026,
            category = BookCategory.TEXTBOOK,
            condition = BookCondition.NEW,
            purchasePrice = 100.0,
            sellingPrice = 120.0,
            lowStockThreshold = 5,
            isActive = true,
            onDone = { },
        )

        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isInstanceOf(CatalogUiState.Error::class.java)
        coVerify(exactly = 0) { bookRepository.updateBook(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }
}
