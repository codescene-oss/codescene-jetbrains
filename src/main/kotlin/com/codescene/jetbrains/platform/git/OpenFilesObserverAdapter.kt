package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.IOpenFilesObserver
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

class OpenFilesObserverAdapter(
    private val project: Project,
) : IOpenFilesObserver {
    override fun getAllVisibleFileNames(): Set<String> {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val result =
            fileEditorManager.openFiles
                .mapNotNull { it.path }
                .filter { it.isNotEmpty() }
                .toSet()
        Log.debug("Open files count=${result.size}", "OpenFilesObserver")
        return result
    }
}
