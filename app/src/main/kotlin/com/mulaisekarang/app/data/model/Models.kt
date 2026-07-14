package com.mulaisekarang.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Mentor(
    val id: Int,
    @SerialName("display_name") val displayName: String,
    val username: String? = null,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
    @SerialName("job_title") val jobTitle: String? = null,
    @SerialName("is_verified_instructor") val isVerifiedInstructor: Boolean = false,
)

@Serializable
data class Category(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String? = null,
)

@Serializable
data class Tag(
    val id: Int,
    val name: String,
    val slug: String,
)

@Serializable
data class Course(
    val id: Int,
    val title: String,
    val description: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    val type: String? = null,
    val level: String? = null,
    @SerialName("regular_price") val regularPrice: Double = 0.0,
    @SerialName("sale_price") val salePrice: Double? = null,
    @SerialName("current_price") val currentPrice: Double = 0.0,
    @SerialName("has_discount") val hasDiscount: Boolean = false,
    val duration: String? = null,
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("reviews_avg_rating") val reviewsAvgRating: Double? = null,
    @SerialName("enrollments_count") val enrollmentsCount: Int = 0,
    @SerialName("is_enrolled") val isEnrolled: Boolean = false,
    val mentor: Mentor? = null,
    val category: Category? = null,
    val tags: List<Tag> = emptyList(),
)

@Serializable
data class Lesson(
    val id: Int,
    val title: String,
    val order: Int = 0,
    @SerialName("duration_hours") val durationHours: Int = 0,
    @SerialName("duration_minutes") val durationMinutes: Int = 0,
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    @SerialName("allow_preview") val allowPreview: Boolean = false,
    @SerialName("is_accessible") val isAccessible: Boolean = false,
    @SerialName("is_completed") val isCompleted: Boolean = false,
)

@Serializable
data class QuizSummary(
    val id: Int,
    val title: String,
    val order: Int = 0,
    @SerialName("questions_count") val questionsCount: Int = 0,
    @SerialName("time_limit") val timeLimit: Int? = null,
    @SerialName("passing_grade") val passingGrade: Int? = null,
    @SerialName("max_attempts") val maxAttempts: Int? = null,
    @SerialName("my_best_score") val myBestScore: Double? = null,
)

@Serializable
data class AssignmentSummary(
    val id: Int,
    val title: String,
    val order: Int = 0,
    @SerialName("total_points") val totalPoints: Int = 0,
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("my_submission_status") val mySubmissionStatus: String? = null,
)

@Serializable
data class Topic(
    val id: Int,
    val title: String,
    val order: Int = 0,
    val lessons: List<Lesson> = emptyList(),
    val quizzes: List<QuizSummary> = emptyList(),
    val assignments: List<AssignmentSummary> = emptyList(),
)

@Serializable
data class Review(
    val id: Int,
    val rating: Int,
    val body: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val user: Mentor? = null,
)

@Serializable
data class CourseDetail(
    val id: Int,
    val title: String,
    val description: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    val type: String? = null,
    val level: String? = null,
    @SerialName("regular_price") val regularPrice: Double = 0.0,
    @SerialName("sale_price") val salePrice: Double? = null,
    @SerialName("current_price") val currentPrice: Double = 0.0,
    @SerialName("has_discount") val hasDiscount: Boolean = false,
    val duration: String? = null,
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("reviews_avg_rating") val reviewsAvgRating: Double? = null,
    @SerialName("enrollments_count") val enrollmentsCount: Int = 0,
    @SerialName("is_enrolled") val isEnrolled: Boolean = false,
    @SerialName("lessons_count") val lessonsCount: Int = 0,
    val mentor: Mentor? = null,
    val category: Category? = null,
    val tags: List<Tag> = emptyList(),
    val topics: List<Topic> = emptyList(),
    val reviews: List<Review> = emptyList(),
    @SerialName("next_lesson_id") val nextLessonId: Int? = null,
)

@Serializable
data class PaginationMeta(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("last_page") val lastPage: Int,
    @SerialName("per_page") val perPage: Int,
    val total: Int,
)

@Serializable
data class CoursesResponse(
    val data: List<Course>,
    val meta: PaginationMeta,
)

@Serializable
data class CourseDetailResponse(
    val data: CourseDetail,
)

@Serializable
data class CategoriesResponse(
    val data: List<Category>,
)

@Serializable
data class InstructorDetail(
    val id: Int,
    @SerialName("display_name") val displayName: String,
    val username: String? = null,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
    @SerialName("job_title") val jobTitle: String? = null,
    @SerialName("is_verified_instructor") val isVerifiedInstructor: Boolean = false,
    val bio: String? = null,
    val rating: Double? = null,
    @SerialName("student_count") val studentCount: Int? = null,
    @SerialName("course_count") val courseCount: Int = 0,
)

@Serializable
data class InstructorDetailResponse(
    val data: InstructorDetail,
    val courses: List<Course> = emptyList(),
)

@Serializable
data class QuizQuestion(
    val id: Int,
    val question: String,
    val type: String,
    val options: List<String> = emptyList(),
    val order: Int = 0,
)

