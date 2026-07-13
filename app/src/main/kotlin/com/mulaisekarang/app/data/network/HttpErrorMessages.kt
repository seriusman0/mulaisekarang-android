package com.mulaisekarang.app.data.network

import com.mulaisekarang.app.data.model.ApiErrorBody
import kotlinx.serialization.json.Json
import retrofit2.HttpException

const val SESSION_EXPIRED_MESSAGE = "Sesi Anda telah berakhir, silakan login kembali."

private val errorBodyJson = Json { ignoreUnknownKeys = true }

fun Throwable.userMessage(fallback: String): String = when {
    this is HttpException && code() == 401 -> SESSION_EXPIRED_MESSAGE
    this is HttpException && code() == 403 -> "Anda tidak memiliki akses untuk melakukan ini."
    this is HttpException && code() == 404 -> "Data yang Anda cari tidak ditemukan."
    this is HttpException && code() == 409 -> runCatching {
        errorBodyJson.decodeFromString(ApiErrorBody.serializer(), response()?.errorBody()?.string() ?: "")
    }.getOrNull()?.message ?: fallback
    this is HttpException && code() >= 500 -> "Terjadi gangguan pada server, coba lagi nanti."
    else -> message ?: fallback
}
