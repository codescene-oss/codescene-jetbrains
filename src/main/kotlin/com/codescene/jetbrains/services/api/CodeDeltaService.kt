package com.codescene.jetbrains.services.api

import codescene.devtools.ide.DevToolsAPI
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.data.CodeDelta
import com.codescene.jetbrains.data.CodeReview
import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.UIRefreshService
import com.codescene.jetbrains.services.cache.DeltaCacheEntry
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.codescene.jetbrains.services.cache.ReviewCacheQuery
import com.codescene.jetbrains.services.cache.ReviewCacheService
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Log
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
    private val uiRefreshService: UIRefreshService = UIRefreshService.getInstance(project)
    private val deltaCacheService: DeltaCacheService = DeltaCacheService.getInstance(project)
    private val reviewCacheService: ReviewCacheService = ReviewCacheService.getInstance(project)

    companion object {
        fun getInstance(project: Project): CodeDeltaService = project.service<CodeDeltaService>()
    }

    override val scope = CoroutineScope(Dispatchers.IO)

    override val activeReviewCalls = mutableMapOf<String, Job>()

    override fun review(editor: Editor) {
        reviewFile(editor) {
            performDeltaAnalysis(editor)

            project.messageBus.syncPublisher(ToolWindowRefreshNotifier.TOPIC)
                .refresh(editor.virtualFile)
        }
    }

    override fun getActiveApiCalls() = CodeSceneCodeVisionProvider.activeDeltaApiCalls

    private suspend fun performDeltaAnalysis(editor: Editor) {
        val path = editor.virtualFile.path

        val oldCode = GitService.getInstance(project).getHeadCommit(editor.virtualFile)

        val cachedReview = reviewCacheService.get(ReviewCacheQuery(editor.document.text, path))
            .also { if (it != null) Log.debug("Found cached review for new file: ${path}") }

        val delta = getDeltaResponse(editor, oldCode, cachedReview)

        handleDeltaResponse(editor, delta, oldCode)
    }

    private fun getDeltaResponse(editor: Editor, oldCode: String, cachedReview: CodeReview?) =
        runWithClassLoaderChange {
            var rawScore: String? = null
            val path = editor.virtualFile.path

            if (oldCode != "") {
                Log.debug("Initiating delta review using HEAD commit for $path.")

                val oldCodeReview = Json.decodeFromString<CodeReview>(DevToolsAPI.review(path, oldCode))
                rawScore = oldCodeReview.rawScore
            }

            val newCodeReview =
                cachedReview ?: Json.decodeFromString<CodeReview>(DevToolsAPI.review(path, editor.document.text))

            val delta = DevToolsAPI.delta(rawScore, newCodeReview.rawScore)

            delta
        }

    private suspend fun handleDeltaResponse(editor: Editor, deltaJson: String, oldCode: String) {
        val path = editor.virtualFile.path
        val currentCode = editor.document.text

        if (deltaJson == "null") {
            Log.info("Received null response from $CODESCENE delta API.")

            deltaCacheService.invalidate(path)
        } else {
            val parsedDelta = Json.decodeFromString<CodeDelta>(deltaJson)

            val cacheEntry = DeltaCacheEntry(path, oldCode, currentCode, parsedDelta)

            deltaCacheService.put(cacheEntry)
            uiRefreshService.refreshCodeVision(editor, listOf("CodeHealthCodeVisionProvider"))
        }
    }
}