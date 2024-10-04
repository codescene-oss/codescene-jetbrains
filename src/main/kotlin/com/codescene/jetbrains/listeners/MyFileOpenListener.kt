package com.codescene.jetbrains.listeners

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile

class MyFileOpenListener : FileEditorManagerListener {
    private val project: Project? = ProjectManager.getInstance().openProjects.firstOrNull()

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        readFileContents(file)
    }

    private fun readFileContents(file: VirtualFile) {
        //TODO
//        if (file.isValid && project != null) {
//            val bytes = file.contentsToByteArray()
//            val code = bytes.toString(Charsets.UTF_8)
//            val relativePath = VfsUtil.getRelativePath(file, project.baseDir)
//
//            println("Calling CodeScene API...")
//            try {
//                val data = api.review(relativePath, code)
//
//                thisLogger().info("Received response from CS API: \n ...data (TBD)")
//            } catch (e: Exception) {
//                thisLogger().error(e.message)
//            }
//        }
    }
}