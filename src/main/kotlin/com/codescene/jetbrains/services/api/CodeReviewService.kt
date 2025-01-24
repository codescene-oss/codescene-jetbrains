package com.codescene.jetbrains.services.api

import com.codescene.ExtensionAPI
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
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
class CodeReviewService(project: Project) : CodeSceneService() {
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
            uiRefreshService.refreshUI(editor)
        }
    }

    override fun getActiveApiCalls() = CodeSceneCodeVisionProvider.activeReviewApiCalls

    private fun performCodeReview(editor: Editor) {
        val file = editor.virtualFile
        val path = file.path
        val fileName = file.name
        val code = editor.document.text

        val result = runWithClassLoaderChange { ExtensionAPI.review(path, code) }

        val entry = ReviewCacheEntry(fileContents = code, filePath = path, response = result)
        cacheService.put(entry)

        Log.debug("Review response cached for file $fileName with path $path")
    }
}