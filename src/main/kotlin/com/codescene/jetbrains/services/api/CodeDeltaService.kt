package com.codescene.jetbrains.services.api

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.data.delta.Delta
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.UIRefreshService
import com.codescene.jetbrains.services.cache.DeltaCacheEntry
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

@Service(Service.Level.PROJECT)
class CodeDeltaService(private val project: Project) : CodeSceneService() {
    private val uiRefreshService: UIRefreshService = UIRefreshService.getInstance(project)
    private val deltaCacheService: DeltaCacheService = DeltaCacheService.getInstance(project)

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

        //TODO; delete after testing
        println("Old code: $oldCode\nNew code: ${editor.document.text}")

        val delta = runWithClassLoaderChange { ExtensionAPI.delta(oldReview, newReview) }

        if (delta?.oldScore == null) delta?.oldScore = 10.0

        return delta
    }

    private suspend fun handleDeltaResponse(editor: Editor, delta: Delta?, oldCode: String) {
        val path = editor.virtualFile.path
        val currentCode = editor.document.text

        if (delta == null) {
            Log.info("Received null response from $CODESCENE delta API.", "$serviceImplementation - ${project.name}")

            deltaCacheService.invalidate(path)
        } else
            uiRefreshService.refreshCodeVision(editor, listOf("CodeHealthCodeVisionProvider"))

        val cacheEntry = DeltaCacheEntry(path, oldCode, currentCode, delta)
        deltaCacheService.put(cacheEntry)
    }
}