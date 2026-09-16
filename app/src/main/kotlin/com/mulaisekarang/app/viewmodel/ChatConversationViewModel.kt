package com.mulaisekarang.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mulaisekarang.app.data.ChatRepository
import com.mulaisekarang.app.data.model.ChatMessage
import com.mulaisekarang.app.data.model.Conversation
import com.mulaisekarang.app.data.network.isChatPaywall
import com.mulaisekarang.app.data.network.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val POLL_INTERVAL_MS = 4000L

class ProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgressUpdate: (Int) -> Unit
) : RequestBody() {
    override fun contentType(): MediaType? = delegate.contentType()
    override fun contentLength(): Long = delegate.contentLength()
    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink)
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }
    private inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {
        private var bytesWritten = 0L
        private val contentLength = contentLength()
        override fun write(source: okio.Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            if (contentLength > 0) {
                val progress = ((bytesWritten.toFloat() / contentLength) * 100).toInt()
                onProgressUpdate(progress)
            }
        }
    }
}

sealed interface ChatConversationUiState {
    data object Loading : ChatConversationUiState
    data class Loaded(val messages: List<ChatMessage>) : ChatConversationUiState
    data class Error(val message: String) : ChatConversationUiState
}

@HiltViewModel
class ChatConversationViewModel @Inject constructor(
    private val repository: ChatRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val conversationId: Int = checkNotNull(savedStateHandle["conversationId"])

    private val _uiState = MutableStateFlow<ChatConversationUiState>(ChatConversationUiState.Loading)
    val uiState: StateFlow<ChatConversationUiState> = _uiState.asStateFlow()

    private val _conversation = MutableStateFlow<Conversation?>(null)
    val conversation: StateFlow<Conversation?> = _conversation.asStateFlow()

    private val _sendError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val sendError: SharedFlow<String> = _sendError.asSharedFlow()

    private val _paywallRequired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val paywallRequired: SharedFlow<Unit> = _paywallRequired.asSharedFlow()

    private var pollingJob: Job? = null
    
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Int?>(null)
    val uploadProgress: StateFlow<Int?> = _uploadProgress.asStateFlow()

    private val _typingIndicator = MutableStateFlow<String?>(null)
    val typingIndicator: StateFlow<String?> = _typingIndicator.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.conversations() }
                .onSuccess { list -> _conversation.value = list.find { it.id == conversationId } }
        }
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun refresh() {
        runCatching { repository.messages(conversationId) }
            .onSuccess { messages -> _uiState.update { ChatConversationUiState.Loaded(messages) } }
            .onFailure { e ->
                if (_uiState.value !is ChatConversationUiState.Loaded) {
                    _uiState.update { ChatConversationUiState.Error(e.message ?: "Gagal memuat pesan.") }
                }
            }
    }

    fun sendMessage(body: String) {
        if (body.isBlank() || _isSending.value) return
        _isSending.value = true
        viewModelScope.launch {
            runCatching { repository.sendMessage(conversationId, body) }
                .onSuccess { refresh() }
                .onFailure { e ->
                    // An expired add-on is not a transient send failure — send
                    // the student to the paywall rather than a retry snackbar.
                    if (e.isChatPaywall()) {
                        _paywallRequired.emit(Unit)
                    } else {
                        _sendError.emit(e.userMessage("Pesan gagal terkirim."))
                    }
                }
            _isSending.value = false
        }
    }

    fun sendAttachment(uri: Uri, context: Context, bodyText: String) {
        if (_isSending.value) return
        _isSending.value = true
        _uploadProgress.value = 0
        viewModelScope.launch {
            runCatching {
                val filePart = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val requestBody = bytes.toRequestBody("application/octet-stream".toMediaTypeOrNull(), 0, bytes.size)
                    val progressBody = ProgressRequestBody(requestBody) { progress ->
                        _uploadProgress.value = progress
                    }
                    MultipartBody.Part.createFormData("file", "attachment", progressBody)
                } ?: throw Exception("Cannot read file")
                
                val fields = mutableMapOf<String, okhttp3.RequestBody>()
                if (bodyText.isNotBlank()) {
                    fields["body"] = bodyText.toRequestBody("text/plain".toMediaTypeOrNull())
                }
                
                repository.sendAttachment(conversationId, fields, filePart)
            }
                .onSuccess { refresh() }
                .onFailure { e ->
                    if (e.isChatPaywall()) _paywallRequired.emit(Unit)
                    else _sendError.emit(e.userMessage("Gagal mengunggah file."))
                }
            _isSending.value = false
            _uploadProgress.value = null
        }
    }

    fun onTyping() {
        // Placeholder for Pusher client sendTypingEvent logic
        // Panggil sendTypingEvent() setiap kali teks input berubah, tapi debounce minimal 1 detik agar tidak spam.
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
