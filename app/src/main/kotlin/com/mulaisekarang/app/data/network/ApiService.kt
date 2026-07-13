package com.mulaisekarang.app.data.network

import com.mulaisekarang.app.data.model.AuthResponse
import com.mulaisekarang.app.data.model.CategoriesResponse
import com.mulaisekarang.app.data.model.CheckoutResponse
import com.mulaisekarang.app.data.model.ConversationResponse
import com.mulaisekarang.app.data.model.ConversationsResponse
import com.mulaisekarang.app.data.model.CourseDetailResponse
import com.mulaisekarang.app.data.model.CoursesResponse
import com.mulaisekarang.app.data.model.CompleteLessonResponse
import com.mulaisekarang.app.data.model.EnrollmentsResponse
import com.mulaisekarang.app.data.model.GoogleLoginRequest
import com.mulaisekarang.app.data.model.LessonDetailResponse
import com.mulaisekarang.app.data.model.LoginRequest
import com.mulaisekarang.app.data.model.MeResponse
import com.mulaisekarang.app.data.model.MessageResponse
import com.mulaisekarang.app.data.model.MessagesResponse
import com.mulaisekarang.app.data.model.PaymentStatusResponse
import com.mulaisekarang.app.data.model.RegisterRequest
import com.mulaisekarang.app.data.model.SendMessageRequest
import com.mulaisekarang.app.data.model.SendMessageResponse
import com.mulaisekarang.app.data.model.StartConversationRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
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

    @GET("courses")
    suspend fun courses(
        @Query("q") search: String? = null,
        @Query("category[]") categories: List<String> = emptyList(),
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 1,
    ): CoursesResponse

    @GET("courses/{id}")
    suspend fun courseDetail(@Path("id") id: Int): CourseDetailResponse

    @GET("categories")
    suspend fun categories(): CategoriesResponse

    @GET("my-courses")
    suspend fun myCourses(@Query("page") page: Int = 1): EnrollmentsResponse

    @POST("courses/{courseId}/checkout")
    suspend fun checkout(@Path("courseId") courseId: Int): CheckoutResponse

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

    @GET("chat/conversations")
    suspend fun conversations(): ConversationsResponse

    @POST("chat/conversations")
    suspend fun startConversation(@Body body: StartConversationRequest): ConversationResponse

    @GET("chat/conversations/{id}/messages")
    suspend fun messages(@Path("id") conversationId: Int): MessagesResponse

    @POST("chat/conversations/{id}/messages")
    suspend fun sendMessage(@Path("id") conversationId: Int, @Body body: SendMessageRequest): SendMessageResponse
}
