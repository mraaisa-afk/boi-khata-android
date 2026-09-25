package com.boikhata.feature.home

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import com.google.common.truth.Truth.assertThat

/**
 * P15 / A1 (owner device ruling — regression from PR #75's D95 header):
 *
 * MainViewModel:95 resolves the header shop name as
 * `tenantInfoRepository.fetchShopName(tenantId) ?: phone` — when the tenants
 * Firestore doc has no `name` field (or the lookup throws, e.g. offline), the
 * shop name IS the phone number. PR #75's header then rendered the same number
 * TWICE: once large (headlineSmall, the shop-name slot) and once small
 * (bodyMedium, the phone line directly beneath).
 *
 * The fix suppresses the small phone line exactly when it duplicates the shop
 * name — and ONLY in that case (a real shop name still pairs with the phone
 * line, so the P14 design is preserved for tenants whose doc has a name).
 *
 * These tests drive the REAL production composable (ERR-013 lesson — no
 * hardcoded gate values): if the duplicate render ever returns, the first
 * test fails with 2 matched nodes instead of 1.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "en")
class HomeAppBarPhoneRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(shopName: String, phone: String) {
        composeRule.setContent {
            MaterialTheme {
                HomeAppBar(shopName = shopName, phone = phone, isLicensed = false)
            }
        }
    }

    @Test
    fun fallbackCase_shopNameIsThePhone_rendersTheNumberExactlyOnce() {
        // MainViewModel fallback state: shopName := phone (owner's device).
        setContent(shopName = PHONE, phone = PHONE)
        composeRule.onAllNodesWithText(PHONE, useUnmergedTree = true).fetchSemanticsNodes().let { nodes ->
            assertThat(nodes.size).isEqualTo(1)
        }
    }

    @Test
    fun distinctShopNameAndPhone_rendersBothLines() {
        // Healthy tenants doc (name present): P14 design intact — shop name large,
        // phone line beneath. Guards against over-suppressing the phone line.
        setContent(shopName = SHOP_NAME, phone = PHONE)
        composeRule.onNodeWithText(SHOP_NAME, useUnmergedTree = true).assertExists()
        composeRule.onAllNodesWithText(PHONE, useUnmergedTree = true).fetchSemanticsNodes().let { nodes ->
            assertThat(nodes.size).isEqualTo(1)
        }
    }

    private companion object {
        const val PHONE = "01712345678"
        const val SHOP_NAME = "বই ঘর লাইব্রেরি"
    }
}
