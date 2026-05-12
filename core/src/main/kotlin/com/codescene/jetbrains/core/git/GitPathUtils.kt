package com.codescene.jetbrains.core.git

import java.io.File
import java.nio.file.Paths

private fun hasWindowsDriveLetter(path: String): Boolean = path.length >= 3 && path[1] == ':' && path[2] == '/'

fun pathComparisonKey(path: String): String {
    val normalized = path.replace('\\', '/')
    return if (hasWindowsDriveLetter(normalized)) normalized.lowercase() else normalized
}

fun gitRelativeComparisonKey(
    gitRootPath: String,
    filePath: String,
): String {
    val root = pathComparisonKey(gitRootPath).trimEnd('/')
    val file = pathComparisonKey(filePath)
    val prefix = "$root/"
    return when {
        file == root -> ""
        file.startsWith(prefix) -> file.removePrefix(prefix)
        else -> file
    }
}

fun pathCacheKey(path: String): String = pathComparisonKey(path)

fun pathFileName(path: String): String = path.replace('\\', '/').substringAfterLast('/')

fun isPathUnderRoot(
    path: String,
    rootPath: String,
): Boolean {
    val root = pathComparisonKey(rootPath).trimEnd('/')
    val file = pathComparisonKey(path)
    return file == root || file.startsWith("$root/")
}

data class WorkspacePrefix(
    val normalizedWorkspacePath: String,
    val workspacePrefix: String,
)

fun createWorkspacePrefix(workspacePath: String): WorkspacePrefix {
    val normalizedWorkspacePath = Paths.get(workspacePath).toAbsolutePath().normalize().toString()
    val workspacePrefix =
        if (normalizedWorkspacePath.endsWith(File.separator)) {
            normalizedWorkspacePath
        } else {
            normalizedWorkspacePath + File.separator
        }
    return WorkspacePrefix(normalizedWorkspacePath, workspacePrefix)
}

fun isFileInWorkspace(
    file: String,
    gitRootPath: String,
    normalizedWorkspacePath: String,
    workspacePrefix: String,
): Boolean {
    val normalizedGitRootPath = Paths.get(gitRootPath).normalize().toString()
    val normalizedFile = file.replace('/', File.separatorChar)
    val absolutePath = Paths.get(normalizedGitRootPath, normalizedFile).toAbsolutePath().normalize().toString()

    if (!File(absolutePath).exists()) {
        return false
    }

    return absolutePath.startsWith(workspacePrefix) || absolutePath == normalizedWorkspacePath
}

fun convertGitPathToAbsolutePath(
    file: String,
    gitRootPath: String,
): String {
    val normalizedGitRootPath = Paths.get(gitRootPath).normalize().toString()
    val normalizedFile = file.replace('/', File.separatorChar)
    return Paths.get(normalizedGitRootPath, normalizedFile).toAbsolutePath().normalize().toString()
}

fun convertGitPathToWorkspacePath(
    file: String,
    gitRootPath: String,
    normalizedWorkspacePath: String,
): String {
    val absolutePath = convertGitPathToAbsolutePath(file, gitRootPath)
    val workspacePath = Paths.get(normalizedWorkspacePath)
    val filePath = Paths.get(absolutePath)
    return workspacePath.relativize(filePath).toString()
}
