# Phase 3: watch and inventory Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Each open project asks the persistent `cs-ide` server to watch its git roots with `cs-ide/watchFiles`. The plugin applies the server's id-less `fileReview` and `deltaReview` results through the Phase 2 matcher. It keeps the server's changed-file inventory and uses it to prune the Code Health Monitor. Watches come back after the server restarts.

**Architecture:** Core owns everything that decides something: the watch wire (`WatchClient`, `WatchNotices`), scope computation (`WatchScopes`), the IDE-wide union of project scopes and its resend rules (`WatchRegistry`), the per-project inventory state machine (`WorkspaceWatch`), current-bytes reading (`WorkspaceFileReader`), the pipeline's acceptance of results that were never submitted, and monitor pruning (`InventoryMonitorSync`). `CsIdeServer` gains session ids and a notify that never spawns. Platform code is limited to two services and one coordinator. They subscribe to IntelliJ topics, read content roots, documents and disk bytes, and carry out the effects core returns. Every callback only enqueues. One coroutine per project and one for the application drain those queues on `Dispatchers.IO`.

**Tech Stack:** Kotlin 1.9 / JVM 17 toolchain, Jackson from the extension API fat jar, kotlinx-coroutines (platform; `compileOnly` in core), JUnit 4 + MockK, IntelliJ Platform 2024.3+ (`pluginSinceBuild` 243, verified against 2024.3.7 jars).

Spec: `docs/superpowers/specs/2026-10-12-cs-ide-json-rpc-migration-design.md` (Phase 3). Packages: `docs/superpowers/plans/2026-10-13-phase3-agent-packages.md`. Server source: `../codescene/cli` (`codescene.cli.ide`, `ide_server.clj`, `workspace.clj`, `workspace/watch*.clj`, `e2e/wire_contract_test.clj`); jar `codescene.extension:api` `1.0-fdef705` contains the same watch functions.

## Global Constraints

- Business logic in `core`; platform code is a thin adapter. A platform file may branch only on IntelliJ state (is a document loaded, is a path supported). Everything else is a core call.
- No new code comments; keep existing ones.
- Every changed `.kt` file stays at or below 325 lines (`bb -f .github/check-class-size-mine.clj`). The cap counts every line, including imports and blank lines. Budgets per file are in the tasks. When a test file would pass its budget, split it as the task says. Do not grow `WorkspaceReviewPipelineTest.kt` (323 lines).
- Outbound JSON keys are kebab-case; inbound payloads are read with the `RpcJson` helpers, which accept camelCase and kebab-case.
- No new runtime dependencies.
- Never send `baseline-revision`. `watchFiles` params are exactly `{repo-root, relative-paths}`; `stopWatchFiles` params are exactly `{repo-root}`.
- `watchFiles` and `stopWatchFiles` are notifications. Never wait for a response to either. `getWatchInventory` is a request.
- Threads. Work never runs on the JSON-RPC reader thread or on the EDT. Notification handlers, session listeners, session-start listeners, `CompletableFuture` completions of `getWatchInventory`, IntelliJ message-bus listeners and `delay` timers only call `Channel.trySend`. One consumer coroutine per project (`WorkspaceReviewService`) and one for the application (`CsIdeWatchService`) drain those channels on `Dispatchers.IO`. Nothing else mutates `WorkspaceReviewPipeline`, `WorkspaceWatch` or `WatchRegistry`.
- Ordering. All events from the reader thread reach a project channel in wire order, because the handler and the request future both enqueue from that thread. Core state machines must give the right result for that order and must not assume any other.
- Do not delete Phase 4 items: baseline code, `BaselineReviewCacheService`, `StaleItemCleanup`, `PeriodicChangeListerService`, `GitChangeObserver*`, `Git4IdeaChangeLister`, git observers. `CsIdeClient` request methods and ACE requests stay.
- One commit per task. Do not commit the failing test on its own.
- On Windows, `make` hangs. Set `JAVA_HOME` to `C:\Users\marti\.jdks\graalvm-jdk-17.0.12` and run `.\gradlew.bat`. ktlint is `.\gradlew.bat :core:ktlintCheck -PktlintFailOnError=true`, plus `:ktlintCheck` when the change touches `src/`. Class size is `bb -f .github/check-class-size-mine.clj`.
- CodeScene MCP `pre_commit_code_health_safeguard` before each commit.
- No `Co-authored` trailers. After committing, `git log -1 --format=%B` must not contain one; amend it away if it appears.
- Never merge or push `feature/cs-12077-cs-ide-json-rpc`, and never commit in `C:\Git\codescene-jetbrains`.

## Server behaviour this plan relies on

Checked in `../codescene/cli` source and with the real server from the jar on 2026-10-13:

- `watchFiles` is handled on a future and has no response. An absolute entry in `relative-paths` is dropped with "Watch path must be relative to the repository". A missing `relative-paths` means `["."]`.
- A second `watchFiles` for the same root replaces its roots (`["a"]` then `["b"]` leaves only `b`). Nested roots collapse (`a` and `a/b` become `a`).
- A new watch always sends `watchInventoryChanged`, even when it is empty, before its id-less `fileReview` / `deltaReview` results, followed by `reviewProgress` with `count: 0`. Re-sending identical roots requeues the whole inventory (reviews and progress) and sends no inventory notification.
- A file edit sends `watchInventoryChanged` when the set changed, then the reviews. HEAD, ref and config changes are handled by the server: it refreshes settings and, when the merge-base moved, rebuilds the inventory, rescores and requeues, and sends `watchInventoryChanged` when the set changed.
- A file restored to its baseline content stays in the inventory. Its `fileReview` carries `gitBlobSha`; its `deltaReview` has `result: null` and therefore no SHAs.
- `getWatchInventory` returns `{repoRoot, files}` with sorted relative paths. For a root that is not watched it fails with a JSON-RPC error whose message is `Repository is not watched`.
- `stopWatchFiles` without `relative-paths` stops the root and sends `watchInventoryChanged` with empty `files` when the inventory was not empty. The server also stops a watch by itself after three watcher recoveries within a minute, with the same empty notification.
- Notifications carry the root in the server's canonical OS form (for example `C:\Users\...` on Windows).
- `reviewProgress` is global (no repository) and is only sent when reviews finish. An idle watch sends nothing.
- Inventory is `git diff --name-only <merge-base>` plus `git ls-files --others --exclude-standard`, filtered to supported, regular files under the watch roots. The merge-base falls back through `baseline_branch`, `origin/HEAD`, `origin/main`, `origin/master`, `main`, `master`.

## Resolved decisions

