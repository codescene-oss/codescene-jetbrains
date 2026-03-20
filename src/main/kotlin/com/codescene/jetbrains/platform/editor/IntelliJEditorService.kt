package com.codescene.jetbrains.platform.editor

import com.codescene.jetbrains.core.contracts.IEditorService
import com.codescene.jetbrains.platform.util.ReplaceCodeSnippetArgs
import com.codescene.jetbrains.platform.util.replaceCodeSnippet
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

@Service(Service.Level.PROJECT)
class IntelliJEditorService(private val project: Project) : IEditorService {
    companion object {
        fun getInstance(project: Project): IntelliJEditorService = project.service<IntelliJEditorService>()
    }

    override fun getSelectedFilePath(): String? {
        return FileEditorManager.getInstance(project).selectedTextEditor?.virtualFile?.path
    }

    override fun openFile(filePath: String) {
        val file = LocalFileSystem.getInstance().findFileByIoFile(File(filePath)) ?: return
        val descriptor = OpenFileDescriptor(project, file)
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
    }

    override fun replaceCodeSnippet(
        filePath: String,
        startLine: Int,
        endLine: Int,
        newContent: String,
    ) {
        replaceCodeSnippet(
            ReplaceCodeSnippetArgs(
                project = project,
                filePath = filePath,
                startLine = startLine,
                endLine = endLine,
                newContent = newContent,
            ),
        )
    }
}
