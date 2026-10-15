package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.contracts.ILogger

data class TimedResult<T>(val result: T, val elapsedMs: Long)

open class BaseService(
    protected val log: ILogger,
) {
    private val serviceImplementation = this::class.java.simpleName

    protected fun <T> runTimed(action: () -> T): TimedResult<T> {
        return try {
            val startTime = System.currentTimeMillis()
            val result = action()
            val elapsedTime = System.currentTimeMillis() - startTime
            log.info("Received response from CodeScene API in ${elapsedTime}ms", serviceImplementation)
            TimedResult(result, elapsedTime)
        } catch (e: Exception) {
            log.debug("Exception during CodeScene API operation. Error message: ${e.message}", serviceImplementation)
            throw (e)
        }
    }
}
