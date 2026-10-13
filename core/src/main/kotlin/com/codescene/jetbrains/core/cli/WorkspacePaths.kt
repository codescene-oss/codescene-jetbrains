package com.codescene.jetbrains.core.cli

object WorkspacePaths {
    fun sameRoot(
        left: String,
        right: String,
        windows: Boolean,
    ): Boolean = normalizeRoot(left).equals(normalizeRoot(right), ignoreCase = windows)

    fun normalizeRel(path: String): String {
        var result = path.replace('\\', '/')
        while (result.startsWith("./")) result = result.removePrefix("./")
        return result
    }

    private fun normalizeRoot(root: String): String {
        val slashed = root.replace('\\', '/')
        return slashed.trimEnd('/').ifEmpty { slashed.take(1) }
    }
}
