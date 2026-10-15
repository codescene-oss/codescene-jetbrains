package com.codescene.jetbrains.core.cli

import java.net.ConnectException
import java.net.UnknownHostException
import java.net.http.HttpTimeoutException
import java.util.concurrent.ExecutionException

class CsIdeUnavailableException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

object CsIdeErrors {
    private val connectivityTypes =
        listOf(UnknownHostException::class.java, HttpTimeoutException::class.java)

    private val connectivityNames =
        listOf(
            "java.net.ConnectException",
            "java.net.UnknownHostException",
            "java.net.http.HttpConnectTimeoutException",
            "java.net.http.HttpTimeoutException",
        )

    fun unwrap(error: Throwable): Exception {
        val root = rootCause(error)
        return when {
            root is CsIdeUnavailableException -> root
            root is ConnectException -> root
            isConnectivityFailure(root) -> ConnectException(root.message ?: root.javaClass.name)
            root is Exception -> root
            else -> Exception(root.message.orEmpty(), root)
        }
    }

    private fun isConnectivityFailure(error: Throwable): Boolean {
        if (connectivityTypes.any { it.isInstance(error) }) return true
        val message = error.message.orEmpty()
        return connectivityNames.any { message.contains(it) }
    }

    private fun rootCause(error: Throwable): Throwable {
        var current = error
        while (current is ExecutionException && current.cause != null) {
            current = current.cause!!
        }
        return current
    }
}