- **Current bytes for id-less results.** An id-less result is accepted only when its git-blob SHA equals the file's current bytes: the UTF-8 text of the loaded `Document` when one is loaded (`FileDocumentManager.getCachedDocument`), otherwise the raw bytes on disk. This is the Phase 2 rule ("a loaded `Document` is always a buffer"); the spec's "dirty document" wording is corrected to "loaded document". Core `WorkspaceFileReader` decides; platform supplies the two reads. For watch results the disk read is `java.nio.file.Files.readAllBytes`, not the VFS, because the VFS can lag an external write and a dropped watch result is never resent. The Phase 2 submitter keeps its VFS read; a stale read there only costs a resend on a later poll.
- **Results for files never submitted.** Watch results arrive for paths the plugin never submitted. The pipeline asks an `observe(repoRoot, relPath)` callback for the current file and runs the same `acceptsResult` check against it. There is no second matcher. Observing applies only when the path has no in-flight buffer; an in-flight buffer drops every id-less result for that path, as in Phase 2.
- **Null id-less deltas pair with their review.** A `deltaReview` with no id and `result: null` has no SHA. It is accepted when the path's last accepted id-less `fileReview` is still waiting for its delta and the current bytes still hash to that review's SHA. That is how a file restored to its baseline leaves the monitor. (User decision.)
- **Untracked files.** `Git4IdeaChangeLister` lists at most five untracked files per directory unless they are open or saved. The server lists all of them. That superset is accepted: the monitor shows every untracked supported file. Parity means equal results on the other feature-branch scenarios. (User decision.)
- **HEAD moves.** On a branch or revision change of a git root, the project clears accepted state for that root's paths in the pipeline and resubmits unsaved documents under the root (`FileDocumentManager.getUnsavedDocuments`) as buffers, so they are scored against the new merge-base. It then schedules an inventory confirm. (User decision.)
- **Session loss.** After a session loss the application service retries `watchFiles` for every desired root 2 s later, then after the server's remaining backoff clamped to 2–30 s, until it succeeds or no project wants a watch. When any request or notify starts a new session first, the session-start listener resends instead. There is no acknowledgement. (User decision.)
- **One `watchFiles` per git root, IDE-wide.** The server keeps one watch per root, and a second `watchFiles` replaces the first. Two projects that share a git root must therefore send the union of their scopes. An application-level `WatchRegistry` keeps that union, sends it whenever it changes, and sends `stopWatchFiles` only when no open project needs the root. Each project filters inventory and watch results to its own scope.
- **Scopes.** Each project content root maps to its nearest enclosing git root with the content root's relative path (`.` when it is the git root). A content root at or above a git root watches that whole git root (`.`). Paths under one root are collapsed (nested and duplicate entries removed, `.` wins) and sorted. Git roots without a content root are not watched. Content roots come from `ProjectRootManager.getContentRoots()`; git roots from `GitRepositoryManager.getRepositories()`.
- **Inventory.** `watchInventoryChanged` replaces the project's set for that root wholesale. `getWatchInventory` is a confirm: it runs when no push arrived within 10 s of a `watchFiles` send, a scope change or a HEAD move, and after an empty push (to tell an empty watch from a stopped one). A fetch result is discarded when a push for the same root arrived after the fetch was issued, because pushes are the server's latest state. Sending `getWatchInventory` right after `watchFiles` is not done: the server handles both concurrently and could answer "not watched".
- **Lost watch.** A confirm that fails with `Repository is not watched` reports the root as lost. The application re-watches a lost root at most once per 60 s.
- **Inventory consumers.** The monitor is pruned by `InventoryMonitorSync`: entries under a watched root that are neither in the inventory nor open in an editor are hidden; inventory paths are re-included. It runs on every applied inventory and on `reviewProgress` `count: 0`. While a project watches the poller's git root, `PeriodicChangeListerService` skips its `StaleItemCleanup` and only submits.
- **Poller in Phase 3.** `PeriodicChangeListerService` keeps calling `submitPaths` every 9 s. Phase 2 dedup means an unchanged tree sends nothing. Phase 4 deletes the poller and `StaleItemCleanup` once `WatchInventoryParityLiveTest` (Task 7) passes in CI. The parity scenarios are listed in Task 7.
- **Progress.** The single `ReviewProgressIndicator` from Phase 2 stays. No new indicator and no per-file indicator. The first watch shows `CodeScene analysis (N remaining)` for its requeue; an idle watch sends no `reviewProgress`, so the indicator stays down. `reviewProgress` is global, so every open project shows the same count; that limitation is unchanged from Phase 2.
- **Project close.** `WorkspaceReviewService.dispose` enqueues `removeProject` on the application service when that service exists (`serviceIfCreated`). Application dispose sends nothing; closing the server ends every watch.
- **macOS JNA.** Unchanged: the default JNA unpack (spike result recorded in the spec).

## File structure

`core/src/main/kotlin/com/codescene/jetbrains/core/cli/`

| File | Responsibility | Budget |
| --- | --- | --- |
| `CsIdeServer.kt` (modify) | Session ids, `notifyInSession`, `notifyIfRunning`, `liveSession`, `cooldownRemainingMs`, session-start listeners | ≤ 315 |
| `CsIdeNotificationHub.kt` (modify) | Session-start listener list | ≤ 80 |
| `WatchClient.kt` (create) | `watchFiles`, `stopWatchFiles`, `getWatchInventory` JSON; `WatchInventory`; `WatchNotices` | ≤ 110 |
| `WatchScopes.kt` (create) | Content roots to `{gitRoot: relativePaths}`; path collapsing | ≤ 90 |
| `WorkspacePaths.kt` (modify) | `rootKey`, `relativeTo` | ≤ 55 |
| `WorkspaceFileReader.kt` (create) | Current SHA and text of a file: loaded document wins, else disk bytes | ≤ 30 |
| `WorkspaceReviewPipeline.kt` (modify) | `observe` callback, id-less acceptance for observed files, null-delta pairing, `forgetAccepted` | ≤ 250 |
| `WorkspaceReviewMatching.kt` (modify) | `SubmittedDisk.documentText`, `awaitingDelta`, `observedState` | ≤ 140 |
| `WatchRegistry.kt` (create) | IDE-wide desired roots, sent state, resend and stop commands | ≤ 170 |
| `WorkspaceWatch.kt` (create) | Per-project scopes, inventory, confirm tokens, generations, HEAD tracking, path resolution | ≤ 230 |

`core/src/main/kotlin/com/codescene/jetbrains/core/cleanup/`

| File | Responsibility | Budget |
| --- | --- | --- |
| `InventoryMonitorSync.kt` (create) | Hide monitor entries outside inventory ∪ open files, re-include inventory paths | ≤ 50 |

`src/main/kotlin/com/codescene/jetbrains/platform/`

| File | Responsibility | Budget |
| --- | --- | --- |
| `cli/CsIdeServerService.kt` (modify) | `watchClient()`, `addSessionStartListener`, `liveSession`, `cooldownRemainingMs` | ≤ 80 |
| `watch/CsIdeWatchService.kt` (create) | App service: one consumer, `WatchRegistry`, sends, retries, sent fan-out | ≤ 170 |
| `review/WorkspaceWatchCoordinator.kt` (create) | Project topics, scope and HEAD reads, effect execution, `observe` | ≤ 210 |
| `review/WorkspaceReviewService.kt` (modify) | Watch events on its channel, `observe`, `startWatching`, `isWatched`, dispose | ≤ 200 |
| `listeners/ProjectStartupActivity.kt` (modify) | Call `startWatching` before the poller starts | ≤ 165 |
| `git/PeriodicChangeListerService.kt` (modify) | Skip `StaleItemCleanup` when the git root is watched | ≤ 110 |

Tests are listed per task with budgets. No existing platform test references the modified platform files (`rg` checked on 2026-10-13), so no one-line platform test updates are expected. If one fails to compile, fix it in the same package and list it in the hand-off.

---

### Task 1: Server sessions

**Files:**
- Modify: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/CsIdeServer.kt`
- Modify: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/CsIdeNotificationHub.kt`
- Test: create `core/src/test/kotlin/com/codescene/jetbrains/core/cli/CsIdeServerSessionTest.kt` (≤ 230 lines)

**Interfaces:**
- Consumes: Phase 2 `CsIdeServer` (`notify`, `ensureStarted`, `detach`, `hub`), `FakeCsIdeProcessFactory`, `CsIdeBackoff`.
- Produces on `CsIdeServer`:
  - `fun notifyInSession(method: String, params: JsonNode): Long`
  - `fun notify(method: String, params: JsonNode)` (unchanged behaviour; delegates to `notifyInSession`)
  - `fun notifyIfRunning(method: String, params: JsonNode): Boolean`
  - `fun liveSession(): Long`
  - `fun cooldownRemainingMs(): Long`
  - `fun addSessionStartListener(listener: (Long) -> Unit): AutoCloseable`
- Produces on `CsIdeNotificationHub`: `fun addSessionStartListener(listener: (Long) -> Unit): AutoCloseable`, `fun sessionStarted(session: Long)`

Session ids start at 1 and increase by one for every `startProcess`. `startProcess` takes the next id under `lock` before it creates the connection. The id is published with `connection` and `process`, and `detach` resets it to 0. `ensureStarted` returns the connection together with its id, so `notifyInSession` returns the id of the session the frame was written to. `request` keeps its behaviour.

`onNotification` for `cs-ide/start` calls `recordMetadata`, then `hub.sessionStarted(id)`, then `hub.dispatch`, then completes the start signal. Start listeners therefore run on the reader thread before `waitForStart` returns. They must only enqueue: a listener that calls `request` or `notify` deadlocks on `startLock`. `hub.sessionStarted` catches and logs (warn) a throwing listener, like `dispatch`.

`notifyIfRunning` returns false without spawning when the server is closed or has no live connection (connection null or process not alive). It ignores the backoff. It writes the frame outside `lock` and returns false when the write throws. It does not reset the backoff.

`liveSession` is the published id when the connection is set and the process is alive, else 0. `cooldownRemainingMs` is `backoff.remainingMs(clock())`.

- [ ] **Step 1: Write the failing tests**

Use the `CsIdeServerNotifyTest` setup (fake factory that writes `cs-ide/start` from a daemon thread, `CsIdeBackoff(initialMs = 100, maxMs = 10_000)`, `clock` from an `AtomicLong`). Tests:

