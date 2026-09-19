package com.auracode.hiposim.di

import com.auracode.hiposim.feature.auth.data.FakeAuthRepository
import com.auracode.hiposim.feature.auth.domain.AuthRepository
import com.auracode.hiposim.feature.simulation.data.FakeSimulationRepository
import com.auracode.hiposim.feature.simulation.domain.SimulationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Swap each fake for its API-backed repository when the backend exists. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSimulationRepository(impl: FakeSimulationRepository): SimulationRepository

    /** Singleton: the session must be shared by every screen. */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository
}
