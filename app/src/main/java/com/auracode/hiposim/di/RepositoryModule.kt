package com.auracode.hiposim.di

import com.auracode.hiposim.feature.auth.data.DefaultAuthRepository
import com.auracode.hiposim.feature.auth.domain.AuthRepository
import com.auracode.hiposim.feature.simulation.data.DefaultSimulationRepository
import com.auracode.hiposim.feature.simulation.domain.SimulationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds each domain repository to its implementation.
 *
 * Every repository has a single implementation, and the fake or real choice is made one level below, in
 * [ServiceModule]. The implementations are singletons themselves: they hold the session and the current simulation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindSimulationRepository(impl: DefaultSimulationRepository): SimulationRepository

    @Binds
    abstract fun bindAuthRepository(impl: DefaultAuthRepository): AuthRepository
}
