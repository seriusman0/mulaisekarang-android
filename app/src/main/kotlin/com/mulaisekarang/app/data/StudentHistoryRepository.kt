package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.StudentQuizAttemptDetail
import com.mulaisekarang.app.data.model.StudentQuizAttemptsResponse
import com.mulaisekarang.app.data.model.StudentSubmission
import com.mulaisekarang.app.data.model.StudentSubmissionsResponse
import com.mulaisekarang.app.data.network.ApiService
import javax.inject.Inject

/** The student's own quiz attempts and assignment submissions. */
class StudentHistoryRepository @Inject constructor(private val api: ApiService) {

    suspend fun quizAttempts(courseId: Int? = null, page: Int = 1): StudentQuizAttemptsResponse =
        api.quizAttempts(courseId, page)

    suspend fun quizAttempt(attemptId: Int): StudentQuizAttemptDetail = api.quizAttempt(attemptId).data

    suspend fun submissions(
        courseId: Int? = null,
        status: String? = null,
        page: Int = 1,
    ): StudentSubmissionsResponse = api.assignmentSubmissions(courseId, status, page)

    suspend fun submission(submissionId: Int): StudentSubmission = api.assignmentSubmission(submissionId).data
}
