package com.codescene.jetbrains.util

import com.codescene.jetbrains.data.ChangeDetails
import com.codescene.jetbrains.data.Function

private fun pluralize(word: String, amount: Int) = if (amount > 1) "${word}s" else word

private fun MutableList<String>.addIssueInformation(details: List<ChangeDetails>) {
    val codeSmells = details.size

    this.add("Contains $codeSmells ${pluralize("issue", codeSmells)} degrading code health")
}

fun getFunctionDeltaTooltip(function: Function, details: List<ChangeDetails>): String {
    val tooltip = mutableListOf("Function \"${function.name}\"")

    tooltip.addIssueInformation(details)
    //TODO: ACE information

    return tooltip.joinToString(separator = " • ")
}