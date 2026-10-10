package com.auracode.hiposim.di

import com.auracode.hiposim.feature.auth.data.FakeAuthRepository
import com.auracode.hiposim.feature.auth.domain.AuthRepository
import com.auracode.hiposim.feature.simulation.data.RemoteSimulationRepository
import com.auracode.hiposim.feature.simulation.domain.SimulationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Simulations come from the API. Swap the auth fake for its API-backed repository when sign in exists. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    /** Singleton: the last simulation must be shared by the form, Results and Registration screens. */
    @Binds
    @Singleton
    abstract fun bindSimulationRepository(impl: RemoteSimulationRepository): SimulationRepository

    /** Singleton: the session must be shared by every screen. */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository
}
