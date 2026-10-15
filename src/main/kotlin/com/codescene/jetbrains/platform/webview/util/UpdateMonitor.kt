package com.codescene.jetbrains.platform.webview.util

import com.codescene.jetbrains.core.flag.RuntimeFlags
import com.codescene.jetbrains.core.git.pathFileName
import com.codescene.jetbrains.core.mapper.CodeHealthMonitorMapper
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.util.resolveAceCandidatesForMonitor
import com.codescene.jetbrains.core.util.resolveFeatureFlags
import com.codescene.jetbrains.core.util.resolveFunctionToRefactor
import com.codescene.jetbrains.core.util.toAutoRefactorConfig
import com.codescene.jetbrains.platform.api.CachedReviewService
import com.codescene.jetbrains.platform.delta.PlatformDeltaCacheService
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.icons.CodeSceneIcons.CODESCENE_TW
import com.codescene.jetbrains.platform.review.PlatformAceRefactorableFunctionsCacheService
import com.codescene.jetbrains.platform.review.WorkspaceReviewService
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.UpdateToolWindowIconParams
import com.codescene.jetbrains.platform.util.updateToolWindowIcon
import com.codescene.jetbrains.platform.webview.handler.CwfMessageHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import git4idea.repo.GitRepositoryManager

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
private val codeHealthMonitorMapper = CodeHealthMonitorMapper(java.io.File.separatorChar)

fun updateMonitor(project: Project) {
    val queueTime = System.currentTimeMillis()
    ApplicationManager.getApplication().invokeLater {
        if (project.isDisposed) return@invokeLater
        val waitTime = System.currentTimeMillis() - queueTime
        Log.info("updateMonitor invokeLater executed after ${waitTime}ms queue delay", "UpdateMonitor")
        updateMonitorImpl(project)
    }
}

private fun updateMonitorImpl(project: Project) {
    Log.info("Updating monitor for project '${project.name}'...")
    val startTime = System.currentTimeMillis()

    val services = CodeSceneProjectServiceProvider.getInstance(project)
    val deltaResults = PlatformDeltaCacheService.getInstance(project).getAll()
    val runningPaths = CachedReviewService.getInstance(project).activeReviewCalls.toList()
    val progress = WorkspaceReviewService.getInstance(project).reviewProgressSnapshot()
    val repoRoots = GitRepositoryManager.getInstance(project).repositories.map { it.root.path }
    val candidatePaths =
        buildList {
            addAll(runningPaths)
            FileEditorManager.getInstance(project).openFiles.forEach { file -> add(file.path) }
        }

    val shortNames = runningPaths.map { pathFileName(it) }
    Log.info("Active jobs: $shortNames deltaResults=${deltaResults.size}", "UpdateMonitor")
    runningPaths.forEach { Log.info("activeJob path=$it", "UpdateMonitor") }
    deltaResults.forEach { (path, _) -> Log.info("deltaResult path=$path", "UpdateMonitor") }

    val settings = services.settingsProvider.currentState()
    val update =
        codeHealthMonitorMapper.buildUpdate(
            deltaResults = deltaResults,
            runningPaths = runningPaths,
            progress = progress,
            repoRoots = repoRoots,
            candidatePaths = candidatePaths,
            windows = SystemInfo.isWindows,
            functionToRefactorResolver = { filePath, contentSha, fn ->
                val cache = PlatformAceRefactorableFunctionsCacheService.getInstance(project)
                val candidates = resolveAceCandidatesForMonitor(cache, filePath, contentSha)
                resolveFunctionToRefactor(candidates, fn)
            },
            autoRefactorConfig = toAutoRefactorConfig(settings),
            featureFlags = resolveFeatureFlags(settings.aceTokenConfigured),
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
    markCodeHealthMonitorStateForUiTests(project, runningPaths.size, deltaResults.size)

    val elapsedTime = System.currentTimeMillis() - startTime
    Log.info("Completed updating monitor for project '${project.name}' in ${elapsedTime}ms", "UpdateMonitor")
}
