package com.mulaisekarang.app.data.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SessionEventBus {
    private val _events = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    /** Non-suspending: called from an OkHttp interceptor thread, not a coroutine. */
    fun notifySessionExpired() {
        _events.tryEmit(Unit)
    }
}
