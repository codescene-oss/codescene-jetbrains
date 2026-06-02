package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.handler.isCwfLocalFilePathAllowed
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import git4idea.repo.GitRepositoryManager
import java.nio.file.Paths

fun cwfAllowedLocalFileRoots(project: Project): Set<String> {
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

fun isCwfLocalFilePathAllowedForProject(
    project: Project,
    filePath: String,
): Boolean = isCwfLocalFilePathAllowed(filePath, cwfAllowedLocalFileRoots(project))
