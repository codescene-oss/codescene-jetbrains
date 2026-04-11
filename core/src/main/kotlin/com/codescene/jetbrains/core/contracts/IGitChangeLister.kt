package com.codescene.jetbrains.core.contracts

interface IGitChangeLister {
    suspend fun getAllChangedFiles(
        gitRootPath: String,
        workspacePath: String,
        filesToExcludeFromHeuristic: Set<String> = emptySet(),
    ): Set<String>
}
