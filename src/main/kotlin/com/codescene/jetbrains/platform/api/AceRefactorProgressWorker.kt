package com.codescene.jetbrains.platform.api

import com.codescene.data.ace.RefactoringOptions
import com.codescene.jetbrains.core.models.RefactoringRequest
import com.codescene.jetbrains.core.review.AceRefactoringOrchestrator
import com.codescene.jetbrains.core.review.AceRefactoringResult
import com.codescene.jetbrains.core.review.AceRefactoringRunCoordinator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import java.util.concurrent.atomic.AtomicReference

internal data class AceRefactorWorker(
    val thread: Thread,
    val result: AtomicReference<AceRefactoringResult?>,
    val error: AtomicReference<Throwable?>,
)

internal fun startAceRefactorWorker(
    refactoringOrchestrator: AceRefactoringOrchestrator,
    requestForRun: RefactoringRequest,
    effectiveOptions: RefactoringOptions,
): AceRefactorWorker {
    val result = AtomicReference<AceRefactoringResult?>()
    val error = AtomicReference<Throwable?>()
    val thread =
        Thread(
            {
                try {
                    result.set(
                        refactoringOrchestrator.runRefactor(
                            request = requestForRun,
                            options = effectiveOptions,
                        ),
                    )
                } catch (t: Throwable) {
                    error.set(t)
                }
            },
            "CodeScene-ACE-Refactor",
        )
    return AceRefactorWorker(thread, result, error)
}

internal fun awaitWorkerRespectingProgress(
    worker: AceRefactorWorker,
    gen: Long,
    indicator: ProgressIndicator,
    runCoordinator: AceRefactoringRunCoordinator,
): Boolean {
    try {
        while (worker.thread.isAlive) {
            if (!runCoordinator.isLatest(gen)) {
                worker.thread.interrupt()
                return false
            }
            indicator.checkCanceled()
            worker.thread.join(100)
        }
    } catch (_: ProcessCanceledException) {
        worker.thread.interrupt()
        return false
    }
    return true
}
