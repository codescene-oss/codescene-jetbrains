package com.codescene.jetbrains.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteIntentReadAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.RuntimeException

@Service(Service.Level.PROJECT)
class CodeNavigationService(val project: Project) {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        fun getInstance(project: Project): CodeNavigationService = project.service<CodeNavigationService>()
    }

    fun focusOnLine(filePath: String, line: Int) {
        scope.launch {
            val file = getFileByName(filePath) ?: return@launch

            openEditorAndMoveCaret(file, line)
        }
    }

    private fun getFileByName(filePath: String): VirtualFile? {
        val fileName = File(filePath).name
        val searchScope = GlobalSearchScope.projectScope(project)

        val file = runReadAction {
            FilenameIndex.getVirtualFilesByName(fileName, searchScope)
                .firstOrNull { it.path.endsWith(filePath) }
        }

        return file
    }

    private suspend fun openEditorAndMoveCaret(file: VirtualFile, line: Int) = withContext(Dispatchers.Main) {
        // Only calculate/capture data in runReadAction, NOT open the editor!
        val openFileDescriptor = runReadAction {
            OpenFileDescriptor(project, file, line - 1, 0)
        }

        // Open the editor outside runReadAction
        val editor = WriteIntentReadAction.compute<Editor?, RuntimeException> {
            FileEditorManager.getInstance(project)
                .openTextEditor(openFileDescriptor, true)
        }
        if (editor != null) {
            moveCaret(editor, line)
        }
    }

    private fun moveCaret(editor: Editor, line: Int) {
        val caretModel = editor.caretModel

        runWriteAction {
            val position = LogicalPosition(line - 1, 0)

            caretModel.moveToLogicalPosition(position)
            editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        }
    }
}