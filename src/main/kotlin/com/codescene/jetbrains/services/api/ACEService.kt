package com.codescene.jetbrains.services.api

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.CodeParams
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.PreflightResponse
import com.codescene.data.ace.RefactoringOptions
import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import com.codescene.jetbrains.config.global.AceStatus
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.BaseService
import com.codescene.jetbrains.services.UIRefreshService
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionCacheEntry
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionsCacheService
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import kotlinx.coroutines.*

@Service
class AceService : BaseService(), Disposable {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val timeout: Long = 15_000
    private val settings = CodeSceneGlobalSettingsStore.getInstance().state
    private var status: AceStatus = settings.aceStatus
    private val serviceImplementation: String = this::class.java.simpleName

    companion object {
        fun getInstance(): AceService = service<AceService>()
    }

    fun getPreflightInfo(forceRefresh: Boolean = false): PreflightResponse? {
        val settings = CodeSceneGlobalSettingsStore.getInstance().state
        var preflightInfo: PreflightResponse? = null

        if (settings.enableAutoRefactor) {

            try {
                runWithClassLoaderChange {
                    preflightInfo = ExtensionAPI.preflight(forceRefresh)
                }
                //todo: change to debug after implementation done
                Log.warn("Preflight info fetched: $preflightInfo")
                settings.aceStatus = AceStatus.ACTIVATED
                Log.warn("ACE status is $status")
            } catch (e: TimeoutCancellationException) {
                Log.warn("Preflight info fetching timed out")
                settings.aceStatus = AceStatus.ERROR
                Log.warn("ACE status is $status")
            } catch (e: Exception) {
                Log.error("Error during preflight info fetching: ${e.message}")
                settings.aceStatus = AceStatus.ERROR
                Log.warn("ACE status is $status")
            }
        } else {
            settings.aceStatus = AceStatus.DEACTIVATED
            Log.warn("ACE status is $status")
        }

        return preflightInfo
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

    //WIP
    fun refactor(function: FnToRefactor, options: RefactoringOptions? = null) {
        Log.debug("Initiating refactor for function ${function.name}...")

        scope.launch {
            val result = runWithClassLoaderChange {
                if (options == null)
                    ExtensionAPI.refactor(function) else ExtensionAPI.refactor(function, options)
            }

            println("Refactoring result: $result")

            //TODO: After refactoring, open the panel with the results
        }
    }

    private fun refactorableFunctionsHandler(editor: Editor, getFunctions: () -> List<FnToRefactor>) {
        val project = editor.project!!
        val path = editor.virtualFile.path
        val service = "${serviceImplementation} - ${project.name}"

        scope.launch {
            val result = runWithClassLoaderChange { getFunctions() }

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
    }
}
