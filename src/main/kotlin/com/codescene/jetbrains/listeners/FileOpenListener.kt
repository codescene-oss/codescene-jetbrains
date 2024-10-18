package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.services.CodeSceneService
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FileOpenListener : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (file.isValid) {
            CoroutineScope(Dispatchers.IO).launch {
                CodeSceneService.getInstance(source.project).reviewCode(file)
            }
        }
    }
}