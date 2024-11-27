package com.codescene.jetbrains.services.api

import codescene.devtools.ide.DevToolsAPI
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.data.CodeReview
import com.codescene.jetbrains.services.UIRefreshService
import com.codescene.jetbrains.services.cache.ReviewCacheEntry
import com.codescene.jetbrains.services.cache.ReviewCacheService
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

@Service(Service.Level.PROJECT)
class CodeReviewService(project: Project) : CodeSceneService() {
    private val cacheService: ReviewCacheService = ReviewCacheService.getInstance(project)
    private val uiRefreshService: UIRefreshService = UIRefreshService.getInstance(project)

    companion object {
        fun getInstance(project: Project): CodeReviewService = project.service<CodeReviewService>()
    }

    override fun review(editor: Editor) {
        val filePath = editor.virtualFile.path
        val fileName = editor.virtualFile.name

        activeReviewCalls[filePath]?.cancel()

        activeReviewCalls[filePath] = scope.launch {
            delay(debounceDelay)

            Log.info("No cached review for file $fileName at path $filePath. Initiating $CODESCENE review.")

            try {
                performCodeReview(editor)

                uiRefreshService.refreshUI(editor)

                CodeSceneCodeVisionProvider.markApiCallComplete(
                    filePath,
                    CodeSceneCodeVisionProvider.activeReviewApiCalls
                )
            } catch (e: CancellationException) {
                Log.info("Code review canceled for file $fileName.")
            } catch (e: Exception) {
                Log.error("Error during code review for file $fileName - ${e.message}")
            } finally {
                activeReviewCalls.remove(filePath)
            }
        }
    }

    override fun markApiCallComplete(filePath: String) {
        CodeSceneCodeVisionProvider.markApiCallComplete(filePath, CodeSceneCodeVisionProvider.activeReviewApiCalls)
    }

    private fun performCodeReview(editor: Editor) {
        val file = editor.virtualFile
        val path = file.path
        val fileName = file.name
        val code = editor.document.text

        val result = runWithClassLoaderChange {
            DevToolsAPI.review(path, code)
        }

        val parsedData = Json.decodeFromString<CodeReview>(result)

        val entry = ReviewCacheEntry(fileContents = code, filePath = path, response = parsedData)
        cacheService.put(entry)

        Log.debug("Review response cached for file $fileName with path $path")
    }
}