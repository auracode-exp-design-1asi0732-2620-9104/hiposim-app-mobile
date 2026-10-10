package com.auracode.hiposim

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.auracode.hiposim.core.designsystem.component.LEGAL_FOOTER_TAG
import com.auracode.hiposim.core.designsystem.component.LegalFooter
import com.auracode.hiposim.core.designsystem.theme.HipoSimTheme
import com.auracode.hiposim.core.legal.LegalLinks
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** US17: the Terms and Conditions and the Privacy Policy are reachable from the footer of the app. */
class LegalFooterTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val opened = mutableListOf<String>()
    private val recordingUriHandler =
        object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }

    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun showFooter() {
        composeRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides recordingUriHandler) {
                HipoSimTheme { LegalFooter() }
            }
        }
    }

    @Test
    fun footerShowsBothLinksWithA48DpTouchTarget() {
        showFooter()

        composeRule.onNodeWithTag(LEGAL_FOOTER_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(text(R.string.legal_terms_description))
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithContentDescription(text(R.string.legal_privacy_description))
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun termsLinkOpensTheTermsSectionOfTheLandingPage() {
        showFooter()

        composeRule.onNodeWithContentDescription(text(R.string.legal_terms_description)).performClick()

        assertEquals(listOf(LegalLinks.termsUrl()), opened)
    }

    @Test
    fun privacyLinkOpensThePrivacySectionOfTheLandingPage() {
        showFooter()

        composeRule.onNodeWithContentDescription(text(R.string.legal_privacy_description)).performClick()

        assertEquals(listOf(LegalLinks.privacyUrl()), opened)
    }
}