1. `notifyInSession returns 1 for the first session and 2 after a respawn`: first call returns 1 and the peer reads the frame; `factory.killCurrentProcess()`; wait until `liveSession() == 0`; advance the clock past the cooldown; second call returns 2; `factory.starts.get() == 2`.
2. `liveSession is 0 before start, the session id while running, and 0 after close`.
3. `start listeners receive each session id after metadata is set`: the listener records `id to server.metadata()`; after two sessions the records are `[1 to CsIdeServerMetadata("abc-sha", "1.2.3"), 2 to CsIdeServerMetadata("abc-sha", "1.2.3")]`.
4. `a start listener runs before notification handlers see cs-ide start`: both append to one list; the list starts with `"session 1"` then `"cs-ide/start"`.
5. `a throwing start listener does not block other listeners or startup`: the second listener still records 1 and `notifyInSession` returns 1.
6. `a closed start listener registration is not called`.
7. `notifyIfRunning without a server returns false and starts nothing`: returns false; `factory.starts.get() == 0`.
8. `notifyIfRunning writes a frame without an id when a session is live`: after `notifyInSession`, `notifyIfRunning("cs-ide/stopWatchFiles", params)` is true; the peer reads a frame with that method, those params and no `id`.
9. `notifyIfRunning returns false after close and during cooldown without spawning`: record a backoff failure with no live session, call returns false and `starts` stays 0; after close it returns false.
10. `cooldownRemainingMs is 0 initially and follows the backoff`: after `backoff.recordFailure(0)` with clock 40 it is 60.
11. `notify still throws CsIdeUnavailableException during cooldown`: regression for the delegation.

- [ ] **Step 2: Run the tests and confirm they fail to compile**

Run: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.CsIdeServerSessionTest"`

Expected: compilation failure (unresolved `notifyInSession`, `notifyIfRunning`, `liveSession`, `cooldownRemainingMs`, `addSessionStartListener`).

- [ ] **Step 3: Implement**

```kotlin
private data class LiveConnection(val rpc: JsonRpcConnection, val session: Long)

fun notifyInSession(
    method: String,
    params: JsonNode,
): Long {
    ensureAvailable()
    val live = ensureStarted()
    try {
        live.rpc.notify(method, params)
    } catch (e: Exception) {
        throw CsIdeUnavailableException("cs-ide server failed to send $method: ${e.message}", e)
    }
    backoff.reset()
    return live.session
}

fun notifyIfRunning(
    method: String,
    params: JsonNode,
): Boolean {
    val rpc =
        synchronized(lock) {
            if (closed.get() || process?.isAlive() != true) return false
            connection ?: return false
        }
    return runCatching { rpc.notify(method, params) }.isSuccess
}
```

- [ ] **Step 4: Run the new tests and the existing server tests**

Run: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.CsIdeServer*"`

Expected: PASS, including `CsIdeServerTest`, `CsIdeServerNotifyTest`, `CsIdeServerEdgeCaseTest` unchanged.

- [ ] **Step 5: Lint, size, safeguard, commit**

`.\gradlew.bat :core:ktlintCheck -PktlintFailOnError=true`, `bb -f .github/check-class-size-mine.clj`, MCP `pre_commit_code_health_safeguard`, then commit `feat(cli): track cs-ide server sessions`.

---

### Task 2: Watch wire and scopes

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WatchClient.kt`
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WatchScopes.kt`
- Modify: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspacePaths.kt`
- Test: create `WatchClientTest.kt` (≤ 170), `WatchScopesTest.kt` (≤ 150); modify `WorkspacePathsTest.kt` (49 → ≤ 85), all in `core/src/test/kotlin/com/codescene/jetbrains/core/cli/`

**Interfaces:**
- Consumes: `CsIdeTransport`, `RpcJson`, `CsIdeErrors.unwrap`, `JsonRpcException`, `WorkspacePaths.normalizeRel`.
- Produces:
  - `data class WatchInventory(val repoRoot: String, val files: Set<String>)`
  - `object WatchNotices { const val INVENTORY_CHANGED = "cs-ide/watchInventoryChanged"; fun inventory(params: JsonNode): WatchInventory? }`
  - `class WatchClient(notifyInSession: (String, JsonNode) -> Long, notifyIfRunning: (String, JsonNode) -> Boolean, transport: CsIdeTransport)` with `fun watchFiles(repoRoot: String, relativePaths: List<String>): Long`, `fun stopWatchFiles(repoRoot: String): Boolean`, `fun getWatchInventory(repoRoot: String): CompletableFuture<WatchInventory>`, and `companion object { fun isNotWatched(error: Throwable): Boolean }`
  - `object WatchScopes { fun scopes(gitRoots: List<String>, contentRoots: List<String>, windows: Boolean): Map<String, List<String>>; fun collapse(paths: Collection<String>, windows: Boolean): List<String> }`
  - `WorkspacePaths.rootKey(root: String, windows: Boolean): String` and `WorkspacePaths.relativeTo(root: String, path: String, windows: Boolean): String?`

`watchFiles` sends `cs-ide/watchFiles` `{"repo-root": repoRoot, "relative-paths": relativePaths}` through `notifyInSession` and returns its session id. `stopWatchFiles` sends `cs-ide/stopWatchFiles` `{"repo-root": repoRoot}` through `notifyIfRunning` and returns its result. `getWatchInventory` requests `cs-ide/getWatchInventory` `{"repo-root": repoRoot}` and maps the result through `WatchNotices.inventory`; a result that does not parse fails the future with `IllegalStateException("Malformed cs-ide/getWatchInventory result")`. `isNotWatched` is true when `CsIdeErrors.unwrap(error)` is a `JsonRpcException` whose message contains `not watched` (ignoring case).

`WatchNotices.inventory` reads `repoRoot` and `files`. It returns null when `repoRoot` is missing or `files` is not an array. Entries go through `normalizeRel`.

`rootKey` normalizes slashes, trims a trailing slash and lowercases on Windows, matching `sameRoot`. `relativeTo` returns `.` when `path` is the root, the slash-normalized remainder when `path` is under the root (separator-aware: `/repo2` is not under `/repo`), else null; comparison ignores case on Windows and the returned remainder keeps the case of `path`.

`scopes`: for each content root, pick the git root with the longest key for which `relativeTo(gitRoot, contentRoot)` is not null, and add that relative path. Then add `.` for every git root for which `relativeTo(contentRoot, gitRoot)` is not null. Each git root's list goes through `collapse`. The map keeps the input order and form of `gitRoots` and omits git roots without paths. `collapse` returns `["."]` when any entry is `.`, otherwise removes duplicates and entries under another entry (prefix plus `/`), and sorts.

- [ ] **Step 1: Write the failing tests**

`WatchClientTest` (fakes record method and params):

1. `watchFiles writes repo-root and relative-paths only and returns the session`: params equal `{"repo-root":"/repo","relative-paths":["src","test"]}` exactly (field names are `["repo-root","relative-paths"]`, so no `baseline-revision`); return value is the fake's 7.
2. `stopWatchFiles goes through notifyIfRunning`: params equal `{"repo-root":"/repo"}`; returns the fake's false; the `notifyInSession` fake was not called.
3. `getWatchInventory requests the root and parses the result`: request method `cs-ide/getWatchInventory`, params `{"repo-root":"/repo"}`; result `{"repoRoot":"C:\\repo","files":["b.kt","a\\c.kt"]}` gives `WatchInventory("C:\\repo", setOf("b.kt", "a/c.kt"))`.
4. `a malformed result fails the future`: result `{}` fails with `IllegalStateException`.
5. `isNotWatched is true only for the not-watched RPC error`: `ExecutionException(JsonRpcException(-32603, "Repository is not watched"))` is true; `JsonRpcException(-32603, "boom")` false; `CsIdeUnavailableException("cs-ide server is closed")` false.
6. `inventory parses camelCase and kebab-case`, `inventory with empty files is an empty set`, `inventory without repoRoot or with non-array files is null`.

`WatchScopesTest`:

1. Content root equal to git root gives `{"/repo": ["."]}`.
2. Content roots `/repo/b/c` and `/repo/a` give `{"/repo": ["a", "b/c"]}`.
3. Nested `/repo/a` and `/repo/a/b` give `["a"]`; `/repo` plus `/repo/a` give `["."]`.
4. Content root `/w` above git root `/w/repo` gives `{"/w/repo": ["."]}`.
5. Nested git roots `/p` and `/p/sub` with content root `/p/sub/x` give `{"/p/sub": ["x"]}` only.
6. Content root outside every git root, and a git root without content roots, are omitted.
7. Sibling prefix: git root `/repo`, content root `/repo2` gives an empty map.
8. Windows: git root `C:/Repo`, content root `c:\repo\src` gives `{"C:/Repo": ["src"]}`; with `windows = false` and `/Repo` versus `/repo/src` the map is empty.
9. `collapse(listOf("b", "a/x", "a", "b"), false)` is `["a", "b"]`.

`WorkspacePathsTest` additions: `rootKey("C:\\Repo\\", true) == "c:/repo"`; `rootKey("/Repo/", false) == "/Repo"`; `relativeTo("/repo", "/repo", false) == "."`; `relativeTo("/repo", "/repo/src/A.kt", false) == "src/A.kt"`; `relativeTo("/repo", "/repo2/A.kt", false) == null`; `relativeTo("C:\\Repo", "c:/repo/Src", true) == "Src"`.

- [ ] **Step 2: Run the tests and confirm they fail to compile**

Run: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.WatchClientTest" --tests "com.codescene.jetbrains.core.cli.WatchScopesTest" --tests "com.codescene.jetbrains.core.cli.WorkspacePathsTest"`

