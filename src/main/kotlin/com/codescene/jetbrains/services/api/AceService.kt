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
import com.codescene.jetbrains.services.UIRefreshService
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionCacheEntry
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionsCacheService
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
    val refactoringResult: RefactorResponse,
    val fileName: String = "",
    val focusLine: Int? = null,
    var refactoringWindowType: String = ""
)

@Service
class AceService : BaseService(), Disposable {
    var lastFunctionToRefactor: FnToRefactor? = null
        private set

    private val scope = CoroutineScope(Dispatchers.IO)
    private val dispatcher = Dispatchers.IO

    private val refactoringScope = CoroutineScope(Dispatchers.IO)
    private val serviceImplementation: String = this::class.java.simpleName

    companion object {
        fun getInstance(): AceService = service<AceService>()
    }

    suspend fun runPreflight(force: Boolean = false): PreflightResponse? {
        return if (CodeSceneGlobalSettingsStore.getInstance().state.enableAutoRefactor) {
            getPreflight(force)
        } else {
            CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.DEACTIVATED
            null
        }
    }

    private suspend fun getPreflight(force: Boolean): PreflightResponse? {
        Log.debug("Getting ACE preflight data from server...", serviceImplementation)

        return withContext(dispatcher) {
            var preflight: PreflightResponse? = null
            try {
                preflight = runWithClassLoaderChange {
                    ExtensionAPI.preflight(force)
                }
                Log.info("Preflight info fetched from the server", serviceImplementation)

            } catch (e: Exception) {
                if (e.message == "Operation timed out") {
                    Log.warn("Preflight info fetching timed out", serviceImplementation)
                } else {
                    Log.warn("Error during preflight info fetching: ${e.message}", serviceImplementation)
                }
            }
            if (force) {
                setAceStatus(preflight)
            }

            preflight
        }
    }

    /**
     * Retrieves refactorable functions based on the Delta result.
     *
     * The Delta review focuses only on newly introduced code smells, meaning it may return
     * fewer refactorable functions compared to a full review.
     */
    fun getRefactorableFunctions(params: CodeParams, delta: Delta, editor: Editor) {
        Log.debug(
            "Getting refactorable functions for ${editor.virtualFile.path} based on Delta review...",
            serviceImplementation
        )

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
        Log.debug(
            "Getting refactorable functions for ${editor.virtualFile.path} based on review with ${codeSmells}...",
            serviceImplementation
        )

        refactorableFunctionsHandler(editor) { ExtensionAPI.fnToRefactor(params, codeSmells) }
    }

    fun refactor(params: RefactoringParams, options: RefactoringOptions? = null) {
        val (project, _, function, source) = params
        lastFunctionToRefactor = function
        Log.debug("Initiating refactor for function ${function!!.name}...", serviceImplementation)

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

    private fun handleRefactoring(params: RefactoringParams, options: RefactoringOptions? = null) {
        val function = params.function
        val startTime = System.nanoTime()

        val result = runWithClassLoaderChange {
            if (options == null) ExtensionAPI.refactor(function)
            else ExtensionAPI.refactor(function, options)
        }

        val durationMillis = (System.nanoTime() - startTime) / 1_000_000
        Log.debug("Refactoring ${function!!.name} took ${durationMillis}ms.", serviceImplementation)

        result?.let {
            handleRefactoringResult(params, RefactoredFunction(
                function.name,
                result,
                params.editor?.virtualFile?.name ?: "",
                params.function.range.startLine
            ), durationMillis)
        }
    }

    private fun refactorableFunctionsHandler(editor: Editor, getFunctions: () -> List<FnToRefactor>) {
        val project = editor.project!!
        val path = editor.virtualFile.path

        scope.launch {
            val result = runWithClassLoaderChange { getFunctions() }

            val entry = AceRefactorableFunctionCacheEntry(path, editor.document.text, result)
            AceRefactorableFunctionsCacheService.getInstance(project).put(entry)

            if (result.isNotEmpty()) {
                Log.info(
                    "Found ${result.size} refactorable function(s) in $path",
                    "${serviceImplementation} - ${project.name}"
                )

                val uiService = UIRefreshService.getInstance(project)
                uiService.refreshUI(editor, listOf("ACECodeVisionProvider"))
            }
        }
    }

    private fun setAceStatus(preflightInfo: PreflightResponse?) {
        if (preflightInfo == null) {
            CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.ERROR
        } else {
            CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.ACTIVATED
        }
    }

    override fun dispose() {
        scope.cancel()
        refactoringScope.cancel()
    }
}
