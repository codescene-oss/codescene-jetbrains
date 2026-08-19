package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.platform.api.CachedReviewService
import com.codescene.jetbrains.platform.util.isFileSupported
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project

object DocumentChangeReviewListener {
    fun install(
        project: Project,
        disposable: Disposable,
    ) {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    if (project.isDisposed) return
                    val file = FileDocumentManager.getInstance().getFile(event.document) ?: return
                    if (!isFileSupported(project, file)) return
                    val editor =
                        FileEditorManager.getInstance(project).getEditors(file)
                            .firstNotNullOfOrNull { (it as? TextEditor)?.editor }
                            ?: return
                    CachedReviewService.getInstance(project).review(editor)
                }
            },
            disposable,
        )
    }
}
