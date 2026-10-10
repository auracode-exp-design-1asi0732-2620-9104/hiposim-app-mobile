package com.auracode.hiposim.feature.simulation.domain

import com.auracode.hiposim.feature.simulation.data.DefaultSimulationRepository
import com.auracode.hiposim.testing.StubSimulationService
import com.auracode.hiposim.testing.contractExampleParams
import com.auracode.hiposim.testing.fakeBackedSimulationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException

class SimulationUseCasesTest {
    @Test
    fun simulate_returnsTheSimulation_andObserveExposesIt() =
        runTest {
            val repository = fakeBackedSimulationRepository()
            val observe = ObserveCurrentSimulationUseCase(repository)
            assertNull(observe().value)

            val result = SimulateLoanUseCase(repository)(contractExampleParams)

            val simulation = (result as SimulationResult.Success).simulation
            assertSame(simulation, observe().value)
            assertEquals(contractExampleParams.propertyPrice, simulation.propertyPrice)
        }

    @Test
    fun simulate_passesFailuresThrough_andLeavesNoCurrentSimulation() =
        runTest {
            val repository = DefaultSimulationRepository(StubSimulationService { throw IOException("offline") })

            val result = SimulateLoanUseCase(repository)(contractExampleParams)

            assertEquals(SimulationResult.Failure(SimulationFailure.NoConnection), result)
            assertNull(ObserveCurrentSimulationUseCase(repository)().value)
        }
}
