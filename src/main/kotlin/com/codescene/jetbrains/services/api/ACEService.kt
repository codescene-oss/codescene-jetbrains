package com.codescene.jetbrains.services.api

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.CodeParams
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.PreflightResponse
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

    fun getPreflightInfo(): AceStatus {
        var preflightInfo: PreflightResponse? = null

        if (settings.enableAutoRefactor) {

            try {
                runWithClassLoaderChange {
                    preflightInfo = ExtensionAPI.preflight(true)
                }
                //todo: change to debug after implementation done
                Log.warn("Preflight info fetched: $preflightInfo")
                status = AceStatus.ACTIVATED
                Log.warn("ACE status is $status")
            } catch (e: TimeoutCancellationException) {
                Log.warn("Preflight info fetching timed out")
                status = AceStatus.ERROR
                Log.warn("ACE status is $status")
            } catch (e: Exception) {
                Log.error("Error during preflight info fetching: ${e.message}")
                status = AceStatus.ERROR
                Log.warn("ACE status is $status")
            }
        } else {
            status = AceStatus.DEACTIVATED
            Log.warn("ACE status is $status")
        }
        return status
    }

    fun getStatus(): AceStatus {
        return status
    }

    fun setStatus(newStatus: AceStatus) {
        status = newStatus
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

    private fun refactorableFunctionsHandler(editor: Editor, getFunctions: () -> List<FnToRefactor>) {
        val project = editor.project!!
        val path = editor.virtualFile.path
        val service = "${serviceImplementation} - ${project.name}"

        //TODO: check if language is supported in ACE before making call.
        if (status != AceStatus.ACTIVATED) {
            Log.warn("ACE is not ready to process the request. Current status: ${status.name}", service)
            return
        }

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
