package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.EnrollmentsResponse
import com.mulaisekarang.app.data.network.ApiService
import javax.inject.Inject

class EnrollmentRepository @Inject constructor(private val api: ApiService) {

    suspend fun myCourses(page: Int = 1): EnrollmentsResponse = api.myCourses(page)
}
