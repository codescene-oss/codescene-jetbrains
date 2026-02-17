package com.codescene.jetbrains.util

import com.codescene.data.delta.ChangeDetail
import com.codescene.data.delta.Delta
import com.codescene.data.delta.Function
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.config.global.MonitorTreeSortOptions
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.cache.DeltaCacheQuery
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.StringUtil.pluralize
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out. Remove tests as well.
private fun MutableList<String>.countFixesAndDegradations(details: List<ChangeDetail>) {
    val fixesAndDegradations = details.groupingBy { isPositiveChange(it.changeType) }.eachCount()

    fixesAndDegradations[true]?.let { add("$it ${pluralize("issue", it)} fixed") }
    fixesAndDegradations[false]?.let { add("$it ${pluralize("issue", it)} degrading code health") }
}

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
fun getFunctionDeltaTooltip(function: Function, details: List<ChangeDetail>): String {
    val tooltip = mutableListOf("Function \"${function.name}\"")

    tooltip.countFixesAndDegradations(details)
    //TODO: ACE information

    return tooltip.joinToString(separator = " • ")
}

fun getCachedDelta(editor: Editor): Pair<Boolean, Delta?> {
    val project = editor.project!!

    val oldCode = GitService.getInstance(project).getBranchCreationCommitCode(editor.virtualFile)
    val cacheQuery = DeltaCacheQuery(editor.virtualFile.path, oldCode, editor.document.text)

    return DeltaCacheService.getInstance(project)
        .get(cacheQuery)
}

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
fun sortDeltaFindings(
    map: ConcurrentHashMap<String, Delta>
): List<Map.Entry<String, Delta>> {
    val entryList = map.entries.toList()
    val sortingCriteria = CodeSceneGlobalSettingsStore.getInstance().state.monitorTreeSortOption

    return when (sortingCriteria) {
        MonitorTreeSortOptions.FILE_NAME -> entryList.sortedBy { it.key }
        MonitorTreeSortOptions.SCORE_ASCENDING -> entryList.sortedByDescending { safeScoreDelta(it.value.newScore, it.value.oldScore) }
        MonitorTreeSortOptions.SCORE_DESCENDING -> entryList.sortedBy { safeScoreDelta(it.value.newScore, it.value.oldScore) }
    }
}

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
private fun safeScoreDelta(newScore: Optional<Double>, oldScore: Optional<Double>): Double {
    val old = oldScore.orElse(null)
    val new = newScore.orElse(null)

    return if (old != null && new != null) old - new else Double.NEGATIVE_INFINITY
}

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
fun extractFileName(input: String): String? {
    val regex = """<html>([^<]+)<span""".toRegex()

    val matchResult = regex.find(input)

    return matchResult?.groups?.get(1)?.value
}
