package com.auracode.hiposim

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.test.espresso.Espresso
import com.auracode.hiposim.feature.leads.data.DemoAgency
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** US21: a signed in buyer sends the simulation to an agency with explicit consent. Runs with the in-memory API. */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class SendToAgencyFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.onNodeWithText(text(R.string.simulation_submit)).performScrollTo().performClick()
        waitFor(R.string.results_title)
    }

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun waitFor(id: Int) {
        composeRule.waitUntilAtLeastOneExists(hasText(text(id)), TIMEOUT_MILLIS)
    }

    private fun register() {
        composeRule.onNodeWithText(text(R.string.send_quote)).performClick()
        composeRule.onNodeWithText(text(R.string.unlock_sheet_email)).performClick()
        composeRule.onNodeWithText(text(R.string.field_name)).performTextInput("Carlos Mendoza")
        composeRule.onNodeWithText(text(R.string.field_email)).performTextInput("carlos@ejemplo.com")
        val phoneLabel = text(R.string.field_phone) + " (" + text(R.string.field_phone_hint) + ")"
        composeRule.onNodeWithText(phoneLabel).performTextInput("987654321")
        composeRule.onNodeWithText(text(R.string.field_password)).performTextInput("secret123")
        Espresso.closeSoftKeyboard()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(text(R.string.terms_text)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.consent_text)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.register_submit)).performScrollTo().performClick()
        waitFor(R.string.send_quote)
    }

    private fun openSendSheet() {
        composeRule.onNodeWithText(text(R.string.send_quote)).performClick()
        waitFor(R.string.send_sheet_title)
        composeRule.waitUntilAtLeastOneExists(hasText(DemoAgency.NAME), TIMEOUT_MILLIS)
    }

    @Test
    fun guest_isAskedForAnAccount_andSeesNoAgencies() {
        composeRule.onNodeWithText(text(R.string.send_quote)).performClick()

        composeRule.onNodeWithText(text(R.string.unlock_sheet_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.send_sheet_title)).assertDoesNotExist()
        composeRule.onNodeWithText(DemoAgency.NAME).assertDoesNotExist()
    }

    @Test
    fun signedIn_sendWithoutTheConsent_showsTheErrorAndDoesNotConfirm() {
        register()
        openSendSheet()

        composeRule.onNodeWithText(text(R.string.send_submit)).performScrollTo().performClick()

        composeRule.onNodeWithText(text(R.string.error_send_consent_required)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(successMessage()).assertDoesNotExist()
    }

    @Test
    fun signedIn_authorizesAndSends_andGetsTheConfirmation() {
        register()
        openSendSheet()

        composeRule.onNodeWithText(text(R.string.send_consent_text)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.send_submit)).performScrollTo().performClick()

        composeRule.waitUntilAtLeastOneExists(hasText(successMessage()), TIMEOUT_MILLIS)
        composeRule.onNodeWithText(successMessage()).assertIsDisplayed()

        composeRule.onNodeWithText(text(R.string.send_done)).performClick()
        composeRule.waitUntilDoesNotExist(hasText(text(R.string.send_sheet_title)), TIMEOUT_MILLIS)
        composeRule.onNodeWithText(text(R.string.results_title)).assertIsDisplayed()
    }

    private fun successMessage() = composeRule.activity.getString(R.string.send_success, DemoAgency.NAME)

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
