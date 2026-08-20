package com.mulaisekarang.app.data.network

import com.mulaisekarang.app.data.model.AssignmentDetailResponse
import com.mulaisekarang.app.data.model.AssignmentSubmissionResponse
import com.mulaisekarang.app.data.model.AuthResponse
import com.mulaisekarang.app.data.model.CartPreviewResponse
import com.mulaisekarang.app.data.model.CartRequest
import com.mulaisekarang.app.data.model.CategoriesResponse
import com.mulaisekarang.app.data.model.ChatSubscriptionResponse
import com.mulaisekarang.app.data.model.ChatTrialResponse
import com.mulaisekarang.app.data.model.MyReviewsResponse
import com.mulaisekarang.app.data.model.NotificationResponse
import com.mulaisekarang.app.data.model.NotificationsResponse
import com.mulaisekarang.app.data.model.ReviewResponse
import com.mulaisekarang.app.data.model.ReviewsResponse
import com.mulaisekarang.app.data.model.StudentQuizAttemptDetailResponse
import com.mulaisekarang.app.data.model.StudentQuizAttemptsResponse
import com.mulaisekarang.app.data.model.StudentSubmissionResponse
import com.mulaisekarang.app.data.model.StudentSubmissionsResponse
import com.mulaisekarang.app.data.model.SubmitReviewRequest
import com.mulaisekarang.app.data.model.UnreadCountResponse
import com.mulaisekarang.app.data.model.ChangePasswordRequest
import com.mulaisekarang.app.data.model.CheckoutResponse
import com.mulaisekarang.app.data.model.CourseCheckoutRequest
import com.mulaisekarang.app.data.model.ConversationResponse
import com.mulaisekarang.app.data.model.ConversationsResponse
import com.mulaisekarang.app.data.model.CourseDetailResponse
import com.mulaisekarang.app.data.model.CoursesResponse
import com.mulaisekarang.app.data.model.CompleteLessonResponse
import com.mulaisekarang.app.data.model.CreateGroupRequest
import com.mulaisekarang.app.data.model.DashboardSummaryResponse
import com.mulaisekarang.app.data.model.EligibleContactsResponse
import com.mulaisekarang.app.data.model.ForgotPasswordRequest
import com.mulaisekarang.app.data.model.InstructorDetailResponse
import com.mulaisekarang.app.data.model.ResetPasswordRequest
import com.mulaisekarang.app.data.model.EnrollmentsResponse
import com.mulaisekarang.app.data.model.GoogleLoginRequest
import com.mulaisekarang.app.data.model.LessonDetailResponse
import com.mulaisekarang.app.data.model.LoginRequest
import com.mulaisekarang.app.data.model.MeResponse
import com.mulaisekarang.app.data.model.MessageResponse
import com.mulaisekarang.app.data.model.MessagesResponse
import com.mulaisekarang.app.data.model.PaymentStatusResponse
import com.mulaisekarang.app.data.model.QuizDetailResponse
import com.mulaisekarang.app.data.model.QuizResultResponse
import com.mulaisekarang.app.data.model.RegisterRequest
import com.mulaisekarang.app.data.model.SendMessageRequest
import com.mulaisekarang.app.data.model.SendMessageResponse
import com.mulaisekarang.app.data.model.StartConversationRequest
import com.mulaisekarang.app.data.model.SubmitQuizAnswersRequest
import com.mulaisekarang.app.data.model.TransactionsResponse
import com.mulaisekarang.app.data.model.CourseResponse
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
import com.mulaisekarang.app.data.model.WalletResponse
import com.mulaisekarang.app.data.model.WithdrawalResponse
import com.mulaisekarang.app.data.model.WithdrawalsResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/google")
    suspend fun googleLogin(@Body body: GoogleLoginRequest): AuthResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): MessageResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): MessageResponse

    @GET("auth/me")
    suspend fun me(): MeResponse

    @POST("auth/logout")
    suspend fun logout(): MessageResponse

    @Multipart
    @POST("profile")
    suspend fun updateProfile(
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part photo: MultipartBody.Part?,
    ): MeResponse

    @PATCH("profile/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): MessageResponse

    @GET("courses")
    suspend fun courses(
        @Query("q") search: String? = null,
        @Query("category[]") categories: List<String> = emptyList(),
        @Query("sort") sort: String? = null,
        @Query("featured") featured: Boolean? = null,
        @Query("page") page: Int = 1,
    ): CoursesResponse

    @GET("courses/{id}")
    suspend fun courseDetail(@Path("id") id: Int): CourseDetailResponse

    @GET("categories")
    suspend fun categories(): CategoriesResponse

    @GET("instructors/{username}")
    suspend fun instructorDetail(@Path("username") username: String): InstructorDetailResponse

    @GET("my-courses")
    suspend fun myCourses(@Query("page") page: Int = 1): EnrollmentsResponse

    @GET("transactions")
    suspend fun transactions(@Query("page") page: Int = 1): TransactionsResponse

    @GET("dashboard/summary")
    suspend fun dashboardSummary(): DashboardSummaryResponse

    @POST("courses/{courseId}/checkout")
    suspend fun checkout(
        @Path("courseId") courseId: Int,
        @Body request: CourseCheckoutRequest? = null
    ): CheckoutResponse

    @GET("payment/status/{referenceId}")
    suspend fun paymentStatus(@Path("referenceId") referenceId: String): PaymentStatusResponse

    @GET("courses/{courseId}/lessons/{lessonId}")
    suspend fun lessonDetail(
        @Path("courseId") courseId: Int,
        @Path("lessonId") lessonId: Int,
    ): LessonDetailResponse

    @POST("courses/{courseId}/lessons/{lessonId}/complete")
    suspend fun completeLesson(
        @Path("courseId") courseId: Int,
        @Path("lessonId") lessonId: Int,
    ): CompleteLessonResponse

    @GET("quizzes/{id}")
    suspend fun quizDetail(@Path("id") id: Int): QuizDetailResponse

    @POST("quizzes/{id}/attempts")
    suspend fun submitQuizAttempt(@Path("id") id: Int, @Body body: SubmitQuizAnswersRequest): QuizResultResponse

    @GET("assignments/{id}")
    suspend fun assignmentDetail(@Path("id") id: Int): AssignmentDetailResponse

    @Multipart
    @POST("assignments/{id}/submissions")
    suspend fun submitAssignment(
        @Path("id") id: Int,
        @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part?,
    ): AssignmentSubmissionResponse

    @GET("chat/conversations")
    suspend fun conversations(): ConversationsResponse

    @POST("chat/conversations")
    suspend fun startConversation(@Body body: StartConversationRequest): ConversationResponse

    @GET("chat/eligible-contacts")
    suspend fun eligibleContacts(): EligibleContactsResponse

    @POST("chat/groups")
    suspend fun createGroup(@Body body: CreateGroupRequest): ConversationResponse

    @GET("chat/conversations/{id}/messages")
    suspend fun messages(@Path("id") conversationId: Int): MessagesResponse

    @POST("chat/conversations/{id}/messages")
    suspend fun sendMessage(@Path("id") conversationId: Int, @Body body: SendMessageRequest): SendMessageResponse

    // ---- Student / subscriber (see ../mulaisekarang/fdd/api/student-api.md) ----

    @GET("courses/{id}/reviews")
    suspend fun courseReviews(
        @Path("id") courseId: Int,
        @Query("page") page: Int = 1,
    ): ReviewsResponse

    @POST("courses/{id}/reviews")
    suspend fun submitReview(
        @Path("id") courseId: Int,
        @Body body: SubmitReviewRequest,
    ): ReviewResponse

    @GET("my-reviews")
    suspend fun myReviews(@Query("page") page: Int = 1): MyReviewsResponse

    @GET("quiz-attempts")
    suspend fun quizAttempts(
        @Query("course_id") courseId: Int? = null,
        @Query("page") page: Int = 1,
    ): StudentQuizAttemptsResponse

    @GET("quiz-attempts/{id}")
    suspend fun quizAttempt(@Path("id") attemptId: Int): StudentQuizAttemptDetailResponse

    @GET("assignment-submissions")
    suspend fun assignmentSubmissions(
        @Query("course_id") courseId: Int? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
    ): StudentSubmissionsResponse

    @GET("assignment-submissions/{id}")
    suspend fun assignmentSubmission(@Path("id") submissionId: Int): StudentSubmissionResponse

    @POST("cart/preview")
    suspend fun cartPreview(@Body body: CartRequest): CartPreviewResponse

    @POST("cart/checkout")
    suspend fun cartCheckout(@Body body: CartRequest): CheckoutResponse

    @GET("chat/subscription")
    suspend fun chatSubscription(): ChatSubscriptionResponse

    @POST("chat/subscription/trial")
    suspend fun startChatTrial(): ChatTrialResponse

    @POST("chat/subscription/purchase")
    suspend fun purchaseChatSubscription(): CheckoutResponse

    @GET("notifications")
    suspend fun notifications(
        @Query("unread") unreadOnly: Boolean? = null,
        @Query("page") page: Int = 1,
    ): NotificationsResponse

    @POST("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): NotificationResponse

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead(): UnreadCountResponse

    // ---- Instructor portal (role = tutor_instructor) ----

    @GET("instructor/dashboard")
    suspend fun instructorDashboard(): InstructorDashboardResponse

    @GET("instructor/earnings")
    suspend fun instructorEarnings(
        @Query("course_id") courseId: Int? = null,
        @Query("page") page: Int = 1,
    ): EarningsResponse

    @GET("instructor/orders")
    suspend fun instructorOrders(@Query("page") page: Int = 1): TransactionsResponse

    @GET("instructor/wallet")
    suspend fun instructorWallet(): WalletResponse

    @GET("instructor/withdrawals")
    suspend fun instructorWithdrawals(@Query("page") page: Int = 1): WithdrawalsResponse

    @POST("instructor/withdrawals")
    suspend fun instructorRequestWithdrawal(@Body body: CreateWithdrawalRequest): WithdrawalResponse

    @GET("instructor/assignment-submissions")
    suspend fun instructorAssignmentSubmissions(
        @Query("course_id") courseId: Int? = null,
        @Query("page") page: Int = 1,
    ): InstructorSubmissionsResponse

    @POST("instructor/assignment-submissions/{id}/grade")
    suspend fun instructorGradeAssignment(
        @Path("id") submissionId: Int,
        @Body body: GradeAssignmentRequest,
    ): InstructorSubmissionResponse

    @GET("instructor/quiz-attempts")
    suspend fun instructorQuizAttempts(
        @Query("course_id") courseId: Int? = null,
        @Query("page") page: Int = 1,
    ): InstructorQuizAttemptsResponse

    @POST("instructor/quiz-attempts/{id}/grade")
    suspend fun instructorGradeQuiz(
        @Path("id") attemptId: Int,
        @Body body: GradeQuizRequest,
    ): InstructorQuizAttemptResponse

    @GET("instructor/courses")
    suspend fun instructorCourses(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
    ): CoursesResponse

    @GET("instructor/courses/{id}")
    suspend fun instructorCourse(@Path("id") id: Int): CourseDetailResponse

    @POST("instructor/courses")
    suspend fun instructorCreateCourse(@Body body: SaveCourseRequest): CourseResponse

    @PUT("instructor/courses/{id}")
    suspend fun instructorUpdateCourse(@Path("id") id: Int, @Body body: SaveCourseRequest): CourseResponse

    @DELETE("instructor/courses/{id}")
    suspend fun instructorDeleteCourse(@Path("id") id: Int): DeletedResponse

    @GET("app/version")
    suspend fun getAppVersion(
        @Query("channel") channel: String,
        @Query("current_version_code") currentVersionCode: Int
    ): com.mulaisekarang.app.data.model.AppVersionResponse
}
