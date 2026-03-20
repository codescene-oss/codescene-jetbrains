package com.codescene.jetbrains.platform.di

import com.codescene.jetbrains.core.contracts.IAceRefactorableFunctionsCache
import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.IEditorService
import com.codescene.jetbrains.core.contracts.IFileSystem
import com.codescene.jetbrains.core.contracts.IGitService
import com.codescene.jetbrains.core.contracts.INotificationService
import com.codescene.jetbrains.core.contracts.IProgressService
import com.codescene.jetbrains.core.contracts.IReviewCacheService
import com.codescene.jetbrains.core.contracts.ISettingsProvider
import com.codescene.jetbrains.core.contracts.ITelemetryService
import com.codescene.jetbrains.core.contracts.IUIRefreshService
import com.codescene.jetbrains.platform.delta.PlatformDeltaCacheService
import com.codescene.jetbrains.platform.editor.IntelliJEditorService
import com.codescene.jetbrains.platform.editor.UIRefreshService
import com.codescene.jetbrains.platform.git.Git4IdeaGitService
import com.codescene.jetbrains.platform.notifications.IntelliJNotificationService
import com.codescene.jetbrains.platform.progress.IntelliJProgressService
import com.codescene.jetbrains.platform.review.PlatformAceRefactorableFunctionsCacheService
import com.codescene.jetbrains.platform.review.PlatformReviewCacheService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class CodeSceneProjectServiceProvider(
    private val project: Project,
) {
    val settingsProvider: ISettingsProvider by lazy {
        CodeSceneApplicationServiceProvider.getInstance().settingsProvider
    }
    val telemetryService: ITelemetryService by lazy {
        CodeSceneApplicationServiceProvider.getInstance().telemetryService
    }
    val gitService: IGitService by lazy { project.service<Git4IdeaGitService>() }
    val editorService: IEditorService by lazy { project.service<IntelliJEditorService>() }
    val fileSystem: IFileSystem by lazy {
        CodeSceneApplicationServiceProvider.getInstance().fileSystem
    }
    val notificationService: INotificationService by lazy { project.service<IntelliJNotificationService>() }
    val reviewCacheService: IReviewCacheService by lazy { project.service<PlatformReviewCacheService>() }
    val deltaCacheService: IDeltaCacheService by lazy { project.service<PlatformDeltaCacheService>() }
    val aceRefactorableFunctionsCache: IAceRefactorableFunctionsCache by lazy {
        project.service<PlatformAceRefactorableFunctionsCacheService>()
    }
    val uiRefreshService: IUIRefreshService by lazy { project.service<UIRefreshService>() }
    val progressService: IProgressService by lazy { project.service<IntelliJProgressService>() }

    companion object {
        fun getInstance(project: Project): CodeSceneProjectServiceProvider =
            project.service<CodeSceneProjectServiceProvider>()
    }
}
