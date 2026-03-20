package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.models.FailureType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class CodeReviewer(
    private val scope: CoroutineScope,
    private val debounceDelayMs: Long = TimeUnit.SECONDS.toMillis(3),
) {
    private val activeCalls = ConcurrentHashMap<String, Job>()

    fun reviewFile(
        filePath: String,
        timeout: Long = 60_000,
        runWithProgress: suspend (suspend () -> Unit) -> Unit,
        performAction: suspend () -> Unit,
        onError: (FailureType, String?) -> Unit,
        onScheduled: (() -> Unit)? = null,
        onFinished: (() -> Unit)? = null,
    ) {
        activeCalls[filePath]?.cancel()

        lateinit var job: Job
        job =
            scope.launch {
                try {
                    withTimeout(timeout) {
                        runWithProgress {
                            delay(debounceDelayMs)
                            performAction()
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    onError(FailureType.TIMED_OUT, e.message)
                } catch (e: CancellationException) {
                    onError(FailureType.CANCELLED, e.message)
                    throw e
                } catch (e: Exception) {
                    onError(FailureType.FAILED, e.message)
                } finally {
                    activeCalls.remove(filePath, job)
                    onFinished?.invoke()
                }
            }

        activeCalls[filePath] = job
        onScheduled?.invoke()
    }

    fun cancel(filePath: String): Boolean {
        val job = activeCalls.remove(filePath) ?: return false
        job.cancel()
        return true
    }

    fun activeFilePaths(): Set<String> = activeCalls.keys

    fun dispose() {
        activeCalls.values.forEach { it.cancel() }
        activeCalls.clear()
    }
}
