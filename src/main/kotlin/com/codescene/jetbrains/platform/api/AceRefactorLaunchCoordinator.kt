package com.codescene.jetbrains.platform.api

import com.codescene.jetbrains.core.review.AceRefactoringRunCoordinator
import com.intellij.openapi.progress.ProgressIndicator
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class AceRefactorLaunchCoordinator(
    private val scope: CoroutineScope,
    private val runCoordinator: AceRefactoringRunCoordinator,
) {
    private data class RefactorProgressSlot(
        val generation: Long,
        val indicator: ProgressIndicator,
    )

    private val lock = Any()
    private var activeJob: Job? = null
    private val activeProgressSlot = AtomicReference<RefactorProgressSlot?>(null)

    fun startRefactor(body: suspend (Long) -> Unit) {
        synchronized(lock) {
            activeProgressSlot.getAndSet(null)?.indicator?.cancel()
            activeJob?.cancel()
            val gen = runCoordinator.nextGeneration()
            activeJob = scope.launch { body(gen) }
        }
    }

    fun cancelActiveRefactor() {
        synchronized(lock) {
            activeProgressSlot.getAndSet(null)?.indicator?.cancel()
            activeJob?.cancel()
            activeJob = null
            runCoordinator.nextGeneration()
        }
    }

    fun attachRefactorProgress(
        generation: Long,
        indicator: ProgressIndicator?,
    ) {
        if (indicator == null || !runCoordinator.isLatest(generation)) {
            return
        }
        activeProgressSlot.set(RefactorProgressSlot(generation, indicator))
    }

    fun detachRefactorProgress(
        generation: Long,
        indicator: ProgressIndicator?,
    ) {
        if (indicator == null) {
            return
        }
        activeProgressSlot.compareAndSet(RefactorProgressSlot(generation, indicator), null)
    }
}
