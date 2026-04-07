package com.codescene.jetbrains.core.review

import com.codescene.data.ace.RefactorResponse
import com.codescene.data.ace.RefactoringOptions
import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.models.RefactoringRequest
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.util.resolveAceFailureStatus
import com.codescene.jetbrains.core.util.resolveActivatedAceStatus

data class AceRefactoringResult(
    val response: RefactorResponse,
    val elapsedMs: Long,
)

class AceRefactoringOrchestrator(
    private val logger: ILogger,
    private val serviceName: String,
    private val executeRefactor: (RefactoringRequest, RefactoringOptions) -> TimedResult<RefactorResponse?>,
    private val getToken: () -> String,
    private val onStatusChange: (AceStatus) -> Unit,
    private val onRequested: (RefactoringRequest) -> Unit,
    private val onPerformance: (RefactoringRequest, Long) -> Unit,
) {
    fun runRefactor(
        request: RefactoringRequest,
        options: RefactoringOptions,
    ): AceRefactoringResult? {
        onRequested(request)

        return try {
            val timedResult = executeRefactor(request, options)
            val result = timedResult.result
            val elapsedMs = timedResult.elapsedMs
            onPerformance(request, elapsedMs)
            logger.debug("Refactoring ${request.function.name} took ${elapsedMs}ms.", serviceName)
            onStatusChange(resolveActivatedAceStatus(getToken()))
            result?.let { AceRefactoringResult(it, elapsedMs) }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (e: Exception) {
            if (e.hasInterruptedCause()) {
                Thread.currentThread().interrupt()
                return null
            }
            val newStatus = resolveAceFailureStatus(e)
            logger.warn("Problem occurred during ACE refactoring: ${e.message}")
            onStatusChange(newStatus)
            throw e
        }
    }
}

private fun Throwable.hasInterruptedCause(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is InterruptedException) {
            return true
        }
        current = current.cause
    }
    return false
}
