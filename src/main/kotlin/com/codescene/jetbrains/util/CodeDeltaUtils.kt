package com.codescene.jetbrains.util

import com.codescene.ExtensionAPI
import com.codescene.data.delta.Delta
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.config.global.MonitorTreeSortOptions
import com.codescene.jetbrains.flag.RuntimeFlags
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.api.AceService
import com.codescene.jetbrains.services.api.deltamodels.DeltaChangeDetail
import com.codescene.jetbrains.services.api.deltamodels.DeltaFunction
import com.codescene.jetbrains.services.api.deltamodels.DeltaMapper
import com.codescene.jetbrains.services.api.deltamodels.NativeDelta
import com.codescene.jetbrains.services.cache.DeltaCacheQuery
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.StringUtil.pluralize
import java.util.concurrent.ConcurrentHashMap

private fun MutableList<String>.countFixesAndDegradations(details: List<DeltaChangeDetail>) {
    val fixesAndDegradations =
        details.groupingBy { isPositiveChange(it.changeType) }
            .eachCount()

    fixesAndDegradations[true]?.let { add("$it ${pluralize("issue", it)} fixed") }
    fixesAndDegradations[false]?.let { add("$it ${pluralize("issue", it)} degrading code health") }
}

fun getFunctionDeltaTooltip(function: DeltaFunction?, details: List<DeltaChangeDetail>): String {
    val tooltip = mutableListOf("Function \"${function?.name}\"")

    tooltip.countFixesAndDegradations(details)
    //TODO: ACE information

    return tooltip.joinToString(separator = " • ")
}

fun getCachedDelta(editor: Editor): Pair<Boolean, NativeDelta?> {
    val project = editor.project!!

    val oldCode = GitService.getInstance(project).getBranchCreationCommitCode(editor.virtualFile)
    val cacheQuery = DeltaCacheQuery(editor.virtualFile.path, oldCode, editor.document.text)

    return DeltaCacheService.getInstance(project)
        .get(cacheQuery)
}

fun addRefactorableFunctionsToDeltaResult(
    path: String,
    currentCode: String,
    delta: Delta?,
    editor: Editor
): NativeDelta? {
    delta ?: return null
    val refactorableFunctions =
        if (RuntimeFlags.aceFeature && CodeSceneGlobalSettingsStore.getInstance().state.enableAutoRefactor)
            AceService.getInstance()
                .getRefactorableFunctions(ExtensionAPI.CodeParams(currentCode, path), delta, editor)
        else emptyList()

    return DeltaMapper.fromOriginal(delta, refactorableFunctions)
}

fun extractFileName(input: String): String? {
    val regex = """<html>([^<]+)<span""".toRegex()

    val matchResult = regex.find(input)

    return matchResult?.groups?.get(1)?.value
}

fun sortDeltaFindings(
    map: ConcurrentHashMap<String, NativeDelta>
): List<Map.Entry<String, NativeDelta>> {
    val entryList = map.entries.toList()
    val sortingCriteria = CodeSceneGlobalSettingsStore.getInstance().state.monitorTreeSortOption

    return when (sortingCriteria) {
        MonitorTreeSortOptions.FILE_NAME -> entryList.sortedBy { it.key }
        MonitorTreeSortOptions.SCORE_ASCENDING -> entryList.sortedByDescending { it.value.oldScore - it.value.newScore }
        MonitorTreeSortOptions.SCORE_DESCENDING -> entryList.sortedBy { it.value.oldScore - it.value.newScore }
    }
}