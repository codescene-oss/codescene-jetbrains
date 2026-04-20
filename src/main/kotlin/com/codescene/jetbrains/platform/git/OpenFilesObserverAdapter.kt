package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.IOpenFilesObserver
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

class OpenFilesObserverAdapter(
    private val project: Project,
) : IOpenFilesObserver {
    override fun getAllVisibleFileNames(): Set<String> {
        val fileEditorManager = FileEditorManager.getInstance(project)
        return fileEditorManager.openFiles
            .mapNotNull { it.path }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
