package com.codescene.jetbrains.platform.webview.util

import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.webview.WebViewInitializer
import com.codescene.jetbrains.platform.webview.handler.CwfMessageHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project

fun updateDocs(project: Project) {
    ApplicationManager.getApplication().invokeLater {
        if (project.isDisposed) return@invokeLater

        val browser = WebViewInitializer.getInstance(project).getBrowser(View.DOCS) ?: return@invokeLater

        val message = docsRefreshMessage(project) ?: return@invokeLater

        Log.info("Updating docs view for project '${project.name}'", "UpdateDocs")
        CwfMessageHandler.getInstance(project).postMessage(View.DOCS, message, browser)
    }
}
