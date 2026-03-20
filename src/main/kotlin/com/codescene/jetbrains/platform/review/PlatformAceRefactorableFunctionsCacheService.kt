package com.codescene.jetbrains.platform.review

import com.codescene.jetbrains.core.contracts.IAceRefactorableFunctionsCache
import com.codescene.jetbrains.core.review.AceRefactorableFunctionsCacheService
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class PlatformAceRefactorableFunctionsCacheService :
    AceRefactorableFunctionsCacheService(Log),
    IAceRefactorableFunctionsCache {
    companion object {
        fun getInstance(project: Project): PlatformAceRefactorableFunctionsCacheService =
            project.service<PlatformAceRefactorableFunctionsCacheService>()
    }
}
