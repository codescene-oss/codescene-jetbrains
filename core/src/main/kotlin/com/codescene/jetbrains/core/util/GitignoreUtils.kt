package com.codescene.jetbrains.core.util

fun isExcludedByGitignore(
    fileExtension: String?,
    ignoredFiles: List<String>,
): Boolean = fileExtension != null && ignoredFiles.any { it.removePrefix(".") == fileExtension }
