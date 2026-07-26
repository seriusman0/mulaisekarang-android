package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.AppNotification
import com.mulaisekarang.app.data.model.NotificationsResponse
import com.mulaisekarang.app.data.network.ApiService
import javax.inject.Inject

class NotificationRepository @Inject constructor(private val api: ApiService) {

    suspend fun notifications(unreadOnly: Boolean = false, page: Int = 1): NotificationsResponse =
        api.notifications(if (unreadOnly) true else null, page)

    suspend fun markRead(id: String): AppNotification = api.markNotificationRead(id).data

    suspend fun markAllRead(): Int = api.markAllNotificationsRead().data.unreadCount
}
