package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.InstructorDetailResponse
import com.mulaisekarang.app.data.network.ApiService
import javax.inject.Inject

class InstructorRepository @Inject constructor(private val api: ApiService) {

    suspend fun instructorDetail(username: String): InstructorDetailResponse = api.instructorDetail(username)
}
