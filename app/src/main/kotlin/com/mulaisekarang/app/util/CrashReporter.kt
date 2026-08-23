package com.mulaisekarang.app.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mulaisekarang.app.data.model.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around Crashlytics so call sites don't reach for the Firebase
 * SDK directly — keeps crash/breadcrumb reporting swappable and out of every
 * screen's imports.
 */
@Singleton
class CrashReporter @Inject constructor() {

    private val crashlytics get() = FirebaseCrashlytics.getInstance()

    /** Tag crashes/logs with who hit them, without sending PII like email. */
    fun setUser(user: User) {
        crashlytics.setUserId(user.id.toString())
        crashlytics.setCustomKey("user_role", user.role)
    }

    fun clearUser() {
        crashlytics.setUserId("")
    }

    /** Breadcrumb for a non-fatal failure — shows up in the crash timeline
     * even when it didn't itself crash the app. */
    fun logNonFatal(tag: String, throwable: Throwable) {
        crashlytics.log(tag)
        crashlytics.recordException(throwable)
    }

    fun log(message: String) {
        crashlytics.log(message)
    }
}
