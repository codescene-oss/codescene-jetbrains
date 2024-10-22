package com.codescene.jetbrains.services

import codescene.devtools.ide.DevToolsAPI
import com.codescene.jetbrains.data.ApiResponse
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Service(Service.Level.PROJECT)
class CodeSceneService(project: Project) {
    private val logger = Logger.getInstance(CodeSceneService::class.java)
    private val cacheService: ReviewCacheService = ReviewCacheService.getInstance(project)

    companion object {
        fun getInstance(project: Project): CodeSceneService = project.service<CodeSceneService>()
    }

    //TODO: Formalize logs
    suspend fun reviewCode(editor: Editor): ApiResponse? {
        val path = editor.virtualFile.path
        val file = editor.virtualFile
        val code = editor.document.text

        return try {
            runWithClassLoaderAndIOContext {
                println("No cache. Calling API...")

                val result = DevToolsAPI.review(file.path, code)

                val parsedData = Json.decodeFromString<ApiResponse>(result)

                cacheService.cacheResponse(path, code, parsedData)

                println("Returning API result...")

                return@runWithClassLoaderAndIOContext parsedData
            }
        } catch (e: Exception) {
            logger.error("Error during code review: ${e.message}", e)

            return null
        }
    }

    private suspend fun <T> runWithClassLoaderAndIOContext(action: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
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
}