package com.auracode.hiposim.feature.simulation.data

import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.feature.simulation.domain.SimulationFailure
import com.auracode.hiposim.feature.simulation.domain.SimulationResult
import com.auracode.hiposim.testing.StubSimulationService
import com.auracode.hiposim.testing.contractExampleParams
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Whether the simulation on screen belongs to the active session, which the API requires to accept a lead. */
class SimulationOwnershipTest {
    private val tokenStore = TokenStore()
    private val service = StubSimulationService()
    private val repository = DefaultSimulationRepository(service, tokenStore)

    @Test
    fun withoutASimulation_nothingBelongsToTheSession() {
        tokenStore.token = "buyer-a"

        assertFalse(repository.currentBelongsToSession)
    }

    @Test
    fun calculatedAsGuest_doesNotBelongToALaterSession() =
        runTest {
            repository.simulate(contractExampleParams)
            assertFalse(repository.currentBelongsToSession)

            tokenStore.token = "buyer-a"
            assertFalse(repository.currentBelongsToSession)
        }

    @Test
    fun calculatedWithTheSession_belongsToItUntilTheSessionChanges() =
        runTest {
            tokenStore.token = "buyer-a"
            repository.simulate(contractExampleParams)
            assertTrue(repository.currentBelongsToSession)

            tokenStore.token = "buyer-b"
            assertFalse(repository.currentBelongsToSession)

            tokenStore.clear()
            assertFalse(repository.currentBelongsToSession)
        }

    @Test
    fun recalculateCurrent_repeatsTheSameRequestUnderTheSession() =
        runTest {
            repository.simulate(contractExampleParams)
            val guestId = repository.currentSimulation.value?.id
            tokenStore.token = "buyer-a"

            val result = repository.recalculateCurrent()

            assertTrue(result is SimulationResult.Success)
            assertEquals(2, service.requests.size)
            assertEquals(service.requests[0], service.requests[1])
            assertNotEquals(guestId, repository.currentSimulation.value?.id)
            assertTrue(repository.currentBelongsToSession)
        }

    @Test
    fun recalculateCurrent_withoutASimulation_fails() =
        runTest {
            assertEquals(SimulationResult.Failure(SimulationFailure.Unexpected), repository.recalculateCurrent())
            assertTrue(service.requests.isEmpty())
        }

    @Test
    fun aFailedRecalculation_keepsTheSimulationAndItsOwner() =
        runTest {
            repository.simulate(contractExampleParams)
            val guestId = repository.currentSimulation.value?.id
            tokenStore.token = "buyer-a"
            service.answer = { throw IOException("offline") }

            val result = repository.recalculateCurrent()

            assertEquals(SimulationResult.Failure(SimulationFailure.NoConnection), result)
            assertEquals(guestId, repository.currentSimulation.value?.id)
            assertFalse(repository.currentBelongsToSession)
        }
}
