package com.codescene.jetbrains.core.git

import com.codescene.jetbrains.core.contracts.IGitChangeLister
import java.io.File

class TestGitChangeListerForCommitted(private val testRepoPath: File) : IGitChangeLister {
    override suspend fun getAllChangedFiles(
        gitRootPath: String,
        workspacePath: String,
        filesToExcludeFromHeuristic: Set<String>,
    ): Set<String> {
        val baseCommit = getMergeBase(gitRootPath) ?: return emptySet()
        if (baseCommit.isEmpty()) return emptySet()

        val output = exec("git", "diff", "--name-only", "$baseCommit...HEAD")
        if (output.isEmpty()) return emptySet()

        val files = mutableSetOf<String>()
        val (normalizedWorkspacePath, workspacePrefix) = createWorkspacePrefix(workspacePath)

        for (line in output.lines()) {
            val filePath = line.trim()
            if (filePath.isEmpty()) continue

            val absolutePath = File(gitRootPath, filePath).absolutePath

            if (absolutePath.startsWith(
                    workspacePrefix,
                ) && File(absolutePath).exists() && shouldReviewFile(absolutePath)
            ) {
                val relativeToWorkspace = convertGitPathToWorkspacePath(filePath, gitRootPath, normalizedWorkspacePath)
                files.add(relativeToWorkspace)
            }
        }

        return files
    }

    private fun exec(vararg command: String): String {
        val process =
            ProcessBuilder(*command)
                .directory(testRepoPath)
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return output
    }

    private fun getMergeBase(gitRootPath: String): String? {
        val currentBranch = exec("git", "rev-parse", "--abbrev-ref", "HEAD").trim()

        val mainBranchNames = listOf("main", "master", "develop", "trunk", "dev")
        if (mainBranchNames.any { it.equals(currentBranch, ignoreCase = true) }) {
            return exec("git", "rev-parse", "HEAD").trim()
        }

        for (mainName in mainBranchNames) {
            val refsToTry = listOf(mainName, "origin/$mainName")
            for (ref in refsToTry) {
                val output = exec("git", "merge-base", currentBranch, ref).trim()
                if (output.isNotEmpty() && !output.startsWith("fatal")) {
                    return output
                }
            }
        }

        return null
    }
}
