package com.mulaisekarang.app.data.network

import com.mulaisekarang.app.data.TokenStore
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor @Inject constructor(private val tokenStore: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.currentTokenValue
        val request = chain.request().newBuilder()
            .addHeader("Accept", "application/json")
            .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
            .build()
        return chain.proceed(request)
    }
}
