package com.codescene.jetbrains.core.cli

object WatchScopes {
    fun scopes(
        gitRoots: List<String>,
        contentRoots: List<String>,
        windows: Boolean,
    ): Map<String, List<String>> {
        if (contentRoots.isEmpty() && gitRoots.isNotEmpty()) {
            return gitRoots.associateWith { listOf(".") }
        }
        val pathsByRoot = linkedMapOf<String, MutableList<String>>()
        gitRoots.forEach { pathsByRoot[it] = mutableListOf() }
        contentRoots.forEach { contentRoot ->
            addContentRoot(gitRoots, contentRoot, windows, pathsByRoot)
        }
        return buildMap {
            for (gitRoot in gitRoots) {
                val collapsed = collapse(pathsByRoot.getValue(gitRoot), windows)
                if (collapsed.isNotEmpty()) put(gitRoot, collapsed)
            }
        }
    }

    fun collapse(
        paths: Collection<String>,
        windows: Boolean,
    ): List<String> {
        val normalized = paths.map { WorkspacePaths.normalizeRel(it) }
        if (normalized.any { it == "." }) return listOf(".")
        val distinct = normalized.distinct().sorted()
        return distinct.filter { path ->
            distinct.none { other -> other != path && isUnder(other, path, windows) }
        }
    }

    private fun addContentRoot(
        gitRoots: List<String>,
        contentRoot: String,
        windows: Boolean,
        pathsByRoot: MutableMap<String, MutableList<String>>,
    ) {
        enclosingGitRoot(gitRoots, contentRoot, windows)?.let { gitRoot ->
            val rel = WorkspacePaths.relativeTo(gitRoot, contentRoot, windows)!!
            pathsByRoot.getValue(gitRoot).add(rel)
        }
        gitRoots.filter { WorkspacePaths.relativeTo(contentRoot, it, windows) != null }.forEach { gitRoot ->
            pathsByRoot.getValue(gitRoot).add(".")
        }
    }

    private fun enclosingGitRoot(
        gitRoots: List<String>,
        contentRoot: String,
        windows: Boolean,
    ): String? =
        gitRoots
            .filter { WorkspacePaths.relativeTo(it, contentRoot, windows) != null }
            .maxByOrNull { WorkspacePaths.rootKey(it, windows).length }

    private fun isUnder(
        parent: String,
        child: String,
        windows: Boolean,
    ): Boolean {
        val parentCmp = if (windows) parent.lowercase() else parent
        val childCmp = if (windows) child.lowercase() else child
        return childCmp.startsWith("$parentCmp/")
    }
}
