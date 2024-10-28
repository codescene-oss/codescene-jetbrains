package com.codescene.jetbrains.services

import codescene.devtools.ide.DevToolsAPI
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.data.ApiResponse
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class CodeSceneService(project: Project) {
    private val cacheService: ReviewCacheService = ReviewCacheService.getInstance(project)
    private val uiRefreshService: UIRefreshService = UIRefreshService.getInstance(project)

    private var debounceJob: Job? = null
    private val debounceDelay: Long = TimeUnit.SECONDS.toMillis(3)

    companion object {
        fun getInstance(project: Project): CodeSceneService = project.service<CodeSceneService>()
    }

    fun reviewCode(editor: Editor) {
        debounceJob?.cancel()

        val filePath = editor.virtualFile.path
        val fileName = editor.virtualFile.name

        try {
            CoroutineScope(Dispatchers.IO).launch {
                delay(debounceDelay)

                Log.info("No cached review for file: $fileName at path: $filePath. Initiating API call to CodeScene for review data.")

                performCodeReview(editor)

                uiRefreshService.refreshUI(editor)

                CodeSceneCodeVisionProvider.isApiCallInProgress = false
            }
        } catch (e: Exception) {
            Log.error("Error during code review for file: $fileName - ${e.message}")
        }
    }

    private fun performCodeReview(editor: Editor) {
        val file = editor.virtualFile
        val path = file.path
        val fileName = file.name
        val code = editor.document.text

        runWithClassLoaderChange {
            val startTime = System.currentTimeMillis()

            val result = DevToolsAPI.review(file.path, code)

            val elapsedTime = System.currentTimeMillis() - startTime
            Log.info("Received response from CodeScene API for file: $fileName in ${elapsedTime}ms")

            val parsedData = Json.decodeFromString<ApiResponse>(result)

            val entry = CacheEntry(fileContents = code, filePath = path, response = parsedData)
            cacheService.cacheResponse(entry)

            Log.debug("Review response cached for file: $fileName with path: $path")
        }
    }

    private fun <T> runWithClassLoaderChange(action: () -> T) {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        val classLoader = this@CodeSceneService.javaClass.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        try {
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
}