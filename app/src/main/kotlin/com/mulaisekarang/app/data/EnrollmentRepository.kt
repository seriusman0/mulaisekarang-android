package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.EnrollmentsResponse
import com.mulaisekarang.app.data.network.ApiService

class EnrollmentRepository(private val api: ApiService) {

    suspend fun myCourses(page: Int = 1): EnrollmentsResponse = api.myCourses(page)
}