Expected: compilation failure.

- [ ] **Step 3: Implement**

```kotlin
fun watchFiles(
    repoRoot: String,
    relativePaths: List<String>,
): Long {
    val params = RpcJson.objectNode().put("repo-root", repoRoot)
    params.putArray("relative-paths").apply { relativePaths.forEach(::add) }
    return notifyInSession(WATCH_FILES, params)
}
```

- [ ] **Step 4: Run the tests**

Same command as step 2. Expected: PASS.

- [ ] **Step 5: Lint, size, safeguard, commit**

Commit `feat(cli): add cs-ide watch wire and scopes`.

---

### Task 3: Pipeline accepts watch results

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceFileReader.kt`
- Modify: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewPipeline.kt`
- Modify: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewMatching.kt`
- Test: create `WorkspaceFileReaderTest.kt` (≤ 70), `WorkspaceReviewPipelineFixture.kt` (test helper, ≤ 120), `WorkspaceReviewWatchResultsTest.kt` (≤ 200), `WorkspaceReviewWatchStateTest.kt` (≤ 170), all in `core/src/test/kotlin/com/codescene/jetbrains/core/cli/`. Do not edit `WorkspaceReviewPipelineTest.kt` or `WorkspaceReviewSubmitterTest.kt`; both must still pass.

**Interfaces:**
- Consumes: Phase 2 `WorkspaceReviewPipeline`, `WorkspaceReviewMatching`, `GitBlobSha`, `WorkspacePaths`.
- Produces:
  - `data class CurrentFile(val gitBlobSha: String, val text: String?)`
  - `class WorkspaceFileReader(private val openText: (String) -> String?, private val readBytes: (String) -> ByteArray?) { fun current(absolutePath: String): CurrentFile? }`
  - `data class ObservedFile(val absolutePath: String, val repoRoot: String, val current: CurrentFile)`
  - `WorkspaceReviewPipeline(sender, sink, newId, windows, clock, observe: (repoRoot: String, relPath: String) -> ObservedFile? = { _, _ -> null })`
  - Member `fun forgetAccepted(repoRoot: String)` on `WorkspaceReviewPipeline`
  - Internal: `SubmittedDisk(..., val documentText: String? = null)`, `WorkspaceReviewMatching.awaitingDelta(state: PathState?): String?`, `WorkspaceReviewMatching.observedState(previous: PathState?, observed: ObservedFile, relPath: String, now: Long): PathState`

`WorkspaceFileReader.current`: when `openText` returns text, `CurrentFile(GitBlobSha.ofString(text), text)`; else when `readBytes` returns bytes, `CurrentFile(GitBlobSha.ofBytes(bytes), null)`; else null. `readBytes` is only called when there is no text.

Results with an id keep the Phase 2 path (`accept`), and `observe` is not called for them. Id-less `fileReview` and `deltaReview` go through one new function:

```kotlin
private fun acceptIdLess(
    key: NoticeKey,
    expectedSha: (PathState?) -> String?,
    change: (PathState) -> PathState,
): Submission? =
    synchronized(lock) {
        val state = find(key)
        if (state != null && state.inFlight && state.current is SubmittedBuffer) return null
        val sha = expectedSha(state) ?: return null
        val observed = observe(key.repoRoot, key.relPath)
        val target =
            if (observed == null) {
                state ?: return null
            } else {
                WorkspaceReviewMatching.observedState(state, observed, key.relPath, clock())
            }
        if (!WorkspaceReviewMatching.acceptsResult(target.current, key, sha)) return null
        state?.let { states.remove(it.current.absolutePath) }
        states[target.current.absolutePath] = change(target)
        target.current
    }
```

`fileReview` passes `{ notice.gitBlobSha }`. `deltaReview` passes `{ s -> notice.newGitBlobSha ?: if (notice.delta == null) WorkspaceReviewMatching.awaitingDelta(s) else null }`.

`observedState` builds `SubmittedDisk(observed.absolutePath, observed.repoRoot, normalizeRel(relPath), observed.current.gitBlobSha, submittedAt, documentText = observed.current.text)`. When `previous` has the same git-blob SHA it returns `previous.copy(current = thatSubmission)` with `submittedAt` from `previous`; otherwise `PathState(thatSubmission, inFlight = false, accepted = false)` with `submittedAt = now`. `awaitingDelta` returns `state.current.gitBlobSha` when `state.current is SubmittedDisk && state.accepted && !state.deltaAccepted`, else null. `presentFor` uses the buffer content or `documentText`, so a watch result for a loaded document presents with its text and refreshes the annotator like a buffer result.

`forgetAccepted(repoRoot)` sets `accepted = false` and `deltaAccepted = false` on every state whose root is `sameRoot` with `repoRoot`; `inFlight` is kept.

- [ ] **Step 1: Write the failing tests**

`WorkspaceReviewPipelineFixture` holds the recording sink, `pipeline(windows, observe)`, `fileReview`, `deltaReview` and `reviewProgress` JSON builders and `sha(content)`, copied from `WorkspaceReviewPipelineTest` so that file stays untouched.

`WorkspaceFileReaderTest`:

1. Loaded text wins over bytes: text `"a\r\n"`, bytes `"a\n"` gives `CurrentFile(GitBlobSha.ofString("a\r\n"), "a\r\n")` and `readBytes` is not called.
2. No text, bytes `0xC3, 0xBC` gives `CurrentFile(GitBlobSha.ofBytes(bytes), null)`.
3. Empty text gives `CurrentFile("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391", "")`.
4. Neither gives null.

`WorkspaceReviewWatchResultsTest` (observe returns `ObservedFile("/repo/src/App.kt", "/repo", current)` for `("/repo", "src/App.kt")` unless the test says otherwise):

1. `a watch result for a never-submitted file presents once without content`: current `CurrentFile("s1", null)`; id-less `fileReview` with `s1` presents one review with `absolutePath == "/repo/src/App.kt"`, `content == null`, `gitBlobSha == "s1"`; id-less `deltaReview` with new SHA `s1` presents one delta with `scoredContent == null`; nothing was sent; `activePaths()` is empty.
2. `a watch result for a loaded document presents with the document text`: current `CurrentFile(sha("t"), "t")`; the review has `content == "t"` and the delta `scoredContent == "t"`.
3. `a watch result whose SHA is not the current bytes is dropped`: current `s2`; `fileReview` and `deltaReview` with `s1` present nothing.
4. `a disk SHA that is no longer current is dropped and the current one is accepted`: submit disk `d1`; current `d2`; id-less `fileReview` `d1` presents nothing; `d2` presents one review.
5. `an id-less result is dropped while a buffer is in flight`: submit buffer `"a"`; current `CurrentFile(sha("a"), "a")`; id-less `fileReview` with `sha("a")` presents nothing and `activePaths()` still contains the path; `fileReview("id1", sha("a"))` then presents.
6. `without an observed file a never-submitted path is dropped`: observe returns null; id-less `fileReview` `s1` presents nothing.
7. `observe is not called for results with an id`: an id-matched review and delta leave the observe call counter at 0.

`WorkspaceReviewWatchStateTest`:

1. `a null id-less delta pairs with the accepted review`: current `s1`; a null-result id-less `deltaReview` before any review presents nothing; after id-less `fileReview` `s1`, a null-result `deltaReview` presents one delta with `delta == null`, `oldGitBlobSha == null`, `newGitBlobSha == null`; a second null-result delta presents nothing more.
2. `a null id-less delta is dropped when the bytes changed after the review`: accept `fileReview` `s1`; current becomes `s2`; a null-result delta presents nothing.
3. `an accepted watch result dedups the same disk SHA but not a buffer`: current `sha("a")`; accept id-less review and delta; `submit` of `DiskSubmission("/repo/src/App.kt", "src/App.kt", sha("a"))` sends nothing; `submit` of buffer `"a"` sends `ReviewFileWire("src/App.kt", "id1", "a")`.
4. `forgetAccepted resends accepted content for that repository only`: accept buffers for `/repo` and `/other` (review and delta by id); `forgetAccepted("/repo")`; resubmitting both sends only the `/repo` buffer.
5. `forgetAccepted matches the root ignoring case and slashes on Windows`: pipeline with `windows = true`, accepted buffer under `C:\repo`; `forgetAccepted("c:/repo/")` makes it send again.
6. `sessionLost keeps an observed file whose delta was accepted`: accept review and delta through observe; `sessionLost()`; a disk submit with the same SHA sends nothing.

- [ ] **Step 2: Run the tests and confirm they fail to compile**

Run: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.WorkspaceFileReaderTest" --tests "com.codescene.jetbrains.core.cli.WorkspaceReviewWatch*"`

