package com.mulaisekarang.app.data.network

import com.mulaisekarang.app.data.TokenStore
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class SessionExpiredInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val sessionEventBus: SessionEventBus,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            if (e is java.net.UnknownHostException || e is java.net.ConnectException || e is java.net.SocketTimeoutException) {
                sessionEventBus.notifyMaintenanceMode()
            }
            throw e
        }

        val wasAuthenticated = request.header("Authorization") != null
        if (response.code == 401 && wasAuthenticated) {
            runBlocking { tokenStore.clearToken() }
            sessionEventBus.notifySessionExpired()
        }
        
        // Handle Cloudflare 530 and other server errors
        if (response.code == 530 || response.code >= 500) {
            sessionEventBus.notifyMaintenanceMode()
        }
        
        return response
    }
}
