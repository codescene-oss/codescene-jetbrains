package com.codescene.jetbrains.core.util

fun adjustIndentation(
    anchorFirstLineText: String,
    newContent: String,
): String {
    val targetIndent = anchorFirstLineText.takeWhile { it.isWhitespace() }
    if (targetIndent.isEmpty()) return newContent

    val newContentLines = newContent.split("\n")

    val newContentIndent =
        newContentLines
            .filter { it.isNotBlank() }
            .map { it.takeWhile { ch -> ch.isWhitespace() } }
            .filter { it.isNotEmpty() }
            .minByOrNull { it.length }
            ?: ""

    val newContentFirstNonBlankLine = newContentLines.firstOrNull { it.isNotBlank() } ?: return newContent
    val shouldSkipAdditionalRepetition = newContentFirstNonBlankLine.firstOrNull()?.isWhitespace() ?: false

    return adjustLines(newContentLines, shouldSkipAdditionalRepetition, targetIndent, newContentIndent)
}

fun adjustIndentationOrOriginal(
    anchorFirstLineText: String?,
    newContent: String,
): String = anchorFirstLineText?.let { adjustIndentation(it, newContent) } ?: newContent

fun adjustLines(
    newContentLines: List<String>,
    shouldSkipAdditionalRepetition: Boolean,
    targetIndent: String,
    newContentIndent: String,
) = newContentLines.joinToString("\n") { line ->
    if (line.isBlank()) {
        line
    } else {
        val prefixRepeats = countPrefixRepeats(line, newContentIndent)
        val repetition = if (shouldSkipAdditionalRepetition) prefixRepeats else prefixRepeats + 1

        targetIndent.repeat(repetition) + line.trimStart()
    }
}

fun countPrefixRepeats(
    text: String,
    prefix: String,
): Int {
    if (prefix.isEmpty()) return 0

    var count = 0
    var index = 0

    while (text.startsWith(prefix, index)) {
        count++
        index += prefix.length
    }

    return count
}
