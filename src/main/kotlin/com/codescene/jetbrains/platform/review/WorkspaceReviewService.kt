package com.codescene.jetbrains.platform.review

import com.codescene.jetbrains.core.cli.RepoLocation
import com.codescene.jetbrains.core.cli.WorkspacePaths
import com.codescene.jetbrains.core.cli.WorkspaceReviewClient
import com.codescene.jetbrains.core.cli.WorkspaceReviewPipeline
import com.codescene.jetbrains.core.cli.WorkspaceReviewSubmitter
import com.codescene.jetbrains.core.git.pathCacheKey
import com.codescene.jetbrains.core.util.ConsumerGuard
import com.codescene.jetbrains.platform.cli.CsIdeServerService
import com.codescene.jetbrains.platform.git.Git4IdeaGitService
import com.codescene.jetbrains.platform.progress.ReviewProgressIndicator
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.isPathSupportedForReview
import com.fasterxml.jackson.databind.JsonNode
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import java.util.concurrent.atomic.AtomicBoolean
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
    private val coordinator =
        WorkspaceWatchCoordinator(
            project = project,
            scope = scope,
            enqueue = { incomingEvents.trySend(IncomingReviewEvent.Watch(it)) },
            reseed = ::reseed,
        )
    private val pipeline =
        WorkspaceReviewPipeline(
            WorkspaceReviewClient(csIdeServer::notify),
            presenter,
            windows = SystemInfo.isWindows,
            observe = coordinator::observe,
        )
    private val submitter =
        WorkspaceReviewSubmitter(
            pipeline = pipeline,
            locate = locate@{ path ->
                val repoRoot = gitService.getRepoRoot(path) ?: return@locate null
                val rel = gitService.getRepoRelativePath(path) ?: return@locate null
                RepoLocation(repoRoot, WorkspacePaths.normalizeRel(rel))
            },
            isSupported = { isPathSupportedForReview(project, it) },
        )
    private val incomingEvents = Channel<IncomingReviewEvent>(Channel.UNLIMITED)
    private val watchingStarted = AtomicBoolean()
    private val guard = ConsumerGuard(Log, "WorkspaceReviewService") { it is ControlFlowException }

    init {
        scope.launch {
            for (event in incomingEvents) {
                guard.handle(event, ::handleIncoming)
            }
        }
    }

    private fun handleIncoming(event: IncomingReviewEvent) {
        when (event) {
            is IncomingReviewEvent.Notification -> {
                pipeline.onNotification(event.method, event.params)
                coordinator.onNotification(event.method, event.params)
            }
            IncomingReviewEvent.SessionLost -> pipeline.sessionLost()
            is IncomingReviewEvent.Watch -> coordinator.handle(event.event)
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

    fun startWatching() {
        if (!watchingStarted.compareAndSet(false, true)) return
        coordinator.start(this)
    }

    fun submitEditor(
        absolutePath: String,
        content: String,
    ) {
        submitter.submitEditor(absolutePath, content)
    }

    fun cancel(absolutePath: String) {
        pipeline.cancel(absolutePath)
    }

    fun activePaths(): Set<String> = pipeline.activePaths()

    private fun reseed(repoRoot: String) {
        pipeline.forgetAccepted(repoRoot)
        val prefix = pathCacheKey(repoRoot).trimEnd('/') + "/"
        val documents =
            ReadAction.compute<List<Pair<String, String>>, Exception> {
                FileDocumentManager.getInstance().unsavedDocuments.mapNotNull { document ->
                    val file = FileDocumentManager.getInstance().getFile(document) ?: return@mapNotNull null
                    val path = file.path
                    if (!pathCacheKey(path).startsWith(prefix)) return@mapNotNull null
                    if (!isPathSupportedForReview(project, path)) return@mapNotNull null
                    path to document.text
                }
            }
        documents.forEach { (path, text) -> submitter.submitEditor(path, text) }
        Log.info("HEAD moved in $repoRoot; reseeding ${documents.size} unsaved documents")
    }

    override fun dispose() {
        coordinator.dispose()
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

        data class Watch(
            val event: WorkspaceWatchCoordinator.WatchEvent,
        ) : IncomingReviewEvent
    }
}
