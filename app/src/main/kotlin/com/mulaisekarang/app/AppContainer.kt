package com.mulaisekarang.app

import android.content.Context
import com.mulaisekarang.app.data.AuthRepository
import com.mulaisekarang.app.data.ChatRepository
import com.mulaisekarang.app.data.CourseRepository
import com.mulaisekarang.app.data.EnrollmentRepository
import com.mulaisekarang.app.data.LessonRepository
import com.mulaisekarang.app.data.TokenStore
import com.mulaisekarang.app.data.network.NetworkModule
import com.mulaisekarang.app.data.network.SessionEventBus

class AppContainer(context: Context) {
    val tokenStore = TokenStore(context.applicationContext)
    val sessionEventBus = SessionEventBus()
    private val apiService = NetworkModule.apiService(tokenStore, sessionEventBus)

    val authRepository = AuthRepository(apiService, tokenStore)
    val courseRepository = CourseRepository(apiService)
    val enrollmentRepository = EnrollmentRepository(apiService)
    val chatRepository = ChatRepository(apiService)
    val lessonRepository = LessonRepository(apiService)
}
