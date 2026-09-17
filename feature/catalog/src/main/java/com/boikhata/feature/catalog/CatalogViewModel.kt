package com.boikhata.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.core.domain.enums.BookCategory
import com.boikhata.core.domain.enums.BookCondition
import com.boikhata.core.domain.model.Book
import com.boikhata.core.domain.repository.BookRepository
import com.boikhata.core.domain.text.BengaliNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val bookRepository: BookRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var currentTenantId: String = ""
    private var allBooks: List<Book> = emptyList()

    fun loadCatalog(tenantId: String) {
        currentTenantId = tenantId
        viewModelScope.launch {
            _uiState.value = CatalogUiState.Loading
            try {
                allBooks = bookRepository.getBooks(tenantId)
                _uiState.value = CatalogUiState.Success(allBooks, _searchQuery.value)
            } catch (e: Exception) {
                _uiState.value = CatalogUiState.Error(e.message ?: "ত্রুটি")
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _uiState.value = CatalogUiState.Success(allBooks, "")
        } else {
            val normalized = BengaliNormalizer.normalize(query)
            val filtered = allBooks.filter { book ->
                BengaliNormalizer.normalize(book.titleBn).contains(normalized, ignoreCase = true) ||
                (book.titleEn?.contains(query, ignoreCase = true) == true) ||
                book.author.contains(query, ignoreCase = true) ||
                (book.isbn?.contains(query, ignoreCase = true) == true)
            }
            _uiState.value = CatalogUiState.Success(filtered, query)
        }
    }

    fun addBook(
        isbn: String?,
        titleBn: String,
        titleEn: String?,
        author: String,
        publisher: String,
        classLevel: String,
        subject: String,
        editionYear: Int,
        category: BookCategory,
        condition: BookCondition,
        purchasePrice: Double,
        sellingPrice: Double,
        initialStock: Int,
        lowStockThreshold: Int,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                bookRepository.addBook(
                    currentTenantId, isbn, titleBn, titleEn, author, publisher,
                    classLevel, subject, editionYear, category, condition,
                    purchasePrice, sellingPrice, initialStock, lowStockThreshold,
                )
                loadCatalog(currentTenantId)
                onDone()
            } catch (e: Exception) {
                _uiState.value = CatalogUiState.Error(e.message ?: "সেভ ব্যর্থ")
            }
        }
    }

    fun updateBook(
        id: String,
        isbn: String?,
        titleBn: String,
        titleEn: String?,
        author: String,
        publisher: String,
        classLevel: String,
        subject: String,
        editionYear: Int,
        category: BookCategory,
        condition: BookCondition,
        purchasePrice: Double,
        sellingPrice: Double,
        lowStockThreshold: Int,
        isActive: Boolean,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                bookRepository.updateBook(
                    currentTenantId, id, isbn, titleBn, titleEn, author, publisher,
                    classLevel, subject, editionYear, category, condition,
                    purchasePrice, sellingPrice, lowStockThreshold, isActive,
                )
                loadCatalog(currentTenantId)
                onDone()
            } catch (e: Exception) {
                _uiState.value = CatalogUiState.Error(e.message ?: "আপডেট ব্যর্থ")
            }
        }
    }
}

sealed interface CatalogUiState {
    data object Loading : CatalogUiState
    data class Success(val books: List<Book>, val searchQuery: String) : CatalogUiState
    data class Error(val message: String) : CatalogUiState
}
