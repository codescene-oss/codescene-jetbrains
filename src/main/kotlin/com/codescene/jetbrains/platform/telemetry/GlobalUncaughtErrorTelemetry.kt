package com.codescene.jetbrains.platform.telemetry

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Key

private val UNCAUGHT_TELEMETRY_INSTALLED = Key.create<Boolean>("codescene.uncaught.error.telemetry")

fun installGlobalUncaughtErrorTelemetry() {
    val app = ApplicationManager.getApplication()
    if (app.getUserData(UNCAUGHT_TELEMETRY_INSTALLED) == true) return
    app.putUserData(UNCAUGHT_TELEMETRY_INSTALLED, true)
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
        try {
            TelemetryService.getInstance().logUnhandledError(exception, emptyMap())
        } catch (_: Throwable) {
        }
        previous?.uncaughtException(thread, exception)
    }
}
