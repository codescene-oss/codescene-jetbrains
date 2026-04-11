package com.codescene.jetbrains.platform.git

import com.intellij.openapi.project.Project
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.repo.GitRepository

class Git4IdeaCommandExecutor(private val project: Project) : GitCommandExecutor {
    override fun runDiff(
        repository: GitRepository,
        baseCommit: String,
    ): List<String> {
        val handler =
            createGitLineHandler(project, repository.root, GitCommand.DIFF).apply {
                addParameters("--name-only")
                addParameters("$baseCommit...HEAD")
            }
        val result = Git.getInstance().runCommand(handler)
        return if (result.success()) result.output else emptyList()
    }

    override fun runRevParse(repository: GitRepository): String? {
        val handler =
            createGitLineHandler(project, repository.root, GitCommand.REV_PARSE).apply {
                addParameters("HEAD")
            }
        val result = Git.getInstance().runCommand(handler)
        return if (result.success() && result.output.isNotEmpty()) {
            result.output.first().trim()
        } else {
            null
        }
    }

    override fun runMergeBase(
        repository: GitRepository,
        rev1: String,
        rev2: String,
    ): String? {
        val handler =
            createGitLineHandler(project, repository.root, GitCommand.MERGE_BASE).apply {
                addParameters(rev1, rev2)
            }
        val result = Git.getInstance().runCommand(handler)
        return if (result.success()) {
            result.output.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }
    }
}
