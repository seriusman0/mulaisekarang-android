package com.mulaisekarang.app.data.network

import com.mulaisekarang.app.data.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class SessionExpiredInterceptor(
    private val tokenStore: TokenStore,
    private val sessionEventBus: SessionEventBus,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val wasAuthenticated = request.header("Authorization") != null
        if (response.code == 401 && wasAuthenticated) {
            runBlocking { tokenStore.clearToken() }
            sessionEventBus.notifySessionExpired()
        }
        return response
    }
}
