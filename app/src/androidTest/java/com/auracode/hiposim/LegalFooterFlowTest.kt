package com.auracode.hiposim

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import com.auracode.hiposim.core.designsystem.component.LEGAL_FOOTER_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** US17: the legal footer is on the main screens. Runs with the in-memory API (`USE_FAKE_API`). */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class LegalFooterFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun text(id: Int) = composeRule.activity.getString(id)

    @Test
    fun footerIsOnTheSimulationFormAndOnTheResults() {
        composeRule.onNodeWithTag(LEGAL_FOOTER_TAG).performScrollTo().assertIsDisplayed()

        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithText(text(R.string.simulation_submit)).performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(text(R.string.results_title)), 15_000)

        composeRule.onNodeWithTag(LEGAL_FOOTER_TAG).performScrollTo().assertIsDisplayed()
    }
}
