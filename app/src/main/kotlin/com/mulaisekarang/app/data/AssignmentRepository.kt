package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.AssignmentDetail
import com.mulaisekarang.app.data.model.AssignmentSubmissionInfo
import com.mulaisekarang.app.data.network.ApiService
import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class AssignmentRepository @Inject constructor(private val api: ApiService) {

    suspend fun assignmentDetail(id: Int): AssignmentDetail = api.assignmentDetail(id).data

    suspend fun submit(id: Int, repositoryUrl: String?, file: File?): AssignmentSubmissionInfo {
        val fields = buildMap<String, RequestBody> {
            repositoryUrl?.let { put("repository_url", it.toRequestBody("text/plain".toMediaType())) }
        }
        val filePart = file?.let {
            MultipartBody.Part.createFormData("file", it.name, it.asRequestBody("application/octet-stream".toMediaType()))
        }
        return api.submitAssignment(id, fields, filePart).data
    }
}
