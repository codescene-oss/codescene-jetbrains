package com.codescene.jetbrains.core.cli

class WorkspaceWatch(
    private val windows: Boolean,
) {
    private val lock = Any()
    private val roots = linkedMapOf<String, RootState>()
    private var nextToken = 1L

    fun setScopes(scopes: Map<String, List<String>>): List<WatchEffect> =
        synchronized(lock) {
            val effects = mutableListOf<WatchEffect>()
            val desiredKeys = scopes.map { WorkspacePaths.rootKey(it.key, windows) }.toSet()
            roots.keys.filter { it !in desiredKeys }.forEach { roots.remove(it) }
            for ((root, paths) in scopes) {
                val key = WorkspacePaths.rootKey(root, windows)
                val existing = roots[key]
                if (existing == null) {
                    val state = RootState(localRoot = root, paths = paths)
                    roots[key] = state
                    effects += scheduleConfirm(state)
                } else if (existing.paths != paths) {
                    existing.paths = paths
                    effects += scheduleConfirm(existing)
                }
            }
            effects
        }

    fun scopes(): Map<String, List<String>> =
        synchronized(lock) {
            roots.values.associate { it.localRoot to it.paths }
        }

    fun watchSent(repoRoot: String): List<WatchEffect> =
        synchronized(lock) {
            val state = stateFor(repoRoot) ?: return emptyList()
            listOf(scheduleConfirm(state))
        }

    fun confirmDue(
        repoRoot: String,
        token: Long,
    ): List<WatchEffect> =
        synchronized(lock) {
            val state = stateFor(repoRoot) ?: return emptyList()
            if (state.pendingToken != token) return emptyList()
            state.pendingToken = 0
            listOf(WatchEffect.FetchInventory(state.localRoot, state.generation))
        }

    fun inventoryPushed(inventory: WatchInventory): List<WatchEffect> =
        synchronized(lock) {
            val state = stateFor(inventory.repoRoot) ?: return emptyList()
            state.generation++
            state.pendingToken = 0
            val effects = mutableListOf<WatchEffect>(applyInventory(state, inventory.files))
            if (inventory.files.isEmpty()) {
                effects += WatchEffect.FetchInventory(state.localRoot, state.generation)
            }
            effects
        }

    fun inventoryFetched(
        inventory: WatchInventory,
        generation: Long,
    ): List<WatchEffect> =
        synchronized(lock) {
            val state = stateFor(inventory.repoRoot) ?: return emptyList()
            if (generation != state.generation) return emptyList()
            state.pendingToken = 0
            listOf(applyInventory(state, inventory.files))
        }

    fun inventoryFetchFailed(
        repoRoot: String,
        notWatched: Boolean,
    ): List<WatchEffect> =
        synchronized(lock) {
            val state = stateFor(repoRoot) ?: return emptyList()
            if (!notWatched) return emptyList()
            listOf(WatchEffect.ReportLost(state.localRoot))
        }

    fun headObserved(
        repoRoot: String,
        branch: String?,
        revision: String?,
    ): List<WatchEffect> =
        synchronized(lock) {
            val state = stateFor(repoRoot) ?: return emptyList()
            val head = HeadState(branch, revision)
            val previous = state.lastHead
            if (previous == null) {
                state.lastHead = head
                return emptyList()
            }
            if (previous == head) return emptyList()
            state.lastHead = head
            listOf(WatchEffect.Reseed(state.localRoot), scheduleConfirm(state))
        }

    fun progressIdle(): List<WatchEffect> =
        synchronized(lock) {
            roots.values
                .filter { it.inventory != null }
                .map { state ->
                    WatchEffect.SyncMonitor(state.localRoot, absolutePaths(state))
                }
        }

    fun resolve(
        repoRoot: String,
        relPath: String,
    ): ResolvedPath? =
        synchronized(lock) {
            val state = stateFor(repoRoot) ?: return null
            val normalized = WorkspacePaths.normalizeRel(relPath)
            if (!inScope(normalized, state.paths)) return null
            val base = state.localRoot.trimEnd('/', '\\')
            ResolvedPath(state.localRoot, "$base/$normalized")
        }

    fun isWatched(repoRoot: String): Boolean =
        synchronized(lock) {
            stateFor(repoRoot)?.inventory != null
        }

    fun inventory(repoRoot: String): Set<String>? =
        synchronized(lock) {
            stateFor(repoRoot)?.inventory
        }

    private fun scheduleConfirm(state: RootState): WatchEffect.ScheduleConfirm {
        val token = nextToken++
        state.pendingToken = token
        return WatchEffect.ScheduleConfirm(state.localRoot, token, CONFIRM_DELAY_MS)
    }

    private fun applyInventory(
        state: RootState,
        files: Set<String>,
    ): WatchEffect.SyncMonitor {
        val filtered = files.filter { inScope(WorkspacePaths.normalizeRel(it), state.paths) }.toSet()
        state.inventory = filtered
        return WatchEffect.SyncMonitor(state.localRoot, absolutePaths(state))
    }

    private fun absolutePaths(state: RootState): Set<String> {
        val base = state.localRoot.trimEnd('/', '\\')
        val inventory = state.inventory ?: return emptySet()
        return inventory.map { "$base/${WorkspacePaths.normalizeRel(it)}" }.toSet()
    }

    private fun inScope(
        relPath: String,
        paths: List<String>,
    ): Boolean {
        if (paths.contains(".")) return true
        return paths.any { scope ->
            val normalized = WorkspacePaths.normalizeRel(scope)
            if (windows) {
                relPath.equals(normalized, ignoreCase = true) ||
                    relPath.startsWith("$normalized/", ignoreCase = true)
            } else {
                relPath == normalized || relPath.startsWith("$normalized/")
            }
        }
    }

    private fun stateFor(repoRoot: String): RootState? {
        val key = WorkspacePaths.rootKey(repoRoot, windows)
        return roots[key]
    }

    private data class HeadState(
        val branch: String?,
        val revision: String?,
    )

    private class RootState(
        val localRoot: String,
        var paths: List<String>,
    ) {
        var inventory: Set<String>? = null
        var pendingToken: Long = 0
        var generation: Long = 0
        var lastHead: HeadState? = null
    }

    companion object {
        const val CONFIRM_DELAY_MS = 10_000L
    }
}