@Serializable
data class QuizDetail(
    val id: Int,
    val title: String,
    @SerialName("time_limit") val timeLimit: Int? = null,
    @SerialName("passing_grade") val passingGrade: Int? = null,
    @SerialName("max_attempts") val maxAttempts: Int? = null,
    @SerialName("attempts_used") val attemptsUsed: Int = 0,
    val questions: List<QuizQuestion> = emptyList(),
)

@Serializable
data class QuizDetailResponse(val data: QuizDetail)

@Serializable
data class SubmitQuizAnswersRequest(val answers: Map<String, String>)

@Serializable
data class QuizResult(
    val score: Double,
    val passed: Boolean,
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("total_count") val totalCount: Int,
)

@Serializable
data class QuizResultResponse(val data: QuizResult)

@Serializable
data class AssignmentSubmissionInfo(
    val id: Int,
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("repository_url") val repositoryUrl: String? = null,
    val status: String,
    val grade: Int? = null,
    val feedback: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
)

@Serializable
data class AssignmentDetail(
    val id: Int,
    val title: String,
    val instructions: String,
    @SerialName("total_points") val totalPoints: Int = 0,
    @SerialName("due_at") val dueAt: String? = null,
    @SerialName("allow_resubmission") val allowResubmission: Boolean = true,
    @SerialName("my_submission") val mySubmission: AssignmentSubmissionInfo? = null,
)

@Serializable
data class AssignmentDetailResponse(val data: AssignmentDetail)

@Serializable
data class AssignmentSubmissionResponse(val data: AssignmentSubmissionInfo)

@Serializable
data class DashboardSummary(
    @SerialName("total_learning_hours") val totalLearningHours: Double = 0.0,
    @SerialName("enrolled_courses_count") val enrolledCoursesCount: Int = 0,
)

@Serializable
data class DashboardSummaryResponse(
    val data: DashboardSummary,
)

@Serializable
data class User(
    val id: Int,
    val name: String,
    @SerialName("display_name") val displayName: String,
    val username: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val email: String,
    val role: String,
    val bio: String? = null,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
    @SerialName("is_verified_instructor") val isVerifiedInstructor: Boolean = false,
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: User,
)

@Serializable
data class MeResponse(
    val user: User,
)

@Serializable
data class RegisterRequest(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val username: String,
    val email: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class ForgotPasswordRequest(
    val email: String,
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val token: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
)

@Serializable
data class GoogleLoginRequest(
    @SerialName("id_token") val idToken: String,
)

@Serializable
data class MessageResponse(
    val message: String,
)

@Serializable
data class ApiErrorBody(
    val message: String? = null,
    val errors: Map<String, List<String>>? = null,
)

@Serializable
data class Enrollment(
    val id: Int,
    val status: String,
    @SerialName("progress_percent") val progressPercent: Int = 0,
    @SerialName("enrolled_at") val enrolledAt: String? = null,
    val course: Course,
)

@Serializable
data class EnrollmentsResponse(
    val data: List<Enrollment>,
    val meta: PaginationMeta,
)

@Serializable
data class Conversation(
    val id: Int,
    val type: String? = "mentor",
    val title: String? = null,
    @SerialName("other_party") val otherParty: Mentor? = null,
    val participants: List<Mentor>? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
)

@Serializable
data class ConversationsResponse(
    val data: List<Conversation>,
)

@Serializable
data class ConversationResponse(
    val data: Conversation,
)

@Serializable
data class ChatMessage(
    val id: Int,
    @SerialName("conversation_id") val conversationId: Int,
    @SerialName("sender_id") val senderId: Int,
    @SerialName("sender_name") val senderName: String? = null,
    @SerialName("is_mine") val isMine: Boolean,
    val body: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class MessagesResponse(
    val data: List<ChatMessage>,
)

@Serializable
data class SendMessageResponse(
    val data: List<ChatMessage>,
)

@Serializable
data class StartConversationRequest(
    val username: String,
)

@Serializable
data class CreateGroupRequest(
    val title: String,
    @SerialName("participant_usernames") val participantUsernames: List<String>,
)

@Serializable
data class EligibleContactsResponse(
    val data: List<Mentor>,
)

@Serializable
data class SendMessageRequest(
    val body: String,
)

@Serializable
data class LessonDetail(
    val id: Int,
    val title: String,
    val content: String? = null,
    @SerialName("duration_hours") val durationHours: Int = 0,
    @SerialName("duration_minutes") val durationMinutes: Int = 0,
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    @SerialName("allow_preview") val allowPreview: Boolean = false,
    @SerialName("is_accessible") val isAccessible: Boolean = false,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("video_stream_url") val videoStreamUrl: String? = null,
)

@Serializable
data class LessonDetailResponse(
    val data: LessonDetail,
)

@Serializable
data class CompleteLessonResult(
    @SerialName("completed_lesson_id") val completedLessonId: Int,
    @SerialName("next_lesson_id") val nextLessonId: Int? = null,
    @SerialName("progress_percent") val progressPercent: Int = 0,
    @SerialName("course_completed") val courseCompleted: Boolean = false,
)

@Serializable
data class CompleteLessonResponse(
    val data: CompleteLessonResult,
)

@Serializable
data class CheckoutResponse(
    val status: String,
    @SerialName("reference_id") val referenceId: String? = null,
    @SerialName("invoice_url") val invoiceUrl: String? = null,
)

@Serializable
data class PaymentStatusResponse(
    val status: String,
)
