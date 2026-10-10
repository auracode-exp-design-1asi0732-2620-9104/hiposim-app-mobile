package com.auracode.hiposim

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.espresso.Espresso
import com.auracode.hiposim.core.util.PeruFormat
import com.auracode.hiposim.feature.simulation.domain.SimulationConfig
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** US01: the buyer fills the form and reaches the results. Runs with the in-memory API (`USE_FAKE_API`). */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class SimulationFormToResultsFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun submit() {
        // Typing leaves the keyboard open; it covers the button and scrolls the form.
        Espresso.closeSoftKeyboard()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(text(R.string.simulation_submit)).performScrollTo().performClick()
    }

    private fun waitForResults() {
        composeRule.waitUntilAtLeastOneExists(hasText(text(R.string.results_title)), RESULTS_TIMEOUT_MILLIS)
    }

    @Test
    fun appStartsOnTheForm_simulateShowsTheResults_backReturnsToTheForm() {
        composeRule.onNodeWithText(text(R.string.simulation_form_title)).assertIsDisplayed()

        composeRule.onNodeWithText(text(R.string.field_property_price)).performTextReplacement("300000")
        composeRule.onNodeWithText(text(R.string.field_down_payment)).performTextReplacement("60000")
        composeRule.onNodeWithText(text(R.string.field_term_years)).performTextReplacement("15")
        submit()
        waitForResults()

        composeRule.onNodeWithText("S/ 300,000").assertIsDisplayed()
        composeRule.onNodeWithText("S/ 60,000").assertIsDisplayed()
        composeRule.onNodeWithText("S/ 240,000").assertIsDisplayed()
        // TCEA and IRR show the same figure, so the TCEA badge is found by its description.
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.tcea_description, "8.85%"))
            .assertIsDisplayed()

        composeRule.onNodeWithContentDescription(text(R.string.action_back)).performClick()
        composeRule.onNodeWithText(text(R.string.simulation_form_title)).assertIsDisplayed()
    }

    @Test
    fun downPaymentBelowTheMinimum_showsTheErrorNextToTheFieldAndStaysOnTheForm() {
        composeRule.onNodeWithText(text(R.string.field_down_payment)).performTextReplacement("1000")
        submit()

        val minimum = PeruFormat.percent(SimulationConfig.MIN_DOWN_PAYMENT_RATE, fractionDigits = 0)
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.error_down_payment_minimum, minimum))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.simulation_form_title)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun bonusRequestedForAHomeInRange_showsTheSubsidy() {
        composeRule.onNodeWithText(text(R.string.bonus_checkbox_title)).performScrollTo().performClick()
        submit()
        waitForResults()

        composeRule.onNodeWithText(text(R.string.subsidy_banner_title)).assertIsDisplayed()
    }

    /** US04: the indicators of the Landing example (S/ 280,000, 15%, 20 years, bonus applied, 8.85%). */
    @Test
    fun results_showTheRealIndicators_andExplainTheNegativeNpv() {
        composeRule.onNodeWithText(text(R.string.bonus_checkbox_title)).performScrollTo().performClick()
        submit()
        waitForResults()

        composeRule.onNodeWithText("S/ 1,909").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.tcea_description, "8.85%"))
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.npv_label)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("-S/ 15,999").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.npv_explanation)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun bonusRequestedForAHomeOutOfRange_isExplainedNotTreatedAsAnError() {
        composeRule.onNodeWithText(text(R.string.field_property_price)).performTextReplacement("450000")
        composeRule.onNodeWithText(text(R.string.field_down_payment)).performTextReplacement("90000")
        composeRule.onNodeWithText(text(R.string.bonus_checkbox_title)).performScrollTo().performClick()
        submit()
        waitForResults()

        composeRule.onNodeWithText(text(R.string.bonus_not_eligible_title)).assertIsDisplayed()
        composeRule.onNodeWithText("S/ 360,000").assertIsDisplayed()
    }

    private companion object {
        const val RESULTS_TIMEOUT_MILLIS = 5_000L
    }
}
