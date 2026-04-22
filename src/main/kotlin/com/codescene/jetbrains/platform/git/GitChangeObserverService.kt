package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.git.FileSystemAdapter
import com.codescene.jetbrains.core.review.FileEventHandler
import com.codescene.jetbrains.platform.api.CachedReviewService
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.webview.util.updateMonitor
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import git4idea.repo.GitRepositoryManager

@Service(Service.Level.PROJECT)
class GitChangeObserverService(
    private val project: Project,
) : Disposable {
    companion object {
        fun getInstance(project: Project): GitChangeObserverService = project.service<GitChangeObserverService>()
    }

    private var observer: GitChangeObserverAdapter? = null
    private var vfsEventBridge: VfsEventBridge? = null
    private var savedFilesTracker: SavedFilesTrackerAdapter? = null

    fun start() {
        val workspacePath = project.basePath ?: return
        val gitRootPath = resolveGitRootPath(workspacePath) ?: return

        val serviceProvider = CodeSceneProjectServiceProvider.getInstance(project)
        val fileEventHandler =
            FileEventHandler(
                serviceProvider.deltaCacheService,
                serviceProvider.reviewCacheService,
                serviceProvider.baselineReviewCacheService,
            )

        val tracker = SavedFilesTrackerAdapter(project)
        tracker.start()
        savedFilesTracker = tracker
        Disposer.register(this, tracker)

        val openFilesObserver = OpenFilesObserverAdapter(project)
        val gitChangeLister = Git4IdeaChangeLister.getInstance(project)
        val fileSystem = FileSystemAdapter()

        val gitChangeObserver =
            GitChangeObserverAdapter(
                gitChangeLister = gitChangeLister,
                savedFilesTracker = tracker,
                openFilesObserver = openFilesObserver,
                fileSystem = fileSystem,
                onFileDeleted = { filePath ->
                    ApplicationManager.getApplication().invokeLater {
                        fileEventHandler.handleDelete(filePath)
                        updateMonitor(project)
                    }
                },
                onFileChanged = { filePath ->
                    ApplicationManager.getApplication().invokeLater {
                        val editor = getEditorForFile(filePath)
                        if (editor != null) {
                            CachedReviewService.getInstance(project).review(editor)
                        } else {
                            CachedReviewService.getInstance(project).reviewByPath(filePath)
                        }
                    }
                },
                workspacePath = workspacePath,
                gitRootPath = gitRootPath,
            )
        observer = gitChangeObserver
        Disposer.register(this, gitChangeObserver)

        val bridge = VfsEventBridge(project, workspacePath, gitChangeObserver)
        vfsEventBridge = bridge
        Disposer.register(this, bridge)

        bridge.start()
        gitChangeObserver.start()
    }

    private fun resolveGitRootPath(workspacePath: String): String? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(workspacePath) ?: return null
        val repository = GitRepositoryManager.getInstance(project).getRepositoryForFile(virtualFile)
        return repository?.root?.path ?: workspacePath
    }

    private fun getEditorForFile(filePath: String): com.intellij.openapi.editor.Editor? {
        val file = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return null
        return FileEditorManager.getInstance(project).getEditors(file).firstNotNullOfOrNull { fe ->
            (fe as? TextEditor)?.editor
        }
    }

    override fun dispose() {
        savedFilesTracker = null
        vfsEventBridge = null
        observer = null
    }
}
