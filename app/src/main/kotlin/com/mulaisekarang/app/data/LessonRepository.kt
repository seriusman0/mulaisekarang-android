package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.CompleteLessonResult
import com.mulaisekarang.app.data.model.LessonDetail
import com.mulaisekarang.app.data.network.ApiService
import javax.inject.Inject

class LessonRepository @Inject constructor(private val api: ApiService) {

    suspend fun lessonDetail(courseId: Int, lessonId: Int): LessonDetail =
        api.lessonDetail(courseId, lessonId).data

    suspend fun completeLesson(courseId: Int, lessonId: Int): CompleteLessonResult =
        api.completeLesson(courseId, lessonId).data
}
