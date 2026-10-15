package com.codescene.jetbrains.core.cli

import com.codescene.jetbrains.core.contracts.ILogger
import com.fasterxml.jackson.databind.JsonNode
import java.util.concurrent.CopyOnWriteArrayList

class CsIdeNotificationHub(
    private val logger: ILogger,
) {
    private val handlers = CopyOnWriteArrayList<Registration<(String, JsonNode) -> Unit>>()
    private val sessionListeners = CopyOnWriteArrayList<Registration<() -> Unit>>()
    private val sessionStartListeners = CopyOnWriteArrayList<Registration<(Long) -> Unit>>()

    fun addHandler(handler: (String, JsonNode) -> Unit): AutoCloseable = register(handlers, handler)

    fun addSessionListener(listener: () -> Unit): AutoCloseable = register(sessionListeners, listener)

    fun addSessionStartListener(listener: (Long) -> Unit): AutoCloseable = register(sessionStartListeners, listener)

    fun sessionStarted(session: Long) {
        sessionStartListeners.forEach { registration ->
            guarded("session start listener") { registration.callback(session) }
        }
    }

    fun dispatch(
        method: String,
        params: JsonNode,
    ) {
        handlers.forEach { registration ->
            guarded("notification handler for $method") { registration.callback(method, params) }
        }
    }

    fun sessionLost() {
        sessionListeners.forEach { registration -> guarded("session listener") { registration.callback() } }
    }

    private fun <T> register(
        registrations: CopyOnWriteArrayList<Registration<T>>,
        callback: T,
    ): AutoCloseable {
        val registration = Registration(callback)
        registrations.add(registration)
        return AutoCloseable { registrations.remove(registration) }
    }

    private fun guarded(
        what: String,
        action: () -> Unit,
    ) {
        runCatching(action).onFailure { logger.warn("cs-ide $what failed: ${it.message}", SERVICE) }
    }

    private class Registration<T>(
        val callback: T,
    )

    private companion object {
        private const val SERVICE = "CsIdeNotificationHub"
    }
}
