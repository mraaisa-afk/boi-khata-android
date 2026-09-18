package com.boikhata.feature.khata

import android.content.Context
import com.boikhata.core.domain.repository.BillRepository
import com.boikhata.core.domain.repository.KhataRepository
import com.boikhata.core.domain.repository.LicenseRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
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
 * U-002: the add-customer write must thread the explicit tenant AND the
 * optional «পূর্বের বাকি» opening due into the repository's atomic
 * addCustomerWithOpeningDue call (D86 rule 1 pattern), and the B-004 blank-
 * tenant fail-fast must stay intact.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KhataViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var khataRepository: KhataRepository
    private lateinit var viewModel: KhataViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        khataRepository = mockk(relaxed = true)
        viewModel = KhataViewModel(
            khataRepository = khataRepository,
            billRepository = mockk(relaxed = true),
            licenseRepository = mockk(relaxed = true),
            appContext = mockk<Context>(relaxed = true),
        )
    }

    @After
    fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `addCustomer threads tenant and opening due into the atomic repository call`() = runTest {
        var done = false
        viewModel.addCustomer(
            tenantId = "t_shop1",
            nameBn = "করিম মামা",
            phone = null,
            address = "বাজার রোড",
            creditLimit = 5000.0,
            openingDue = 800.0,
            onDone = { done = true },
        )
        advanceUntilIdle()

        coVerify(exactly = 1) {
            khataRepository.addCustomerWithOpeningDue(
                tenantId = "t_shop1",
                nameBn = "করিম মামা",
                phone = null,
                address = "বাজার রোড",
                creditLimit = 5000.0,
                openingDue = 800.0,
                collectedByUserId = "u_1",
            )
        }
        assertThat(done).isTrue()
    }

    @Test
    fun `addCustomer defaults opening due to zero when the field is unused`() = runTest {
        viewModel.addCustomer(
            tenantId = "t_shop1",
            nameBn = "রহিম",
            phone = null,
            address = null,
            creditLimit = 0.0,
            onDone = { },
        )
        advanceUntilIdle()

        coVerify(exactly = 1) {
            khataRepository.addCustomerWithOpeningDue(
                tenantId = "t_shop1", nameBn = "রহিম", phone = null, address = null,
                creditLimit = 0.0, openingDue = 0.0, collectedByUserId = "u_1",
            )
        }
    }

    @Test
    fun `blank tenant fails fast and never reaches the repository (B-004 regression)`() = runTest {
        viewModel.addCustomer(
            tenantId = "",
            nameBn = "করিম",
            phone = null,
            address = null,
            creditLimit = 0.0,
            openingDue = 800.0,
            onDone = { },
        )
        advanceUntilIdle()

        coVerify(exactly = 0) { khataRepository.addCustomerWithOpeningDue(any(), any(), any(), any(), any(), any(), any()) }
        assertThat(viewModel.listState.value).isInstanceOf(KhataListUiState.Error::class.java)
    }
}
