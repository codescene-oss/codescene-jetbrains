package com.codescene.jetbrains.platform.review

import com.codescene.jetbrains.core.cleanup.InventoryMonitorSync
import com.codescene.jetbrains.core.cli.LoadedText
import com.codescene.jetbrains.core.cli.ObservedFile
import com.codescene.jetbrains.core.cli.WatchClient
import com.codescene.jetbrains.core.cli.WatchEffect
import com.codescene.jetbrains.core.cli.WatchGitRoots
import com.codescene.jetbrains.core.cli.WatchInventory
import com.codescene.jetbrains.core.cli.WatchNotices
import com.codescene.jetbrains.core.cli.WatchScopes
import com.codescene.jetbrains.core.cli.WorkspaceFileReader
import com.codescene.jetbrains.core.cli.WorkspaceReviewNotices
import com.codescene.jetbrains.core.cli.WorkspaceWatch
import com.codescene.jetbrains.platform.cli.CsIdeServerService
import com.codescene.jetbrains.platform.delta.PlatformDeltaCacheService
import com.codescene.jetbrains.platform.git.OpenFilesObserverAdapter
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.isPathSupportedForReview
import com.codescene.jetbrains.platform.watch.CsIdeWatchService
import com.fasterxml.jackson.databind.JsonNode
import com.intellij.dvcs.repo.VcsRepositoryManager
import com.intellij.dvcs.repo.VcsRepositoryMappingListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.messages.MessageBusConnection
import git4idea.GitVcs
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import git4idea.repo.GitRepositoryManager
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WorkspaceWatchCoordinator(
    private val project: Project,
    private val scope: CoroutineScope,
    private val enqueue: (WatchEvent) -> Unit,
    private val reseed: (String) -> Unit,
) {
    private val projectKey = project.locationHash
    private val watch = WorkspaceWatch(SystemInfo.isWindows)
    private val reader =
        WorkspaceFileReader(
            openText = { path ->
                ReadAction.compute<LoadedText?, Exception> {
                    LocalFileSystem.getInstance().findFileByPath(path)?.let { file ->
                        FileDocumentManager.getInstance().getCachedDocument(file)?.let { document ->
                            val manager = FileDocumentManager.getInstance()
                            LoadedText(document.text, manager.isDocumentUnsaved(document))
                        }
                    }
                }
            },
            readBytes = { path ->
                val filePath = Path.of(path)
                if (!Files.isRegularFile(filePath)) return@WorkspaceFileReader null
                runCatching { Files.readAllBytes(filePath) }.getOrNull()
            },
        )
    private val sync = InventoryMonitorSync(PlatformDeltaCacheService.getInstance(project), Log)
    private val openFiles = OpenFilesObserverAdapter(project)
    private val watchClient = CsIdeServerService.getInstance().watchClient()
    private var sentListenerRegistration: AutoCloseable? = null
    private var busConnection: MessageBusConnection? = null
    private var rootsRetryAttempt = 0

    fun start(parent: Disposable) {
        val connection = project.messageBus.connect(parent)
        busConnection = connection
        connection.subscribe(
            ModuleRootListener.TOPIC,
            object : ModuleRootListener {
                override fun rootsChanged(event: ModuleRootEvent) = enqueue(WatchEvent.RootsChanged)
            },
        )
        connection.subscribe(
            VcsRepositoryManager.VCS_REPOSITORY_MAPPING_UPDATED,
            VcsRepositoryMappingListener { enqueue(WatchEvent.RootsChanged) },
        )
        connection.subscribe(
            GitRepository.GIT_REPO_CHANGE,
            GitRepositoryChangeListener { repo ->
                enqueue(WatchEvent.RootsChanged)
                enqueue(WatchEvent.HeadChanged(repo.root.path, repo.currentBranchName, repo.currentRevision))
            },
        )
        sentListenerRegistration =
            CsIdeWatchService.getInstance().addWatchSentListener { enqueue(WatchEvent.WatchSent(it)) }
        enqueue(WatchEvent.RootsChanged)
    }

    fun handle(event: WatchEvent) {
        val effects =
            when (event) {
                WatchEvent.RootsChanged -> return onRootsChanged()
                is WatchEvent.HeadChanged -> watch.headObserved(event.repoRoot, event.branch, event.revision)
                is WatchEvent.WatchSent -> watch.watchSent(event.repoRoot)
                is WatchEvent.ConfirmDue -> watch.confirmDue(event.repoRoot, event.token)
                is WatchEvent.InventoryFetched -> watch.inventoryFetched(event.inventory, event.generation)
                is WatchEvent.InventoryFetchFailed -> watch.inventoryFetchFailed(event.repoRoot, event.notWatched)
            }
        executeEffects(effects)
    }

    fun onNotification(
        method: String,
        params: JsonNode,
    ) {
        when (method) {
            WatchNotices.INVENTORY_CHANGED -> {
                val inventory = WatchNotices.inventory(params) ?: return
                Log.info("cs-ide inventory for ${inventory.repoRoot}: ${inventory.files.size} files (push)")
                executeEffects(watch.inventoryPushed(inventory))
            }
            "cs-ide/reviewProgress" -> {
                if (WorkspaceReviewNotices.reviewProgress(params)?.count == 0) {
                    executeEffects(watch.progressIdle())
                }
            }
        }
    }

    fun observe(
        repoRoot: String,
        relPath: String,
    ): ObservedFile? {
        val resolved = watch.resolve(repoRoot, relPath) ?: return null
        if (!isPathSupportedForReview(project, resolved.absolutePath)) return null
        val current = reader.current(resolved.absolutePath) ?: return null
        return ObservedFile(resolved.absolutePath, resolved.repoRoot, current)
    }

    fun dispose() {
        sentListenerRegistration?.close()
        busConnection?.disconnect()
        serviceIfCreated<CsIdeWatchService>()?.removeProject(projectKey)
    }

    private fun onRootsChanged() {
        val snapshot =
            ReadAction.compute<RootsSnapshot, Exception> {
                val repos = GitRepositoryManager.getInstance(project).repositories
                val repositoryRoots = repos.map { it.root.path }
                val mappingRoots =
                    ProjectLevelVcsManager.getInstance(project).allVcsRoots
                        .mapNotNull { vcsRoot ->
                            if (vcsRoot.vcs?.name == GitVcs.NAME) vcsRoot.path.path else null
                        }
                val gitRoots =
                    WatchGitRoots.union(repositoryRoots, mappingRoots, SystemInfo.isWindows)
                val contentRoots = ProjectRootManager.getInstance(project).contentRoots.map { it.path }
                val scopes = WatchScopes.scopes(gitRoots, contentRoots, SystemInfo.isWindows)
                val heads = repos.map { Triple(it.root.path, it.currentBranchName, it.currentRevision) }
                RootsSnapshot(
                    scopes = scopes,
                    heads = heads,
                    gitRepoCount = repos.size,
                    mappingRootCount = mappingRoots.size,
                    contentRootCount = contentRoots.size,
                    gitRootCount = gitRoots.size,
                )
            }
        Log.info(
            "watch roots snapshot: gitRepos=${snapshot.gitRepoCount} " +
                "vcsMappings=${snapshot.mappingRootCount} contentRoots=${snapshot.contentRootCount} " +
                "gitRoots=${snapshot.gitRootCount} scopes=${snapshot.scopes.size}",
            "WorkspaceWatchCoordinator",
        )
        if (snapshot.scopes.isEmpty() && snapshot.shouldRetryRoots()) {
            scheduleRootsRetry()
        } else {
            rootsRetryAttempt = 0
        }
        executeEffects(watch.setScopes(snapshot.scopes))
        CsIdeWatchService.getInstance().setProjectScopes(projectKey, snapshot.scopes)
        snapshot.heads.forEach { (root, branch, revision) ->
            executeEffects(watch.headObserved(root, branch, revision))
        }
    }

    private fun scheduleRootsRetry() {
        if (rootsRetryAttempt >= MAX_ROOTS_RETRIES) return
        val delayMs = INITIAL_ROOTS_RETRY_MS * (1L shl rootsRetryAttempt)
        rootsRetryAttempt++
        scope.launch {
            delay(delayMs)
            enqueue(WatchEvent.RootsChanged)
        }
    }

    private fun executeEffects(effects: List<WatchEffect>) {
        for (effect in effects) {
            when (effect) {
                is WatchEffect.FetchInventory -> fetchInventory(effect)
                is WatchEffect.ScheduleConfirm ->
                    scope.launch {
                        delay(effect.delayMs)
                        enqueue(WatchEvent.ConfirmDue(effect.repoRoot, effect.token))
                    }
                is WatchEffect.ReportLost -> CsIdeWatchService.getInstance().reportLost(effect.repoRoot)
                is WatchEffect.SyncMonitor ->
                    sync.sync(
                        effect.repoRoot,
                        effect.absolutePaths,
                        openFiles.getAllVisibleFileNames(),
                        effect.excludedRoots,
                    )
                is WatchEffect.Reseed -> reseed(effect.repoRoot)
            }
        }
    }

    private fun fetchInventory(effect: WatchEffect.FetchInventory) {
        watchClient.getWatchInventory(effect.repoRoot).whenComplete { inventory, error ->
            if (error == null && inventory != null) {
                Log.info("cs-ide inventory for ${inventory.repoRoot}: ${inventory.files.size} files (fetch)")
                enqueue(WatchEvent.InventoryFetched(inventory, effect.generation))
            } else {
                val failure = error ?: IllegalStateException("missing inventory")
                Log.warn("cs-ide inventory fetch failed for ${effect.repoRoot}: ${failure.message}")
                enqueue(WatchEvent.InventoryFetchFailed(effect.repoRoot, WatchClient.isNotWatched(failure)))
            }
        }
    }

    private data class RootsSnapshot(
        val scopes: Map<String, List<String>>,
        val heads: List<Triple<String, String?, String?>>,
        val gitRepoCount: Int,
        val mappingRootCount: Int,
        val contentRootCount: Int,
        val gitRootCount: Int,
    ) {
        fun shouldRetryRoots(): Boolean =
            scopes.isEmpty() && (mappingRootCount > 0 || gitRepoCount > 0 || contentRootCount > 0)
    }

    private companion object {
        const val MAX_ROOTS_RETRIES = 5
        const val INITIAL_ROOTS_RETRY_MS = 1_000L
    }

    sealed interface WatchEvent {
        data object RootsChanged : WatchEvent

        data class HeadChanged(val repoRoot: String, val branch: String?, val revision: String?) : WatchEvent

        data class WatchSent(val repoRoot: String) : WatchEvent

        data class ConfirmDue(val repoRoot: String, val token: Long) : WatchEvent

        data class InventoryFetched(val inventory: WatchInventory, val generation: Long) : WatchEvent

        data class InventoryFetchFailed(val repoRoot: String, val notWatched: Boolean) : WatchEvent
    }
}
