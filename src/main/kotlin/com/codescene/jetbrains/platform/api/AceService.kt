package com.codescene.jetbrains.platform.api

import com.codescene.data.ace.PreflightResponse
import com.codescene.data.ace.RefactoringOptions
import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import com.codescene.jetbrains.core.cli.AceFileParams
import com.codescene.jetbrains.core.cli.CliCacheParams
import com.codescene.jetbrains.core.cli.FnsToRefactorRequest
import com.codescene.jetbrains.core.contracts.IAceService
import com.codescene.jetbrains.core.git.pathForLog
import com.codescene.jetbrains.core.review.AcePreflightOrchestrator
import com.codescene.jetbrains.core.review.AceRefactorableFunctionCacheEntry
import com.codescene.jetbrains.core.review.AceRefactoringOrchestrator
import com.codescene.jetbrains.core.review.AceRefactoringRunCoordinator
import com.codescene.jetbrains.core.review.BaseService
import com.codescene.jetbrains.core.review.RefactorableFunctionsOrchestrator
import com.codescene.jetbrains.core.util.Constants.ACE
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.core.util.normalizeAbsolutePath
import com.codescene.jetbrains.platform.cli.CsIdeServerService
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.telemetry.StatsCollectorService
import com.codescene.jetbrains.platform.util.AceEntryOrchestrator
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.RefactoringParams
import com.codescene.jetbrains.platform.webview.util.updateMonitor
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

internal sealed class RefactorableFunctionsSource {
    abstract fun analysisLabel(): String

    open fun reviewSmells(): List<com.codescene.data.review.CodeSmell>? = null

    open fun reviewDelta(): Delta? = null

    fun fetch(
        params: AceFileParams,
        cacheParams: CliCacheParams,
    ): List<com.codescene.data.ace.FnToRefactor> =
        CsIdeServerService.getInstance().client().fnsToRefactor(
            FnsToRefactorRequest(
                fileName = params.fileName,
                fileContent = params.content,
                cachePath = cacheParams.path,
                codeSmells = reviewSmells(),
                delta = reviewDelta(),
            ),
        )

    data class FromReview(
        val review: Review,
    ) : RefactorableFunctionsSource() {
        private val codeSmells by lazy {
            review.fileLevelCodeSmells + review.functionLevelCodeSmells.flatMap { it.codeSmells }
        }

        override fun analysisLabel(): String = "review with ${codeSmells.size} code smell(s)"

        override fun reviewSmells(): List<com.codescene.data.review.CodeSmell> = codeSmells
    }

    data class FromDelta(
        val delta: Delta,
    ) : RefactorableFunctionsSource() {
        override fun analysisLabel(): String = "delta"

        override fun reviewDelta(): Delta = delta
    }
}

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
     * Retrieves refactorable functions from a [RefactorableFunctionsSource].
     *
     * Use [RefactorableFunctionsSource.FromReview] for full review results (more comprehensive than delta).
     * Use [RefactorableFunctionsSource.FromDelta] for delta-based analysis.
     */
    internal suspend fun getRefactorableFunctions(
        project: Project,
        filePath: String,
        currentCode: String,
        params: AceFileParams,
        cacheParams: CliCacheParams,
        source: RefactorableFunctionsSource,
    ): Boolean {
        Log.debug(
            "Getting refactorable functions for ${pathForLog(filePath)} based on ${source.analysisLabel()}...",
            serviceImplementation,
        )
        return refactorableFunctionsHandler(project, filePath, currentCode) {
            source.fetch(params, cacheParams)
        }
    }

    fun refactor(
        params: RefactoringParams,
        options: RefactoringOptions? = null,
    ) {
        val (project, editor, request) = params
        val effectiveOptions =
            options ?: RefactoringOptions().apply {
                setToken(settingsProvider.getAceAuthToken())
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

    fun cancelActiveRefactor() {
        refactorLaunchCoordinator.cancelActiveRefactor()
    }

    private suspend fun refactorableFunctionsHandler(
        project: Project,
        filePath: String,
        content: String,
        getFunctions: () -> List<com.codescene.data.ace.FnToRefactor>,
    ): Boolean {
        val path = normalizeAbsolutePath(filePath)
        val projectServiceProvider = CodeSceneProjectServiceProvider.getInstance(project)
        val orchestrator =
            RefactorableFunctionsOrchestrator(
                logger = Log,
                cache = projectServiceProvider.aceRefactorableFunctionsCache,
            )

        val result =
            orchestrator.fetchAndCache(
                filePath = path,
                content = content,
                serviceName = "$serviceImplementation - ${project.name}",
                getFunctions = { timed { getFunctions() } },
            )

        val entry = AceRefactorableFunctionCacheEntry(result.filePath, result.content, result.functions)
        AceEntryOrchestrator.getInstance(project).updateCurrentAceView(entry)
        updateMonitor(project)
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
                    timed {
                        com.codescene.jetbrains.platform.cli.CsIdeServerService.getInstance().client()
                            .preflight(bypassCache)
                    }
                }
            },
            onStatusChange = { status -> AceEntryOrchestrator.handleAceStatusChange(status) },
        )

    private fun buildRefactoringOrchestrator(): AceRefactoringOrchestrator =
        AceRefactoringOrchestrator(
            logger = Log,
            serviceName = serviceImplementation,
            executeRefactor = { request, options ->
                timed {
                    com.codescene.jetbrains.platform.cli.CsIdeServerService.getInstance().client()
                        .refactor(request.function, options)
                }
            },
            getToken = { settingsProvider.getAceAuthToken() },
            onStatusChange = { status -> AceEntryOrchestrator.handleAceStatusChange(status) },
            onRequested = { request ->
                telemetryService.logUsage(
                    TelemetryEvents.ACE_REFACTOR_REQUESTED,
                    mapOf(
                        "source" to request.source.value,
                        "skipCache" to request.skipCache,
                        "traceId" to request.traceId,
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
                StatsCollectorService.getInstance().recordAnalysis(
                    request.filePath.substringAfterLast('/', request.filePath),
                    elapsedMs.toDouble(),
                )
            },
        )
}
