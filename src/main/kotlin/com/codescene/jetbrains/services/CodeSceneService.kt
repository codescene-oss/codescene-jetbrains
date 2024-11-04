package com.codescene.jetbrains.services

import codescene.devtools.ide.DevToolsAPI
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.data.ApiResponse
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class CodeSceneService(project: Project) : Disposable {
    private val cacheService: ReviewCacheService = ReviewCacheService.getInstance(project)
    private val uiRefreshService: UIRefreshService = UIRefreshService.getInstance(project)

    private val scope = CoroutineScope(Dispatchers.IO)
    private val debounceDelay: Long = TimeUnit.SECONDS.toMillis(3)
    private val activeFileReviews = mutableMapOf<String, Job>()

    companion object {
        fun getInstance(project: Project): CodeSceneService = project.service<CodeSceneService>()
    }

    fun reviewCode(editor: Editor) {
        val filePath = editor.virtualFile.path
        val fileName = editor.virtualFile.name

        activeFileReviews[filePath]?.cancel()

        activeFileReviews[filePath] = scope.launch {
            delay(debounceDelay)

            Log.info("No cached review for file $fileName at path $filePath. Initiating $CODESCENE review.")

            try {
                performCodeReview(editor)

                uiRefreshService.refreshUI(editor)

                CodeSceneCodeVisionProvider.markApiCallComplete(filePath)
            } catch (e: CancellationException) {
                Log.info("Code review canceled for file $fileName.")
            } catch (e: Exception) {
                Log.error("Error during code review for file $fileName - ${e.message}")
            } finally {
                activeFileReviews.remove(filePath)
            }
        }
    }

    fun cancelFileReview(filePath: String) {
        activeFileReviews[filePath]?.let { job ->
            job.cancel()

            Log.info("Cancelling active $CODESCENE review for file '$filePath' because it was closed.")

            activeFileReviews.remove(filePath)
            CodeSceneCodeVisionProvider.markApiCallComplete(filePath)
        } ?: Log.debug("No active $CODESCENE review found for file: $filePath")
    }

    private fun performCodeReview(editor: Editor) {
        val file = editor.virtualFile
        val path = file.path
        val fileName = file.name
        val code = editor.document.text

        val result = runWithClassLoaderChange {
            val startTime = System.currentTimeMillis()

            val response = DevToolsAPI.review(path, code)

            val elapsedTime = System.currentTimeMillis() - startTime
            Log.info("Received response from CodeScene API for file $fileName in ${elapsedTime}ms")

            response
        }

        val parsedData = Json.decodeFromString<ApiResponse>(result)

        val entry = CacheEntry(fileContents = code, filePath = path, response = parsedData)
        cacheService.cacheResponse(entry)

        Log.debug("Review response cached for file $fileName with path $path")
    }

    private fun <T> runWithClassLoaderChange(action: () -> T): T {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        val classLoader = this@CodeSceneService.javaClass.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        return try {
            Log.debug("Switching to plugin's ClassLoader: ${classLoader.javaClass.name}")

            action()
        } catch (e: Exception) {
            Log.error("Exception during ClassLoader change operation: ${e.message}")

            throw (e)
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader

            Log.debug("Reverted to original ClassLoader: ${originalClassLoader.javaClass.name}")
        }
    }

    override fun dispose() {
        activeFileReviews.values.forEach { it.cancel() }
        activeFileReviews.clear()

        scope.cancel()
    }
}