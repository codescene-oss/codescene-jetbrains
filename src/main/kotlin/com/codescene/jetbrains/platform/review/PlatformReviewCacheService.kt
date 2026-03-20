package com.codescene.jetbrains.platform.review

import com.codescene.jetbrains.core.contracts.IReviewCacheService
import com.codescene.jetbrains.core.review.ReviewCacheService
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class PlatformReviewCacheService : ReviewCacheService(Log), IReviewCacheService {
    companion object {
        fun getInstance(project: Project): PlatformReviewCacheService = project.service<PlatformReviewCacheService>()
    }
}
