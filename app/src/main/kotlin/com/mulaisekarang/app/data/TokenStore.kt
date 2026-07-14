package com.mulaisekarang.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore by preferencesDataStore(name = "auth")

@Singleton
class TokenStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val tokenKey = stringPreferencesKey("token")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[tokenKey] }

    private val currentTokenState: StateFlow<String?> =
        tokenFlow.stateIn(scope, SharingStarted.Eagerly, null)

    /** Synchronous, non-suspending read for use on OkHttp interceptor threads. */
    val currentTokenValue: String? get() = currentTokenState.value

    suspend fun currentToken(): String? = tokenFlow.first()

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[tokenKey] = token }
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.remove(tokenKey) }
    }
}
