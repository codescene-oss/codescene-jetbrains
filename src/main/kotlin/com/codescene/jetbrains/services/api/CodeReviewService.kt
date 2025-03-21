package com.codescene.jetbrains.services.api

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.CodeParams
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.data.review.Review
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.UIRefreshService
import com.codescene.jetbrains.services.cache.ReviewCacheEntry
import com.codescene.jetbrains.services.cache.ReviewCacheService
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

@Service(Service.Level.PROJECT)
class CodeReviewService(private val project: Project) : CodeSceneService() {
    private val cacheService: ReviewCacheService = ReviewCacheService.getInstance(project)
    private val uiRefreshService: UIRefreshService = UIRefreshService.getInstance(project)

    companion object {
        fun getInstance(project: Project): CodeReviewService = project.service<CodeReviewService>()
    }

    override val scope = CoroutineScope(Dispatchers.IO)

    override val activeReviewCalls = mutableMapOf<String, Job>()

    override fun review(editor: Editor) {
        reviewFile(editor) {
            performCodeReview(editor)
            uiRefreshService.refreshUI(editor, CodeSceneCodeVisionProvider.getProviders())
        }
    }

    override fun getActiveApiCalls() = CodeSceneCodeVisionProvider.activeReviewApiCalls

    private fun performCodeReview(editor: Editor) {
        val file = editor.virtualFile
        val path = file.path
        val fileName = file.name
        val code = editor.document.text

        val params = ReviewParams(path, code)
        val result = runWithClassLoaderChange { ExtensionAPI.review(params) }

        val entry = ReviewCacheEntry(fileContents = code, filePath = path, response = result)
        cacheService.put(entry)

        checkContainsRefactorableFunctions(editor, result)

        Log.debug(
            "Review response cached for file $fileName with path $path",
            "$serviceImplementation - ${project.name}"
        )
    }

    private fun checkContainsRefactorableFunctions(editor: Editor, result: Review) {
        val aceEnabled = CodeSceneGlobalSettingsStore.getInstance().state.enableAutoRefactor

        if (aceEnabled) {
            val aceParams = CodeParams(editor.document.text, editor.virtualFile.extension)
            AceService.getInstance().getRefactorableFunctions(aceParams, result, editor)
        }
    }
}