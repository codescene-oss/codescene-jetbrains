package com.codescene.jetbrains.core.cli

object WatchGitRoots {
    fun union(
        repositoryRoots: Collection<String>,
        mappingRoots: Collection<String>,
        windows: Boolean,
    ): List<String> {
        val seen = linkedSetOf<String>()
        val ordered = mutableListOf<String>()
        for (root in repositoryRoots + mappingRoots) {
            val key = WorkspacePaths.rootKey(root, windows)
            if (seen.add(key)) {
                ordered.add(root)
            }
        }
        return ordered
    }
}
