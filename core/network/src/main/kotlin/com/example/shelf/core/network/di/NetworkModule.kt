package com.example.shelf.core.network.di

import android.content.Context
import android.os.Build
import com.example.shelf.core.network.BuildConfig
import com.example.shelf.core.network.OpenLibraryApi
import com.example.shelf.core.network.OpenLibraryApiFactory
import com.example.shelf.core.network.OpenLibraryDataSource
import com.example.shelf.core.network.RetrofitOpenLibraryDataSource
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NetworkBindings {
    @Binds
    fun bindOpenLibraryDataSource(impl: RetrofitOpenLibraryDataSource): OpenLibraryDataSource
}

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    private const val CACHE_SIZE_BYTES = 10L * 1024 * 1024
    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun provideJson(): Json = OpenLibraryApiFactory.json

    /** Shared by Retrofit and Coil, so cover downloads also identify the app. */
    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val userAgent = userAgent(context)
        return OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "http_cache"), CACHE_SIZE_BYTES))
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(
                Interceptor { chain ->
                    chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build())
                },
            )
            .apply {
                // Logging only in debug builds; R8 removes this branch and the class in release.
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()
    }

    /**
     * OkHttp is created lazily on the first request (off the main thread), not when the graph
     * is built, because creating it touches the disk cache.
     */
    @Provides
    @Singleton
    fun provideOpenLibraryApi(okHttpClient: Lazy<OkHttpClient>, json: Json): OpenLibraryApi =
        OpenLibraryApiFactory.create(
            baseUrl = OpenLibraryApi.BASE_URL,
            callFactory = Call.Factory { request -> okHttpClient.get().newCall(request) },
            json = json,
        )

    /**
     * Open Library asks API clients to identify themselves. Only the app name, version and package
     * are sent; nothing about the user.
     */
    private fun userAgent(context: Context): String {
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
        return "Shelf/$version (Android ${Build.VERSION.RELEASE}; ${context.packageName})"
    }
}
