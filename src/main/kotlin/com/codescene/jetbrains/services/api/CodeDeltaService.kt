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
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

@Service(Service.Level.PROJECT)
class CodeDeltaService(project: Project) : CodeSceneService() {
    private val uiRefreshService: UIRefreshService = UIRefreshService.getInstance(project)
    private val deltaCacheService: DeltaCacheService = DeltaCacheService.getInstance(project)
    private val reviewCacheService: ReviewCacheService = ReviewCacheService.getInstance(project)

    companion object {
        fun getInstance(project: Project): CodeDeltaService = project.service<CodeDeltaService>()
    }

    override val scope = CoroutineScope(Dispatchers.IO)

    override val activeReviewCalls = mutableMapOf<String, Job>()

    override fun review(editor: Editor) {
        val path = editor.virtualFile.path

        activeReviewCalls[path]?.cancel()

        try {
            activeReviewCalls[path] = scope.launch {
                delay(debounceDelay)

                performDeltaAnalysis(editor)

                editor.project!!.messageBus.syncPublisher(ToolWindowRefreshNotifier.TOPIC).refresh(editor)
                CodeSceneCodeVisionProvider.markApiCallComplete(path, CodeSceneCodeVisionProvider.activeDeltaApiCalls)
            }
        } catch (e: Exception) {
            Log.error("Error during delta analysis for file - ${e.message}")
        }
    }

    private suspend fun performDeltaAnalysis(editor: Editor) {
        val project = editor.project!!
        val path = editor.virtualFile.path
        val currentCode = editor.document.text

        val oldCode = GitService.getInstance(project).getHeadCommit(editor.virtualFile).also { if (it == "") return }
        val cachedReview = reviewCacheService.get(ReviewCacheQuery(currentCode, path))
            .also { if (it != null) Log.debug("Found cached review for new file: ${path}") }

        val delta = runWithClassLoaderChange {
            val oldCodeReview = Json.decodeFromString<CodeReview>(DevToolsAPI.review(path, oldCode))
            val newCodeReview = cachedReview ?: Json.decodeFromString<CodeReview>(DevToolsAPI.review(path, currentCode))

            val delta = DevToolsAPI.delta(oldCodeReview.rawScore, newCodeReview.rawScore)

            delta
        }

        if (delta == "null") {
            Log.info("Received no response from $CODESCENE delta API.")
            deltaCacheService.invalidate(path)
        } else {
            val parsedDelta = Json.decodeFromString<CodeDelta>(delta)

            val cacheEntry = DeltaCacheEntry(path, oldCode, currentCode, parsedDelta)

            deltaCacheService.put(cacheEntry)
            uiRefreshService.refreshCodeVision(editor, listOf("CodeHealthCodeVisionProvider"))
        }
    }
}