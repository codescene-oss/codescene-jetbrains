package com.codescene.jetbrains.core.util

import java.io.File

private val supportedLanguages =
    mapOf(
        "js" to "javascript",
        "mjs" to "javascript",
        "sj" to "javascript",
        "jsx" to "javascriptreact",
        "ts" to "typescript",
        "tsx" to "typescriptreact",
        "brs" to "brightscript",
        "bs" to "brighterscript",
        "cls" to "apex",
        "tgr" to "apex",
        "trigger" to "apex",
        "c" to "c",
        "clj" to "clojure",
        "cljc" to "clojure",
        "cljs" to "clojure",
        "cc" to "cpp",
        "cpp" to "cpp",
        "cxx" to "cpp",
        "h" to "cpp",
        "hh" to "cpp",
        "hpp" to "cpp",
        "hxx" to "cpp",
        "ipp" to "cpp",
        "m" to "objective-c",
        "mm" to listOf("objective-c", "objective-cpp"),
        "cs" to "csharp",
        "erl" to "erlang",
        "go" to "go",
        "groovy" to "groovy",
        "java" to "java",
        "kt" to "kotlin",
        "php" to "php",
        "pm" to listOf("perl", "perl6"),
        "pl" to listOf("perl", "perl6"),
        "ps1" to "powershell",
        "psd1" to "powershell",
        "psm1" to "powershell",
        "py" to "python",
        "rb" to "ruby",
        "rs" to "rust",
        "swift" to "swift",
        "vb" to "vb",
        "vue" to "vue",
        "dart" to "dart",
        "scala" to "scala",
    )

fun readGitignore(projectPath: String?): List<String> {
    val gitignoreFile = File(projectPath, ".gitignore")

    return if (gitignoreFile.exists()) {
        gitignoreFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
    } else {
        emptyList()
    }
}

fun isSupportedLanguage(extension: String) = supportedLanguages.containsKey(extension)

fun isFileSupportedForAnalysis(
    extension: String?,
    inProjectContent: Boolean,
    excludeGitignoreFiles: Boolean,
    ignoredByGitignore: Boolean,
): Boolean {
    val supportedExtension = extension?.let(::isSupportedLanguage) == true
    val excludedByGitignore = excludeGitignoreFiles && ignoredByGitignore
    return supportedExtension && !excludedByGitignore && inProjectContent
}

fun formatCodeSmellMessage(
    category: String,
    details: String,
): String = if (details.isNotEmpty()) "$category ($details)" else category

fun linePairToOffsets(
    startLineOneBased: Int,
    endLineOneBased: Int,
    lineStartOffset: (lineIndex0Based: Int) -> Int,
    lineEndOffset: (lineIndex0Based: Int) -> Int,
): Pair<Int, Int> {
    val start = lineStartOffset(startLineOneBased - 1)
    val end = lineEndOffset(endLineOneBased - 1)
    return start to end
}
