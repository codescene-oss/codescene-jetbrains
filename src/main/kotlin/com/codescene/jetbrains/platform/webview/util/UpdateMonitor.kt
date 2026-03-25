package com.codescene.jetbrains.platform.webview.util

import com.codescene.jetbrains.core.flag.RuntimeFlags
import com.codescene.jetbrains.core.mapper.CodeHealthMonitorMapper
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.review.AceRefactorableFunctionCacheQuery
import com.codescene.jetbrains.core.util.resolveFunctionToRefactor
import com.codescene.jetbrains.core.util.toAutoRefactorConfig
import com.codescene.jetbrains.platform.api.CachedReviewService
import com.codescene.jetbrains.platform.delta.PlatformDeltaCacheService
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.icons.CodeSceneIcons.CODESCENE_TW
import com.codescene.jetbrains.platform.review.PlatformAceRefactorableFunctionsCacheService
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.UpdateToolWindowIconParams
import com.codescene.jetbrains.platform.util.updateToolWindowIcon
import com.codescene.jetbrains.platform.webview.handler.CwfMessageHandler
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
private val codeHealthMonitorMapper = CodeHealthMonitorMapper()

fun updateMonitor(project: Project) {
    Log.info("Updating monitor for project '${project.name}'...")

    val services = CodeSceneProjectServiceProvider.getInstance(project)
    val deltaResults = PlatformDeltaCacheService.getInstance(project).getAll()
    val activeJobs = CachedReviewService.getInstance(project).activeReviewCalls.toList()

    val update =
        codeHealthMonitorMapper.buildUpdate(
            deltaResults = deltaResults,
            activeJobs = activeJobs,
            functionToRefactorResolver = { filePath, contentSha, fn ->
                val cache = PlatformAceRefactorableFunctionsCacheService.getInstance(project)
                val candidates = cache.get(AceRefactorableFunctionCacheQuery(filePath, contentSha))
                resolveFunctionToRefactor(candidates, fn)
            },
            autoRefactorConfig = toAutoRefactorConfig(services.settingsProvider.currentState()),
            devmode = RuntimeFlags.isDevMode,
        )

    updateToolWindowIcon(
        UpdateToolWindowIconParams(
            project = project,
            baseIcon = CODESCENE_TW,
            toolWindowId = "CodeScene",
            hasNotification = update.hasNotification,
        ),
    )
    CwfMessageHandler.getInstance(project).postMessage(View.HOME, update.message)
}
