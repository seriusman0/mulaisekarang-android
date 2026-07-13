package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.GoogleLoginRequest
import com.mulaisekarang.app.data.model.LoginRequest
import com.mulaisekarang.app.data.model.RegisterRequest
import com.mulaisekarang.app.data.model.User
import com.mulaisekarang.app.data.network.ApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AuthRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore,
) {
    val isLoggedIn get() = tokenStore.tokenFlow

    suspend fun login(email: String, password: String): User {
        val response = api.login(LoginRequest(email, password))
        tokenStore.saveToken(response.token)
        return response.user
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String,
    ): User {
        val response = api.register(
            RegisterRequest(
                firstName = firstName,
                lastName = lastName,
                username = username,
                email = email,
                password = password,
                passwordConfirmation = password,
            )
        )
        tokenStore.saveToken(response.token)
        return response.user
    }

    suspend fun loginWithGoogle(idToken: String): User {
        val response = api.googleLogin(GoogleLoginRequest(idToken))
        tokenStore.saveToken(response.token)
        return response.user
    }

    suspend fun me(): User = api.me().user

    suspend fun updateProfile(
        firstName: String,
        lastName: String?,
        username: String?,
        bio: String?,
        photoFile: File?,
    ): User {
        fun part(value: String?): RequestBody = (value ?: "").toRequestBody("text/plain".toMediaType())

        val fields = buildMap {
            put("first_name", part(firstName))
            lastName?.let { put("last_name", part(it)) }
            username?.let { put("username", part(it)) }
            bio?.let { put("bio", part(it)) }
        }
        val photoPart = photoFile?.let {
            MultipartBody.Part.createFormData(
                "profile_photo",
                it.name,
                it.asRequestBody("image/*".toMediaType()),
            )
        }
        return api.updateProfile(fields, photoPart).user
    }

    suspend fun logout() {
        runCatching { api.logout() }
        tokenStore.clearToken()
    }

    suspend fun hasToken(): Boolean = tokenStore.currentToken() != null
}
