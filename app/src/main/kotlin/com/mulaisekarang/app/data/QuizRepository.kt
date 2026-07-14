package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.QuizDetail
import com.mulaisekarang.app.data.model.QuizResult
import com.mulaisekarang.app.data.model.SubmitQuizAnswersRequest
import com.mulaisekarang.app.data.network.ApiService

class QuizRepository(private val api: ApiService) {

    suspend fun quizDetail(id: Int): QuizDetail = api.quizDetail(id).data

    suspend fun submitAttempt(id: Int, answers: Map<Int, String>): QuizResult =
        api.submitQuizAttempt(id, SubmitQuizAnswersRequest(answers.mapKeys { it.key.toString() })).data
}
