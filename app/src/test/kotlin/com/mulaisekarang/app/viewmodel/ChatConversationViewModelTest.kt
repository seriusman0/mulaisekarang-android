package com.mulaisekarang.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.mulaisekarang.app.data.ChatRepository
import com.mulaisekarang.app.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ChatConversationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: ChatRepository = mockk()

    private fun viewModel(): ChatConversationViewModel {
        coEvery { repository.conversations() } returns emptyList()
        return ChatConversationViewModel(repository, SavedStateHandle(mapOf("conversationId" to 1)))
    }

    @Test
    fun `sendMessage failure emits sendError instead of failing silently`() = runTest {
        val viewModel = viewModel()
        coEvery { repository.messages(1) } returns emptyList()
        coEvery { repository.sendMessage(1, "halo") } throws RuntimeException("Network error")
        advanceUntilIdle()

        viewModel.sendError.test {
            viewModel.sendMessage("halo")
            advanceUntilIdle()
            assertEquals("Network error", awaitItem())
        }
    }

    @Test
    fun `refresh failure surfaces as Error state when nothing loaded yet`() = runTest {
        val viewModel = viewModel()
        coEvery { repository.messages(1) } throws RuntimeException("Gagal memuat pesan.")

        // startPolling() loops forever (while(isActive) { refresh(); delay(4s) }); advanceUntilIdle()
        // would advance virtual time forever with it running, so only let the first iteration run
        // then stop the loop before asserting.
        viewModel.startPolling()
        runCurrent()
        viewModel.stopPolling()

        assertTrue(viewModel.uiState.value is ChatConversationUiState.Error)
    }
}
