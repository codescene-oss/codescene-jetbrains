package com.codescene.jetbrains.services.api

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.data.delta.Delta
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.components.webview.data.*
import com.codescene.jetbrains.components.webview.handler.CwfMessageHandler
import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.UIRefreshService
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.cache.DeltaCacheEntry
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Constants.DELTA
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.TelemetryEvents
import com.codescene.jetbrains.util.getTelemetryInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json

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

            updateMonitor()
            project.messageBus.syncPublisher(ToolWindowRefreshNotifier.TOPIC).refresh(editor.virtualFile)
        }
    }

    override fun getActiveApiCalls() = CodeSceneCodeVisionProvider.activeDeltaApiCalls

    // TODO: move
    private fun updateMonitor() {
        val json = Json {
            encodeDefaults = true
            prettyPrint = true
        }

        val deltaResults = DeltaCacheService.getInstance(project).getAll()

        val cwfData = CwfData(
            view = View.HOME.value,
            pro = true,
            devmode = true,
            featureFlags = listOf("open-settings"),
            data = HomeData(
                signedIn = true,
                fileDeltaData = deltaResults.map { result ->
                    val deltaResponse = result.second.deltaApiResponse

                    FileDeltaData(
                        file = File(
                            fileName = result.first,
                        ),
                        delta = DeltaForFile(
                            oldScore = deltaResponse?.oldScore?.get() ?: 0.0,
                            newScore = deltaResponse?.newScore?.get() ?: 0.0,
                            scoreChange = deltaResponse?.scoreChange?.toDouble() ?: 0.0,
                            fileLevelFindings = deltaResponse?.fileLevelFindings
                                ?.map { finding ->
                                    ChangeDetail(
                                        line = finding.line.get(),
                                        description = finding.description,
                                        changeType = finding.changeType.value(),
                                        category = finding.category
                                    )
                                } ?: emptyList(),
                            functionLevelFindings = deltaResponse?.functionLevelFindings
                                ?.map { fn ->
                                    FunctionFinding(
                                        function = FunctionInfo(
                                            name = fn.function.name,
                                            range = Range(
                                                startLine = fn.function.range?.get()?.startLine ?: 0,
                                                endLine = fn.function.range?.get()?.endLine ?: 0,
                                                startColumn = fn.function.range?.get()?.startColumn ?: 0,
                                                endColumn = fn.function.range?.get()?.endColumn ?: 0
                                            )
                                        ),
                                        changeDetails = fn.changeDetails
                                            ?.map { cd ->
                                                ChangeDetail(
                                                    line = cd.line?.get(),
                                                    description = cd.description,
                                                    changeType = cd.changeType.value(),
                                                    category = cd.category
                                                )
                                            } ?: emptyList()
                                    )
                                } ?: emptyList()
                        )
                    )
                }
            )
        )
        val payloadJson = json.encodeToJsonElement(CwfData.serializer(HomeData.serializer()), cwfData)

        val message = CWFMessage(
            messageType = LifecycleMessages.UPDATE_RENDERER.value,
            payload = payloadJson
        )

        val dataJson = json.encodeToString(CWFMessage.serializer(), message)
        CwfMessageHandler.getInstance(project).postMessage(dataJson)
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

        if (result?.oldScore?.isEmpty == true) {
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
}