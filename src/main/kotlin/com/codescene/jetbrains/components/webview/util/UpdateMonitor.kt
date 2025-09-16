package com.codescene.jetbrains.components.webview.util

import com.codescene.jetbrains.components.webview.data.CwfData
import com.codescene.jetbrains.components.webview.data.HomeData
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.handler.CwfMessageHandler
import com.codescene.jetbrains.components.webview.mapper.CodeHealthMonitorMapper
import com.codescene.jetbrains.services.api.CodeDeltaService
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.codescene.jetbrains.util.UpdateToolWindowIconParams
import com.codescene.jetbrains.util.updateToolWindowIcon
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project

/**
 * Updates the Code Health Monitor in the Home view (CWF).
 *
 * This method retrieves the latest delta analysis results from the
 * [DeltaCacheService], maps them to [CwfData] using
 * [CodeHealthMonitorMapper], serializes the data into a JSON string,
 * and posts it to the [CwfMessageHandler] for rendering in the UI.
 *
 * The JSON message is created using [parseMessage], which ensures the
 * correct serializer is used.
 */
fun updateMonitor(project: Project) {
    val mapper = CodeHealthMonitorMapper.getInstance()
    val deltaResults = DeltaCacheService.getInstance(project).getAll()
    val activeJobs = CodeDeltaService.getInstance(project).activeReviewCalls.map { it.key }

    val dataJson = parseMessage(
        mapper = { mapper.toCwfData(deltaResults, activeJobs) },
        serializer = CwfData.serializer(HomeData.serializer())
    )

    updateToolWindowIcon(
        UpdateToolWindowIconParams(
            project = project,
            toolWindowId = "CodeSceneHome", // TODO: update
            baseIcon = AllIcons.Actions.Lightning, // TODO: update
            hasNotification = deltaResults.isNotEmpty()
        )
    )
    CwfMessageHandler.getInstance(project).postMessage(View.HOME, dataJson)
}