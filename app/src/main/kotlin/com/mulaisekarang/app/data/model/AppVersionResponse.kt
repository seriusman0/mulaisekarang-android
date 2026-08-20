package com.mulaisekarang.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppVersionResponse(
    @SerialName("update_available")
    val updateAvailable: Boolean = false,
    @SerialName("update_required")
    val updateRequired: Boolean = false,
    @SerialName("checksum_sha256")
    val checksumSha256: String? = null,
    @SerialName("file_size")
    val fileSize: Long? = null,
    @SerialName("download_url")
    val downloadUrl: String? = null,
    @SerialName("version_name")
    val versionName: String? = null,
    @SerialName("version_code")
    val versionCode: Int? = null,
    @SerialName("release_notes")
    val releaseNotes: String? = null
)
