package com.codescene.jetbrains.listeners

import codescene.devtools.ide.DevToolsAPI
import com.codescene.jetbrains.data.ApiResponse
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.serialization.json.Json

class MyFileOpenListener : FileEditorManagerListener {
    private val project: Project? = ProjectManager.getInstance().openProjects.firstOrNull()

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        reviewCode(file)
    }

    //Work in progress:
    private fun reviewCode(file: VirtualFile) {
        val originalClassLoader = Thread.currentThread().contextClassLoader
        val classLoader = this.javaClass.classLoader
        Thread.currentThread().contextClassLoader = classLoader

        try {
            if (file.isValid && project != null) {
                val bytes = file.contentsToByteArray()
                val code = bytes.toString(Charsets.UTF_8)

                val data = DevToolsAPI.review(file.path, code)

                println("Got response from CodeScene API: $data")

                val parsedData =  Json.decodeFromString<ApiResponse>(data)
                println(parsedData)
            }
        } catch (e: Exception) {
            println(e.message)
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
    }
}