package com.auracode.hiposim

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
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

    private fun openRegisterFromResults() {
        composeRule.onNodeWithText(text(R.string.send_quote)).performClick()
        composeRule.onNodeWithText(text(R.string.unlock_sheet_email)).performClick()
        composeRule.onNodeWithText(text(R.string.register_headline)).assertIsDisplayed()
    }

    private fun fillRegistrationForm(acceptConsent: Boolean) {
        composeRule.onNodeWithText(text(R.string.field_name)).performTextInput("Carlos Mendoza")
        composeRule.onNodeWithText(text(R.string.field_email)).performTextInput("carlos@ejemplo.com")
        val phoneLabel = text(R.string.field_phone) + " (" + text(R.string.field_phone_hint) + ")"
        composeRule.onNodeWithText(phoneLabel).performTextInput("987654321")
        composeRule.onNodeWithText(text(R.string.field_password)).performTextInput("secret123")
        if (acceptConsent) {
            composeRule.onNodeWithText(text(R.string.consent_text)).performScrollTo().performClick()
        }
        composeRule.onNodeWithText(text(R.string.register_submit)).performScrollTo().performClick()
    }

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

    @Test
    fun registerWithoutConsent_staysOnRegisterAndShowsError() {
        openRegisterFromResults()

        fillRegistrationForm(acceptConsent = false)

        composeRule.onNodeWithText(text(R.string.error_consent_required)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.register_headline)).assertIsDisplayed()
    }

    @Test
    fun register_returnsToResultsUnlocked_andSignOutLocksAgain() {
        openRegisterFromResults()
        val lockedDescription = text(R.string.nav_locked_description).format(text(R.string.nav_realtors))

        fillRegistrationForm(acceptConsent = true)

        // Back on Results, signed in: the padlock is gone and the sheet does not open.
        composeRule.onNodeWithText(text(R.string.send_quote)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(lockedDescription).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.send_quote)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(text(R.string.coming_soon)))
        composeRule.onNodeWithText(text(R.string.coming_soon)).assertIsDisplayed()

        // Profile shows the account and lets the user sign out.
        composeRule.onNodeWithText(text(R.string.nav_profile)).performClick()
        composeRule.onNodeWithText("carlos@ejemplo.com").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.account_sign_out)).performClick()

        composeRule.onNodeWithContentDescription(lockedDescription).assertIsDisplayed()
    }
}
