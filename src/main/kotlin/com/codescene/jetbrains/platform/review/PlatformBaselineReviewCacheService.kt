package com.codescene.jetbrains.platform.review

import com.codescene.jetbrains.core.contracts.IBaselineReviewCacheService
import com.codescene.jetbrains.core.review.BaselineReviewCacheService
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class PlatformBaselineReviewCacheService : BaselineReviewCacheService(Log), IBaselineReviewCacheService {
    companion object {
        fun getInstance(project: Project): PlatformBaselineReviewCacheService =
            project.service<PlatformBaselineReviewCacheService>()
    }
}
