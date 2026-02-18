package com.codescene.jetbrains.components.window

import com.codescene.jetbrains.components.webview.WebViewFactory
import com.codescene.jetbrains.components.webview.data.View
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

internal class HomeToolWindowFactory : ToolWindowFactory {
    /**
     * Creates and sets up the content for the *Home* ToolWindow in the JetBrains IDE.
     *
     * The Home view hosts various extension-specific information, including:
     * - Code Health Monitor,
     * - Code smell details,
     * - Important links.
     *
     * @param project The current project within the IDE.
     * @param toolWindow The tool window where the content will be displayed.
     */
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        val content = WebViewFactory.createWebViewComponent(project, View.HOME)

        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}
