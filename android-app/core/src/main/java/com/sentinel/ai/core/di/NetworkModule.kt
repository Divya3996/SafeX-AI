package com.sentinel.ai.core.di

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sentinel.ai.core.coroutines.DispatcherProvider
import com.sentinel.ai.core.network.ApiClient
import com.sentinel.ai.core.network.ConnectivityChecker
import com.sentinel.ai.core.network.HttpClientWrapper
import com.sentinel.ai.core.network.JsonParser
import com.sentinel.ai.core.network.NetworkConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Hilt module that provides all networking singletons.
 *
 * ## What changed in Phase 3.8.1
 * - [provideNetworkConfig] — new, supplies all timeout/retry tuning
 * - [provideGson] — new, shared [Gson] instance used by both Retrofit and
 *   [JsonParser]
 * - [provideConnectivityChecker] — new, offline-first gate
 * - [provideHttpClientWrapper] — new, the primary networking abstraction
 * - [provideJsonParser] — new, thin Gson wrapper for providers
 * - [provideOkHttpClient] now reads timeouts from [NetworkConfig] and adds
 *   a `User-Agent` interceptor and a [HttpLoggingInterceptor]
 * - [provideApiClient] now includes [HttpClientWrapper]
 *
 * ## What was not changed
 * Retrofit is still provided for any future use-case that needs a typed
 * service interface. Current reputation providers use [HttpClientWrapper]
 * directly.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // A fixed base URL is required by Retrofit. Reputation providers build
    // their own absolute URLs and do not use Retrofit service interfaces.
    private const val RETROFIT_BASE_URL = "https://api.sentinel.ai/"

    // -------------------------------------------------------------------------
    // Phase 3.8.1 — new providers
    // -------------------------------------------------------------------------

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig()

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .serializeNulls()
        .create()

    @Provides
    @Singleton
    fun provideConnectivityChecker(
        @ApplicationContext context: Context
    ): ConnectivityChecker = com.sentinel.ai.core.network.AndroidConnectivityChecker(context)

    @Provides
    @Singleton
    fun provideHttpClientWrapper(
        okHttpClient: OkHttpClient,
        connectivityChecker: ConnectivityChecker,
        config: NetworkConfig,
        dispatcherProvider: DispatcherProvider
    ): HttpClientWrapper = HttpClientWrapper(
        okHttpClient = okHttpClient,
        connectivityChecker = connectivityChecker,
        config = config,
        dispatcherProvider = dispatcherProvider
    )

    @Provides
    @Singleton
    fun provideJsonParser(gson: Gson): JsonParser = JsonParser(gson)

    // -------------------------------------------------------------------------
    // Existing providers — enhanced in Phase 3.8.1
    // -------------------------------------------------------------------------

    /**
     * Provides the shared [OkHttpClient].
     *
     * Phase 3.8.1 changes:
     * - Timeouts now read from [NetworkConfig] (no more hard-coded values)
     * - Adds a `User-Agent` interceptor (applies to every outgoing request)
     * - Adds [HttpLoggingInterceptor] at BODY level so request/response
     *   details appear in Logcat during development
     *
     * Note: [HttpLoggingInterceptor] logs at DEBUG level unconditionally here.
     * In a production build the Timber DEBUG tree is not planted (see
     * [com.sentinel.ai.core.utils.Logger.init]), so the interceptor output
     * is silently discarded. No separate BuildConfig flag is needed.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(config: NetworkConfig): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            timber.log.Timber.tag("OkHttp").d(message)
        }.apply {
            level = HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)
            // User-Agent applied to every request at the OkHttp level.
            // Providers can override this per-request via NetworkRequest.headers.
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", config.userAgent)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(RETROFIT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideApiClient(
        okHttpClient: OkHttpClient,
        retrofit: Retrofit,
        httpClientWrapper: HttpClientWrapper
    ): ApiClient = ApiClient(
        okHttpClient = okHttpClient,
        retrofit = retrofit,
        httpClientWrapper = httpClientWrapper
    )
}
