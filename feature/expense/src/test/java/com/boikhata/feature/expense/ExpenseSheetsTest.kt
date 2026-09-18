package com.boikhata.feature.expense

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.boikhata.core.domain.enums.CashbookAccount
import com.boikhata.core.domain.enums.CashbookEntryType
import com.boikhata.core.domain.model.ExpenseCategory
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * B-010 / B-012 regression — the owner's device findings on the expense screen:
 *
 * 1. «খরচ যোগ করুন»: the category "field" was a readOnly TextField fronting a
 *    DropdownMenu whose expanded state was NEVER set true — no category could
 *    ever be selected, so সেভ's `selectedCategory != null` gate stayed gray
 *    forever ("Cannot type any text in the category box"). The amount filter
 *    also silently stripped Bangla-keyboard input (B-007 class).
 * 2. «মালিকের তোলা»: amount+description accepted ০-৯ visually but
 *    `toDoubleOrNull()` rejected them — সেভ stayed gray even with the amount
 *    filled ("even after providing amount and description").
 *
 * These tests drive the REAL production sheets (no hardcoded gate values —
 * ERR-013 lesson): if the parse reverts to ASCII-only or the category picker
 * becomes unreachable again, they fail.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "en")
class ExpenseSheetsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val rent = ExpenseCategory(id = "ec_rent", nameBn = "ভাড়া", icon = "rent", isActive = true)
    private val tea = ExpenseCategory(id = "ec_tea", nameBn = "চা-নাস্তা", icon = "tea", isActive = true)

    // ── parseAmountInput (the single parse the sheets' gates use) ──

    @Test
    fun parseAmountInput_normalizesBanglaDigits() {
        assertThat(parseAmountInput("৫০০")).isEqualTo(500.0)
        assertThat(parseAmountInput("৫০.২৫")).isEqualTo(50.25)
        assertThat(parseAmountInput("50")).isEqualTo(50.0)
        assertThat(parseAmountInput("")).isNull()
        assertThat(parseAmountInput("abc")).isNull()
    }

    // ── Issue 1: Add Expense sheet ──

    @Test
    fun addExpenseSheet_saveEnabledAfterCategoryChipAndBanglaDigitAmount_thenConfirmCarriesParsedValues() {
        var confirmed: Triple<String, Double, CashbookAccount>? = null
        composeRule.setContent {
            AddExpenseSheet(
                categories = listOf(rent, tea),
                onConfirm = { categoryId, amount, _, account -> confirmed = Triple(categoryId, amount, account) },
                onCancel = { },
            )
        }
        composeRule.onNodeWithText("Save").assertIsNotEnabled()

        // Category is now an always-visible chip (the old dropdown could never open)
        composeRule.onNodeWithText("ভাড়া").performClick()
        composeRule.onNodeWithText("Save").assertIsNotEnabled() // amount still empty

        // Bangla-keyboard digits must count toward the gate (B-007 class)
        composeRule.onNodeWithText("Amount").performTextInput("৫০০")
        composeRule.onNodeWithText("Save").assertIsEnabled()
        composeRule.onNodeWithText("Save").performClick()

        assertThat(confirmed).isNotNull()
        assertThat(confirmed!!.first).isEqualTo("ec_rent")
        assertThat(confirmed!!.second).isEqualTo(500.0)
        assertThat(confirmed!!.third).isEqualTo(CashbookAccount.CASH)
    }

    @Test
    fun addExpenseSheet_saveStaysDisabledWithoutCategory_evenWithAmount() {
        composeRule.setContent {
            AddExpenseSheet(categories = listOf(rent), onConfirm = { _, _, _, _ -> }, onCancel = { })
        }
        composeRule.onNodeWithText("Amount").performTextInput("৫০০")
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun addExpenseSheet_emptyCategoryListShowsExplicitEmptyState() {
        // D87 §3: a bounded-dataset picker must never be silently blank.
        composeRule.setContent {
            AddExpenseSheet(categories = emptyList(), onConfirm = { _, _, _, _ -> }, onCancel = { })
        }
        composeRule.onNodeWithText("No expense categories.").assertExists()
    }

    // ── Issue 3: Owner's withdrawal (তোলা) sheet ──

    @Test
    fun addDrawingSheet_banglaDigitAmountEnablesSave_andConfirmCarriesParsedAmount() {
        var confirmedAmount: Double? = null
        composeRule.setContent {
            AddDrawingSheet(onConfirm = { amount, _ -> confirmedAmount = amount }, onCancel = { })
        }
        composeRule.onNodeWithText("Save").assertIsNotEnabled()

        composeRule.onNodeWithText("Amount").performTextInput("৫০০")
        composeRule.onNodeWithText("Save").assertIsEnabled()
        composeRule.onNodeWithText("Save").performClick()

        assertThat(confirmedAmount).isEqualTo(500.0)
    }

    // ── Same-class latent bug: manual cashbook entry sheet ──

    @Test
    fun addCashbookEntrySheet_banglaDigitAmountEnablesSave() {
        var confirmed: Pair<Double, CashbookEntryType>? = null
        composeRule.setContent {
            AddCashbookEntrySheet(
                onConfirm = { _, type, amount, _ -> confirmed = Pair(amount, type) },
                onCancel = { },
            )
        }
        composeRule.onNodeWithText("Amount").performTextInput("৫০০")
        composeRule.onNodeWithText("Save").assertIsEnabled()
        composeRule.onNodeWithText("Save").performClick()

        assertThat(confirmed).isNotNull()
        assertThat(confirmed!!.first).isEqualTo(500.0)
        assertThat(confirmed!!.second).isEqualTo(CashbookEntryType.INCOME)
    }
}
