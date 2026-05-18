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
                files.add(absolutePath)
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

        if (MAIN_LINE_BRANCH_NAMES.any { it.equals(currentBranch, ignoreCase = true) }) {
            return exec("git", "rev-parse", "HEAD").trim()
        }

        return resolveClosestMainLineMergeBase(
            isAncestor = { ancestor, descendant -> gitIsAncestor(ancestor, descendant) },
            mergeBaseForRef = { ref -> gitMergeBaseWithRef(currentBranch, ref) },
        )
    }

    private fun gitMergeBaseWithRef(
        currentBranch: String,
        ref: String,
    ): String? {
        val output = exec("git", "merge-base", currentBranch, ref).trim()
        return if (output.isNotEmpty() && !output.startsWith("fatal")) output else null
    }

    private fun gitIsAncestor(
        ancestor: String,
        descendant: String,
    ): Boolean {
        val process =
            ProcessBuilder("git", "merge-base", "--is-ancestor", ancestor, descendant)
                .directory(testRepoPath)
                .redirectErrorStream(true)
                .start()
        return process.waitFor() == 0
    }
}
