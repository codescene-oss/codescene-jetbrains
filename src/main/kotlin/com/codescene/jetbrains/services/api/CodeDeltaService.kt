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
    companion object {
        fun getInstance(project: Project): CodeDeltaService = project.service<CodeDeltaService>()
    }

    override val scope = CoroutineScope(Dispatchers.IO)

    override val activeReviewCalls = mutableMapOf<String, Job>()

    override fun review(editor: Editor) {
        reviewFile(editor) {
            performDeltaAnalysis(editor)

            project.messageBus.syncPublisher(ToolWindowRefreshNotifier.TOPIC).refresh(editor.virtualFile)
        }
    }

    override fun getActiveApiCalls() = CodeSceneCodeVisionProvider.activeDeltaApiCalls

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

        val delta = runWithClassLoaderChange { ExtensionAPI.delta(oldReview, newReview) }

        if (delta?.oldScore?.isEmpty == true) {
            return Delta(
                10.0, delta.newScore.get(), delta.scoreChange, delta.fileLevelFindings, delta.functionLevelFindings
            )
        }

        return delta
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