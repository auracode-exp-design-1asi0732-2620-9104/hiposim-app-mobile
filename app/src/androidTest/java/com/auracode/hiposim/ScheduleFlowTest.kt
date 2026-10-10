package com.auracode.hiposim

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.espresso.Espresso
import com.auracode.hiposim.feature.simulation.ui.SCHEDULE_LIST_TAG
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** US02: the buyer opens the full payment schedule from Results. Runs with the in-memory API. */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class ScheduleFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun text(id: Int) = composeRule.activity.getString(id)

    private fun installment(number: Int) = composeRule.activity.getString(R.string.installment_number, number)

    private fun simulateAndOpenSchedule(termYears: String? = null) {
        if (termYears != null) {
            composeRule.onNodeWithText(text(R.string.field_term_years)).performTextReplacement(termYears)
            Espresso.closeSoftKeyboard()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithText(text(R.string.simulation_submit)).performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(text(R.string.results_title)), TIMEOUT_MILLIS)
        composeRule.onNodeWithText(text(R.string.schedule_full_action)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.schedule_full_title)).assertIsDisplayed()
    }

    @Test
    fun fullSchedule_showsTheFourFiguresOfTheFirstMonth() {
        simulateAndOpenSchedule()

        // Landing example without bonus: S/ 280,000 - S/ 42,000 = S/ 238,000 financed at 8.85%.
        composeRule.onNodeWithText(installment(1)).assertIsDisplayed()
        composeRule.onNodeWithText("S/ 238,000.00").assertIsDisplayed()
        composeRule.onNodeWithText("S/ 1,687.84").assertIsDisplayed()
        // Every row has the same payment, so the first one is enough.
        composeRule.onAllNodesWithText("S/ 2,066.94").onFirst().assertIsDisplayed()
    }

    @Test
    fun fullSchedule_reachesTheLastInstallmentOfAThirtyYearLoan() {
        simulateAndOpenSchedule(termYears = "30")

        composeRule.onNodeWithTag(SCHEDULE_LIST_TAG).performScrollToNode(hasText(installment(360)))

        composeRule.onNodeWithText(installment(360)).assertIsDisplayed()
        composeRule.onNodeWithText("S/ 0.00").assertIsDisplayed()
    }

    @Test
    fun back_returnsToResults() {
        simulateAndOpenSchedule()

        composeRule.onNodeWithContentDescription(text(R.string.action_back)).performClick()

        composeRule.onNodeWithText(text(R.string.results_title)).assertIsDisplayed()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
