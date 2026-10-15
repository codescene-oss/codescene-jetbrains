package com.codescene.jetbrains.platform.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class CodeSceneProjectLifetime : Disposable {
    override fun dispose() = Unit

    companion object {
        fun getInstance(project: Project): CodeSceneProjectLifetime = project.service()
    }
}
