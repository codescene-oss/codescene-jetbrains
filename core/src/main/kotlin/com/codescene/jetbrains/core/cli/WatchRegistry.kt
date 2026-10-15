package com.codescene.jetbrains.core.cli

sealed interface WatchCommand {
    data class Watch(
        val repoRoot: String,
        val relativePaths: List<String>,
    ) : WatchCommand

    data class Stop(
        val repoRoot: String,
    ) : WatchCommand
}

class WatchRegistry(
    private val windows: Boolean,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private data class SentState(
        val displayRoot: String,
        val paths: List<String>,
        val session: Long,
    )

    private data class DesiredRoot(
        val displayRoot: String,
        val paths: List<String>,
    )

    private val projects = linkedMapOf<String, Map<String, List<String>>>()
    private val sent = linkedMapOf<String, SentState>()
    private val lostRewatchAt = mutableMapOf<String, Long>()

    fun setProject(
        project: String,
        scopes: Map<String, List<String>>,
    ): List<WatchCommand> {
        projects[project] = scopes
        return diffCommands()
    }

    fun removeProject(project: String): List<WatchCommand> {
        projects.remove(project)
        return diffCommands()
    }

    fun sent(
        repoRoot: String,
        relativePaths: List<String>,
        session: Long,
    ) {
        val key = WorkspacePaths.rootKey(repoRoot, windows)
        sent[key] = SentState(repoRoot, relativePaths, session)
    }

    fun stopped(repoRoot: String) {
        sent.remove(WorkspacePaths.rootKey(repoRoot, windows))
    }

    fun sessionStarted(session: Long): List<WatchCommand> = resendWhere { it.session != session }

    fun pending(liveSession: Long): List<WatchCommand> = resendWhere { it.session != liveSession }

    fun lost(repoRoot: String): List<WatchCommand> {
        val key = WorkspacePaths.rootKey(repoRoot, windows)
        val desired = computeDesired()[key] ?: return emptyList()
        val lastAt = lostRewatchAt[key]
        if (lastAt != null && clock() - lastAt < LOST_REWATCH_INTERVAL_MS) return emptyList()
        lostRewatchAt[key] = clock()
        return listOf(WatchCommand.Watch(desired.displayRoot, desired.paths))
    }

    fun hasDesired(): Boolean = computeDesired().isNotEmpty()

    private fun resendWhere(needsResend: (SentState) -> Boolean): List<WatchCommand> {
        val desired = computeDesired()
        return desired.mapNotNull { (key, root) ->
            val state = sent[key]
            if (state == null || needsResend(state)) {
                WatchCommand.Watch(root.displayRoot, root.paths)
            } else {
                null
            }
        }
    }

    private fun diffCommands(): List<WatchCommand> {
        val desired = computeDesired()
        return stopCommands(desired) + watchCommands(desired)
    }

    private fun stopCommands(desired: Map<String, DesiredRoot>): List<WatchCommand> =
        sent.keys.filter { it !in desired }.map { WatchCommand.Stop(sent.getValue(it).displayRoot) }

    private fun watchCommands(desired: Map<String, DesiredRoot>): List<WatchCommand> =
        desired
            .filter { (key, root) -> sent[key]?.paths != root.paths }
            .map { (_, root) -> WatchCommand.Watch(root.displayRoot, root.paths) }

    private fun computeDesired(): Map<String, DesiredRoot> {
        val unionByKey = linkedMapOf<String, Pair<String, MutableList<String>>>()
        for ((_, scopes) in projects) {
            for ((displayRoot, paths) in scopes) {
                val key = WorkspacePaths.rootKey(displayRoot, windows)
                val entry = unionByKey.getOrPut(key) { displayRoot to mutableListOf() }
                entry.second.addAll(paths)
            }
        }
        return unionByKey.mapValues { (_, pair) ->
            DesiredRoot(pair.first, WatchScopes.collapse(pair.second, windows))
        }
    }

    companion object {
        const val FIRST_RETRY_MS = 2_000L
        const val MAX_RETRY_MS = 30_000L
        const val LOST_REWATCH_INTERVAL_MS = 60_000L

        fun retryDelayMs(cooldownRemainingMs: Long): Long = cooldownRemainingMs.coerceIn(FIRST_RETRY_MS, MAX_RETRY_MS)
    }
}
