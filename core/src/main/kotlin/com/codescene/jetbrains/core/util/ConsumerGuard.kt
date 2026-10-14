package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.contracts.ILogger
import kotlin.coroutines.cancellation.CancellationException

class ConsumerGuard(
    private val logger: ILogger,
    private val service: String,
    private val isControlFlow: (Throwable) -> Boolean = { false },
) {
    fun <T : Any> handle(
        event: T,
        block: (T) -> Unit,
    ) {
        try {
            block(event)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isControlFlow(e)) throw e
            logger.warn("${event::class.simpleName} failed: ${e.message ?: e::class.java.name}", service)
        }
    }
}
