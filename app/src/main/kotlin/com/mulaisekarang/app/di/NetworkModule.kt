package com.mulaisekarang.app.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mulaisekarang.app.BuildConfig
import com.mulaisekarang.app.data.network.ApiService
import com.mulaisekarang.app.data.network.AuthInterceptor
import com.mulaisekarang.app.data.network.SessionExpiredInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        authInterceptor: AuthInterceptor,
        sessionExpiredInterceptor: SessionExpiredInterceptor,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val cacheSize = (50 * 1024 * 1024).toLong()
        val cache = okhttp3.Cache(context.cacheDir, cacheSize)

        lateinit var okHttpClient: OkHttpClient

        val offlineFirstInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            
            // Proceed normally. If it's a GET request with valid cache, OkHttp will return it instantly 
            // because our rewriteResponseInterceptor sets a 7-day max-age.
            val response = chain.proceed(request)

            if (request.method == "GET" && response.networkResponse == null) {
                // If networkResponse is null, it means the response was served entirely from cache.
                val isForceNetwork = request.cacheControl.noCache || request.header("Cache-Control")?.contains("no-cache") == true
                if (!isForceNetwork) {
                    // Trigger a background network fetch to update the cache for the next load
                    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val freshRequest = request.newBuilder()
                                .cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
                                .build()
                            okHttpClient.newCall(freshRequest).execute().close()
                        } catch (e: Exception) {
                            // Ignore background fetch errors
                        }
                    }
                }
            }

            response
        }

        val rewriteResponseInterceptor = okhttp3.Interceptor { chain ->
            val response = chain.proceed(chain.request())
            if (chain.request().method == "GET") {
                response.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", "public, max-age=${7 * 24 * 60 * 60}")
                    .build()
            } else {
                response
            }
        }

        okHttpClient = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(offlineFirstInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(sessionExpiredInterceptor)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().addHeader("X-Client", "android").build())
            }
            .addInterceptor(logging)
            .addNetworkInterceptor(rewriteResponseInterceptor)
            .build()

        return okHttpClient
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create()

    @Provides
    @Singleton
    fun provideOfflineApiService(client: OkHttpClient, json: Json): com.mulaisekarang.app.data.network.OfflineApiService {
        val contentType = "application/json".toMediaType()
        val retrofitV2 = Retrofit.Builder()
            .baseUrl("https://mulaisekarang.com/api/v2/mobile/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        return retrofitV2.create(com.mulaisekarang.app.data.network.OfflineApiService::class.java)
    }
}