Expected: compilation failure.

- [ ] **Step 3: Implement** as described above.

- [ ] **Step 4: Run the new tests and every Phase 2 pipeline test**

Run: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.Workspace*"`

Expected: PASS. In particular `WorkspaceReviewPipelineTest.id-less results are accepted only for the disk SHA` still passes (no observe; the null delta there arrives before any accepted review).

- [ ] **Step 5: Lint, size, safeguard, commit**

Commit `feat(cli): accept cs-ide watch results`.

---

### Task 4: Watch registry

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WatchRegistry.kt`
- Test: create `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WatchRegistryTest.kt` (≤ 230)

**Interfaces:**
- Consumes: `WatchScopes.collapse`, `WorkspacePaths.rootKey` (Task 2).
- Produces:

```kotlin
sealed interface WatchCommand {
    data class Watch(val repoRoot: String, val relativePaths: List<String>) : WatchCommand
    data class Stop(val repoRoot: String) : WatchCommand
}

class WatchRegistry(
    private val windows: Boolean,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun setProject(project: String, scopes: Map<String, List<String>>): List<WatchCommand>
    fun removeProject(project: String): List<WatchCommand>
    fun sent(repoRoot: String, relativePaths: List<String>, session: Long)
    fun stopped(repoRoot: String)
    fun sessionStarted(session: Long): List<WatchCommand>
    fun pending(liveSession: Long): List<WatchCommand>
    fun lost(repoRoot: String): List<WatchCommand>
    fun hasDesired(): Boolean

    companion object {
        const val FIRST_RETRY_MS = 2_000L
        const val MAX_RETRY_MS = 30_000L
        const val LOST_REWATCH_INTERVAL_MS = 60_000L
        fun retryDelayMs(cooldownRemainingMs: Long): Long = cooldownRemainingMs.coerceIn(FIRST_RETRY_MS, MAX_RETRY_MS)
    }
}
```

Not thread-safe; only the application consumer calls it. Roots are keyed by `rootKey`; the display form is the form of the project that first brought the root. A root's desired paths are `collapse` of the union of every project's paths for it.

- `setProject` replaces that project's scopes. It returns `Watch` for each desired root whose desired paths differ from its last sent paths (or that was never sent), and `Stop` for each root that is no longer desired and was sent. `removeProject` is `setProject(project, emptyMap())` plus forgetting the project.
- `sent` records paths and session for the root. `stopped` forgets the sent state.
- `sessionStarted(k)` returns `Watch` for every desired root whose sent session is not `k`.
- `pending(live)` returns `Watch` for every desired root whose sent session is not `live` (with `live == 0`, all desired roots).
- `lost(root)` returns `Watch` for a desired root when it has not been re-watched for loss in the last `LOST_REWATCH_INTERVAL_MS`, and records the time; otherwise empty. A root that is not desired returns empty.

- [ ] **Step 1: Write the failing tests**

Unless a test says otherwise, it calls `sent(root, paths, 1)` for every `Watch` it receives, as the service does after a successful write.

1. `the first project emits one watch per root`: `setProject("p1", {"/a": ["."], "/b": ["src"]})` is `[Watch("/a", ["."]), Watch("/b", ["src"])]`.
2. `unchanged scopes after sent emit nothing`.
3. `a second project on the same root sends the union`: p1 `["a"]`, p2 `["b"]` gives `Watch("/r", ["a", "b"])`; p3 `["."]` gives `Watch("/r", ["."])`; p4 `["a/x"]` gives nothing more.
4. `Windows root keys ignore case and slashes`: p1 `C:\Repo`, p2 `c:/repo/` give one root, displayed `C:\Repo`; with `windows = false`, `/Repo` and `/repo` are two roots.
5. `removing one of two projects shrinks the union`: `removeProject("p2")` gives `Watch("/r", ["a"])`.
6. `removing the last project stops a sent root and not an unsent one`: sent root gives `[Stop("/r")]`; a root never sent gives `[]`.
7. `a scope change that drops a root stops it`.
8. `sessionStarted re-emits roots sent in another session`: roots sent in session 1; `sessionStarted(2)` gives both; after `sent(…, 2)` for one, `sessionStarted(2)` gives only the other.
9. `pending with no live session emits every desired root and skips roots sent in the live session`.
10. `lost re-watches once per minute`: `lost("/r")` at 0 gives one `Watch`; at 59_999 gives `[]`; at 60_000 gives one `Watch`; `lost("/unknown")` gives `[]`.
11. `retryDelayMs clamps to 2 to 30 seconds`: 0 → 2_000, 5_000 → 5_000, 60_000 → 30_000.
12. `hasDesired is false after every project is removed`.

- [ ] **Step 2: Run and confirm compile failure**

Run: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.WatchRegistryTest"`

- [ ] **Step 3: Implement**

- [ ] **Step 4: Run the tests.** Expected: PASS.

- [ ] **Step 5: Lint, size, safeguard, commit**

Commit `feat(cli): keep one cs-ide watch per git root`.

---

### Task 5: Project watch state and monitor sync

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceWatch.kt`
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cleanup/InventoryMonitorSync.kt`
- Test: create `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceWatchScopeTest.kt` (≤ 190), `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceWatchInventoryTest.kt` (≤ 210), `core/src/test/kotlin/com/codescene/jetbrains/core/cleanup/InventoryMonitorSyncTest.kt` (≤ 150)

**Interfaces:**
- Consumes: `WatchInventory`, `WorkspacePaths.rootKey`, `WorkspacePaths.normalizeRel` (Task 2); `IDeltaCacheService`, `ILogger`, `pathCacheKey`; test double `InMemoryDeltaCacheService`.
- Produces:

```kotlin
sealed interface WatchEffect {
    data class FetchInventory(val repoRoot: String, val generation: Long) : WatchEffect
    data class ScheduleConfirm(val repoRoot: String, val token: Long, val delayMs: Long) : WatchEffect
    data class ReportLost(val repoRoot: String) : WatchEffect
    data class SyncMonitor(val repoRoot: String, val absolutePaths: Set<String>) : WatchEffect
    data class Reseed(val repoRoot: String) : WatchEffect
}

data class ResolvedPath(val repoRoot: String, val absolutePath: String)

class WorkspaceWatch(private val windows: Boolean) {
    fun setScopes(scopes: Map<String, List<String>>): List<WatchEffect>
    fun scopes(): Map<String, List<String>>
    fun watchSent(repoRoot: String): List<WatchEffect>
    fun confirmDue(repoRoot: String, token: Long): List<WatchEffect>
    fun inventoryPushed(inventory: WatchInventory): List<WatchEffect>
    fun inventoryFetched(inventory: WatchInventory, generation: Long): List<WatchEffect>
    fun inventoryFetchFailed(repoRoot: String, notWatched: Boolean): List<WatchEffect>
    fun headObserved(repoRoot: String, branch: String?, revision: String?): List<WatchEffect>
    fun progressIdle(): List<WatchEffect>
    fun resolve(repoRoot: String, relPath: String): ResolvedPath?
    fun isWatched(repoRoot: String): Boolean
    fun inventory(repoRoot: String): Set<String>?

    companion object {
        const val CONFIRM_DELAY_MS = 10_000L
    }
}

class InventoryMonitorSync(
    private val deltaCacheService: IDeltaCacheService,
    private val logger: ILogger,
) {
    fun sync(repoRoot: String, inventoryPaths: Set<String>, openPaths: Set<String>): Int
}
```

`WorkspaceWatch` guards its state with one lock, like the pipeline. Mutations come only from the project consumer; the lock lets the poller thread call `isWatched`. Each root state holds the project's local root form, its paths, the inventory (relative paths, null until the first push or fetch), the pending confirm token (0 for none), the push generation, and the last head. Every effect carries the local root form. Matching an incoming root uses `rootKey`; unknown roots return no effects.

- `setScopes`: a new root, or a root whose paths changed, gets a fresh token and `ScheduleConfirm(root, token, CONFIRM_DELAY_MS)`. A removed root is dropped with no effect: its monitor entries stay until the poller's `StaleItemCleanup` runs again for that unwatched root, or Phase 4.
- `watchSent`: a fresh token and `ScheduleConfirm`.
- `confirmDue`: only the current token emits `FetchInventory(root, generation)`, and clears the token.
- `inventoryPushed`: increments the generation, clears the token, replaces the inventory with the files inside the project's scope, and emits `SyncMonitor(root, absolute paths)`. An empty push also emits `FetchInventory(root, generation)` after it.
- `inventoryFetched`: ignored when `generation` is not the root's current generation; otherwise applied like a push, without the empty-push fetch.
- `inventoryFetchFailed`: `ReportLost(root)` when `notWatched`, else nothing. The inventory is kept.
- `headObserved`: the first value for a root is recorded silently. A different branch or revision afterwards emits `Reseed(root)` then a fresh `ScheduleConfirm`.
- `progressIdle`: `SyncMonitor` for every root with a non-null inventory.
- `resolve`: normalizes `relPath`; returns `ResolvedPath(localRoot, localRoot.trimEnd('/', '\\') + "/" + rel)` when the path is inside one of the root's paths (`.` covers all; otherwise equal to an entry or under it plus `/`, ignoring case on Windows), else null.
- `isWatched`: true when the root has a non-null inventory.

