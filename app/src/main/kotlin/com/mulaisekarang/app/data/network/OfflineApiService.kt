package com.mulaisekarang.app.data.network

import com.mulaisekarang.app.data.model.DownloadUrlResponse
import com.mulaisekarang.app.data.model.OfflineManifestResponse
import com.mulaisekarang.app.data.model.SyncProgressRequest
import com.mulaisekarang.app.data.model.SyncProgressResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * API v2 khusus fitur offline.
 * Base URL tetap: https://mulaisekarang.com/api/v2/mobile/
 * Auth: Bearer token (sama seperti v1, header Authorization)
 */
interface OfflineApiService {

    @GET("courses/{courseId}/offline-manifest")
    suspend fun getOfflineManifest(
        @Path("courseId") courseId: Int
    ): OfflineManifestResponse

    @GET("lessons/{lessonId}/download-url")
    suspend fun getDownloadUrl(
        @Path("lessonId") lessonId: Int
    ): DownloadUrlResponse

    @POST("sync-progress")
    suspend fun syncProgress(
        @Body request: SyncProgressRequest
    ): SyncProgressResponse
}
