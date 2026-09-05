# CODING_STANDARDS.md — Boi-Khata Coder Standards

> These standards apply to all code written by the Boi-Khata Coder agent.
> When in doubt, prefer clarity over cleverness, and explicit over implicit.
> Consult `CONVENTIONS.md` for domain-specific patterns.

> **Module map (from `settings.gradle.kts`):**
> `:app`, `:core:database`, `:core:domain`, `:core:cloud`, `:core:designsystem`,
> `:core:common`, `:feature:*` (home, sale, catalog, khata, expense, supplier,
> reports, subscription, melamode, support), `:shared:receipt`.
> There is no `:data:*` module. Room entities, DAOs, migrations and repositories
> live in `:core:database`. Pure calculators and planners live in `:core:domain`.

---

## Kotlin Idioms

### Use sealed classes for domain states

```kotlin
// DO: sealed class for finite states
sealed class LicenseState {
    object Grace : LicenseState()
    object Active : LicenseState()
    data class Expired(val daysAgo: Int) : LicenseState()
}

// DON'T: enum where a sealed class carries more context
enum class LicenseState { GRACE, ACTIVE, EXPIRED }
```

### Use data classes for value objects

```kotlin
// DO
data class Money(val amountPaise: Long) {
    operator fun plus(other: Money) = Money(amountPaise + other.amountPaise)
}

// DON'T: raw Long or String for money
val price: Long = 1500 // which unit? taka? paise?
```

### Prefer exhaustive `when` over if-else chains

```kotlin
// DO
when (val state = licenseState) {
    is LicenseState.Grace -> showGraceBanner()
    is LicenseState.Active -> hideAllBanners()
    is LicenseState.Expired -> showExpiredDialog(state.daysAgo)
}
```

### Extension functions: pure utilities only

```kotlin
// DO
fun Long.toPaisaString(): String = NumberFormatter.format(this)

// DON'T: extensions with side effects or injected dependencies
fun Context.openKhataScreen() { }
```

### Coroutines: `Flow` for streams, `suspend` for one-shot

```kotlin
// DO
fun observeKhataEntries(customerId: Long): Flow<List<KhataEntry>>
suspend fun insertEntry(entry: KhataEntry): Result<Unit>
```

---

## Result Type & Error Handling

### Use the repo's own `Result<T>`, not `kotlin.Result`

```kotlin
// Import path: com.boikhata.core.common.Result
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
```

### Repository functions return `Result<T>`

```kotlin
// DO
suspend fun insertSale(sale: Sale): Result<Long> {
    return try {
        val id = saleDao.insert(sale.toEntity())
        Result.Success(id)
    } catch (e: SQLiteConstraintException) {
        Result.Error(e, "Duplicate bill number")
    }
}

// DON'T: throw from a repository
```

### ViewModel maps `Result<T>` to UI state

```kotlin
viewModelScope.launch {
    _uiState.value = SaleUiState.Loading
    when (val result = saleRepository.insertSale(sale)) {
        is Result.Success -> _uiState.value = SaleUiState.Success(result.data)
        is Result.Error -> _uiState.value = SaleUiState.Error(result.message ?: "হয়নি")
        else -> {}
    }
}
```

### `LicenseWriteGuard` runs before the write

```kotlin
suspend fun insertSale(sale: Sale): Result<Long> {
    licenseWriteGuard.checkOrThrow()
    return try { /* ... */ } catch (e: Exception) { Result.Error(e) }
}
```

---

## Dependency Injection (Hilt)

### Constructor injection only

```kotlin
// DO
class SaleRepository @Inject constructor(
    private val saleDao: SaleDao,
    private val licenseWriteGuard: LicenseWriteGuard,
    private val periodLockGuard: PeriodLockGuard,
)

// DON'T: field injection in a non-Android class
```

### `@HiltViewModel` for ViewModels — never instantiate manually

```kotlin
@HiltViewModel
class SaleViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
) : ViewModel()
```

### Bind interfaces in Hilt modules

```kotlin
@Module @InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindSaleRepo(impl: SaleRepositoryImpl): SaleRepository
}
```

---

## Jetpack Compose Patterns

### State hoisting: leaf composables stay stateless

```kotlin
// DO
@Composable
fun KhataEntryRow(entry: KhataEntry, onPayClick: () -> Unit) { }

// DON'T: leaf reads from the ViewModel directly
```

### Collect state at the screen level only

```kotlin
@Composable
fun KhataScreen(viewModel: KhataViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    KhataContent(state = uiState, onEvent = viewModel::onEvent)
}
```

### Side effects belong in effect handlers

```kotlin
// DO
LaunchedEffect(Unit) { viewModel.loadEntries(customerId) }

// DON'T: launching from the composable body runs on every recomposition
```

### Bengali strings always via `stringResource()`

```kotlin
// DO
Text(text = stringResource(R.string.khata_customer_name))

// DON'T (guardrail G19)
Text(text = "গ্রাহকের নাম")
```

---

## Repository Pattern

- A repository is the single source of truth for a domain entity
- It coordinates the DAO (Room), remote (Firestore) and guard checks
- It NEVER returns Room entities to a ViewModel — always map to domain models
- DAO functions stay internal to `:core:database` and are never called from a ViewModel

---

## Naming Conventions

| Type | Pattern | Example |
| --- | --- | --- |
| Room entity | `<Domain>Entity` | `BillEntity`, `KhataEntryEntity` |
| DAO | `<Domain>Dao` | `BillDao`, `KhataEntryDao` |
| Repository interface | `<Domain>Repository` | `SaleRepository` |
| Repository impl | `<Domain>RepositoryImpl` | `SaleRepositoryImpl` |
| ViewModel | `<Screen>ViewModel` | `SaleViewModel` |
| UI state | `<Screen>UiState` | `SaleUiState` |
| Hilt module | `<Module>Module` | `RepositoryModule` |
| Calculator | `<Domain>Calculator` | `AgingCalculator`, `PnLCalculator` |
| Builder | `<Domain>Builder` | `ReceiptBuilder` |
| Migration | `Migration<N>To<N+1>` | `Migration4To5` |

> Note: the migration naming in this repo is `Migration1To2`, `Migration2To3`,
> `Migration3To4`, `Migration4To5` — not `MigrationV1_V2`.

---

## What NOT To Do

- NEVER use `GlobalScope` — always a lifecycle-bound scope
- NEVER use `runBlocking` outside tests
- NEVER call `Thread.sleep()` — use coroutine `delay`
- NEVER suppress lint without a comment
- NEVER use `!!` — use safe calls plus Elvis
- NEVER hardcode strings in Kotlin or Compose (G19)
- NEVER use `String` for money (G35)
- NEVER access a Room DAO from outside `:core:database`

---

*Last updated: 2026-09-05 · Maintained by: Builder + Sakira Suva*
*Referenced by: AGENT_PLAYBOOK.md Step 5, AGENT_GUARDRAILS.md G19/G35*
