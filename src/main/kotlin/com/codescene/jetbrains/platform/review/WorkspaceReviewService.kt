package com.codescene.jetbrains.platform.review

import com.codescene.jetbrains.core.cli.RepoLocation
import com.codescene.jetbrains.core.cli.WorkspacePaths
import com.codescene.jetbrains.core.cli.WorkspaceReviewClient
import com.codescene.jetbrains.core.cli.WorkspaceReviewPipeline
import com.codescene.jetbrains.core.cli.WorkspaceReviewSubmitter
import com.codescene.jetbrains.platform.cli.CsIdeServerService
import com.codescene.jetbrains.platform.git.Git4IdeaGitService
import com.codescene.jetbrains.platform.progress.ReviewProgressIndicator
import com.codescene.jetbrains.platform.util.isPathSupportedForReview
import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class WorkspaceReviewService(
    private val project: Project,
) : Disposable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val csIdeServer = CsIdeServerService.getInstance()
    private val gitService = Git4IdeaGitService.getInstance(project)
    private val progressIndicator = ReviewProgressIndicator(project, scope)
    private val presenter = WorkspaceReviewPresenter(project, scope, progressIndicator)
    private val pipeline =
        WorkspaceReviewPipeline(
            WorkspaceReviewClient(csIdeServer::notify),
            presenter,
            windows = SystemInfo.isWindows,
        )
    private val submitter =
        WorkspaceReviewSubmitter(
            pipeline = pipeline,
            locate = locate@{ path ->
                val repoRoot = gitService.getRepoRoot(path) ?: return@locate null
                val rel = gitService.getRepoRelativePath(path) ?: return@locate null
                RepoLocation(repoRoot, WorkspacePaths.normalizeRel(rel))
            },
            openText = { path ->
                ReadAction.compute<String?, Exception> {
                    val file = LocalFileSystem.getInstance().findFileByPath(path)
                    if (file == null || !file.isValid) {
                        return@compute null
                    }
                    FileDocumentManager.getInstance().getCachedDocument(file)?.text
                }
            },
            readBytes = { path ->
                ReadAction.compute<ByteArray?, Exception> {
                    val file = LocalFileSystem.getInstance().findFileByPath(path)
                    if (file == null || !file.isValid) {
                        return@compute null
                    }
                    file.contentsToByteArray()
                }
            },
            isSupported = { isPathSupportedForReview(project, it) },
        )
    private val incomingEvents = Channel<IncomingReviewEvent>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (event in incomingEvents) {
                when (event) {
                    is IncomingReviewEvent.Notification ->
                        pipeline.onNotification(event.method, event.params)
                    IncomingReviewEvent.SessionLost -> pipeline.sessionLost()
                }
            }
        }
    }

    private val notificationRegistration =
        csIdeServer.addNotificationHandler { method, params ->
            incomingEvents.trySend(IncomingReviewEvent.Notification(method, params))
        }
    private val sessionRegistration =
        csIdeServer.addSessionListener {
            incomingEvents.trySend(IncomingReviewEvent.SessionLost)
        }

    fun submitEditor(
        absolutePath: String,
        content: String,
    ) {
        submitter.submitEditor(absolutePath, content)
    }

    fun submitPaths(paths: List<String>) {
        submitter.submitPaths(paths)
    }

    fun cancel(absolutePath: String) {
        pipeline.cancel(absolutePath)
    }

    fun activePaths(): Set<String> = pipeline.activePaths()

    override fun dispose() {
        notificationRegistration.close()
        sessionRegistration.close()
        incomingEvents.close()
        scope.cancel()
    }

    companion object {
        fun getInstance(project: Project): WorkspaceReviewService = project.service()
    }

    private sealed interface IncomingReviewEvent {
        data class Notification(
            val method: String,
            val params: JsonNode,
        ) : IncomingReviewEvent

        data object SessionLost : IncomingReviewEvent
    }
}
