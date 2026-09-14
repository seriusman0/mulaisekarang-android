package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.ChatMessage
import com.mulaisekarang.app.data.model.Conversation
import com.mulaisekarang.app.data.model.CreateGroupRequest
import com.mulaisekarang.app.data.model.Mentor
import com.mulaisekarang.app.data.model.SendMessageRequest
import com.mulaisekarang.app.data.model.StartConversationRequest
import com.mulaisekarang.app.data.network.ApiService
import javax.inject.Inject

class ChatRepository @Inject constructor(private val api: ApiService) {

    suspend fun conversations(): List<Conversation> = api.conversations().data

    suspend fun startConversation(username: String): Conversation =
        api.startConversation(StartConversationRequest(username)).data

    suspend fun startDirectConversation(username: String): Conversation =
        api.startDirectConversation(StartConversationRequest(username)).data

    suspend fun eligibleContacts(search: String? = null, perPage: Int = 20, page: Int = 1) = 
        api.eligibleContacts(search, perPage, page)

    suspend fun createGroup(title: String, participantUsernames: List<String>): Conversation =
        api.createGroup(CreateGroupRequest(title, participantUsernames)).data

    suspend fun messages(conversationId: Int): List<ChatMessage> = api.messages(conversationId).data

    suspend fun sendMessage(conversationId: Int, body: String): ChatMessage =
        api.sendMessage(conversationId, SendMessageRequest(body)).data
        
    suspend fun sendAttachment(
        conversationId: Int,
        fields: Map<String, okhttp3.RequestBody>,
        file: okhttp3.MultipartBody.Part
    ): ChatMessage = api.sendAttachment(conversationId, fields, file).data
}
