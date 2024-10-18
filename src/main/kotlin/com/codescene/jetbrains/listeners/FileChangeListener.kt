package com.codescene.jetbrains.listeners

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class DocumentChangeListenerStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val editorFactory = EditorFactory.getInstance()

        println("Registering document listener...")

        editorFactory.eventMulticaster.addDocumentListener(documentListener, project)
    }

    private val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            println("Document changed: ${event.document}")
        }
    }
}