`InventoryMonitorSync.sync` compares `pathCacheKey`s. It hides (`setIncludeInCodeHealthMonitor(path, false)`) every `getAll()` entry under `repoRoot` (key prefix `pathCacheKey(repoRoot).trimEnd('/') + "/"`) that is neither in `inventoryPaths` nor in `openPaths`. Then it calls `setIncludeInCodeHealthMonitor(path, true)` for every inventory path. It returns the number hidden and logs `monitor sync for <root>: hidden=<n>` at info under `InventoryMonitorSync`. The order of syncs and deltas does not matter: a later accepted delta re-includes its entry through `put`, and a later sync converges again.

- [ ] **Step 1: Write the failing tests**

`WorkspaceWatchScopeTest`:

1. `new roots schedule one confirm each`: two roots give two `ScheduleConfirm` with delay 10_000 and distinct tokens.
2. `unchanged scopes emit nothing and changed paths schedule a new confirm`.
3. `a removed root is no longer resolved or watched`.
4. `only the latest confirm token fetches`: after `setScopes` then `watchSent`, the first token gives `[]`; the second gives `[FetchInventory("/repo", 0)]`; calling it again gives `[]`.
5. `watchSent matches another root form on Windows and ignores unknown roots`: scope root `C:/repo`, `watchSent("c:\\Repo")` gives a `ScheduleConfirm("C:/repo", …)`; `watchSent("/other")` gives `[]`.
6. `resolve maps in-scope paths to the local root`: scope `{"C:/repo": ["src"]}` on Windows; `resolve("c:\\repo", "src\\A.kt")` is `ResolvedPath("C:/repo", "C:/repo/src/A.kt")`; `resolve("C:/repo", "test/A.kt")` and `resolve("C:/repo", "srcx/A.kt")` are null; with scope `["."]` every path resolves.
7. `isWatched becomes true after the first push or fetch`.

`WorkspaceWatchInventoryTest`:

1. `a push replaces the inventory filtered to the scope`: scope `["src"]`; push `{src/A.kt, test/B.kt}` gives `[SyncMonitor("/repo", {"/repo/src/A.kt"})]` and `inventory("/repo") == {"src/A.kt"}`; a second push `{src/C.kt}` leaves exactly `{"src/C.kt"}`.
2. `a push clears the pending confirm`.
3. `an empty push syncs and then fetches`: gives `[SyncMonitor("/repo", {}), FetchInventory("/repo", 1)]`.
4. `a fetch is applied only when no push arrived since it was issued`: confirm gives `FetchInventory(gen 0)`; `inventoryFetched(…, 0)` applies; next confirm, a push (gen 1), then `inventoryFetched(…, 0)` gives `[]` and keeps the pushed set.
5. `a not-watched fetch failure reports the root as lost and keeps the inventory`; any other failure gives `[]`.
6. `HEAD tracking`: first `headObserved("/repo", "main", "c1")` gives `[]`; same again `[]`; `("/repo", "feature", "c1")` gives `[Reseed("/repo"), ScheduleConfirm(…)]`; `("/repo", "feature", "c2")` the same shape; unknown root `[]`.
7. `progressIdle syncs every root with an inventory`: two roots, one pushed, gives one `SyncMonitor`.
8. `a push for a root outside the scopes emits nothing`.

`InventoryMonitorSyncTest` (entries built with differing head and current content and a delta with `scoreChange` 1.0, as in `StaleItemCleanupTest`):

1. `hides entries under the root outside inventory and open files`: entries `/repo/a.kt`, `/repo/b.kt`, `/repo/c.kt`; inventory `{/repo/a.kt}`, open `{/repo/b.kt}`; returns 1 and `getAll()` lists `a.kt` and `b.kt`.
2. `leaves other roots and sibling prefixes alone`: entries `/other/x.kt`, `/repo2/y.kt` stay.
3. `re-includes a hidden entry that is back in the inventory`.
4. `compares Windows paths by cache key`: entry `C:/Repo/src/A.kt`, root `c:\repo`, inventory `{C:\Repo\src\A.kt}` keeps it.
5. `inventory paths without a cache entry create nothing`.

- [ ] **Step 2: Run and confirm compile failure**

Run: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.WorkspaceWatch*" --tests "com.codescene.jetbrains.core.cleanup.InventoryMonitorSyncTest"`

- [ ] **Step 3: Implement**

```kotlin
fun inventoryPushed(inventory: WatchInventory): List<WatchEffect> =
    synchronized(lock) {
        val state = stateFor(inventory.repoRoot) ?: return emptyList()
        state.generation++
        state.pendingToken = 0
        val effects = mutableListOf<WatchEffect>(apply(state, inventory.files))
        if (inventory.files.isEmpty()) effects += WatchEffect.FetchInventory(state.localRoot, state.generation)
        effects
    }
```

- [ ] **Step 4: Run the tests.** Expected: PASS. Also run `--tests "com.codescene.jetbrains.core.cleanup.*"` so `StaleItemCleanupTest` still passes.

- [ ] **Step 5: Lint, size, safeguard, commit**

Commit `feat(cli): track cs-ide watch inventory per project`.

---

### Task 6: Platform wiring

**Files:**
- Modify: `src/main/kotlin/com/codescene/jetbrains/platform/cli/CsIdeServerService.kt`
- Create: `src/main/kotlin/com/codescene/jetbrains/platform/watch/CsIdeWatchService.kt`
- Create: `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceWatchCoordinator.kt`
- Modify: `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceReviewService.kt`
- Modify: `src/main/kotlin/com/codescene/jetbrains/platform/listeners/ProjectStartupActivity.kt`
- Modify: `src/main/kotlin/com/codescene/jetbrains/platform/git/PeriodicChangeListerService.kt`

**Interfaces:**
- Consumes: Tasks 1–5.
- Produces:
  - `CsIdeServerService`: `fun watchClient(): WatchClient` (`WatchClient(server::notifyInSession, server::notifyIfRunning, CsIdeTransport(server::request))`), `fun addSessionStartListener(listener: (Long) -> Unit): AutoCloseable`, `fun liveSession(): Long`, `fun cooldownRemainingMs(): Long`.
  - `@Service(Service.Level.APP) class CsIdeWatchService : Disposable` with `fun setProjectScopes(project: String, scopes: Map<String, List<String>>)`, `fun removeProject(project: String)`, `fun reportLost(repoRoot: String)`, `fun addWatchSentListener(listener: (String) -> Unit): AutoCloseable`, `companion object { fun getInstance(): CsIdeWatchService }`.
  - `WorkspaceWatchCoordinator(project: Project, scope: CoroutineScope, enqueue: (WatchEvent) -> Unit, reseed: (String) -> Unit)` with `fun start(parent: Disposable)`, `fun handle(event: WatchEvent)`, `fun onNotification(method: String, params: JsonNode)`, `fun observe(repoRoot: String, relPath: String): ObservedFile?`, `fun isWatched(repoRoot: String): Boolean`, `fun dispose()`, and `sealed interface WatchEvent` with `RootsChanged`, `HeadChanged(repoRoot, branch, revision)`, `WatchSent(repoRoot)`, `ConfirmDue(repoRoot, token)`, `InventoryFetched(inventory, generation)`, `InventoryFetchFailed(repoRoot, notWatched)`.
  - `WorkspaceReviewService`: `fun startWatching()`, `fun isWatched(repoRoot: String): Boolean`.

**`CsIdeWatchService`** (application). Holds `WatchRegistry(SystemInfo.isWindows)`, `CsIdeServerService.getInstance().watchClient()`, a `Channel<WatchServiceEvent>(Channel.UNLIMITED)` and a `CoroutineScope(SupervisorJob() + Dispatchers.IO)` with one consumer launched in `init`. Events: `SetProject`, `RemoveProject`, `SessionStarted(session)`, `SessionLost`, `Tick`, `Lost(repoRoot)`. In `init` it registers `addSessionStartListener { trySend(SessionStarted(it)) }` and `addSessionListener { trySend(SessionLost) }`. The consumer:

