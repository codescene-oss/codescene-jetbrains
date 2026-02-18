package com.codescene.jetbrains.services.api

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.CodeParams
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.PreflightResponse
import com.codescene.data.ace.RefactorResponse
import com.codescene.data.ace.RefactoringOptions
import com.codescene.data.review.Review
import com.codescene.jetbrains.components.webview.util.AceCwfParams
import com.codescene.jetbrains.components.webview.util.updateMonitor
import com.codescene.jetbrains.config.global.AceStatus
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.flag.RuntimeFlags
import com.codescene.jetbrains.services.UIRefreshService
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionCacheEntry
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionsCacheService
import com.codescene.jetbrains.util.Constants.ACE
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.RefactoringParams
import com.codescene.jetbrains.util.TelemetryEvents
import com.codescene.jetbrains.util.getActivatedAceStatus
import com.codescene.jetbrains.util.handleAceStatusChange
import com.codescene.jetbrains.util.handleRefactoringResult
import com.codescene.jetbrains.util.openAceErrorView
import com.codescene.jetbrains.util.updateCurrentAceView
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RefactoredFunction(
    val name: String,
    val refactoringResult: RefactorResponse,
    val fileName: String = "",
    val startLine: Int? = null,
    val endLine: Int? = null,
    val startColumn: Int? = null,
    val endColumn: Int? = null,
    var refactoringWindowType: String = "",
)

@Service
class AceService :
    BaseService(),
    Disposable {
    private val refactoringScope = CoroutineScope(Dispatchers.IO)
    private val refactorableFunctionsScope = CoroutineScope(Dispatchers.IO)
    private val serviceImplementation: String = this::class.java.simpleName

    companion object {
        fun getInstance(): AceService = service<AceService>()
    }

    suspend fun runPreflight(force: Boolean = false) =
        if (RuntimeFlags.aceFeature && CodeSceneGlobalSettingsStore.getInstance().state.enableAutoRefactor) {
            getPreflight(force)
        } else {
            CodeSceneGlobalSettingsStore.getInstance().state.aceStatus = AceStatus.DEACTIVATED
            null
        }

    private suspend fun getPreflight(force: Boolean): PreflightResponse? {
        Log.debug("Getting ACE preflight data from server...", serviceImplementation)

        return withContext(Dispatchers.IO) {
            try {
                val (result, elapsedMs) = runWithClassLoaderChange { ExtensionAPI.preflight(force) }
                Log.info(
                    "Preflight info fetched from the server in ${elapsedMs}ms. Cache bypassed: $force",
                    serviceImplementation,
                )

                if (force) handleAceStatusChange(getActivatedAceStatus())
                result
            } catch (e: Exception) {
                val newStatus =
                    when {
                        e is java.net.ConnectException || e is java.net.http.HttpTimeoutException -> AceStatus.OFFLINE
                        else -> AceStatus.ERROR
                    }
                handleAceStatusChange(newStatus)

                if (newStatus == AceStatus.OFFLINE) {
                    Log.warn(
                        "Preflight request timed out or connection failed. Error message: ${e.message}",
                        serviceImplementation,
                    )
                } else {
                    Log.error(
                        "Error during preflight info fetching. Error message: ${e.message}",
                        serviceImplementation,
                    )
                }
                null
            }
        }
    }

    /**
     * Retrieves refactorable functions based on the full Review result.
     *
     * The Review result provides all refactorable functions in a file. This is a more comprehensive analysis
     * compared to the *Delta review*.
     */
    fun getRefactorableFunctions(
        params: CodeParams,
        review: Review,
        editor: Editor,
    ) {
        val codeSmells = review.fileLevelCodeSmells + review.functionLevelCodeSmells.flatMap { it.codeSmells }
        Log.debug(
            "Getting refactorable functions for ${editor.virtualFile.path} based on review with $codeSmells...",
            serviceImplementation,
        )

        refactorableFunctionsHandler(editor) { ExtensionAPI.fnToRefactor(params, codeSmells) }
    }

    fun refactor(
        params: RefactoringParams,
        options: RefactoringOptions? = null,
    ) {
        val (project, _, function, source) = params
        Log.debug(
            "Initiating refactor for function ${function!!.name}, " +
                "with refactoring targets: ${function.refactoringTargets}...",
            serviceImplementation,
        )

        TelemetryService.getInstance().logUsage(
            TelemetryEvents.ACE_REFACTOR_REQUESTED,
            mutableMapOf(
                Pair("source", source),
                // TODO: Pair("traceId", ...),
                Pair("skipCache", options?.skipCache ?: false),
            ),
        )

        refactoringScope.launch {
            withBackgroundProgress(project, "Refactoring ${function.name}...", cancellable = false) {
                try {
                    handleRefactoring(params, options)

                    handleAceStatusChange(getActivatedAceStatus())
                } catch (e: Exception) {
                    val newStatus = if (e is java.net.http.HttpTimeoutException) AceStatus.OFFLINE else AceStatus.ERROR

                    Log.warn("Problem occurred during ACE refactoring: ${e.message}")
                    handleAceStatusChange(newStatus)

                    openAceErrorView(params.editor, params.function, project, e)
                }
            }
        }
    }

    private fun handleRefactoring(
        params: RefactoringParams,
        options: RefactoringOptions? = null,
    ) {
        val function = params.function

        val (result, elapsedMs) =
            runWithClassLoaderChange {
                if (options == null) {
                    ExtensionAPI.refactor(function)
                } else {
                    ExtensionAPI.refactor(function, options)
                }
            }

        TelemetryService.getInstance().logUsage(
            TelemetryEvents.ANALYSIS_PERFORMANCE,
            mutableMapOf(
                Pair("type", ACE),
                Pair("elapsedMs", elapsedMs),
                Pair(
                    "loc",
                    params.function
                        ?.body
                        ?.lines()
                        ?.size ?: 0,
                ),
                Pair("language", params.editor?.virtualFile?.extension ?: ""),
            ),
        )
        Log.debug("Refactoring ${function!!.name} took ${elapsedMs}ms.", serviceImplementation)

        result?.let {
            val refactoredFunction =
                AceCwfParams(
                    filePath = params.editor!!.virtualFile.path,
                    function = params.function,
                    refactorResponse = result,
                )

            handleRefactoringResult(refactoredFunction, elapsedMs, params.editor)
        }
    }

    private fun refactorableFunctionsHandler(
        editor: Editor,
        getFunctions: () -> List<FnToRefactor>,
    ) {
        val project = editor.project!!
        val path = editor.virtualFile.path

        refactorableFunctionsScope.launch {
            val (result, elapsedMs) = runWithClassLoaderChange { getFunctions() }

            val entry = AceRefactorableFunctionCacheEntry(path, editor.document.text, result)

            AceRefactorableFunctionsCacheService.getInstance(project).put(entry)
            updateCurrentAceView(project, entry)

            if (result.isNotEmpty()) {
                Log.info(
                    "Found ${result.size} refactorable function(s) in file '$path' in ${elapsedMs}ms.",
                    "$serviceImplementation - ${project.name}",
                )

                val uiService = UIRefreshService.getInstance(project)
                uiService.refreshUI(editor, listOf("ACECodeVisionProvider"))

                updateMonitor(project)
            } else {
                Log.info("No refactorable functions have been found for file $path.")
            }
        }
    }

    override fun dispose() {
        refactorableFunctionsScope.cancel()
        refactoringScope.cancel()
    }
}
