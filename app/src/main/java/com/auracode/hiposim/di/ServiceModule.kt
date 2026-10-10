package com.auracode.hiposim.di

import com.auracode.hiposim.BuildConfig
import com.auracode.hiposim.feature.auth.data.AuthService
import com.auracode.hiposim.feature.auth.data.FakeAuthService
import com.auracode.hiposim.feature.leads.data.AgenciesService
import com.auracode.hiposim.feature.leads.data.FakeAgenciesService
import com.auracode.hiposim.feature.leads.data.FakeLeadsService
import com.auracode.hiposim.feature.leads.data.LeadsService
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
 * API services. This is the only place that reads `BuildConfig.USE_FAKE_API` and `USE_FAKE_AGENCIES`: with the flag on, a service is
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

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Lazy<Retrofit>): AuthService =
        if (BuildConfig.USE_FAKE_API) {
            FakeAuthService(latencyMillis = FakeAuthService.DEFAULT_LATENCY_MILLIS)
        } else {
            retrofit.get().create(AuthService::class.java)
        }

    @Provides
    @Singleton
    fun provideLeadsService(retrofit: Lazy<Retrofit>): LeadsService =
        if (BuildConfig.USE_FAKE_API) {
            FakeLeadsService(latencyMillis = FakeLeadsService.DEFAULT_LATENCY_MILLIS)
        } else {
            retrofit.get().create(LeadsService::class.java)
        }

    /**
     * GET /api/agencies is not in the backend yet, so the list stays in memory (`USE_FAKE_AGENCIES`) even
     * when everything else talks to the real API. It is the demo agency of the backend seed, which is real there.
     */
    @Provides
    @Singleton
    fun provideAgenciesService(retrofit: Lazy<Retrofit>): AgenciesService =
        if (BuildConfig.USE_FAKE_API || BuildConfig.USE_FAKE_AGENCIES) {
            FakeAgenciesService()
        } else {
            retrofit.get().create(AgenciesService::class.java)
        }
}
