package com.codescene.jetbrains.core.git

import java.io.File

const val MAX_UNTRACKED_FILES_PER_LOCATION = 5

val supportedExtensions =
    listOf(
        ".js",
        ".jsx",
        ".ts",
        ".tsx",
        ".java",
        ".kt",
        ".kts",
        ".py",
        ".rb",
        ".php",
        ".c",
        ".cpp",
        ".cc",
        ".cxx",
        ".h",
        ".hpp",
        ".cs",
        ".go",
        ".rs",
        ".swift",
        ".scala",
        ".clj",
        ".cljs",
        ".cljc",
        ".ex",
        ".exs",
        ".erl",
        ".hrl",
        ".ml",
        ".mli",
        ".fs",
        ".fsx",
        ".hs",
        ".lhs",
        ".elm",
        ".dart",
        ".lua",
        ".pl",
        ".r",
        ".sh",
        ".bash",
        ".zsh",
        ".sql",
        ".groovy",
        ".gradle",
    )

fun shouldReviewFile(filePath: String): Boolean {
    val fileExt = File(filePath).extension
    return supportedExtensions.any { it.substring(1) == fileExt }
}
