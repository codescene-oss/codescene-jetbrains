package com.codescene.jetbrains.platform.api

import com.codescene.data.ace.RefactoringOptions
import com.codescene.jetbrains.core.models.AceCwfParams
import com.codescene.jetbrains.core.review.AceRefactoringOrchestrator
import com.codescene.jetbrains.core.review.AceRefactoringRunCoordinator
import com.codescene.jetbrains.platform.util.AceEntryOrchestrator
import com.codescene.jetbrains.platform.util.RefactoringParams
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine

internal suspend fun runAceRefactorJob(
    gen: Long,
    launchCoordinator: AceRefactorLaunchCoordinator,
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

    val title = "Refactoring ${request.function.name}..."
    suspendCancellableCoroutine<Unit> { cont ->
        runAceRefactorWithBackgroundTask(
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
        )
    }
}
