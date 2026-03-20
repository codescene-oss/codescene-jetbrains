package com.codescene.jetbrains.platform.progress

import com.codescene.jetbrains.core.contracts.IProgressService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress

@Service(Service.Level.PROJECT)
class IntelliJProgressService(
    private val project: Project,
) : IProgressService {
    override suspend fun <T> runWithProgress(
        title: String,
        action: suspend () -> T,
    ): T =
        withBackgroundProgress(project, title, cancellable = false) {
            action()
        }

    companion object {
        fun getInstance(project: Project): IntelliJProgressService = project.service<IntelliJProgressService>()
    }
}
