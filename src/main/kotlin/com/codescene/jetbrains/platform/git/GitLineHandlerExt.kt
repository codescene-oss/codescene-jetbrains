package com.codescene.jetbrains.platform.git

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import java.io.File

fun createGitLineHandler(
    project: Project?,
    directory: File,
    command: GitCommand,
): GitLineHandler =
    GitLineHandler(project, directory, command).apply {
        addCustomEnvironmentVariable("GIT_OPTIONAL_LOCKS", "0")
    }

fun createGitLineHandler(
    project: Project?,
    vcsRoot: VirtualFile,
    command: GitCommand,
): GitLineHandler =
    GitLineHandler(project, vcsRoot, command).apply {
        addCustomEnvironmentVariable("GIT_OPTIONAL_LOCKS", "0")
    }
