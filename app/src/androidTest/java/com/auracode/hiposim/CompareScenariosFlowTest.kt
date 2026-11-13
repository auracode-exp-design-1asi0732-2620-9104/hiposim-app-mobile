package com.auracode.hiposim

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.auracode.hiposim.feature.auth.domain.AuthRepository
import com.auracode.hiposim.feature.auth.domain.RegisterResult
import com.auracode.hiposim.feature.auth.domain.RegistrationForm
import com.auracode.hiposim.feature.scenarios.domain.SaveScenarioResult
import com.auracode.hiposim.feature.scenarios.domain.SaveScenarioUseCase
import com.auracode.hiposim.feature.scenarios.ui.COMPARE_BUTTON_TAG
import com.auracode.hiposim.feature.scenarios.ui.COMPARISON_TABLE_TAG
import com.auracode.hiposim.feature.scenarios.ui.SCENARIOS_LIST_TAG
import com.auracode.hiposim.feature.scenarios.ui.scenarioSelectTag
import com.auracode.hiposim.feature.simulation.domain.SimulationParams
import com.auracode.hiposim.feature.simulation.domain.SimulationRepository
import com.auracode.hiposim.feature.simulation.domain.SimulationResult
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import javax.inject.Inject

/** US07: real navigation and selection with synthetic scenarios from the in-memory API, never EXP1 evidence. */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class CompareScenariosFlowTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var authRepository: AuthRepository

    @Inject lateinit var simulationRepository: SimulationRepository

    @Inject lateinit var saveScenario: SaveScenarioUseCase

    private val ids = mutableListOf<String>()

    @Before
    fun setUp() {
        check(BuildConfig.USE_FAKE_API) { "This test requires the in-memory API." }
        hiltRule.inject()
        runBlocking {
            val registered =
                authRepository.register(
                    RegistrationForm("US07 Test", "us07@example.com", "900000001", "test12345", true, true),
                )
            assertTrue(registered is RegisterResult.Success)
            repeat(4) { index ->
                val simulated =
                    simulationRepository.simulate(
                        SimulationParams(BigDecimal("280000"), BigDecimal("42000"), 15 + index * 5, false),
                    )
                assertTrue(simulated is SimulationResult.Success)
                val saved = saveScenario("US07 scenario ${index + 1}") as SaveScenarioResult.Success
                ids += saved.scenario.id
            }
        }
        composeRule.onNodeWithText(text(R.string.simulation_submit)).performScrollTo().performClick()
        composeRule.waitUntilAtLeastOneExists(hasText(text(R.string.results_title)), TIMEOUT_MILLIS)
        composeRule.onNodeWithText(text(R.string.nav_history)).performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag(SCENARIOS_LIST_TAG), TIMEOUT_MILLIS)
    }

    @Test
    fun twoScenarios_showTheComparison_andReturningKeepsTheSelection() {
        composeRule.onNodeWithTag(COMPARE_BUTTON_TAG).assertIsNotEnabled()
        pick(0)
        composeRule.onNodeWithTag(COMPARE_BUTTON_TAG).assertIsNotEnabled()
        pick(1)
        composeRule.onNodeWithTag(COMPARE_BUTTON_TAG).assertIsEnabled().performClick()

        composeRule.waitUntilAtLeastOneExists(hasTestTag(COMPARISON_TABLE_TAG), TIMEOUT_MILLIS)
        composeRule.onNodeWithText("US07 scenario 1").assertIsDisplayed()
        composeRule.onNodeWithText("US07 scenario 2").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.action_back)).performClick()
        composeRule.onNodeWithTag(COMPARE_BUTTON_TAG).assertIsEnabled()
        pick(1)
        composeRule.onNodeWithTag(COMPARE_BUTTON_TAG).assertIsNotEnabled()
    }

    @Test
    fun threeScenarios_canBeCompared_andAFourthCannotBePicked() {
        repeat(3, ::pick)
        composeRule.onNodeWithTag(SCENARIOS_LIST_TAG).performScrollToNode(hasTestTag(scenarioSelectTag(ids[3])))
        composeRule.onNodeWithTag(scenarioSelectTag(ids[3])).assertIsNotEnabled()
        composeRule.onNodeWithTag(COMPARE_BUTTON_TAG).performClick()

        composeRule.waitUntilAtLeastOneExists(hasTestTag(COMPARISON_TABLE_TAG), TIMEOUT_MILLIS)
        repeat(3) { index -> composeRule.onNodeWithText("US07 scenario ${index + 1}").assertIsDisplayed() }
    }

    private fun pick(index: Int) {
        val tag = scenarioSelectTag(ids[index])
        composeRule.onNodeWithTag(SCENARIOS_LIST_TAG).performScrollToNode(hasTestTag(tag))
        composeRule.onNodeWithTag(tag).performClick()
    }

    private fun text(id: Int) = composeRule.activity.getString(id)

    private companion object {
        const val TIMEOUT_MILLIS = 30_000L
    }
}
