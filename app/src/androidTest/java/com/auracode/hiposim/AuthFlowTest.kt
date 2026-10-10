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
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.espresso.Espresso
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** US13 and US26: create an account, sign out and sign in again. Runs with the in-memory API (`USE_FAKE_API`). */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class AuthFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        // The account screens are reached from Results, which needs a simulation first.
        composeRule.onNodeWithText(text(R.string.simulation_submit)).performScrollTo().performClick()
        waitFor(R.string.results_title)
    }

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun waitFor(id: Int) {
        composeRule.waitUntilAtLeastOneExists(hasText(text(id)), TIMEOUT_MILLIS)
    }

    private fun closeKeyboard() {
        Espresso.closeSoftKeyboard()
        composeRule.waitForIdle()
    }

    private fun openUnlockSheet() {
        composeRule.onNodeWithText(text(R.string.send_quote)).performClick()
        composeRule.onNodeWithText(text(R.string.unlock_sheet_title)).assertIsDisplayed()
    }

    private fun register() {
        openUnlockSheet()
        composeRule.onNodeWithText(text(R.string.unlock_sheet_email)).performClick()
        composeRule.onNodeWithText(text(R.string.field_name)).performTextInput("Carlos Mendoza")
        composeRule.onNodeWithText(text(R.string.field_email)).performTextInput(EMAIL)
        val phoneLabel = text(R.string.field_phone) + " (" + text(R.string.field_phone_hint) + ")"
        composeRule.onNodeWithText(phoneLabel).performTextInput("987654321")
        composeRule.onNodeWithText(text(R.string.field_password)).performTextInput(PASSWORD)
        closeKeyboard()
        composeRule.onNodeWithText(text(R.string.terms_text)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.consent_text)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.register_submit)).performScrollTo().performClick()
        waitFor(R.string.send_quote)
    }

    private fun signOut() {
        composeRule.onNodeWithText(text(R.string.nav_profile)).performClick()
        composeRule.onNodeWithText(text(R.string.account_sign_out)).performClick()
        composeRule.onNodeWithContentDescription(lockedRealtors()).assertIsDisplayed()
    }

    private fun lockedRealtors() = text(R.string.nav_locked_description).format(text(R.string.nav_realtors))

    private fun submitLogin(password: String) {
        composeRule.onNodeWithText(text(R.string.field_password)).performTextReplacement(password)
        closeKeyboard()
        composeRule.onNodeWithText(text(R.string.login_submit)).performScrollTo().performClick()
    }

    @Test
    fun register_signOut_thenSignInWithTheSameAccount() {
        register()
        signOut()

        openUnlockSheet()
        composeRule.onNodeWithText(text(R.string.unlock_sheet_sign_in)).performClick()
        composeRule.onNodeWithText(text(R.string.login_title)).assertIsDisplayed()

        composeRule.onNodeWithText(text(R.string.field_email)).performTextInput(EMAIL)
        submitLogin(PASSWORD)

        // Back on Results with the session: the padlocks are gone and Profile shows the account.
        waitFor(R.string.send_quote)
        composeRule.onNodeWithContentDescription(lockedRealtors()).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.nav_profile)).performClick()
        composeRule.onNodeWithText(EMAIL).assertIsDisplayed()
    }

    @Test
    fun signInWithTheWrongPassword_showsAGenericMessage_thenTheRightOneWorks() {
        register()
        signOut()
        openUnlockSheet()
        composeRule.onNodeWithText(text(R.string.unlock_sheet_sign_in)).performClick()

        composeRule.onNodeWithText(text(R.string.field_email)).performTextInput(EMAIL)
        submitLogin("another-password")

        waitFor(R.string.error_invalid_credentials)
        composeRule.onNodeWithText(text(R.string.error_invalid_credentials)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.login_title)).performScrollTo().assertIsDisplayed()

        submitLogin(PASSWORD)
        waitFor(R.string.send_quote)
        composeRule.onNodeWithContentDescription(lockedRealtors()).assertDoesNotExist()
    }

    @Test
    fun registrationAndSignIn_linkToEachOther_andCloseBackToResults() {
        openUnlockSheet()
        composeRule.onNodeWithText(text(R.string.unlock_sheet_sign_in)).performClick()
        composeRule.onNodeWithText(text(R.string.login_title)).assertIsDisplayed()

        composeRule.onNodeWithText(text(R.string.login_no_account)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.register_headline)).assertIsDisplayed()

        composeRule.onNodeWithText(text(R.string.register_have_account)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.login_title)).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(text(R.string.register_back_to_results)).performClick()
        composeRule.onNodeWithText(text(R.string.send_quote)).assertIsDisplayed()
    }

    @Test
    fun registerWithoutTheTerms_showsTheErrorAndCreatesNoAccount() {
        openUnlockSheet()
        composeRule.onNodeWithText(text(R.string.unlock_sheet_email)).performClick()
        composeRule.onNodeWithText(text(R.string.field_name)).performTextInput("Carlos Mendoza")
        composeRule.onNodeWithText(text(R.string.field_email)).performTextInput(EMAIL)
        val phoneLabel = text(R.string.field_phone) + " (" + text(R.string.field_phone_hint) + ")"
        composeRule.onNodeWithText(phoneLabel).performTextInput("987654321")
        composeRule.onNodeWithText(text(R.string.field_password)).performTextInput(PASSWORD)
        closeKeyboard()
        composeRule.onNodeWithText(text(R.string.consent_text)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.register_submit)).performScrollTo().performClick()

        composeRule.onNodeWithText(text(R.string.error_terms_required)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.register_headline)).performScrollTo().assertIsDisplayed()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
        const val EMAIL = "carlos@ejemplo.com"
        const val PASSWORD = "secret123"
    }
}
