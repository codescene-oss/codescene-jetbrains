package com.codescene.jetbrains.core.util

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
