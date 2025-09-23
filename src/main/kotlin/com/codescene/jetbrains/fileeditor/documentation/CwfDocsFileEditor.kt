package com.codescene.jetbrains.fileeditor.documentation

import com.codescene.jetbrains.components.webview.WebViewFactory
import com.codescene.jetbrains.components.webview.WebViewInitializer
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.data.view.DocsData
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import java.beans.PropertyChangeListener
import javax.swing.JComponent

internal class CwfDocsFileEditor(
    private val project: Project,
    private val file: VirtualFile,
    data: DocsData
) : UserDataHolderBase(), FileEditor {
    private val component: JComponent

    init {
        val content = WebViewFactory.createWebViewComponent(
            project = project,
            view = View.DOCS,
            initialData = data
        )
        component = content.component
    }

    override fun getComponent() = component

    override fun getFile() = file

    override fun getPreferredFocusedComponent() = component

    override fun getName(): String = file.nameWithoutExtension

    override fun setState(p0: FileEditorState) {
        // No implementation needed
    }

    override fun isModified() = false

    override fun isValid() = file.isValid

    override fun addPropertyChangeListener(p0: PropertyChangeListener) {
        // No implementation needed
    }

    override fun removePropertyChangeListener(p0: PropertyChangeListener) {
        // No implementation needed
    }

    override fun dispose() {
        WebViewInitializer.getInstance(project).unregisterBrowser(View.DOCS)
    }
}