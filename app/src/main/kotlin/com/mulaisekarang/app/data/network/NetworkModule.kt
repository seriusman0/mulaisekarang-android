package com.mulaisekarang.app.data.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.mulaisekarang.app.BuildConfig
import com.mulaisekarang.app.data.TokenStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.create

object NetworkModule {

    private val json = Json { ignoreUnknownKeys = true }

    fun apiService(tokenStore: TokenStore, sessionEventBus: SessionEventBus): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(SessionExpiredInterceptor(tokenStore, sessionEventBus))
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().addHeader("X-Client", "android").build())
            }
            .addInterceptor(logging)
            .build()

        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create()
    }
}
