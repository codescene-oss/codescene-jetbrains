package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.models.FailureType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val LOG_TAG = "CodeReviewer"

class CodeReviewer(
    private val scope: CoroutineScope,
    private val logger: ILogger,
    private val defaultDebounceDelayMs: Long = TimeUnit.SECONDS.toMillis(3),
) {
    private val activeCalls = ConcurrentHashMap<String, Job>()

    fun reviewFile(
        filePath: String,
        timeout: Long = 60_000,
        debounceDelayMs: Long? = null,
        runWithProgress: suspend (suspend () -> Unit) -> Unit,
        performAction: suspend () -> Unit,
        onError: (FailureType, String?) -> Unit,
        onScheduled: (() -> Unit)? = null,
        onFinished: (() -> Unit)? = null,
    ) {
        val delayBeforeRun = debounceDelayMs ?: defaultDebounceDelayMs
        val shortPath = filePath.substringAfterLast('/')

        val existingJob = activeCalls[filePath]
        if (existingJob != null) {
            logger.info("Cancelling existing job file=$shortPath", LOG_TAG)
            existingJob.cancel()
        }

        lateinit var job: Job
        job =
            scope.launch(start = CoroutineStart.LAZY) {
                try {
                    withTimeout(timeout) {
                        runWithProgress {
                            delay(delayBeforeRun)
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
                    val removed = activeCalls.remove(filePath, job)
                    if (removed) {
                        logger.info("Job removed file=$shortPath active=${activeCalls.size}", LOG_TAG)
                        onFinished?.invoke()
                    } else {
                        logger.warn("Job removal failed (replaced?) file=$shortPath", LOG_TAG)
                    }
                }
            }

        logger.info("Job added file=$shortPath active=${activeCalls.size + 1}", LOG_TAG)
        activeCalls[filePath] = job
        onScheduled?.invoke()
        job.start()
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
