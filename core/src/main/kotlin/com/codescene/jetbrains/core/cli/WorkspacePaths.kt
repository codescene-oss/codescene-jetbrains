package com.codescene.jetbrains.core.cli

object WorkspacePaths {
    fun sameRoot(
        left: String,
        right: String,
        windows: Boolean,
    ): Boolean = rootKey(left, windows) == rootKey(right, windows)

    fun rootKey(
        root: String,
        windows: Boolean,
    ): String {
        val normalized = normalizeRoot(root)
        return if (windows) normalized.lowercase() else normalized
    }

    fun relativeTo(
        root: String,
        path: String,
        windows: Boolean,
    ): String? {
        val r = normalizeRoot(root)
        val p = normalizeRoot(path)
        val rCmp = if (windows) r.lowercase() else r
        val pCmp = if (windows) p.lowercase() else p
        if (rCmp == pCmp) return "."
        if (!pCmp.startsWith("$rCmp/")) return null
        return p.substring(r.length).removePrefix("/")
    }

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
