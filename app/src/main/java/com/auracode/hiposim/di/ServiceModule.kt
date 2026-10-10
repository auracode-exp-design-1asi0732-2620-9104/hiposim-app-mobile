package com.auracode.hiposim.di

import com.auracode.hiposim.BuildConfig
import com.auracode.hiposim.feature.simulation.data.FakeSimulationService
import com.auracode.hiposim.feature.simulation.data.SimulationService
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * API services. This is the only place that reads `BuildConfig.USE_FAKE_API`: with the flag on, a service is
 * an in-memory fake shaped like the contract; with it off, Retrofit builds it. Nothing else changes.
 */
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    /** [retrofit] is lazy so the HTTP client is not built when the fake is in use. */
    @Provides
    @Singleton
    fun provideSimulationService(retrofit: Lazy<Retrofit>): SimulationService =
        if (BuildConfig.USE_FAKE_API) {
            FakeSimulationService(latencyMillis = FakeSimulationService.DEFAULT_LATENCY_MILLIS)
        } else {
            retrofit.get().create(SimulationService::class.java)
        }
}
