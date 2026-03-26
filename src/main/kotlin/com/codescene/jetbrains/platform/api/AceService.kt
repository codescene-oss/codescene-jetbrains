package com.codescene.jetbrains.platform.api

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.CacheParams
import com.codescene.ExtensionAPI.CodeParams
import com.codescene.data.ace.PreflightResponse
import com.codescene.data.ace.RefactoringOptions
import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import com.codescene.jetbrains.core.contracts.IAceService
import com.codescene.jetbrains.core.review.AcePreflightOrchestrator
import com.codescene.jetbrains.core.review.AceRefactorableFunctionCacheEntry
import com.codescene.jetbrains.core.review.AceRefactoringOrchestrator
import com.codescene.jetbrains.core.review.AceRefactoringRunCoordinator
import com.codescene.jetbrains.core.review.BaseService
import com.codescene.jetbrains.core.review.RefactorableFunctionsOrchestrator
import com.codescene.jetbrains.core.util.Constants.ACE
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.util.AceEntryOrchestrator
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.RefactoringParams
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

@Service
class AceService :
    BaseService(Log),
    IAceService,
    Disposable {
    private val appServiceProvider = CodeSceneApplicationServiceProvider.getInstance()
    private val settingsProvider = appServiceProvider.settingsProvider
    private val telemetryService = appServiceProvider.telemetryService
    private val refactoringScope = CoroutineScope(Dispatchers.IO)
    private val refactorRunCoordinator = AceRefactoringRunCoordinator()
    private val refactorLaunchCoordinator = AceRefactorLaunchCoordinator(refactoringScope, refactorRunCoordinator)
    private val serviceImplementation: String = this::class.java.simpleName
    private val preflightOrchestrator: AcePreflightOrchestrator by lazy { buildPreflightOrchestrator() }
    private val refactoringOrchestrator: AceRefactoringOrchestrator by lazy { buildRefactoringOrchestrator() }

    companion object {
        fun getInstance(): AceService = service<AceService>()
    }

    override suspend fun runPreflight(force: Boolean): PreflightResponse? =
        preflightOrchestrator.runPreflight(force = force)

    /**
     * Retrieves refactorable functions based on the full Review result.
     *
     * The Review result provides all refactorable functions in a file. This is a more comprehensive analysis
     * compared to the *Delta review*.
     */
    suspend fun getRefactorableFunctions(
        params: CodeParams,
        cacheParams: CacheParams,
        review: Review,
        editor: Editor,
    ): Boolean {
        val codeSmells = review.fileLevelCodeSmells + review.functionLevelCodeSmells.flatMap { it.codeSmells }
        Log.debug(
            "Getting refactorable functions for ${editor.virtualFile.path} based on review with $codeSmells...",
            serviceImplementation,
        )

        return refactorableFunctionsHandler(editor) { ExtensionAPI.fnToRefactor(params, cacheParams, codeSmells) }
    }

    suspend fun getRefactorableFunctions(
        params: CodeParams,
        cacheParams: CacheParams,
        delta: Delta,
        editor: Editor,
    ): Boolean {
        Log.debug(
            "Getting refactorable functions for ${editor.virtualFile.path} based on delta...",
            serviceImplementation,
        )
        return refactorableFunctionsHandler(editor) { ExtensionAPI.fnToRefactor(params, cacheParams, delta) }
    }

    fun refactor(
        params: RefactoringParams,
        options: RefactoringOptions? = null,
    ) {
        val (project, editor, request) = params
        val effectiveOptions =
            options ?: RefactoringOptions().apply {
                setToken(settingsProvider.currentState().aceAuthToken)
                setSkipCache(request.skipCache)
            }
        Log.debug(
            "Initiating refactor for function ${request.function.name}, " +
                "with refactoring targets: ${request.function.refactoringTargets}...",
            serviceImplementation,
        )

        refactorLaunchCoordinator.startRefactor { gen ->
            runAceRefactorJob(
                gen = gen,
                launchCoordinator = refactorLaunchCoordinator,
                runCoordinator = refactorRunCoordinator,
                refactoringOrchestrator = refactoringOrchestrator,
                params = params,
                effectiveOptions = effectiveOptions,
            )
        }
    }

    private suspend fun refactorableFunctionsHandler(
        editor: Editor,
        getFunctions: () -> List<com.codescene.data.ace.FnToRefactor>,
    ): Boolean {
        val project = editor.project!!
        val path = editor.virtualFile.path
        val projectServiceProvider = CodeSceneProjectServiceProvider.getInstance(project)
        val orchestrator =
            RefactorableFunctionsOrchestrator(
                logger = Log,
                cache = projectServiceProvider.aceRefactorableFunctionsCache,
            )

        val result =
            orchestrator.fetchAndCache(
                filePath = path,
                content = editor.document.text,
                serviceName = "$serviceImplementation - ${project.name}",
                getFunctions = { runWithClassLoaderChange { getFunctions() } },
            )

        val entry = AceRefactorableFunctionCacheEntry(result.filePath, result.content, result.functions)
        AceEntryOrchestrator.getInstance(project).updateCurrentAceView(entry)
        return result.functions.isNotEmpty()
    }

    override fun dispose() {
        refactoringScope.cancel()
    }

    private fun buildPreflightOrchestrator(): AcePreflightOrchestrator =
        AcePreflightOrchestrator(
            settingsProvider = settingsProvider,
            logger = Log,
            serviceName = serviceImplementation,
            fetchPreflight = { bypassCache ->
                withContext(Dispatchers.IO) {
                    runWithClassLoaderChange { ExtensionAPI.preflight(bypassCache) }
                }
            },
            onStatusChange = { status -> AceEntryOrchestrator.handleAceStatusChange(status) },
        )

    private fun buildRefactoringOrchestrator(): AceRefactoringOrchestrator =
        AceRefactoringOrchestrator(
            logger = Log,
            serviceName = serviceImplementation,
            executeRefactor = { request, options ->
                runWithClassLoaderChange {
                    ExtensionAPI.refactor(request.function, options)
                }
            },
            getToken = { settingsProvider.currentState().aceAuthToken },
            onStatusChange = { status -> AceEntryOrchestrator.handleAceStatusChange(status) },
            onRequested = { request ->
                telemetryService.logUsage(
                    TelemetryEvents.ACE_REFACTOR_REQUESTED,
                    mutableMapOf(
                        Pair("source", request.source),
                        Pair("skipCache", request.skipCache),
                    ),
                )
            },
            onPerformance = { request, elapsedMs ->
                telemetryService.logUsage(
                    TelemetryEvents.ANALYSIS_PERFORMANCE,
                    mutableMapOf(
                        Pair("type", ACE),
                        Pair("elapsedMs", elapsedMs),
                        Pair("loc", request.function.body.lines().size),
                        Pair("language", request.language ?: ""),
                    ),
                )
            },
        )
}
