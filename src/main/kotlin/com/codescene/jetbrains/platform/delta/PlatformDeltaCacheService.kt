package com.codescene.jetbrains.platform.delta

import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.delta.DeltaCacheService
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class PlatformDeltaCacheService : DeltaCacheService(Log), IDeltaCacheService {
    companion object {
        fun getInstance(project: Project): PlatformDeltaCacheService = project.service<PlatformDeltaCacheService>()
    }
}
