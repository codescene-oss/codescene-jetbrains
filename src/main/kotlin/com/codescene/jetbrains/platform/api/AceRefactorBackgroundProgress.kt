package com.codescene.jetbrains.platform.api

import com.codescene.data.ace.RefactoringOptions
import com.codescene.jetbrains.core.models.AceCwfParams
import com.codescene.jetbrains.core.models.RefactoringRequest
import com.codescene.jetbrains.core.review.AceRefactoringOrchestrator
import com.codescene.jetbrains.core.review.AceRefactoringResult
import com.codescene.jetbrains.core.review.AceRefactoringRunCoordinator
import com.codescene.jetbrains.platform.util.AceEntryOrchestrator
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.isActive

internal fun runAceRefactorWithBackgroundTask(
    project: Project,
    title: String,
    gen: Long,
    launchCoordinator: AceRefactorLaunchCoordinator,
    runCoordinator: AceRefactoringRunCoordinator,
    refactoringOrchestrator: AceRefactoringOrchestrator,
    editor: Editor?,
    request: RefactoringRequest,
    effectiveOptions: RefactoringOptions,
    entryOrchestrator: AceEntryOrchestrator,
    cont: CancellableContinuation<Unit>,
) {
    ProgressManager.getInstance().run(
        aceRefactorBackgroundTask(
            project = project,
            title = title,
            gen = gen,
            launchCoordinator = launchCoordinator,
            runCoordinator = runCoordinator,
            refactoringOrchestrator = refactoringOrchestrator,
            editor = editor,
            request = request,
            effectiveOptions = effectiveOptions,
            entryOrchestrator = entryOrchestrator,
            cont = cont,
        ),
    )
}

private fun aceRefactorBackgroundTask(
    project: Project,
    title: String,
    gen: Long,
    launchCoordinator: AceRefactorLaunchCoordinator,
    runCoordinator: AceRefactoringRunCoordinator,
    refactoringOrchestrator: AceRefactoringOrchestrator,
    editor: Editor?,
    request: RefactoringRequest,
    effectiveOptions: RefactoringOptions,
    entryOrchestrator: AceEntryOrchestrator,
    cont: CancellableContinuation<Unit>,
): Task.Backgroundable =
    object : Task.Backgroundable(project, title, true) {
        override fun run(indicator: ProgressIndicator) {
            launchCoordinator.attachRefactorProgress(gen, indicator)
            try {
                runAceRefactorBlocking(
                    gen = gen,
                    indicator = indicator,
                    runCoordinator = runCoordinator,
                    refactoringOrchestrator = refactoringOrchestrator,
                    project = project,
                    editor = editor,
                    request = request,
                    effectiveOptions = effectiveOptions,
                    entryOrchestrator = entryOrchestrator,
                )
            } finally {
                launchCoordinator.detachRefactorProgress(gen, indicator)
                if (cont.isActive) {
                    cont.resumeWith(Result.success(Unit))
                }
            }
        }
    }

private fun runAceRefactorBlocking(
    gen: Long,
    indicator: ProgressIndicator,
    runCoordinator: AceRefactoringRunCoordinator,
    refactoringOrchestrator: AceRefactoringOrchestrator,
    project: Project,
    editor: Editor?,
    request: RefactoringRequest,
    effectiveOptions: RefactoringOptions,
    entryOrchestrator: AceEntryOrchestrator,
) {
    try {
        val skipUsed = effectiveOptions.skipCache.orElse(request.skipCache)
        effectiveOptions.setSkipCache(skipUsed)
        val requestForRun = request.copy(skipCache = skipUsed)
        val worker =
            startAceRefactorWorker(
                refactoringOrchestrator = refactoringOrchestrator,
                requestForRun = requestForRun,
                effectiveOptions = effectiveOptions,
            )
        worker.thread.start()
        if (!awaitWorkerRespectingProgress(worker, gen, indicator, runCoordinator)) {
            return
        }

        if (!runCoordinator.isLatest(gen) || indicator.isCanceled) {
            return
        }

        worker.error.get()?.let { throw it }

        val result = worker.result.get()
        presentAceRefactorResult(result, editor, requestForRun, entryOrchestrator)
    } catch (_: ProcessCanceledException) {
        return
    } catch (e: Exception) {
        if (!runCoordinator.isLatest(gen) || indicator.isCanceled) {
            return
        }
        val requestForRun = request.copy(skipCache = effectiveOptions.skipCache.orElse(request.skipCache))
        AceEntryOrchestrator.getInstance(project).openAceErrorView(editor, requestForRun, e)
    }
}

private fun presentAceRefactorResult(
    result: AceRefactoringResult?,
    editor: Editor?,
    request: RefactoringRequest,
    entryOrchestrator: AceEntryOrchestrator,
) {
    if (result == null || editor == null) {
        return
    }
    val refactoredFunction =
        AceCwfParams(
            filePath = request.filePath,
            function = request.function,
            loading = false,
            refactorResponse = result.response,
            clientTraceId = request.traceId,
            skipCache = request.skipCache,
        )
    entryOrchestrator.queuePendingAceUpdate(refactoredFunction)
    entryOrchestrator.handleRefactoringResult(refactoredFunction, editor)
}
