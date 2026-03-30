package com.codescene.jetbrains.platform.telemetry

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Key

private val UNCAUGHT_TELEMETRY_INSTALLED = Key.create<Boolean>("codescene.uncaught.error.telemetry")
private val PREVIOUS_UNCAUGHT_EXCEPTION_HANDLER =
    Key.create<Thread.UncaughtExceptionHandler>("codescene.previous.uncaught.exception.handler")

fun installGlobalUncaughtErrorTelemetry() {
    val app = ApplicationManager.getApplication()
    if (app.getUserData(UNCAUGHT_TELEMETRY_INSTALLED) == true) return
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    app.putUserData(PREVIOUS_UNCAUGHT_EXCEPTION_HANDLER, previous)
    app.putUserData(UNCAUGHT_TELEMETRY_INSTALLED, true)
    Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
        try {
            TelemetryService.getInstance().logUnhandledError(exception, emptyMap())
        } catch (_: Throwable) {
        }
        previous?.uncaughtException(thread, exception)
    }
}

fun uninstallGlobalUncaughtErrorTelemetry() {
    val app = ApplicationManager.getApplication()
    val previous = app.getUserData(PREVIOUS_UNCAUGHT_EXCEPTION_HANDLER)
    Thread.setDefaultUncaughtExceptionHandler(previous)
    app.putUserData(PREVIOUS_UNCAUGHT_EXCEPTION_HANDLER, null)
    app.putUserData(UNCAUGHT_TELEMETRY_INSTALLED, null)
}
