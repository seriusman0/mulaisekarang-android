package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.CourseDetailResponse
import com.mulaisekarang.app.data.model.CourseResponse
import com.mulaisekarang.app.data.model.CoursesResponse
import com.mulaisekarang.app.data.model.CreateWithdrawalRequest
import com.mulaisekarang.app.data.model.DeletedResponse
import com.mulaisekarang.app.data.model.EarningsResponse
import com.mulaisekarang.app.data.model.GradeAssignmentRequest
import com.mulaisekarang.app.data.model.GradeQuizRequest
import com.mulaisekarang.app.data.model.InstructorDashboardResponse
import com.mulaisekarang.app.data.model.InstructorQuizAttemptResponse
import com.mulaisekarang.app.data.model.InstructorQuizAttemptsResponse
import com.mulaisekarang.app.data.model.InstructorSubmissionResponse
import com.mulaisekarang.app.data.model.InstructorSubmissionsResponse
import com.mulaisekarang.app.data.model.SaveCourseRequest
import com.mulaisekarang.app.data.model.TransactionsResponse
import com.mulaisekarang.app.data.model.WalletResponse
import com.mulaisekarang.app.data.model.WithdrawalResponse
import com.mulaisekarang.app.data.model.WithdrawalsResponse
import com.mulaisekarang.app.data.network.ApiService
import javax.inject.Inject

/**
 * Thin wrapper over the instructor-portal endpoints. Keeps the public
 * instructor-profile calls in [InstructorRepository] separate from the
 * authenticated tutor_instructor management surface.
 */
class InstructorPortalRepository @Inject constructor(private val api: ApiService) {

    suspend fun dashboard(): InstructorDashboardResponse = api.instructorDashboard()

    suspend fun earnings(courseId: Int? = null, page: Int = 1): EarningsResponse =
        api.instructorEarnings(courseId, page)

    suspend fun orders(page: Int = 1): TransactionsResponse = api.instructorOrders(page)

    suspend fun wallet(): WalletResponse = api.instructorWallet()

    suspend fun withdrawals(page: Int = 1): WithdrawalsResponse = api.instructorWithdrawals(page)

    suspend fun requestWithdrawal(body: CreateWithdrawalRequest): WithdrawalResponse =
        api.instructorRequestWithdrawal(body)

    suspend fun assignmentSubmissions(courseId: Int? = null, page: Int = 1): InstructorSubmissionsResponse =
        api.instructorAssignmentSubmissions(courseId, page)

    suspend fun gradeAssignment(submissionId: Int, body: GradeAssignmentRequest): InstructorSubmissionResponse =
        api.instructorGradeAssignment(submissionId, body)

    suspend fun quizAttempts(courseId: Int? = null, page: Int = 1): InstructorQuizAttemptsResponse =
        api.instructorQuizAttempts(courseId, page)

    suspend fun gradeQuiz(attemptId: Int, body: GradeQuizRequest): InstructorQuizAttemptResponse =
        api.instructorGradeQuiz(attemptId, body)

    suspend fun courses(status: String? = null, page: Int = 1): CoursesResponse =
        api.instructorCourses(status, page)

    suspend fun course(id: Int): CourseDetailResponse = api.instructorCourse(id)

    suspend fun createCourse(body: SaveCourseRequest): CourseResponse = api.instructorCreateCourse(body)

    suspend fun updateCourse(id: Int, body: SaveCourseRequest): CourseResponse =
        api.instructorUpdateCourse(id, body)

    suspend fun deleteCourse(id: Int): DeletedResponse = api.instructorDeleteCourse(id)
}
