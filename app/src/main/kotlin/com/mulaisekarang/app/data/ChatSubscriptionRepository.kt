package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.ChatSubscriptionStatus
import com.mulaisekarang.app.data.model.ChatTrialResult
import com.mulaisekarang.app.data.model.CheckoutResult
import com.mulaisekarang.app.data.network.ApiService
import javax.inject.Inject

class ChatSubscriptionRepository @Inject constructor(private val api: ApiService) {

    suspend fun status(): ChatSubscriptionStatus = api.chatSubscription().data

    suspend fun startTrial(): ChatTrialResult = api.startChatTrial().data

    suspend fun purchase(): CheckoutResult = api.purchaseChatSubscription().data
}
