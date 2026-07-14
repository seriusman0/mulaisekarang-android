package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.Category
import com.mulaisekarang.app.data.model.CheckoutResponse
import com.mulaisekarang.app.data.model.CourseDetail
import com.mulaisekarang.app.data.model.CoursesResponse
import com.mulaisekarang.app.data.network.ApiService

class CourseRepository(private val api: ApiService) {

    suspend fun courses(
        search: String? = null,
        category: String? = null,
        sort: String? = null,
        featured: Boolean? = null,
        page: Int = 1,
    ): CoursesResponse = api.courses(
        search = search?.takeIf { it.isNotBlank() },
        categories = category?.let { listOf(it) } ?: emptyList(),
        sort = sort,
        featured = featured,
        page = page,
    )

    suspend fun courseDetail(id: Int): CourseDetail = api.courseDetail(id).data

    suspend fun categories(): List<Category> = api.categories().data

    suspend fun checkout(courseId: Int): CheckoutResponse = api.checkout(courseId)

    suspend fun paymentStatus(referenceId: String): String = api.paymentStatus(referenceId).status
}
