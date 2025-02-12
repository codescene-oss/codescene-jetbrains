package com.codescene.jetbrains.util

import com.codescene.data.delta.ChangeDetail
import com.codescene.data.delta.Delta
import com.codescene.data.delta.Function
import com.codescene.jetbrains.services.GitService
import com.codescene.jetbrains.services.cache.DeltaCacheQuery
import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.StringUtil.pluralize

private fun MutableList<String>.countFixesAndDegradations(details: List<ChangeDetail>) {
    val fixesAndDegradations = details.groupingBy { isPositiveChange(it.changeType) }.eachCount()

    fixesAndDegradations[true]?.let { add("$it ${pluralize("issue", it)} fixed") }
    fixesAndDegradations[false]?.let { add("$it ${pluralize("issue", it)} degrading code health") }
}

fun getFunctionDeltaTooltip(function: Function, details: List<ChangeDetail>): String {
    val tooltip = mutableListOf("Function \"${function.name}\"")

    tooltip.countFixesAndDegradations(details)
    //TODO: ACE information

    return tooltip.joinToString(separator = " • ")
}

fun getCachedDelta(editor: Editor): Delta? {
    val project = editor.project!!

    val oldCode = GitService.getInstance(project).getHeadCommit(editor.virtualFile)
    val cacheQuery = DeltaCacheQuery(editor.virtualFile.path, oldCode, editor.document.text)

    return DeltaCacheService.getInstance(project)
        .get(cacheQuery)
}