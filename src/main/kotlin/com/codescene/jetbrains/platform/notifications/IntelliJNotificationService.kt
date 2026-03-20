package com.codescene.jetbrains.platform.notifications

import com.codescene.jetbrains.core.contracts.INotificationService
import com.codescene.jetbrains.platform.util.showErrorNotification
import com.codescene.jetbrains.platform.util.showInfoNotification
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class IntelliJNotificationService(private val project: Project) : INotificationService {
    companion object {
        fun getInstance(project: Project): IntelliJNotificationService = project.service<IntelliJNotificationService>()
    }

    override fun showInfo(message: String) {
        showInfoNotification(message, project)
    }

    override fun showError(message: String) {
        showErrorNotification(project, message)
    }
}
