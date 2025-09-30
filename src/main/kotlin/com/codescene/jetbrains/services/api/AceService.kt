package com.codescene.jetbrains.services.api

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.CodeParams
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.PreflightResponse
import com.codescene.data.ace.RefactorResponse
import com.codescene.data.ace.RefactoringOptions
import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.webview.util.AceCwfParams
import com.codescene.jetbrains.components.webview.util.openAceWindow
import com.codescene.jetbrains.components.webview.util.updateMonitor
import com.codescene.jetbrains.config.global.AceStatus
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.UIRefreshService
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionCacheEntry
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionsCacheService
import com.codescene.jetbrains.util.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.*

data class RefactoredFunction(
    val name: String,
    val refactoringResult: RefactorResponse,
    val fileName: String = "",
    val startLine: Int? = null,
    val endLine: Int? = null,
    val startColumn: Int? = null,
    val endColumn: Int? = null,
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

                val settings = CodeSceneGlobalSettingsStore.getInstance().state
                handleStatusChange(
                    settings.aceStatus == AceStatus.OFFLINE,
                    AceStatus.ACTIVATED,
                    UiLabelsBundle.message("backOnline")
                )
            } catch (e: Exception) {
                val settings = CodeSceneGlobalSettingsStore.getInstance().state
                val timedOut = e is java.io.IOException && e.message == "Operation timed out"
                val offline = e is java.net.ConnectException

                if (timedOut || offline) {
                    Log.warn("Preflight info fetching timed out", serviceImplementation)
                    handleStatusChange(
                        settings.aceStatus != AceStatus.OFFLINE,
                        AceStatus.OFFLINE,
                        UiLabelsBundle.message("offlineMode")
                    )
                } else {
                    Log.warn("Error during preflight info fetching. Error message: ${e.message}", serviceImplementation)
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

                    handleStatusChange(
                        CodeSceneGlobalSettingsStore.getInstance().state.aceStatus == AceStatus.OFFLINE,
                        AceStatus.ACTIVATED,
                        UiLabelsBundle.message("backOnline")
                    )
                } catch (e: Exception) {
                    Log.warn("Problem occurred during ACE refactoring: ${e.message}")

                    if (e is java.io.IOException && e.message == "Operation timed out")
                        handleStatusChange(
                            CodeSceneGlobalSettingsStore.getInstance().state.aceStatus != AceStatus.OFFLINE,
                            AceStatus.OFFLINE,
                            UiLabelsBundle.message("offlineMode")
                        )

                    openAceErrorView(params.editor, params.function, project)
                }
            }
        }
    }

    private fun openAceErrorView(editor: Editor?, function: FnToRefactor?, project: Project) {
        if (function != null && editor != null)
            openAceWindow(
                AceCwfParams(
                    error = true,
                    function = function,
                    filePath = editor.virtualFile.path
                ), project
            )
    }

    private fun handleStatusChange(
        shouldNotify: Boolean,
        newStatus: AceStatus,
        message: String
    ) {
        val settings = CodeSceneGlobalSettingsStore.getInstance().state

        if (shouldNotify) {
            settings.aceStatus = newStatus

            Log.info(message)
            ProjectManager.getInstance().openProjects.forEach { project ->
                showInfoNotification(message, project)
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
            val refactoredFunction = AceCwfParams(
                filePath = params.editor!!.virtualFile.path,
                function = params.function,
                refactorResponse = result,
            )

            handleRefactoringResult(refactoredFunction, durationMillis, params.editor)
        }
    }

    private fun refactorableFunctionsHandler(editor: Editor, getFunctions: () -> List<FnToRefactor>) {
        val project = editor.project!!
        val path = editor.virtualFile.path

        scope.launch {
            val result = runWithClassLoaderChange { getFunctions() }

            val entry = AceRefactorableFunctionCacheEntry(path, editor.document.text, result)

            AceRefactorableFunctionsCacheService.getInstance(project).put(entry)
            updateCurrentAceView(project, entry)

            if (result.isNotEmpty()) {
                Log.info(
                    "Found ${result.size} refactorable function(s) in $path",
                    "${serviceImplementation} - ${project.name}"
                )

                val uiService = UIRefreshService.getInstance(project)
                uiService.refreshUI(editor, listOf("ACECodeVisionProvider"))

                updateMonitor(project)
            }
        }
    }

    private fun setAceStatus(preflightInfo: PreflightResponse?) {
        if (preflightInfo == null && CodeSceneGlobalSettingsStore.getInstance().state.aceStatus == AceStatus.OFFLINE) return
        else if (preflightInfo == null) {
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
