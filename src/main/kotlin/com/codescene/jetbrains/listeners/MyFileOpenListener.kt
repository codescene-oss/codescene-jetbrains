package com.codescene.jetbrains.listeners

import codescene.devtools.ide.api
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

class MyFileOpenListener : FileEditorManagerListener {
    private val project: Project? = ProjectManager.getInstance().openProjects.firstOrNull()

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        readFileContents(file)
    }

    private fun readFileContents(file: VirtualFile) {
        if (file.isValid && project != null) {
            val fileContents = VfsUtil.loadText(file)
            val relativePath = VfsUtil.getRelativePath(file, project.baseDir)

            println("File: ${file.name}\nPath:${relativePath}\nContents:\n$fileContents")

            println("Calling CodeScene API...")

            try {
                val data = api.review(relativePath, fileContents)

                thisLogger().info("Received response from CS API: \n$data")
            } catch (e: Exception) {
                thisLogger().error(e.message)
            }
        }
    }
}