package com.codescene.jetbrains.services

import codescene.devtools.ide.DevToolsAPI
import com.codescene.jetbrains.data.ApiResponse
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.charset.Charset

@Service(Service.Level.PROJECT)
class CodeSceneService {
    private val logger = Logger.getInstance(CodeSceneService::class.java)

    companion object {
        fun getInstance(project: Project): CodeSceneService {
            return project.service<CodeSceneService>()
        }
    }

    private suspend fun <T> runWithClassLoaderAndIOContext(action: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            val originalClassLoader = Thread.currentThread().contextClassLoader
            val classLoader = this@CodeSceneService.javaClass.classLoader
            Thread.currentThread().contextClassLoader = classLoader

            try {
                action()
            } finally {
                Thread.currentThread().contextClassLoader = originalClassLoader
            }
        }
    }

    //Work in progress:
    suspend fun reviewCode(file: VirtualFile) {
        val bytes = file.contentsToByteArray()
        val code = bytes.toString(Charset.forName("UTF-8"))

        try {
            runWithClassLoaderAndIOContext {
                val result = DevToolsAPI.review(file.path, code)

                logger.info("Got response from CodeScene API: $result")

                val parsedData = Json.decodeFromString<ApiResponse>(result)
                println("Parsed API Response: $parsedData")
            }
        } catch (e: Exception) {
            logger.error("Error during code review: ${e.message}", e)
        }
    }
}