- `SetProject` / `RemoveProject` / `SessionStarted` / `Lost`: run the registry call and execute its commands.
- `SessionLost`: when `registry.hasDesired()`, schedule a tick in `WatchRegistry.FIRST_RETRY_MS`.
- `Tick`: clear the scheduled flag, then execute `registry.pending(server.liveSession())`.
- `Watch`: `val session = watchClient.watchFiles(root, paths)`, `registry.sent(root, paths, session)`, log `cs-ide watch started for <root> paths=<paths> session=<session>`, then call each sent listener with `root` (each in `runCatching`). On any exception log warn `cs-ide watch send failed for <root>: <message>; retry in <ms>ms` and schedule a tick in `WatchRegistry.retryDelayMs(server.cooldownRemainingMs())`.
- `Stop`: `watchClient.stopWatchFiles(root)`, `registry.stopped(root)`, log `cs-ide watch stopped for <root> (sent=<result>)`.
- `Lost`, when it returns a command: log warn `cs-ide watch lost for <root>; re-watching` before executing.
- At most one tick is scheduled at a time (`scope.launch { delay(ms); events.trySend(Tick) }`; the flag is only touched by the consumer).

`dispose` closes the two registrations and the channel and cancels the scope. It sends nothing.

**`WorkspaceWatchCoordinator`** (project). Holds `WorkspaceWatch(SystemInfo.isWindows)`, `WorkspaceFileReader(openText, readBytes)` where `openText` is the Phase 2 `ReadAction` + `LocalFileSystem.findFileByPath` + `FileDocumentManager.getCachedDocument(file)?.text`, and `readBytes` is `Files.readAllBytes(Path.of(path))` when `Files.isRegularFile` (null otherwise or on `IOException`). Also `InventoryMonitorSync(PlatformDeltaCacheService.getInstance(project), Log)` and `OpenFilesObserverAdapter(project)`. The project key is `project.locationHash`.

- `start(parent)`: `project.messageBus.connect(parent)` subscribes `ModuleRootListener.TOPIC` (`rootsChanged` → `enqueue(RootsChanged)`; delivered on the EDT), `VcsRepositoryManager.VCS_REPOSITORY_MAPPING_UPDATED` (`mappingChanged` → `enqueue(RootsChanged)`), and `GitRepository.GIT_REPO_CHANGE` (`repositoryChanged(repo)` → `enqueue(HeadChanged(repo.root.path, repo.currentBranchName, repo.currentRevision))`). It registers `CsIdeWatchService.getInstance().addWatchSentListener { enqueue(WatchSent(it)) }` and enqueues one `RootsChanged`. Git repositories may be empty right after startup; `VCS_REPOSITORY_MAPPING_UPDATED` arrives once they load.
- `RootsChanged`: inside `ReadAction.compute`, read `ProjectRootManager.getInstance(project).contentRoots.map { it.path }` and `GitRepositoryManager.getInstance(project).repositories`. Compute `WatchScopes.scopes(gitRoots, contentRoots, SystemInfo.isWindows)`, execute `watch.setScopes(scopes)`, call `CsIdeWatchService.getInstance().setProjectScopes(key, scopes)`, then `watch.headObserved` for every repository with its current branch name and revision.
- `HeadChanged`, `WatchSent`, `ConfirmDue`, `InventoryFetched`, `InventoryFetchFailed`: the matching `WorkspaceWatch` call; execute its effects.
- `onNotification`: `cs-ide/watchInventoryChanged` → `WatchNotices.inventory(params)?.let(watch::inventoryPushed)`, log `cs-ide inventory for <root>: <n> files (push)`; `cs-ide/reviewProgress` with `count == 0` → `watch.progressIdle()`.
- Effects:
  - `FetchInventory(root, gen)`: `watchClient.getWatchInventory(root).whenComplete { inv, err -> … }` enqueues `InventoryFetched(inv, gen)` or `InventoryFetchFailed(root, WatchClient.isNotWatched(err))`. The completion runs on the reader thread and only enqueues. The handler logs `cs-ide inventory for <root>: <n> files (fetch)` or warn `cs-ide inventory fetch failed for <root>: <message>`.
  - `ScheduleConfirm(root, token, delay)`: `scope.launch { delay(delay); enqueue(ConfirmDue(root, token)) }`.
  - `ReportLost(root)`: `CsIdeWatchService.getInstance().reportLost(root)`.
  - `SyncMonitor(root, paths)`: `sync.sync(root, paths, openFilesObserver.getAllVisibleFileNames())`.
  - `Reseed(root)`: `reseed(root)`.
- `observe(root, rel)`: `watch.resolve(root, rel)`, then `isPathSupportedForReview(project, absolutePath)`, then `reader.current(absolutePath)`, then `ObservedFile(absolutePath, resolved.repoRoot, current)`.
- `dispose()`: closes the sent-listener registration and calls `serviceIfCreated<CsIdeWatchService>()?.removeProject(key)`.

**`WorkspaceReviewService`.** Adds `data class Watch(val event: WatchEvent) : IncomingReviewEvent`. The coordinator gets `enqueue = { incomingEvents.trySend(IncomingReviewEvent.Watch(it)) }` and `reseed = ::reseed`. The pipeline gets `observe = coordinator::observe`. The consumer handles `Notification` with `pipeline.onNotification` and then `coordinator.onNotification`, and `Watch` with `coordinator.handle`. `reseed(root)` calls `pipeline.forgetAccepted(root)`, reads `FileDocumentManager.getInstance().unsavedDocuments` in a `ReadAction` and maps each to its file path and text. It keeps paths whose `pathCacheKey` starts with `pathCacheKey(root).trimEnd('/') + "/"` and that pass `isPathSupportedForReview`, calls `submitter.submitEditor(path, text)` for each, and logs `HEAD moved in <root>; reseeding <n> unsaved documents`. `startWatching()` calls `coordinator.start(this)` once (an `AtomicBoolean`). `isWatched(root)` delegates. `dispose` calls `coordinator.dispose()` before closing registrations, the channel and the scope.

**`ProjectStartupActivity`.** Calls `WorkspaceReviewService.getInstance(project).startWatching()` right before `periodicChangeListerService.start()`.

**`PeriodicChangeListerService`.** In `pollChangedFiles`, run `staleItemCleanup.cleanupStaleItems` and its log line only when `!WorkspaceReviewService.getInstance(project).isWatched(gitRoot)`. `submitPaths` is unchanged.

IntelliJ APIs, verified in 2024.3.7 jars: `ModuleRootListener.TOPIC` / `rootsChanged(ModuleRootEvent)`; `VcsRepositoryManager.VCS_REPOSITORY_MAPPING_UPDATED` / `VcsRepositoryMappingListener.mappingChanged()` (`intellij.platform.vcs.dvcs.impl`); `GitRepository.GIT_REPO_CHANGE` / `GitRepositoryChangeListener.repositoryChanged(GitRepository)`; `Repository.getRoot()`, `getCurrentBranchName()`, `getCurrentRevision()`; `GitRepositoryManager.getRepositories()`; `ProjectRootManager.getContentRoots()`; `FileDocumentManager.getCachedDocument(VirtualFile)`, `getUnsavedDocuments()`, `getFile(Document)`; `serviceIfCreated<T>()` (`com.intellij.openapi.components`).

- [ ] **Step 1: Implement the six files**

- [ ] **Step 2: Build and run every test**

Run: `.\gradlew.bat test` (core and platform), then `.\gradlew.bat :core:ktlintCheck -PktlintFailOnError=true :ktlintCheck`, then `bb -f .github/check-class-size-mine.clj`.

Expected: all pass. `rg "withBackgroundProgress" src/main` lists only `ReviewProgressIndicator.kt` and `IntelliJProgressService.kt`, as before.

- [ ] **Step 3: Prepare `runIde`**

Kill any leftover sandbox IDE first (it locks `core/build/libs/*.jar`):

