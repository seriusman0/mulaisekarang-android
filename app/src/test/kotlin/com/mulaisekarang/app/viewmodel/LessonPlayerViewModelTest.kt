package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.mulaisekarang.app.data.LessonRepository
import com.mulaisekarang.app.data.TokenStore
import com.mulaisekarang.app.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class LessonPlayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val lessonRepository: LessonRepository = mockk()
    private val tokenStore: TokenStore = mockk()

    private fun viewModel(): LessonPlayerViewModel {
        coEvery { tokenStore.currentToken() } returns "token"
        return LessonPlayerViewModel(
            lessonRepository,
            tokenStore,
            SavedStateHandle(mapOf("courseId" to 1, "lessonId" to 2)),
        )
    }

    @Test
    fun `markComplete failure emits Error event instead of failing silently`() = runTest {
        val viewModel = viewModel()
        coEvery { lessonRepository.completeLesson(1, 2) } throws RuntimeException("Gagal menandai selesai.")

        viewModel.events.test {
            viewModel.markComplete()
            advanceUntilIdle()
            val event = awaitItem()
            assertEquals(LessonPlayerEvent.Error("Gagal menandai selesai."), event)
        }
    }
}
