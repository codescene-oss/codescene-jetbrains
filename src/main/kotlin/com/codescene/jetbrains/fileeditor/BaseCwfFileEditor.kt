package com.codescene.jetbrains.fileeditor

import com.codescene.jetbrains.components.webview.WebViewFactory
import com.codescene.jetbrains.components.webview.WebViewInitializer
import com.codescene.jetbrains.components.webview.data.View
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import java.beans.PropertyChangeListener
import javax.swing.JComponent

/**
 * Generic base class for CWF-based editors.
 *
 * @param T Type of the initial data.
 */
abstract class BaseCwfFileEditor<T>(
    protected val project: Project,
    private val cwfFile: VirtualFile,
    private val view: View,
    initialData: T,
) : UserDataHolderBase(), FileEditor {
    private val component: JComponent =
        WebViewFactory
            .createWebViewComponent(
                view = view,
                project = project,
                initialData = initialData,
            ).component

    override fun getFile(): VirtualFile = cwfFile

    override fun getName(): String = cwfFile.name

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = cwfFile.isValid

    override fun getComponent(): JComponent = component

    override fun getPreferredFocusedComponent(): JComponent = component

    override fun setState(p0: FileEditorState) {
        // No implementation needed
    }

    override fun addPropertyChangeListener(p0: PropertyChangeListener) {
        // No implementation needed
    }

    override fun removePropertyChangeListener(p0: PropertyChangeListener) {
        // No implementation needed
    }

    override fun dispose() {
        WebViewInitializer.getInstance(project).unregisterBrowser(view)
    }
}
