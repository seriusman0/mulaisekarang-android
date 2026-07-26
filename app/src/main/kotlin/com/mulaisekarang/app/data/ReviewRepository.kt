package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.MyReviewsResponse
import com.mulaisekarang.app.data.model.Review
import com.mulaisekarang.app.data.model.ReviewsResponse
import com.mulaisekarang.app.data.model.SubmitReviewRequest
import com.mulaisekarang.app.data.network.ApiService
import javax.inject.Inject

class ReviewRepository @Inject constructor(private val api: ApiService) {

    suspend fun courseReviews(courseId: Int, page: Int = 1): ReviewsResponse =
        api.courseReviews(courseId, page)

    /**
     * Submits or replaces the student's review. The backend answers 201 on
     * create and 200 on update; both carry the same body, so callers do not
     * have to distinguish them.
     */
    suspend fun submitReview(courseId: Int, rating: Int, body: String?): Review =
        api.submitReview(courseId, SubmitReviewRequest(rating, body?.takeIf { it.isNotBlank() })).data

    suspend fun myReviews(page: Int = 1): MyReviewsResponse = api.myReviews(page)
}
