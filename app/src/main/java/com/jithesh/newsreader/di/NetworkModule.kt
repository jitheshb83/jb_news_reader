package com.jithesh.newsreader.di

import android.content.Context
import com.jithesh.newsreader.data.network.FeedFetchService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val HTTP_CACHE_SIZE_BYTES = 5L * 1024 * 1024
private const val PLACEHOLDER_BASE_URL = "https://placeholder.invalid/"
private const val TIMEOUT_SECONDS = 15L

/** Debug builds contribute a logging interceptor here; release builds contribute none. */
@Module
@InstallIn(SingletonComponent::class)
abstract class InterceptorBindingsModule {
    @Multibinds
    abstract fun bindInterceptors(): Set<Interceptor>
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpCache(@ApplicationContext context: Context): Cache =
        Cache(File(context.cacheDir, "http_cache"), HTTP_CACHE_SIZE_BYTES)

    @Provides
    @Singleton
    fun provideOkHttpClient(cache: Cache, interceptors: Set<@JvmSuppressWildcards Interceptor>): OkHttpClient =
        OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .apply { interceptors.forEach { addInterceptor(it) } }
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(okHttpClient)
            .build()

    @Provides
    @Singleton
    fun provideFeedFetchService(retrofit: Retrofit): FeedFetchService =
        retrofit.create(FeedFetchService::class.java)
}
