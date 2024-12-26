package com.codescene.jetbrains.util

import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_TW
import com.codescene.jetbrains.components.codehealth.detail.CodeHealthDetailsPanel
import com.codescene.jetbrains.components.codehealth.monitor.CodeHealthMonitorPanel.Companion.healthMonitoringResults
import com.codescene.jetbrains.components.codehealth.monitor.tree.CodeHealthFinding
import com.codescene.jetbrains.data.CodeDelta
import com.codescene.jetbrains.notifier.CodeHealthDetailsRefreshNotifier
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.ui.JBUI

fun handleChange(project: Project) {
    val details = CodeHealthDetailsPanel.details!!
    val results = healthMonitoringResults[details.filePath]

    if (results != null) {
        val data = when (CodeHealthDetailsPanel.details?.type) {
            CodeHealthDetailsType.FILE -> getFileDetails(details, results)
            CodeHealthDetailsType.HEALTH -> getHealthDetails(details, results)
            CodeHealthDetailsType.FUNCTION -> getFunctionDetails(details, results)
            null -> null
        }

        data?.let { project.messageBus.syncPublisher(CodeHealthDetailsRefreshNotifier.TOPIC).refresh(data) }
    }
}

private fun getFunctionDetails(details: CodeHealthDetails, result: CodeDelta): CodeHealthFinding? {
    val finding = result.functionLevelFindings.find { it.function.name == details.header }

    if (finding != null) {
        return getFunctionFinding(details.filePath, finding.function, finding.changeDetails)
    }

    return null
}

private fun getHealthDetails(details: CodeHealthDetails, result: CodeDelta): CodeHealthFinding? {
    if (details.healthData?.score != round(result.newScore)) {
        return getHealthFinding(details.filePath, result)
    }

    return null
}

private fun getFileDetails(details: CodeHealthDetails, result: CodeDelta): CodeHealthFinding? {
    val finding = result.fileLevelFindings.find { it.category == details.header }

    if (finding != null) {
        return getFileFinding(details.filePath, finding)
    }

    return null
}

fun updateToolWindowIcon(project: Project) {
    val toolWindowManager = ToolWindowManager.getInstance(project)
    val toolWindow = toolWindowManager.getToolWindow(CODESCENE)

    if (toolWindow != null) {
        val originalIcon = CODESCENE_TW

        val notificationIcon = if (healthMonitoringResults.isNotEmpty())
            ExecutionUtil.getIndicator(originalIcon, 10, 10, JBUI.CurrentTheme.IconBadge.INFORMATION)
        else
            originalIcon

        toolWindow.setIcon(notificationIcon)
    }
}