package com.auracode.hiposim.di

import com.auracode.hiposim.BuildConfig
import com.auracode.hiposim.core.analytics.ApiUsageTracker
import com.auracode.hiposim.core.analytics.EventsService
import com.auracode.hiposim.core.analytics.FakeEventsService
import com.auracode.hiposim.core.analytics.UsageTracker
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import retrofit2.Retrofit
import javax.inject.Singleton

/** Usage events (TS10b). With `USE_FAKE_API` they stay in memory; otherwise they go to the API. */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    @Provides
    @Singleton
    fun provideEventsService(retrofit: Lazy<Retrofit>): EventsService =
        if (BuildConfig.USE_FAKE_API) {
            FakeEventsService()
        } else {
            retrofit.get().create(EventsService::class.java)
        }

    /** One tracker, and so one session id, for the whole process. */
    @Provides
    @Singleton
    fun provideUsageTracker(service: EventsService): UsageTracker =
        ApiUsageTracker(service, CoroutineScope(SupervisorJob() + Dispatchers.IO))
}
