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

    override fun runIsAncestor(
        repository: GitRepository,
        ancestor: String,
        descendant: String,
    ): Boolean {
        val handler =
            createGitLineHandler(project, repository.root, GitCommand.MERGE_BASE).apply {
                addParameters("--is-ancestor", ancestor, descendant)
            }
        return Git.getInstance().runCommand(handler).success()
    }

    override fun resolveOriginHeadBranch(repository: GitRepository): String? {
        val handler =
            createGitLineHandler(project, repository.root, GitCommand.REV_PARSE).apply {
                addParameters("--abbrev-ref", "refs/remotes/origin/HEAD")
            }
        val result = Git.getInstance().runCommand(handler)
        if (!result.success() || result.output.isEmpty()) return null
        val ref = result.output.first().trim()
        val originPrefix = "origin/"
        return when {
            ref.startsWith(originPrefix) -> {
                val stripped = ref.removePrefix(originPrefix)
                stripped.takeIf { it.isNotEmpty() && it != "HEAD" }
            }
            ref == "HEAD" || ref.isEmpty() -> null
            else -> ref
        }
    }

    override fun localBranchNames(repository: GitRepository): Set<String> {
        val handler =
            createGitLineHandler(project, repository.root, GitCommand.BRANCH).apply {
                addParameters("--format=%(refname:short)")
            }
        val result = Git.getInstance().runCommand(handler)
        if (!result.success()) return emptySet()
        return result.output
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
