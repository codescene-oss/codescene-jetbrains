package com.codescene.jetbrains.core.git

private fun hasWindowsDriveLetter(path: String): Boolean = path.length >= 3 && path[1] == ':' && path[2] == '/'

fun pathComparisonKey(path: String): String {
    val normalized = path.replace('\\', '/')
    return if (hasWindowsDriveLetter(normalized)) normalized.lowercase() else normalized
}

fun pathCacheKey(path: String): String = pathComparisonKey(path)

fun pathFileName(path: String): String = path.replace('\\', '/').substringAfterLast('/')

const val LOG_FULL_PATHS_PROPERTY = "codescene.diagnostics.logFullPaths"

fun logFullPathsEnabled(): Boolean =
    System.getProperty(LOG_FULL_PATHS_PROPERTY)?.equals("true", ignoreCase = true) == true

fun pathForLog(path: String): String = if (logFullPathsEnabled()) path else pathFileName(path)

fun isPathUnderRoot(
    path: String,
    rootPath: String,
): Boolean {
    val root = pathComparisonKey(rootPath).trimEnd('/')
    val file = pathComparisonKey(path)
    return file == root || file.startsWith("$root/")
}
