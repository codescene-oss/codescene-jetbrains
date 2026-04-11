package com.codescene.jetbrains.core.git

import java.io.File
import java.nio.file.Paths

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
