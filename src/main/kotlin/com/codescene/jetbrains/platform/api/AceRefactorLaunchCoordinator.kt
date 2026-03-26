package com.codescene.jetbrains.platform.api

import com.codescene.jetbrains.core.review.AceRefactoringRunCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class AceRefactorLaunchCoordinator(
    private val scope: CoroutineScope,
    private val runCoordinator: AceRefactoringRunCoordinator,
) {
    private val lock = Any()
    private var activeJob: Job? = null

    fun startRefactor(body: suspend (Long) -> Unit) {
        synchronized(lock) {
            activeJob?.cancel()
            val gen = runCoordinator.nextGeneration()
            activeJob = scope.launch { body(gen) }
        }
    }
}
