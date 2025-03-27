package com.codescene.jetbrains.services.api

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.CodeParams
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.PreflightResponse
import com.codescene.data.ace.RefactorResponse
import com.codescene.data.ace.RefactoringOptions
import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import com.codescene.jetbrains.config.global.AceStatus
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.BaseService
import com.codescene.jetbrains.services.UIRefreshService
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionCacheEntry
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionsCacheService
import com.codescene.jetbrains.services.telemetry.TelemetryService
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.RefactoringParams
import com.codescene.jetbrains.util.TelemetryEvents
import com.codescene.jetbrains.util.handleRefactoringResult
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.*

data class RefactoredFunction(
    val name: String,
    val refactoringResult: RefactorResponse
)

@Service
class AceService : BaseService(), Disposable {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val refactoringScope = CoroutineScope(Dispatchers.IO)
    private val timeout: Long = 3_000
    private val serviceImplementation: String = this::class.java.simpleName

    companion object {
        fun getInstance(): AceService = service<AceService>()
    }

    fun getPreflightInfo(forceRefresh: Boolean = true): PreflightResponse? {
        val settings = CodeSceneGlobalSettingsStore.getInstance().state
        var preflightInfo: PreflightResponse? = null

        if (settings.enableAutoRefactor) {
            scope.launch {
                withTimeoutOrNull(timeout) {
                    try {
                        preflightInfo = runWithClassLoaderChange { ExtensionAPI.preflight(forceRefresh) }

                        Log.warn("Preflight info fetched: $preflightInfo")
                        settings.aceStatus = AceStatus.ACTIVATED
                        Log.warn("ACE status is ${CodeSceneGlobalSettingsStore.getInstance().state.aceStatus}")
                    } catch (e: Exception) {
                        Log.error("Error during preflight info fetching: ${e.message}")
                        settings.aceStatus = AceStatus.ERROR
                        Log.warn("ACE status is ${CodeSceneGlobalSettingsStore.getInstance().state.aceStatus}")
                    }
                } ?: handleTimeout()
            }
        } else {
            settings.aceStatus = AceStatus.DEACTIVATED
            Log.warn("ACE status is ${settings.aceStatus}")
        }

        return preflightInfo
    }

    private fun handleTimeout() {
        Log.warn("Preflight info fetching timed out")
        CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.ERROR
        Log.warn("ACE status is ${CodeSceneGlobalSettingsStore.getInstance().state.aceStatus}")
    }

    /**
     * Retrieves refactorable functions based on the Delta result.
     *
     * The Delta review focuses only on newly introduced code smells, meaning it may return
     * fewer refactorable functions compared to a full review.
     */
    fun getRefactorableFunctions(params: CodeParams, delta: Delta, editor: Editor) {
        Log.debug("Getting refactorable functions for ${editor.virtualFile.path} based on Delta review...")

        refactorableFunctionsHandler(editor) { ExtensionAPI.fnToRefactor(params, delta) }
    }

    /**
     * Retrieves refactorable functions based on the full Review result.
     *
     * The Review result provides all refactorable functions in a file. This is a more comprehensive analysis
     * compared to the *Delta review*.
     */
    fun getRefactorableFunctions(params: CodeParams, review: Review, editor: Editor) {
        val codeSmells = review.fileLevelCodeSmells + review.functionLevelCodeSmells.flatMap { it.codeSmells }
        Log.debug("Getting refactorable functions for ${editor.virtualFile.path} based on review with ${codeSmells}...")

        refactorableFunctionsHandler(editor) { ExtensionAPI.fnToRefactor(params, codeSmells) }
    }

    fun refactor(params: RefactoringParams, options: RefactoringOptions? = null) {
        val (project, _, function, source) = params
        Log.debug("Initiating refactor for function ${function!!.name}...")

        TelemetryService.getInstance().logUsage(
            TelemetryEvents.ACE_REFACTOR_REQUESTED,
            mutableMapOf(
                Pair("source", source),
                //TODO: Pair("traceId", ...),
                Pair("skipCache", options?.skipCache ?: false)
            )
        )

        refactoringScope.launch {
            withBackgroundProgress(project, "Refactoring ${function.name}...", cancellable = false) {
                try {
                    handleRefactoring(params, options)
                } catch (e: Exception) {
                    //TODO: error notification
                    println(e.message)
                }
            }
        }
    }

    private suspend fun handleRefactoring(params: RefactoringParams, options: RefactoringOptions? = null) {
        val function = params.function
        val startTime = System.nanoTime()

        val result = runWithClassLoaderChange(100_000) {
            if (options == null) ExtensionAPI.refactor(function)
            else ExtensionAPI.refactor(function, options)
        }

        val durationMillis = (System.nanoTime() - startTime) / 1_000_000
        Log.debug("Refactoring ${function!!.name} took ${durationMillis}ms.")

        result?.let {
            handleRefactoringResult(params, RefactoredFunction(function.name, result), durationMillis)
        }
    }

    private fun refactorableFunctionsHandler(editor: Editor, getFunctions: () -> List<FnToRefactor>) {
        val project = editor.project!!
        val path = editor.virtualFile.path
        val service = "${serviceImplementation} - ${project.name}"

        scope.launch {
            val result = runWithClassLoaderChange(100_000) { getFunctions() } ?: return@launch

            val entry = AceRefactorableFunctionCacheEntry(path, editor.document.text, result)
            AceRefactorableFunctionsCacheService.getInstance(project).put(entry)

            if (result.isNotEmpty()) {
                Log.info("Found ${result.size} refactorable function(s) in $path", service)

                val uiService = UIRefreshService.getInstance(project)
                uiService.refreshUI(editor, listOf("ACECodeVisionProvider"))
            }
        }
    }

    override fun dispose() {
        scope.cancel()
        refactoringScope.cancel()
    }
}