```powershell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*idea-sandbox*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

In a worktree, copy the gitignored webview bundle: `Copy-Item -Recurse C:\Git\codescene-jetbrains\src\main\resources\cs-cwf src\main\resources\cs-cwf`. If a parallel wave just ran, `.\gradlew.bat --stop` first. Then `.\gradlew.bat runIde`. The log is `build\idea-sandbox\IC-2024.3.7\log\idea.log`.

- [ ] **Step 4: `runIde` smoke checklist**

Prepare a test repository with `origin/HEAD` set, a feature branch with one committed change, one modified tracked file and one new untracked `.kt` file that is never opened. Open it in the sandbox IDE.

User, in the IDE:

- U1. Within a minute the monitor lists the committed change, the modified file and the untracked file, without opening any of them.
- U2. One `CodeScene analysis (N remaining)` indicator appears for the first batch and disappears.
- U3. With nothing changing for 30 seconds the indicator does not come back.
- U4. Typing in an open editor updates annotations, Code Vision and the monitor (buffer path still works), and ACE still runs.
- U5. A new untracked `.kt` file created outside the IDE appears in the monitor; restoring the modified file with `git checkout -- <file>` outside the IDE removes it from the monitor.
- U6. With an unsaved edit in an open editor, switching branch keeps that editor's annotations and the monitor shows the new branch's changes.
- U7. Open a second project from another repository: both work. Close one with File | Close Project: the other keeps updating.

Coordinator, from a shell and `idea.log`:

- C1. Exactly one `cs-ide` child: `Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*clojure.main*codescene.cli.ide*' }` returns one process whose `ParentProcessId` is the sandbox IDE.
- C2. `idea.log` has `cs-ide watch started for <root> paths=[.] session=1` and `cs-ide inventory for <root>: <n> files (push)`, where `n` is the number of supported files in `git diff --name-only $(git merge-base HEAD origin/HEAD)` plus `git ls-files --others --exclude-standard`.
- C3. After an external `Add-Content` to a clean, unopened tracked `.kt` file, a new `cs-ide inventory for <root>: <n+1> files (push)` line appears.
- C4. `Stop-Process -Id <cs-ide pid> -Force`: `idea.log` shows `cs-ide server exited`, then within 35 s `cs-ide watch started for <root> … session=2` and a new inventory push; C1 again returns exactly one process.
- C5. After U6, `idea.log` has `HEAD moved in <root>; reseeding 1 unsaved documents`.
- C6. After U7's close, `idea.log` has `cs-ide watch stopped for <closed root> (sent=true)`.
- C7. During U3, no new `cs-ide inventory` or `watch started` lines; `Poll found` lines continue every 9 s.
- C8. `idea.log` has no exception mentioning `CsIdeWatchService`, `WorkspaceWatchCoordinator` or `WorkspaceWatch`, and no repeating `watch send failed` lines.
- C9. After exiting the IDE, the C1 query returns nothing.

- [ ] **Step 5: Safeguard and commit**

Commit `feat: watch git roots through cs-ide`. The hand-off lists the result of every U and C line.

---

### Task 7: Live watch and parity tests

**Files:**
- Create: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/CsIdeLiveServer.kt` (test helper, ≤ 90)
- Create: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WatchGitFixture.kt` (test helper, ≤ 120)
- Create: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WatchLiveTest.kt` (≤ 250)
- Create: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WatchInventoryParityLiveTest.kt` (≤ 250)

**Interfaces:**
- Consumes: `CsIdeServer.notifyInSession` / `notifyIfRunning` (Task 1), `WatchClient`, `WatchNotices`, `WatchInventory` (Task 2), `WorkspaceReviewNotices`, `GitBlobSha`, `CsIdeLaunch`, `SystemCsIdeProcessFactory`.
- Produces: `object CsIdeLiveServer { fun start(): CsIdeServer; fun stop(server: CsIdeServer) }` (launch and device-id warm-up as in `WorkspaceReviewLiveTest`, waits up to 15 s for the child to die), and `class WatchGitFixture(root: Path)` with `init()`, `write(rel, content)`, `delete(rel)`, `commitAll(message)`, `checkoutNew(branch)`, `setOriginHead(branch)` (`update-ref refs/remotes/origin/<branch> HEAD` and `symbolic-ref refs/remotes/origin/HEAD refs/remotes/origin/<branch>`). `init` runs `git init`, `git symbolic-ref HEAD refs/heads/main` and sets the test user.

Both classes run in the normal `:core:test` task with `CS_IDE_JAVA` honoured, like `WorkspaceReviewLiveTest`. One server per class. Each test registers its handler before sending.

`WatchLiveTest`:

1. `a new watch pushes the inventory before id-less results whose SHA is the disk bytes`: repo with `ReviewSubject.kt` committed on `main` and `setOriginHead("main")`; overwrite it with `complexKotlinCode`; `watchFiles(root, ["."])` returns a session greater than 0. The first `watchInventoryChanged` for the root (compared with `sameRoot`) has `files == {"ReviewSubject.kt"}` and arrives before any `fileReview` for the root. Then an id-less `fileReview` with `gitBlobSha == GitBlobSha.ofBytes(Files.readAllBytes(file))`, an id-less `deltaReview` with the same `newGitBlobSha`, and `reviewProgress` `count == 0`.
2. `an idle watch sends no progress`: after test 1's final progress, 15 s pass with no `reviewProgress`, `fileReview` or `deltaReview`.
3. `getWatchInventory matches the push`: `files == {"ReviewSubject.kt"}`.
4. `restoring the baseline sends a null delta`: write back the committed content; an id-less `fileReview` with the committed SHA, then a `deltaReview` with `result: null`, and `getWatchInventory` still lists the file.
5. `stopWatchFiles empties the inventory and unwatches`: `stopWatchFiles(root)` is true; a `watchInventoryChanged` with empty `files` arrives; `getWatchInventory(root)` fails and `WatchClient.isNotWatched(error)` is true.

`WatchInventoryParityLiveTest`. Each test builds a fresh repository, watches `["."]`, waits for the first push, and asserts the pushed set and the `getWatchInventory` result both equal the expected set. Then it stops the watch. The scenarios mirror `Git4IdeaChangeListerFeatureBranchTest` (and one status case from `Git4IdeaChangeLister`):

| Test | Setup | Expected | Lister |
| --- | --- | --- | --- |
| `origin default merge base only when main and develop exist` | `base.ts` on `main`; `setOriginHead("main")`; `develop` adds `develop-only.ts`; `feature-stacked` from `develop` adds `stacked.ts` | `{develop-only.ts, stacked.ts}` | same: origin default merge-base, `develop` not probed |
| `committed files via merge-base diff on feature branch` | `base.ts` on `main`; origin head `main`; `feature` commits `committed-only.ts` | `{committed-only.ts}` | same |
| `combines status and diff changes on feature branch` | as above with `committed.ts`; untracked `uncommitted.ts` | `{committed.ts, uncommitted.ts}` | same |
| `untracked file in the repository root` | `base.ts` on `main`; untracked `test.ts` | `{test.ts}` | same |
| `modified tracked file on the main line and deleted file excluded` | `a.ts`, `b.ts` on `main`; modify `a.ts`, delete `b.ts` | `{a.ts}` | same (staging `M`; deleted files are not listed) |
| `many untracked files in one directory are all listed` | `base.ts` on `main`; untracked `untracked1.ts` … `untracked10.ts` | all ten | documented difference: the lister keeps at most five per directory plus `filesToExcludeFromHeuristic` (`untracked2.ts`, `untracked5.ts` in its test) |

Phase 4 deletes `PeriodicChangeListerService` and `StaleItemCleanup` when this class passes in CI.

- [ ] **Step 1: Write the tests and helpers**

- [ ] **Step 2: Run**

Run: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.WatchLiveTest" --tests "com.codescene.jetbrains.core.cli.WatchInventoryParityLiveTest"`

Expected: PASS. If a parity row fails, do not change the expected set. Report the server's set in the hand-off; that is a parity finding for the coordinator.

- [ ] **Step 3: Lint, size, safeguard, commit**

Commit `test(cli): pin cs-ide watch and inventory parity`.

---

## Spec coverage

| Spec item (Phase 3) | Task |
| --- | --- |
| One `watchFiles` per git root, `relative-paths` from content roots, `.` at the repo root | 2 (scopes, wire), 4 (union per root), 6 (content roots, git roots) |
| Re-send the full list on root changes | 4 (`setProject`), 6 (`ModuleRootListener`, VCS mapping) |
| `stopWatchFiles` on project close and dispose | 4 (`removeProject`), 6 (`dispose`, `serviceIfCreated`) |
| `watchInventoryChanged` replaces the set wholesale | 5 (`inventoryPushed`), 7 (live) |
| `getWatchInventory` after a restart and after HEAD moves | 5 (confirm after `watchSent` and `headObserved`, generation rule), 6 (session-start resend), 7 (live) |
| Watch results are id-less and use the Phase 2 matcher, no second one | 3 (`observe` + `acceptsResult`) |
| Current bytes: document text when loaded, else raw disk bytes | 3 (`WorkspaceFileReader`), 6 (`getCachedDocument`, `Files.readAllBytes`) |
| Detect a lost watch via `getWatchInventory` | 5 (`inventoryFetchFailed`, empty push), 4 (`lost`), 7 (not-watched error) |
| Session loss and respawn | 1 (session ids, start listener), 4 (`sessionStarted`, `pending`, `retryDelayMs`), 6 (retry ticks) |
| Inventory consumers: monitor and stale cleanup | 5 (`InventoryMonitorSync`), 6 (poller skips `StaleItemCleanup` when watched) |
| Keep `PeriodicChangeListerService` until parity | 6 (unchanged `submitPaths`), 7 (parity test as the Phase 4 gate) |
| Ordering and threads | Global Constraints, 1 (start listener contract), 6 (channels) |
| One progress indicator; idle watch does not raise it | 6 (U2, U3, C7), 7 (`an idle watch sends no progress`) |
| No `baseline-revision` | 2 (`watchFiles` params test) |
| macOS JNA default unpack | No change |
