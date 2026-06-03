package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.git.MainLineBranchContext
import com.codescene.jetbrains.core.git.parseBaselineBranchFromConfig
import com.codescene.jetbrains.core.git.pathComparisonKey
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import git4idea.repo.GitRepository
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class MainLineBranchResolver
    @JvmOverloads
    constructor(
        private val project: Project,
        private val gitExecutor: GitCommandExecutor = Git4IdeaCommandExecutor(project),
    ) {
        private val cache = ConcurrentHashMap<String, MainLineBranchContext>()

        fun contextFor(repository: GitRepository): MainLineBranchContext {
            val cacheKey = pathComparisonKey(repository.root.path)
            return cache.getOrPut(cacheKey) { loadContext(repository) }
        }

        fun invalidate(gitRoot: String) {
            cache.remove(pathComparisonKey(gitRoot))
        }

        private fun loadContext(repository: GitRepository): MainLineBranchContext {
            val configPath = Paths.get(repository.root.path, ".codescene", "config.json")
            val configText =
                if (Files.isRegularFile(configPath)) {
                    Files.readString(configPath)
                } else {
                    null
                }
            return MainLineBranchContext(
                configuredBaseline = parseBaselineBranchFromConfig(configText),
                defaultBranchFromOriginHead = gitExecutor.resolveOriginHeadBranch(repository),
                localBranchNames = gitExecutor.localBranchNames(repository),
            )
        }

        companion object {
            fun getInstance(project: Project): MainLineBranchResolver = project.service<MainLineBranchResolver>()
        }
    }
