package com.codescene.jetbrains.services

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Service(Service.Level.PROJECT)
class CodeNavigationService(val project: Project) {
    companion object {
        fun getInstance(project: Project): CodeNavigationService = project.service<CodeNavigationService>()
    }

    suspend fun focusOnLine(filePath: String, line: Int) {
        val file = getFileByName(filePath) ?: return

        openEditorAndMoveCaret(file, line)
    }

    private fun getFileByName(filePath: String): VirtualFile? {
        val fileName = File(filePath).name

        val file = ReadAction.compute<VirtualFile?, Throwable> {
            FilenameIndex.getVirtualFilesByName(fileName, GlobalSearchScope.projectScope(project))
                .firstOrNull { it.path.endsWith(filePath) } //TODO: check if I need this
        }

        return file
    }

    private suspend fun openEditorAndMoveCaret(file: VirtualFile, line: Int) = withContext(Dispatchers.Main) {
        val openFileDescriptor = OpenFileDescriptor(project, file, line - 1, 0)
        val editor = FileEditorManager.getInstance(project)
            .openTextEditor(openFileDescriptor, true)

        if (editor != null) {
            moveCaret(editor, line)
        }
    }

    private fun moveCaret(editor: Editor, line: Int) {
        val caretModel = editor.caretModel

        WriteCommandAction.runWriteCommandAction(project) {
            val position = LogicalPosition(line - 1, 0)

            caretModel.moveToLogicalPosition(position)
            editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        }
    }
}