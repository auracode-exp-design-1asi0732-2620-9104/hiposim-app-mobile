package com.auracode.hiposim

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ResultsToRegisterFlowTest {
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
    fun sendQuote_opensSheet_continueWithEmail_showsRegister_backReturnsToResults() {
        composeRule.onNodeWithText("S/ 1,909").assertIsDisplayed()

        composeRule.onNodeWithText(text(R.string.send_quote)).performClick()
        composeRule.onNodeWithText(text(R.string.unlock_sheet_title)).assertIsDisplayed()

        composeRule.onNodeWithText(text(R.string.unlock_sheet_email)).performClick()
        composeRule.onNodeWithText(text(R.string.register_headline)).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(text(R.string.register_back_to_results)).performClick()
        composeRule.onNodeWithText(text(R.string.send_quote)).assertIsDisplayed()
    }

    @Test
    fun lockedDestination_opensSheet() {
        composeRule.onNodeWithText(text(R.string.nav_history)).performClick()

        composeRule.onNodeWithText(text(R.string.unlock_sheet_title)).assertIsDisplayed()
    }
}
