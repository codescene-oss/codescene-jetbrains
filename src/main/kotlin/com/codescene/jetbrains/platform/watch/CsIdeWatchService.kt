package com.codescene.jetbrains.platform.watch

import com.codescene.jetbrains.core.cli.WatchClient
import com.codescene.jetbrains.core.cli.WatchCommand
import com.codescene.jetbrains.core.cli.WatchRegistry
import com.codescene.jetbrains.platform.cli.CsIdeServerService
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.util.SystemInfo
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Service(Service.Level.APP)
class CsIdeWatchService : Disposable {
    private val server = CsIdeServerService.getInstance()
    private val watchClient: WatchClient = server.watchClient()
    private val registry = WatchRegistry(SystemInfo.isWindows)
    private val events = Channel<WatchServiceEvent>(Channel.UNLIMITED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sentListeners = CopyOnWriteArrayList<(String) -> Unit>()
    private var tickScheduled = false

    private val sessionStartRegistration =
        server.addSessionStartListener { session ->
            events.trySend(WatchServiceEvent.SessionStarted(session))
        }
    private val sessionLostRegistration =
        server.addSessionListener {
            events.trySend(WatchServiceEvent.SessionLost)
        }

    init {
        scope.launch {
            for (event in events) {
                when (event) {
                    is WatchServiceEvent.SetProject -> executeCommands(registry.setProject(event.project, event.scopes))
                    is WatchServiceEvent.RemoveProject -> executeCommands(registry.removeProject(event.project))
                    is WatchServiceEvent.SessionStarted ->
                        executeCommands(registry.sessionStarted(event.session))
                    WatchServiceEvent.SessionLost -> {
                        if (registry.hasDesired()) scheduleTick(WatchRegistry.FIRST_RETRY_MS)
                    }
                    WatchServiceEvent.Tick -> {
                        tickScheduled = false
                        executeCommands(registry.pending(server.liveSession()))
                    }
                    is WatchServiceEvent.Lost -> {
                        val commands = registry.lost(event.repoRoot)
                        if (commands.isNotEmpty()) {
                            Log.warn("cs-ide watch lost for ${event.repoRoot}; re-watching")
                        }
                        executeCommands(commands)
                    }
                }
            }
        }
    }

    fun setProjectScopes(
        project: String,
        scopes: Map<String, List<String>>,
    ) {
        events.trySend(WatchServiceEvent.SetProject(project, scopes))
    }

    fun removeProject(project: String) {
        events.trySend(WatchServiceEvent.RemoveProject(project))
    }

    fun reportLost(repoRoot: String) {
        events.trySend(WatchServiceEvent.Lost(repoRoot))
    }

    fun addWatchSentListener(listener: (String) -> Unit): AutoCloseable {
        sentListeners.add(listener)
        return AutoCloseable { sentListeners.remove(listener) }
    }

    override fun dispose() {
        sessionStartRegistration.close()
        sessionLostRegistration.close()
        events.close()
        scope.cancel()
    }

    private fun executeCommands(commands: List<WatchCommand>) {
        for (command in commands) {
            when (command) {
                is WatchCommand.Watch -> sendWatch(command.repoRoot, command.relativePaths)
                is WatchCommand.Stop -> sendStop(command.repoRoot)
            }
        }
    }

    private fun sendWatch(
        repoRoot: String,
        paths: List<String>,
    ) {
        try {
            val session = watchClient.watchFiles(repoRoot, paths)
            registry.sent(repoRoot, paths, session)
            Log.info("cs-ide watch started for $repoRoot paths=$paths session=$session")
            for (listener in sentListeners) {
                runCatching { listener(repoRoot) }
            }
        } catch (e: Exception) {
            val delayMs = WatchRegistry.retryDelayMs(server.cooldownRemainingMs())
            Log.warn("cs-ide watch send failed for $repoRoot: ${e.message}; retry in ${delayMs}ms")
            scheduleTick(delayMs)
        }
    }

    private fun sendStop(repoRoot: String) {
        val sent = watchClient.stopWatchFiles(repoRoot)
        registry.stopped(repoRoot)
        Log.info("cs-ide watch stopped for $repoRoot (sent=$sent)")
    }

    private fun scheduleTick(delayMs: Long) {
        if (tickScheduled) return
        tickScheduled = true
        scope.launch {
            delay(delayMs)
            events.trySend(WatchServiceEvent.Tick)
        }
    }

    private sealed interface WatchServiceEvent {
        data class SetProject(
            val project: String,
            val scopes: Map<String, List<String>>,
        ) : WatchServiceEvent

        data class RemoveProject(
            val project: String,
        ) : WatchServiceEvent

        data class SessionStarted(
            val session: Long,
        ) : WatchServiceEvent

        data object SessionLost : WatchServiceEvent

        data object Tick : WatchServiceEvent

        data class Lost(
            val repoRoot: String,
        ) : WatchServiceEvent
    }

    companion object {
        fun getInstance(): CsIdeWatchService = service()
    }
}
