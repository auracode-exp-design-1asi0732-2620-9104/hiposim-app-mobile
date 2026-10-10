package com.auracode.hiposim.di

import com.auracode.hiposim.feature.auth.data.FakeAuthRepository
import com.auracode.hiposim.feature.auth.domain.AuthRepository
import com.auracode.hiposim.feature.simulation.data.DefaultSimulationRepository
import com.auracode.hiposim.feature.simulation.domain.SimulationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds each domain repository to its implementation.
 *
 * Repositories already connected to the API contract (simulation) have a single implementation, and the
 * fake or real choice is made one level below, in [ServiceModule]. The rest are still in-memory fakes.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindSimulationRepository(impl: DefaultSimulationRepository): SimulationRepository

    /** Singleton: the session must be shared by every screen. */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository
}
