package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.ChatMessage
import com.mulaisekarang.app.data.model.Conversation
import com.mulaisekarang.app.data.model.CreateGroupRequest
import com.mulaisekarang.app.data.model.Mentor
import com.mulaisekarang.app.data.model.SendMessageRequest
import com.mulaisekarang.app.data.model.StartConversationRequest
import com.mulaisekarang.app.data.network.ApiService

class ChatRepository(private val api: ApiService) {

    suspend fun conversations(): List<Conversation> = api.conversations().data

    suspend fun startConversation(username: String): Conversation =
        api.startConversation(StartConversationRequest(username)).data

    suspend fun startAiTutorConversation(): Conversation = api.startAiTutorConversation().data

    suspend fun eligibleContacts(): List<Mentor> = api.eligibleContacts().data

    suspend fun createGroup(title: String, participantUsernames: List<String>): Conversation =
        api.createGroup(CreateGroupRequest(title, participantUsernames)).data

    suspend fun messages(conversationId: Int): List<ChatMessage> = api.messages(conversationId).data

    suspend fun sendMessage(conversationId: Int, body: String): List<ChatMessage> =
        api.sendMessage(conversationId, SendMessageRequest(body)).data
}
