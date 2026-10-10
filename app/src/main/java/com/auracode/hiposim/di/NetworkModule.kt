package com.auracode.hiposim.di

import com.auracode.hiposim.BuildConfig
import com.auracode.hiposim.core.network.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** REST client of the HipoSim API. Each feature gets its service with `retrofit.create(...)` in its own module. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val TIMEOUT_SECONDS = 20L
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /** Tolerant on purpose: new fields in the API must not break an installed app. */
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .apply {
                if (BuildConfig.DEBUG) addInterceptor(loggingInterceptor())
            }.build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()

    /** Debug builds only. Bodies are logged to help while the contract settles; the token is never printed. */
    private fun loggingInterceptor() =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader(AuthInterceptor.AUTHORIZATION)
        }
}
