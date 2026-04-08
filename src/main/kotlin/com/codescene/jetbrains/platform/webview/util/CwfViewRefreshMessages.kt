package com.codescene.jetbrains.platform.webview.util

import com.codescene.jetbrains.core.flag.RuntimeFlags
import com.codescene.jetbrains.core.mapper.AceAcknowledgementMapper
import com.codescene.jetbrains.core.mapper.DocumentationMapper
import com.codescene.jetbrains.core.models.CwfData
import com.codescene.jetbrains.core.models.view.AceAcknowledgeData
import com.codescene.jetbrains.core.util.autoRefactorConfigForDocsView
import com.codescene.jetbrains.core.util.parseMessage
import com.codescene.jetbrains.core.util.toAutoRefactorConfig
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.intellij.openapi.project.Project

fun docsRefreshMessage(project: Project): String? {
    val raw = getDocsUserData(project) ?: return null
    val settings = CodeSceneApplicationServiceProvider.getInstance().settingsProvider.currentState()
    val refactorTargetPresent = getDocsFnToRefactor(project) != null
    val enriched =
        raw.copy(
            autoRefactor =
                autoRefactorConfigForDocsView(
                    settings,
                    raw.docType,
                    refactorTargetPresent,
                ),
        )
    return DocumentationMapper().toMessage(enriched, devmode = RuntimeFlags.isDevMode)
}

fun aceAcknowledgeRefreshMessage(project: Project): String? {
    val holder = getAceAcknowledgeUserData(project) ?: return null
    val filePath = holder.aceAcknowledgeData.fileData?.fileName ?: return null
    val mapper = AceAcknowledgementMapper()
    val cwfData =
        mapper.toCwfData(
            filePath = filePath,
            fnToRefactor = holder.fnToRefactor,
            autoRefactorConfig =
                toAutoRefactorConfig(
                    CodeSceneApplicationServiceProvider.getInstance().settingsProvider.currentState(),
                ),
            devmode = RuntimeFlags.isDevMode,
        )
    return parseMessage(
        mapper = { cwfData },
        serializer = CwfData.serializer(AceAcknowledgeData.serializer()),
    )
}
