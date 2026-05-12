package com.codescene.jetbrains.core.git

import com.codescene.jetbrains.core.contracts.IGitChangeLister
import java.io.File

class TestGitChangeListerForStatus(private val testRepoPath: File) : IGitChangeLister {
    private val includedStatuses = setOf("A", "M", "R", "C", "AM", "MM", "??")

    override suspend fun getAllChangedFiles(
        gitRootPath: String,
        workspacePath: String,
        filesToExcludeFromHeuristic: Set<String>,
    ): Set<String> {
        val output = exec("git", "status", "--porcelain", "-z", "-uall")
        if (output.isEmpty()) return emptySet()

        val files = mutableSetOf<String>()
        val (normalizedWorkspacePath, workspacePrefix) = createWorkspacePrefix(workspacePath)
        val untrackedFilesByLocation = mutableMapOf<String, MutableList<String>>()

        val entries = output.split('\u0000').filter { it.isNotEmpty() }
        var i = 0
        while (i < entries.size) {
            val entry = entries[i]
            if (entry.length < 4) {
                i++
                continue
            }

            val status = entry.substring(0, 2)
            var filePath = entry.substring(3)

            if (!includedStatuses.contains(status.trim())) {
                i = skipRenameOrCopyOrigPath(status, i, entries)
                continue
            }

            i = skipRenameOrCopyOrigPath(status, i, entries)

            val absolutePath = File(gitRootPath, filePath).absolutePath

            if (!File(absolutePath).exists() || !absolutePath.startsWith(workspacePrefix)) {
                continue
            }

            if (!shouldReviewFile(absolutePath)) {
                continue
            }

            if (status == "??") {
                val dir = File(filePath).parent ?: "."
                val location = if (dir == ".") "__root__" else dir
                untrackedFilesByLocation.getOrPut(location) { mutableListOf() }.add(filePath)
            } else {
                files.add(absolutePath)
            }
        }

        for ((_, filesList) in untrackedFilesByLocation) {
            val shouldExclude = filesList.size > MAX_UNTRACKED_FILES_PER_LOCATION

            for (filePath in filesList) {
                val absolutePath = File(gitRootPath, filePath).absolutePath
                val shouldExcludeFromHeuristic = filesToExcludeFromHeuristic.contains(absolutePath)

                if ((!shouldExclude || shouldExcludeFromHeuristic) &&
                    File(
                        absolutePath,
                    ).exists() && shouldReviewFile(absolutePath)
                ) {
                    files.add(absolutePath)
                }
            }
        }

        return files
    }

    private fun skipRenameOrCopyOrigPath(
        status: String,
        currentIndex: Int,
        entries: List<String>,
    ): Int {
        var i = currentIndex
        if (status.startsWith('R') || status.startsWith('C')) {
            if (i + 1 < entries.size) {
                i++
            }
        }
        i++
        return i
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
}
