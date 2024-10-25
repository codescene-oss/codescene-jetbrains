package com.codescene.jetbrains.services

import codescene.devtools.ide.DevToolsAPI
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.data.ApiResponse
import com.intellij.codeInsight.codeVision.CodeVisionHost
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class CodeSceneService(project: Project) {
    private val logger = Logger.getInstance(CodeSceneService::class.java)
    private val cacheService: ReviewCacheService = ReviewCacheService.getInstance(project)

    private var debounceJob: Job? = null
    private val debounceDelay: Long = TimeUnit.SECONDS.toMillis(3)

    companion object {
        fun getInstance(project: Project): CodeSceneService = project.service<CodeSceneService>()
    }

    fun reviewCode(editor: Editor) {
        debounceJob?.cancel()

        try {
            CoroutineScope(Dispatchers.IO).launch {
                delay(debounceDelay)

                performCodeReview(editor)

                refreshUI(editor, editor.project!!)
            }
        } catch (e: Exception) {
            logger.error("Error during code review: ${e.message}", e)
        }
    }

    private fun performCodeReview(editor: Editor) {
        val path = editor.virtualFile.path
        val file = editor.virtualFile
        val code = editor.document.text

        runWithClassLoaderChange {
            println("No cache found. Calling API...")

            val result = DevToolsAPI.review(file.path, code)

            val parsedData = Json.decodeFromString<ApiResponse>(result)

            cacheService.cacheResponse(path, code, parsedData)
        }
    }

    private suspend fun refreshUI(editor: Editor, project: Project) = withContext(Dispatchers.Main) {
        val host = project.service<CodeVisionHost>()
        val invalidateSignal = CodeVisionHost.LensInvalidateSignal(
            editor,
            providerIds = CodeSceneCodeVisionProvider.getProviders()
        )

        println("Refreshing code lens...")

        host.invalidateProvider(invalidateSignal)
        DaemonCodeAnalyzer.getInstance(project).restart()

        println("Refreshing external annotations...")

        CodeSceneCodeVisionProvider.isApiCallInProgress = false
    }

    private fun <T> runWithClassLoaderChange(action: () -> T) {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        val classLoader = this@CodeSceneService.javaClass.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        try {
            action()
        } catch (e: Exception) {
            throw (e)
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
    }
}