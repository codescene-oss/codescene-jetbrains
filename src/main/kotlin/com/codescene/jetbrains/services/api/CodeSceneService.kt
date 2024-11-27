package com.codescene.jetbrains.services.api

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import java.util.concurrent.TimeUnit

abstract class CodeSceneService : Disposable {
    protected val scope = CoroutineScope(Dispatchers.IO)
    protected val activeReviewCalls = mutableMapOf<String, Job>()
    protected val debounceDelay: Long = TimeUnit.SECONDS.toMillis(3)

    abstract fun review(editor: Editor)

    protected fun <T> runWithClassLoaderChange(action: () -> T): T {
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

    fun cancelFileReview(filePath: String, calls: MutableSet<String>) {
        val className = this::class.java.simpleName

        activeReviewCalls[filePath]?.let { job ->
            job.cancel()

            Log.info("$className: Cancelling active $CODESCENE review for file '$filePath' because it was closed.")

            activeReviewCalls.remove(filePath)

            CodeSceneCodeVisionProvider.markApiCallComplete(filePath, calls)
        } ?: Log.debug("$className: No active $CODESCENE review found for file: $filePath")
    }

    override fun dispose() {
        activeReviewCalls.values.forEach { it.cancel() }

        activeReviewCalls.clear()

        scope.cancel()
    }
}