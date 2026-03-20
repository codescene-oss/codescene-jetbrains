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

fun formatCodeSmellMessage(
    category: String,
    details: String,
): String = if (details.isNotEmpty()) "$category ($details)" else category
