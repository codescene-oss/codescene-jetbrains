package com.codescene.jetbrains.services.api

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.data.delta.Delta
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.components.webview.data.CwfData
import com.codescene.jetbrains.components.webview.data.HomeData
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.handler.CwfMessageHandler
import com.codescene.jetbrains.components.webview.mapper.CodeHealthMonitorMapper
import com.codescene.jetbrains.components.webview.util.parseMessage
import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.UIRefreshService
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.cache.DeltaCacheEntry
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.codescene.jetbrains.util.*
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Constants.DELTA
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

@Service(Service.Level.PROJECT)
class CodeDeltaService(private val project: Project) : CodeSceneService() {
    companion object {
        fun getInstance(project: Project): CodeDeltaService = project.service<CodeDeltaService>()
    }

    override val scope = CoroutineScope(Dispatchers.IO)

    override val activeReviewCalls = mutableMapOf<String, Job>()

    override fun review(editor: Editor) {
        reviewFile(editor) {
            performDeltaAnalysis(editor)

            project.messageBus.syncPublisher(ToolWindowRefreshNotifier.TOPIC)
                .refresh(editor.virtualFile) // TODO: remove, old CHM implementation
        }
    }

    override fun getActiveApiCalls() = CodeSceneCodeVisionProvider.activeDeltaApiCalls

    override fun removeActiveCall(filePath: String) {
        super.removeActiveCall(filePath)
        updateMonitor()
    }

    override fun addActiveCall(filePath: String, job: Job) {
        super.addActiveCall(filePath, job)
        updateMonitor()
    }

    /**
     * Performs delta analysis by comparing the current editor content against a baseline.
     *
     * The baseline for delta analysis is determined as follows:
     * - If available, it uses the code/content from the branch creation commit.
     * - If the branch creation commit is not found or the analysis is run in a detached HEAD state,
     *   it falls back to comparing against the best score of 10.0.
     */
    private suspend fun performDeltaAnalysis(editor: Editor) {
        val oldCode = GitService
            .getInstance(project)
            .getBranchCreationCommitCode(editor.virtualFile)

        val delta = getDeltaResponse(editor, oldCode)

        handleDeltaResponse(editor, delta, oldCode)
    }

    private fun getDeltaResponse(editor: Editor, oldCode: String): Delta? {
        val path = editor.virtualFile.path

        val oldReview = ReviewParams(path, oldCode)
        val newReview = ReviewParams(path, editor.document.text)

        val (result, elapsedMs) = runWithClassLoaderChange { ExtensionAPI.delta(oldReview, newReview) }

        val telemetryInfo = getTelemetryInfo(editor.virtualFile)
        TelemetryService.getInstance().logUsage(
            TelemetryEvents.ANALYSIS_PERFORMANCE,
            mutableMapOf(
                Pair("type", DELTA),
                Pair("elapsedMs", elapsedMs),
                Pair("loc", telemetryInfo.loc),
                Pair("language", telemetryInfo.language),
            )
        )

        if (result?.oldScore?.isEmpty == true && result?.newScore?.isEmpty == false) {
            return Delta(
                10.0, result.newScore.get(), result.scoreChange, result.fileLevelFindings, result.functionLevelFindings
            )
        }

        return result
    }

    private suspend fun handleDeltaResponse(editor: Editor, delta: Delta?, oldCode: String) {
        val path = editor.virtualFile.path
        val currentCode = editor.document.text
        val cacheService = DeltaCacheService.getInstance(project)

        if (delta == null) {
            Log.info("Received null response from $CODESCENE delta API.", "$serviceImplementation - ${project.name}")

            cacheService.invalidate(path)
        } else {
            UIRefreshService.getInstance(project).refreshCodeVision(editor, listOf("CodeHealthCodeVisionProvider"))
        }

        val cacheEntry = DeltaCacheEntry(path, oldCode, currentCode, delta)
        cacheService.put(cacheEntry)
    }

    /**
     * Updates the Code Health Monitor in the Home view (CWF).
     *
     * This method retrieves the latest delta analysis results from the
     * [DeltaCacheService], maps them to [CwfData] using
     * [CodeHealthMonitorMapper], serializes the data into a JSON string,
     * and posts it to the [CwfMessageHandler] for rendering in the UI.
     *
     * The JSON message is created using [parseMessage], which ensures the
     * correct serializer is used.
     */
    private fun updateMonitor() {
        val mapper = CodeHealthMonitorMapper.getInstance()
        val deltaResults = DeltaCacheService.getInstance(project).getAll()

        val dataJson = parseMessage(
            mapper = { mapper.toCwfData(deltaResults, getActiveApiCalls()) },
            serializer = CwfData.serializer(HomeData.serializer())
        )

        updateToolWindowIcon(
            UpdateToolWindowIconParams(
                project = project,
                toolWindowId = "CodeSceneHome", // TODO: update
                baseIcon = AllIcons.Actions.Lightning, // TODO: update
                hasNotification = deltaResults.isNotEmpty()
            )
        )
        CwfMessageHandler.getInstance(project).postMessage(View.HOME, dataJson)
    }
}