package com.codescene.jetbrains.services

import codescene.devtools.ide.DevToolsAPI
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.data.CodeDelta
import com.codescene.jetbrains.data.CodeReview
import com.codescene.jetbrains.notifier.ToolWindowRefreshNotifier
import com.codescene.jetbrains.services.cache.*
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
    private val deltaCacheService: DeltaCacheService = DeltaCacheService.getInstance(project)
    private val uiRefreshService: UIRefreshService = UIRefreshService.getInstance(project)

    private val reviewScope = CoroutineScope(Dispatchers.IO)
    private val deltaScope = CoroutineScope(Dispatchers.IO)
    private val debounceDelay: Long = TimeUnit.SECONDS.toMillis(3)

    private val activeFileReviews = mutableMapOf<String, Job>()
    private val activeDeltaReviews = mutableMapOf<String, Job>()

    companion object {
        fun getInstance(project: Project): CodeSceneService = project.service<CodeSceneService>()
    }

    fun reviewCode(editor: Editor) {
        val filePath = editor.virtualFile.path
        val fileName = editor.virtualFile.name

        activeFileReviews[filePath]?.cancel()

        activeFileReviews[filePath] = reviewScope.launch {
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
                activeFileReviews.remove(filePath)
            }
        }
    }

    fun codeDelta(editor: Editor) {
        val path = editor.virtualFile.path

        activeDeltaReviews[path]?.cancel()

        try {
            activeDeltaReviews[path] = deltaScope.launch {
                delay(debounceDelay)

                performDeltaAnalysis(editor)

                CodeSceneCodeVisionProvider.markApiCallComplete(
                    path,
                    CodeSceneCodeVisionProvider.activeDeltaApiCalls
                )
            }
        } catch (e: Exception) {
            Log.error("Error during delta analysis for file - ${e.message}")
        }
    }

    data class DeltaResponse(
        val delta: String,
        val oldScore: Double,
        val newScore: Double
    )

    private suspend fun performDeltaAnalysis(editor: Editor) {
        val project = editor.project!!
        val path = editor.virtualFile.path
        val currentCode = editor.document.text

        val oldCode = GitService.getInstance(project).getHeadCommit(editor.virtualFile).also { if (it == "") return }
        val cachedReview = cacheService.get(ReviewCacheQuery(currentCode, path))
            .also { if (it != null) Log.debug("Found cached review for new file: ${path}") }

        val result = runWithClassLoaderChange {
            val oldCodeReview = Json.decodeFromString<CodeReview>(DevToolsAPI.review(path, oldCode))
            val newCodeReview = cachedReview ?: Json.decodeFromString<CodeReview>(DevToolsAPI.review(path, currentCode))

            val delta = DevToolsAPI.delta(oldCodeReview.rawScore, newCodeReview.rawScore)

            DeltaResponse(delta, oldCodeReview.score, newCodeReview.score)
        }

        val (delta, oldScore, newScore) = result //TODO

        //TODO: refactor
        when (delta) {
            "null" -> {
                println("Response is null!")
            }
            else -> {
                val parsedDelta = Json.decodeFromString<CodeDelta>(delta)

                val cacheEntry = DeltaCacheEntry(path, oldCode, currentCode, parsedDelta)

                deltaCacheService.put(cacheEntry)

                editor.project!!.messageBus.syncPublisher(ToolWindowRefreshNotifier.TOPIC).refresh(editor)

                uiRefreshService.refreshCodeVision(editor, listOf("CodeHealthCodeVisionProvider"))
            }
        }
    }

    fun cancelFileReview(filePath: String) {
        activeFileReviews[filePath]?.let { job ->
            job.cancel()

            Log.info("Cancelling active $CODESCENE review for file '$filePath' because it was closed.")

            activeFileReviews.remove(filePath)
            CodeSceneCodeVisionProvider.markApiCallComplete(filePath, CodeSceneCodeVisionProvider.activeReviewApiCalls)
        } ?: Log.debug("No active $CODESCENE review found for file: $filePath")
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

    private fun <T> runWithClassLoaderChange(action: () -> T): T {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        val classLoader = this@CodeSceneService.javaClass.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        return try {
            Log.debug("Switching to plugin's ClassLoader: ${classLoader.javaClass.name}")

            val startTime = System.currentTimeMillis()

            val result = action()

            val elapsedTime = System.currentTimeMillis() - startTime
            Log.info("Received response from CodeScene API in ${elapsedTime}ms")

            result
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
        activeDeltaReviews.values.forEach { it.cancel() }

        activeFileReviews.clear()
        activeDeltaReviews.clear()

        reviewScope.cancel()
        deltaScope.cancel()
    }
}