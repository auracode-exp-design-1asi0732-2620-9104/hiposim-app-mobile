package com.auracode.hiposim.di

import com.auracode.hiposim.feature.simulation.data.FakeSimulationRepository
import com.auracode.hiposim.feature.simulation.domain.SimulationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    /** Swap for the API-backed repository when the backend exists. */
    @Binds
    @Singleton
    abstract fun bindSimulationRepository(impl: FakeSimulationRepository): SimulationRepository
}
