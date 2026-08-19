package com.codescene.jetbrains.platform.cli

import com.codescene.jetbrains.core.cli.CsIdeClient
import com.codescene.jetbrains.core.cli.CsIdeListener
import com.codescene.jetbrains.core.cli.DeltaReviewEvent
import com.codescene.jetbrains.core.cli.FileReviewEvent
import com.codescene.jetbrains.core.cli.GitBlobSha
import com.codescene.jetbrains.core.cli.ReviewFailedEvent
import com.codescene.jetbrains.core.cli.ReviewFile
import com.codescene.jetbrains.core.cli.WorkspaceReviewPipeline
import com.codescene.jetbrains.core.cli.toPosixRelPath
import com.codescene.jetbrains.core.delta.adaptDeltaResult
import com.codescene.jetbrains.core.delta.completeDeltaAnalysis
import com.codescene.jetbrains.core.git.pathFileName
import com.codescene.jetbrains.core.review.completeReviewAnalysis
import com.codescene.jetbrains.core.telemetry.resolveTelemetryInfo
import com.codescene.jetbrains.core.util.extractExtension
import com.codescene.jetbrains.core.util.normalizeAbsolutePath
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.editor.UIRefreshService
import com.codescene.jetbrains.platform.editor.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.platform.git.Git4IdeaChangeLister
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.isFileSupported
import com.codescene.jetbrains.platform.util.isPathSupportedForReview
import com.codescene.jetbrains.platform.util.refreshAceFromDelta
import com.codescene.jetbrains.platform.util.refreshAceFromReview
import com.codescene.jetbrains.platform.webview.util.updateMonitor
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class WorkspaceReviewService(
    private val project: Project,
) : CsIdeListener, Disposable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val services = CodeSceneProjectServiceProvider.getInstance(project)
    private var pipeline: WorkspaceReviewPipeline? = null
    private val watchedRoots = mutableSetOf<String>()

    companion object {
        fun getInstance(project: Project): WorkspaceReviewService = project.service()

        private const val SERVICE = "WorkspaceReviewService"
    }

    fun start() {
        val client =
            try {
                CsIdeServerService.getInstance().ensureStarted()
            } catch (error: Exception) {
                Log.error("Failed to start cs-ide: ${error.message}", SERVICE)
                services.notificationService.showError("CodeScene CLI failed to start: ${error.message}")
                return
            }
        attachPipeline(client)
        val gitRoot = resolveGitRoot() ?: return
        val baseline = baselineRevision(gitRoot)
        watch(gitRoot, baseline)
        submitInitial(gitRoot, baseline)
    }

    fun onServerRestarted() {
        watchedRoots.clear()
        val client = runCatching { CsIdeServerService.getInstance().client() }.getOrNull() ?: return
        attachPipeline(client)
        rewatch()
    }

    fun rewatch() {
        val gitRoot = resolveGitRoot() ?: return
        val baseline = baselineRevision(gitRoot)
        watch(gitRoot, baseline)
    }

    fun submitEditor(editor: Editor) {
        val file = editor.virtualFile ?: return
        if (!isFileSupported(project, file)) return
        val gitRoot = services.gitService.getRepoRoot(file.path) ?: resolveGitRoot() ?: return
        val relPath = services.gitService.getRepoRelativePath(file.path) ?: return
        val content = ReadAction.compute<String, RuntimeException> { editor.document.text }
        pipeline?.submitBuffer(gitRoot, relPath, content, baselineRevision(gitRoot))
    }

    fun submitDiskPath(filePath: String) {
        if (!isPathSupportedForReview(project, filePath)) return
        val gitRoot = services.gitService.getRepoRoot(filePath) ?: resolveGitRoot() ?: return
        val relPath = services.gitService.getRepoRelativePath(filePath) ?: return
        val editor = editorFor(filePath)
        if (editor != null) {
            submitEditor(editor)
            return
        }
        pipeline?.submit(
            repoRoot = gitRoot,
            files = listOf(ReviewFile(relPath = relPath)),
            baselineRevision = baselineRevision(gitRoot),
        )
    }

    private fun attachPipeline(client: CsIdeClient) {
        val created =
            WorkspaceReviewPipeline(client) { repoRoot, relPath ->
                currentGitBlobSha(absolutePath(repoRoot, relPath))
            }
        created.addListener(this)
        client.addListener(created)
        pipeline = created
    }

    override fun dispose() {
        stop()
    }

    fun stop() {
        val client =
            runCatching { CsIdeServerService.getInstance().client() }.getOrNull()
        watchedRoots.forEach { root ->
            client?.stopWatchFiles(root)
        }
        watchedRoots.clear()
        pipeline = null
        scope.cancel()
    }

    override fun onFileReview(event: FileReviewEvent) {
        scope.launch { applyFileReview(event) }
    }

    override fun onDeltaReview(event: DeltaReviewEvent) {
        scope.launch { applyDeltaReview(event) }
    }

    override fun onReviewFailed(event: ReviewFailedEvent) {
        Log.warn("cs-ide review failed path=${event.path} message=${event.message}", SERVICE)
    }

    override fun onError(error: Throwable) {
        Log.warn("cs-ide workspace error: ${error.message}", SERVICE)
    }

    private fun watch(
        gitRoot: String,
        baseline: String?,
    ) {
        val client = CsIdeServerService.getInstance().client()
        if (watchedRoots.add(gitRoot)) {
            client.watchFiles(gitRoot, baseline)
        } else {
            client.stopWatchFiles(gitRoot)
            client.watchFiles(gitRoot, baseline)
        }
    }

    private fun submitInitial(
        gitRoot: String,
        baseline: String?,
    ) {
        val files = mutableListOf<ReviewFile>()
        val openFiles = FileEditorManager.getInstance(project).openFiles
        val submitted = mutableSetOf<String>()
        for (file in openFiles) {
            if (!isFileSupported(project, file)) continue
            val relPath = services.gitService.getRepoRelativePath(file.path) ?: continue
            val document = FileDocumentManager.getInstance().getCachedDocument(file)
            val content = document?.text
            if (content != null && FileDocumentManager.getInstance().isFileModified(file)) {
                files.add(ReviewFile(relPath = relPath, content = content, id = java.util.UUID.randomUUID().toString()))
                submitted.add(toPosixRelPath(relPath))
            }
        }
        val workspace = project.basePath ?: gitRoot
        scope.launch {
            val changed =
                Git4IdeaChangeLister.getInstance(project).getAllChangedFiles(gitRoot, workspace, emptySet())
            for (path in changed) {
                if (!isPathSupportedForReview(project, path)) continue
                val relPath = services.gitService.getRepoRelativePath(path) ?: continue
                if (!submitted.add(toPosixRelPath(relPath))) continue
                files.add(ReviewFile(relPath = relPath))
            }
            if (files.isNotEmpty()) {
                pipeline?.submit(gitRoot, files, baseline)
            }
        }
    }

    private suspend fun applyFileReview(event: FileReviewEvent) {
        val path = absolutePath(event.repoRoot, event.path)
        val content = currentContent(path) ?: return
        val fileName = pathFileName(path)
        completeReviewAnalysis(
            path = path,
            fileName = fileName,
            code = content,
            result = event.result,
            elapsedMs = 0,
            telemetryInfo = resolveTelemetryInfo(content.lines().size, extractExtension(fileName)),
            telemetryService = services.telemetryService,
            reviewCacheService = services.reviewCacheService,
            logger = Log,
            serviceName = SERVICE,
        )
        refreshAceFromReview(project, path, fileName, content, event.result)
        UIRefreshService.getInstance(project).refreshUI(path, CodeSceneCodeVisionProvider.getProviders())
        updateMonitor(project)
    }

    private suspend fun applyDeltaReview(event: DeltaReviewEvent) {
        val path = absolutePath(event.repoRoot, event.path)
        val currentCode = currentContent(path) ?: return
        val baselineCode = services.gitService.getBranchCreationCommitCode(path)
        val fileName = pathFileName(path)
        val delta = adaptDeltaResult(event.result)
        completeDeltaAnalysis(
            path = path,
            oldCode = baselineCode,
            currentCode = currentCode,
            delta = delta,
            telemetryInfo = resolveTelemetryInfo(currentCode.lines().size, extractExtension(fileName)),
            elapsedMs = 0,
            telemetryService = services.telemetryService,
            deltaCacheService = services.deltaCacheService,
            logger = Log,
            serviceName = SERVICE,
        )
        if (delta != null) {
            refreshAceFromDelta(project, path, fileName, currentCode, delta)
        }
        UIRefreshService.getInstance(project).refreshUI(path, CodeSceneCodeVisionProvider.getProviders())
        updateMonitor(project)
    }

    private fun currentGitBlobSha(path: String): String? {
        val file = LocalFileSystem.getInstance().findFileByPath(path)
        if (file != null) {
            val document = FileDocumentManager.getInstance().getCachedDocument(file)
            if (document != null) {
                return GitBlobSha.ofUtf8(document.text)
            }
        }
        val nio = Path.of(path)
        if (!Files.isRegularFile(nio)) return null
        return GitBlobSha.ofBytes(Files.readAllBytes(nio))
    }

    private fun currentContent(path: String): String? {
        val file = LocalFileSystem.getInstance().findFileByPath(path)
        if (file != null) {
            val document = FileDocumentManager.getInstance().getCachedDocument(file)
            if (document != null) return document.text
            return runCatching { String(file.contentsToByteArray(), file.charset) }.getOrNull()
        }
        val nio = Path.of(path)
        if (!Files.isRegularFile(nio)) return null
        return Files.readString(nio)
    }

    private fun absolutePath(
        repoRoot: String,
        relPath: String,
    ): String = normalizeAbsolutePath(Path.of(repoRoot, relPath.replace('/', java.io.File.separatorChar)).toString())

    private fun resolveGitRoot(): String? {
        val workspace = project.basePath ?: return null
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(workspace) ?: return workspace
        return services.gitService.getRepoRoot(virtualFile.path) ?: workspace
    }

    private fun baselineRevision(gitRoot: String): String? {
        val probe = Path.of(gitRoot).resolve(".git").toString()
        return services.gitService.getBranchCreationCommitHash(gitRoot)
            ?: services.gitService.getBranchCreationCommitHash(probe)
    }

    private fun editorFor(filePath: String): Editor? {
        val file = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return null
        return FileEditorManager.getInstance(project).getEditors(file)
            .firstNotNullOfOrNull { (it as? TextEditor)?.editor }
    }
}
