package com.auracode.hiposim

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class SimulationFormFlowTest {
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
    fun appStartsOnTheFormWithoutAnAccount_andShowsTheLandingValues() {
        composeRule.onNodeWithText(text(R.string.simulation_headline)).assertIsDisplayed()
        composeRule.onNodeWithText("280,000").assertIsDisplayed()
        composeRule.onNodeWithText("42,000").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.simulation_submit)).assertIsDisplayed()
    }

    @Test
    fun downPaymentBelowTenPercent_showsTheErrorAndStaysOnTheForm() {
        composeRule.onNodeWithText(text(R.string.field_down_payment)).performTextClearance()
        composeRule.onNodeWithText(text(R.string.field_down_payment)).performTextInput("10000")

        composeRule.onNodeWithText(text(R.string.simulation_submit)).performClick()

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.error_down_payment_too_low, 10))
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.simulation_headline)).assertIsDisplayed()
    }

    @Test
    fun simulate_opensTheResults_andBackReturnsToTheForm() {
        composeRule.onNodeWithText(text(R.string.simulation_submit)).performClick()

        composeRule.waitUntilAtLeastOneExists(hasText(text(R.string.send_quote)))
        composeRule.onNodeWithText("S/ 1,909").assertIsDisplayed()

        composeRule.onNodeWithContentDescription(text(R.string.action_back)).performClick()
        composeRule.onNodeWithText(text(R.string.simulation_headline)).assertIsDisplayed()
    }
}
