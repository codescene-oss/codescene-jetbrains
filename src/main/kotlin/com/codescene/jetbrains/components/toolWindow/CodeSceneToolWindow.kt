package com.codescene.jetbrains.components.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel

class CodeSceneToolWindow(private val project: Project) {
    fun getContent() = JBPanel<JBPanel<*>>().apply {
        //TODO
    }
}