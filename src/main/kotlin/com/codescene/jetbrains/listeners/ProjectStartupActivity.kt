package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileManager

class ProjectStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val disposable = project as Disposable

        VirtualFileManager.getInstance().addAsyncFileListener(FileChangeListener(project), disposable)

        Log.debug("FileEventListener registered for project: ${project.name}")
    }
}