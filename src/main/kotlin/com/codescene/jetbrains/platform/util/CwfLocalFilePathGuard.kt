package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.handler.isCwfLocalFilePathAllowed
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import git4idea.repo.GitRepositoryManager
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
class CwfLocalFilePathGuardService(
    private val project: Project,
) : Disposable {
    @Volatile
    private var cachedRoots: Set<String>? = null

    private val connection = project.messageBus.connect(this)

    init {
        connection.subscribe(
            GitRepository.GIT_REPO_CHANGE,
            GitRepositoryChangeListener { invalidate() },
        )
    }

    fun invalidate() {
        cachedRoots = null
    }

    fun allowedRoots(): Set<String> = cachedRoots ?: computeAllowedRoots().also { cachedRoots = it }

    fun isAllowed(filePath: String): Boolean = isCwfLocalFilePathAllowed(filePath, allowedRoots())

    private fun computeAllowedRoots(): Set<String> {
        val roots = linkedSetOf<String>()
        project.basePath?.let { base ->
            roots.add(Paths.get(base).toAbsolutePath().normalize().toString())
        }
        runReadAction {
            GitRepositoryManager.getInstance(project).repositories.forEach { repository ->
                roots.add(Paths.get(repository.root.path).toAbsolutePath().normalize().toString())
            }
        }
        return roots
    }

    override fun dispose() {
        connection.disconnect()
        cachedRoots = null
    }

    companion object {
        fun getInstance(project: Project): CwfLocalFilePathGuardService = project.service()
    }
}

fun isCwfLocalFilePathAllowedForProject(
    project: Project,
    filePath: String,
): Boolean = CwfLocalFilePathGuardService.getInstance(project).isAllowed(filePath)
