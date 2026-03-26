package com.codescene.jetbrains.platform.api

import com.codescene.data.ace.RefactoringOptions
import com.codescene.jetbrains.core.models.AceCwfParams
import com.codescene.jetbrains.core.models.RefactoringRequest
import com.codescene.jetbrains.core.review.AceRefactoringOrchestrator
import com.codescene.jetbrains.core.review.AceRefactoringResult
import com.codescene.jetbrains.core.review.AceRefactoringRunCoordinator
import com.codescene.jetbrains.platform.util.AceEntryOrchestrator
import com.codescene.jetbrains.platform.util.RefactoringParams
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive

internal suspend fun runAceRefactorJob(
    gen: Long,
    runCoordinator: AceRefactoringRunCoordinator,
    refactoringOrchestrator: AceRefactoringOrchestrator,
    params: RefactoringParams,
    effectiveOptions: RefactoringOptions,
) {
    val (project, editor, request) = params
    val entryOrchestrator = AceEntryOrchestrator.getInstance(project)
    entryOrchestrator.clearPendingAceUpdate()
    if (editor != null) {
        entryOrchestrator.openAceWindowAndAwaitBrowser(
            params =
                AceCwfParams(
                    filePath = request.filePath,
                    function = request.function,
                    loading = true,
                ),
            editor = editor,
        )
    }

    if (!coroutineContext.isActive || !runCoordinator.isLatest(gen)) {
        return
    }

    withBackgroundProgress(project, "Refactoring ${request.function.name}...", cancellable = false) {
        runAceRefactorWithProgress(
            gen = gen,
            runCoordinator = runCoordinator,
            refactoringOrchestrator = refactoringOrchestrator,
            project = project,
            editor = editor,
            request = request,
            effectiveOptions = effectiveOptions,
            entryOrchestrator = entryOrchestrator,
        )
    }
}

private suspend fun runAceRefactorWithProgress(
    gen: Long,
    runCoordinator: AceRefactoringRunCoordinator,
    refactoringOrchestrator: AceRefactoringOrchestrator,
    project: Project,
    editor: Editor?,
    request: RefactoringRequest,
    effectiveOptions: RefactoringOptions,
    entryOrchestrator: AceEntryOrchestrator,
) {
    try {
        val result =
            refactoringOrchestrator.runRefactor(
                request = request.copy(skipCache = effectiveOptions.skipCache.orElse(request.skipCache)),
                options = effectiveOptions,
            )

        if (!runCoordinator.isLatest(gen) || !coroutineContext.isActive) {
            return
        }

        presentAceRefactorResult(result, editor, request, entryOrchestrator)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (!runCoordinator.isLatest(gen) || !coroutineContext.isActive) {
            return
        }
        AceEntryOrchestrator.getInstance(project).openAceErrorView(editor, request, e)
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
        )
    entryOrchestrator.queuePendingAceUpdate(refactoredFunction)
    entryOrchestrator.handleRefactoringResult(refactoredFunction, editor)
}
