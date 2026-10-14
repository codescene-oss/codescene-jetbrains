# Phase 4: legacy deletion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Delete the plugin's own git observation, change listing, baseline resolution and baseline caches now that the `cs-ide` server watches git roots (Phase 3). After this phase the plugin submits only editor buffers; every other file reaches the Code Health Monitor through the server's watch. The 9 s poller and `StaleItemCleanup` go last, once the Phase 3 live tests pass in CI on Linux, Windows and macOS.

**Architecture:** Nothing new is designed. Phase 3's `WorkspaceWatch` and `InventoryMonitorSync` already prune the monitor for watched roots; this phase closes one gap (a root that leaves the project's scopes) and makes the two consumer coroutines survive a failing event. Core gains one small class (`ConsumerGuard`). The rest is deletion: classes, their tests, and the pass-throughs that only deleted code called. Platform code shrinks; no platform file gains a decision.

**Tech Stack:** Kotlin 1.9 / JVM 17 toolchain, JUnit 4 + MockK, IntelliJ Platform 2024.3+ (`pluginSinceBuild` 243, verified against 2024.3.7 jars), git4idea `idea/233.15619.7` source in `~/intellij-community/plugins/git4idea`.

Spec: `docs/superpowers/specs/2026-10-12-cs-ide-json-rpc-migration-design.md` (Phase 4). Packages: `docs/superpowers/plans/2026-10-14-phase4-agent-packages.md`. Server source: `../codescene/cli/src/codescene/devtools/ide/workspace/README.md` and the `workspace/watch*` namespaces.

## Global Constraints

- Business logic in `core`; platform code is a thin adapter. A platform file may branch only on IntelliJ state.
- No new code comments; keep existing ones. A comment that a deletion makes false is reworded in place, not removed (only `FileEventHandler.handleDelete`, Task 3).
- Every changed `.kt` file stays at or below 325 lines (`bb -f .github/check-class-size-mine.clj`; it checks every `.kt` file changed since master, counting every line). Budgets per file are in the tasks. `WorkspaceReviewPipeline.kt` is at its 250-line budget and only shrinks in this phase.
- Deleting a class deletes every test, fixture and test double that exists only for it. After each task, `rg` for every deleted top-level symbol over `src`, `core/src` and `benchmarks` returns nothing. Docs are not searched.
- `plugin.xml` does not change. Every deleted service is a light `@Service`; `PluginDescriptorTest` keeps passing.
- Until Task 5 starts (after the CI gate), do not delete or change: `PeriodicChangeListerService`, `StaleItemCleanup`, `Git4IdeaChangeLister`, `MainLineBranchResolver`, `Git4IdeaCommandExecutor`, `GitCommandExecutor`, `GitLineHandlerExt.kt`, `GitChangeLister.kt`, `CommittedChangesCommand.kt`, `FileSystemAdapter.kt`, `IGitChangeLister`, `ClosestMainLineMergeBase.kt`, `MainLineBranchContext.kt`, `CodesceneRepoConfig.kt`, `createWorkspacePrefix` and its group in `GitPathUtils.kt`, `isWatched` (three layers), `submitPaths` (two layers), `DiskSubmission`, `CachedReviewService.reviewByPath`.
- Never send `baseline-revision`. Threads are unchanged from Phase 3: callbacks only enqueue; one consumer per project (`WorkspaceReviewService`) and one for the application (`CsIdeWatchService`) on `Dispatchers.IO`.
- Path keys that may hold Windows paths go through `WorkspacePaths.rootKey` or `pathCacheKey`, never raw string equality.
- One commit per task. Do not commit a failing test on its own.
- On Windows, `make` hangs. Set `JAVA_HOME` to `C:\Users\marti\.jdks\graalvm-jdk-17.0.12` and run `.\gradlew.bat` with a 10-minute timeout. ktlint is `.\gradlew.bat :core:ktlintCheck -PktlintFailOnError=true`, plus `:ktlintCheck` when the change touches `src/` (max line length 120). Tests are `:core:test` and `:test`. Benchmarks compile with `.\gradlew.bat :benchmarks:jmhJar`.
- Before any Gradle run, kill a leftover sandbox IDE: `Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*idea-sandbox*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }`.
- CodeScene MCP `pre_commit_code_health_safeguard` before each commit.
- No `Co-authored` text anywhere. After committing, `git log -1 --format=%B` must not contain one; amend it away if it appears.
- Changed files have no UTF-8 BOM: `Format-Hex -Path <file> -Count 3` must not show `EF BB BF`.
- Never merge or push `feature/cs-12077-cs-ide-json-rpc`, and never commit in `C:\Git\codescene-jetbrains`.

## Server behaviour this plan relies on

Checked in `../codescene/cli/src/codescene/devtools/ide/workspace/README.md` and the Phase 3 live tests on 2026-10-14:

- The inventory is the supported regular files under the watch roots that differ from the merge-base (`git diff`), plus every untracked non-ignored file. Without a merge-base, analysis falls back to HEAD and the inventory is the tracked paths plus untracked files.
- The baseline ref is `baseline_branch` from `.codescene/config.json` (`origin/<branch>`, then `<branch>`) when set; otherwise the first existing of a valid `origin/HEAD` target, `origin/main`, `origin/master`, `main`, `master`. The merge-base is `git merge-base HEAD <ref>`. Refs are not fetched.
- A `.gitignore` or other ignore-source change rescans: the server rebuilds the inventory and rescores it. A `.codescene/code-health-rules.json` change keeps the inventory and rescores it. A `.codescene/config.json` change and HEAD, local branch, `origin` ref or packed-refs changes refresh settings; the server rebuilds and rescores only when the merge-base moved.
- Rescores arrive as id-less `fileReview` / `deltaReview` with the file's git-blob SHA. The Phase 2 matcher accepts a repeated id-less result for the same SHA (`WorkspaceReviewMatching.acceptsResult` does not check `accepted`), so a rescore after a rules change replaces the monitor entry.
- A new or re-sent watch pushes `watchInventoryChanged` before its results. A lost watch is detected by `getWatchInventory` and re-watched within 60 s (Phase 3).

IntelliJ behaviour this plan relies on (git4idea `idea/233.15619.7`):

- `GitCheckoutOperation.doExecute` calls `saveAllDocuments()` before checking out. A branch switch from the IDE therefore leaves no unsaved documents, and Phase 3's reseed logs `reseeding 0`. A checkout from a terminal leaves them unsaved and logs `reseeding 1`. That explains Phase 3 smoke C5; no code change.

## Resolved decisions

- **CI gate.** The user pushes the integration branch now so #194 runs the Phase 3 live tests. Task 5 (poller, `StaleItemCleanup`, git listing) starts only when #194 has a run whose head contains `407e97a` (`test(cli): pin cs-ide watch and inventory parity`) and whose `Test` jobs on `ubuntu-latest`, `windows-latest` and `macos-latest` all succeeded. `WatchLiveTest` and `WatchInventoryParityLiveTest` have no skip conditions and run in `:core:test` under `./gradlew check`, so a green job means they passed. "CodeScene Code Coverage (master)" is not part of the gate; the coordinator reports its state. (User decision: push now.)
- **What waits for the gate.** Everything except Tasks 5 and 6 proceeds without it. Task 6 (disk submissions) depends on Task 5, because the poller is a `submitPaths` caller, so it is gated by dependency.
- **Disk submissions are deleted.** After Task 5 the only `submitPaths` caller is `CachedReviewService.reviewByPath`, reached from `replayQueuedReview` when the file has no editor. Task 6 deletes `reviewByPath`, both `submitPaths`, `DiskSubmission`, `queueDisk` and the `disks` parameter, and simplifies the matcher for the invariant that a `SubmittedDisk` comes only from `observedState` and is never in flight. A queued review whose editor has closed is dropped; the watch reviews closed files. (User decision.)
- **Phase 3 follow-ups.** Included: a per-event guard in both consumer loops (Task 2). Deferred explicitly (user decision): the nested-root monitor flapping (an outer root's sync hides a nested watched root's entries until the nested root's next sync), logging inventory pushes only for the project's own roots, one shared `cs-ide/reviewProgress` constant, the three `WorkspaceWatch` rule exemptions in `.codescene/code-health-rules.json` (two of them duplicate the global fallback), and logging the process exit code when the stream closes first (Phase 3 smoke C4; recovery is correct, only the log is thin). Smoke C5 is explained above and needs no change.
- **Rules and ignore changes with unsaved buffers.** With `CodesceneRepoFilesListener` and `GitIgnoreFilesListener` gone, the server rescans or rescores inventory files after `.gitignore` or `.codescene/*.json` changes. An unsaved buffer keeps its score until its next edit; before, the plugin invalidated and re-reviewed it. Accepted and recorded in DECISIONLOG. (User decision.)
- **Monitor pruning without the poller.** `InventoryMonitorSync` already prunes watched roots on every applied inventory and on `reviewProgress` `count: 0`; it does not depend on `StaleItemCleanup`. Projects or content roots with no git root have no monitor entries to prune: `WorkspaceReviewSubmitter.locate` needs a git root, and watch results only exist for watched roots. A lost watch keeps its last entries until the re-watch push (≤ 60 s) syncs them. The gap: a root that leaves the project's scopes (content root or repository removed) is dropped by `WorkspaceWatch.setScopes` without a sync, and the poller's cleanup used to catch it for the project's base root. Task 5 makes `setScopes` emit `SyncMonitor(root, emptySet())` for a removed root that had an inventory, which hides its entries except open files.
- **Startup.** `ProjectStartupActivity.runStartup` keeps the telemetry status log, the settings change listener, the plugin state listener, `FileChangeListener` (rename, move and delete through `FileEventProcessor`), `WorkspaceReviewService.startWatching()`, tool-window telemetry and the ACE preflight. It loses `GitChangeObserverService.start()` (Task 1) and `PeriodicChangeListerService.start()` (Task 5).
- **Kept git service.** `Git4IdeaGitService` keeps `getRepoRoot`, `getRepoRelativePath` (`WorkspaceReviewService.locate`) and `isIgnored` (the project-content filter in `CodeSmellUtils.isFileSupportedForAnalysis`), with `resolveRepository`, `findDeepestMatchingRepository` and `getRepositoryContext`. `IGitService` shrinks to those three. `Git4IdeaGitServiceTest` covers only kept methods and stays unchanged.
- **CLI cache helpers.** `isExpiredCliCacheEntry` stays: `PlatformCliCacheService` prunes the live ACE `.review-caches` directory with it. `resolveBaselineCliCacheFileName` goes; its only caller is the benchmark fixture, which inlines `"benchmark-base:$path"` so benchmark inputs stay identical. `resolveCliCacheFileName` returns its first argument; its three ACE call sites and the benchmark use the path directly. ACE cache file names are unchanged (the absolute path), so existing ACE caches stay valid. Delta caching is keyed by path and SHA in `DeltaCacheService` and is untouched; the baseline review cache was only read by deleted code.
- **`IOpenFilesObserver`** stays: `OpenFilesObserverAdapter` implements it and `WorkspaceWatchCoordinator` uses the adapter.
- **Settings and UI.** No setting, action or tool-window element refers to baselines, the poller or the observers. `HomeViewData.commitBaseline` and `MessageTypes.COMMIT_BASELINE` are webview protocol models and stay.
- **Records.** CHANGELOG `Unreleased` gets three `Changed` lines and DECISIONLOG gets `DEC-2026-10-14-001` (Task 5), because the merge-base order loses `develop`, `trunk`, `dev` and `development` and the reflog fallback, and a repository with none of the server's candidate refs now falls back to HEAD with every tracked file in the inventory.
- **Smoke.** Two `runIde` smokes: after the ungated deletions (Task 4) and after the gated ones (Task 7). Each is split into checks the user makes in the IDE (U lines, asked one at a time) and checks the coordinator makes from the shell and `idea.log` (C lines). Both reuse `C:\Temp\cs12077-smoke` (`repo-a` with `origin/HEAD`, branches `feature` and `other`, modified and untracked files and `src/AceProbe.java` for ACE; `repo-b` as the second project). ACE runs on C/C++, C#, Java, JavaScript and TypeScript, not Kotlin, and is started with its button. IntelliJ saves documents when its window loses focus, so checks that need an unsaved buffer keep focus in the IDE and use its built-in terminal.

## File structure

Deleted files are listed per task. Totals: Task 1 deletes 10 main and 12 test files; Task 3 deletes 4 main and 2 test files; Task 5 deletes 14 main and 21 test files.

`core/src/main/kotlin/com/codescene/jetbrains/core/`

| File | Change | Task | Budget |
| --- | --- | --- | --- |
| `util/ConsumerGuard.kt` (create) | Run one event; log and swallow non-control-flow exceptions | 2 | ≤ 35 |
| `git/GitPathUtils.kt` (modify) | Drop `gitRelativeComparisonKey` (Task 1), then the workspace-prefix group (Task 5) | 1, 5 | ≤ 86, then ≤ 40 |
| `review/FileEventHandler.kt` (modify) | Two caches | 3 | ≤ 50 |
| `contracts/IGitService.kt` (modify) | `getRepoRelativePath`, `getRepoRoot`, `isIgnored` | 3 | ≤ 10 |
| `util/CliCacheUtils.kt` (modify) | Only `isExpiredCliCacheEntry` | 3 | ≤ 10 |
| `cli/WorkspaceWatch.kt` (modify) | Removed root syncs to empty; no `isWatched` | 5 | ≤ 195 |
| `cli/WorkspaceReviewPipeline.kt` (modify) | Buffers only | 6 | ≤ 225 |
| `cli/WorkspaceReviewMatching.kt` (modify) | No disk-in-flight rules | 6 | ≤ 132 |
| `cli/WorkspaceReviewSubmitter.kt` (modify) | `submitEditor` only | 6 | ≤ 30 |

`src/main/kotlin/com/codescene/jetbrains/platform/`

| File | Change | Task | Budget |
| --- | --- | --- | --- |
| `listeners/ProjectStartupActivity.kt` (modify) | No observer start (1), no poller start (5) | 1, 5 | ≤ 160, then ≤ 157 |
| `review/WorkspaceReviewService.kt` (modify) | Guarded consumer (2), no `isWatched` (5), no disk reads or `submitPaths` (6) | 2, 5, 6 | ≤ 185, ≤ 182, ≤ 160 |
| `watch/CsIdeWatchService.kt` (modify) | Guarded consumer | 2 | ≤ 175 |
| `review/WorkspaceWatchCoordinator.kt` (modify) | No `isWatched` | 5 | ≤ 206 |
| `git/Git4IdeaGitService.kt` (modify) | No baseline code | 3 | ≤ 110 |
| `di/CodeSceneProjectServiceProvider.kt` (modify) | No `baselineReviewCacheService` | 3 | ≤ 58 |
| `listeners/FileEventProcessor.kt` (modify) | Two-cache `FileEventHandler` | 3 | ≤ 119 |
| `util/AceFileRefresh.kt`, `util/AceEntryOrchestrator.kt` (modify) | Use the path as the CLI file name | 3 | ≤ 68, ≤ 265 |
| `api/CachedReviewService.kt` (modify) | No `reviewByPath`; replay needs an editor | 6 | ≤ 118 |

Other:

| File | Change | Task | Budget |
| --- | --- | --- | --- |
| `benchmarks/src/jmh/kotlin/com/codescene/jetbrains/benchmarks/BenchmarkFixtures.kt` | Inline both cache-name helpers | 3 | ≤ 105 |
| `CHANGELOG.md` | Three `Unreleased` / `Changed` lines | 5 | — |
| `DECISIONLOG.md` | TOC line 8 and `DEC-2026-10-14-001` | 5 | — |
| `src/main/resources/META-INF/plugin.xml` | No change | — | — |

Test files changed or created are listed with budgets in each task.

---

### Task 1: Delete the git change observers

**Files:**

Delete (main, 10):
- `core/src/main/kotlin/com/codescene/jetbrains/core/git/GitChangeObserver.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/git/SavedFilesTracker.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/contracts/ISavedFilesTracker.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/git/GitChangeObserverService.kt` (with `scheduleGitChangeReview`)
- `src/main/kotlin/com/codescene/jetbrains/platform/git/GitChangeObserverAdapter.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/git/VfsEventBridge.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/git/GitRepoStateListener.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/git/SavedFilesTrackerAdapter.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/git/CodesceneRepoFilesListener.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/git/GitIgnoreFilesListener.kt`

Delete (tests, 12):
- `core/src/test/kotlin/com/codescene/jetbrains/core/git/GitChangeObserverTest.kt`
- `core/src/test/kotlin/com/codescene/jetbrains/core/git/GitChangeObserverPrePopulationTest.kt`
- `core/src/test/kotlin/com/codescene/jetbrains/core/git/GitChangeObserverIntegrationTest.kt`
- `core/src/test/kotlin/com/codescene/jetbrains/core/git/GitChangeObserverTestSupport.kt` (holds `MockGitService`)
- `core/src/test/kotlin/com/codescene/jetbrains/core/git/SavedFilesTrackerTest.kt`
- `src/test/kotlin/com/codescene/jetbrains/platform/git/VfsEventBridgeTest.kt`
- `src/test/kotlin/com/codescene/jetbrains/platform/git/GitRepoStateListenerTest.kt`
- `src/test/kotlin/com/codescene/jetbrains/platform/git/CodesceneRepoFilesListenerTest.kt`
- `src/test/kotlin/com/codescene/jetbrains/platform/git/GitIgnoreFilesListenerTest.kt`
- `src/test/kotlin/com/codescene/jetbrains/platform/git/GitChangeObserverServiceTest.kt`
- `src/test/kotlin/com/codescene/jetbrains/platform/git/GitChangeObserverCallbackTest.kt`
- `src/test/kotlin/com/codescene/jetbrains/platform/git/GitChangeObserverReviewSchedulingTest.kt`

Modify:
- `src/main/kotlin/com/codescene/jetbrains/platform/listeners/ProjectStartupActivity.kt` (≤ 160): remove the `GitChangeObserverService` import and the two lines that get and start it. Keep everything else in `runStartup` in its current order. Remove an import only when ktlint reports it unused.
- `core/src/main/kotlin/com/codescene/jetbrains/core/git/GitPathUtils.kt` (≤ 86): delete `gitRelativeComparisonKey`. Its only callers were `GitChangeObserver` and `GitRepoStateListener`.

**Interfaces:**
- Consumes: nothing new.
- Produces: nothing. `MainLineBranchResolver.invalidate` loses its only caller (`CodesceneRepoFilesListener`) and stays until Task 5 deletes the class. `FileEventHandler`, `PlatformBaselineReviewCacheService` and `IGitService` are untouched here (Task 3).

- [ ] **Step 1: Delete and edit**

`git rm` the 22 files, then make the two edits.

- [ ] **Step 2: Prove nothing refers to them**

Run: `rg -n "GitChangeObserver|SavedFilesTracker|ISavedFilesTracker|VfsEventBridge|GitRepoStateListener|CodesceneRepoFilesListener|GitIgnoreFilesListener|scheduleGitChangeReview|gitRelativeComparisonKey|MockGitService" src core/src benchmarks`

Expected: only KDoc mentions inside `PeriodicChangeListerService.kt` (deleted in Task 5). No code reference.

- [ ] **Step 3: Build and test**

Run: `.\gradlew.bat :core:test :test`, then `.\gradlew.bat :core:ktlintCheck -PktlintFailOnError=true :ktlintCheck`, then `bb -f .github/check-class-size-mine.clj`.

Expected: all pass. No remaining test referenced a deleted class (checked with `rg` on 2026-10-14).

- [ ] **Step 4: Safeguard and commit**

Commit `refactor: delete git change observers`.

---

### Task 2: Guard the consumer loops

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/util/ConsumerGuard.kt` (≤ 35)
- Create: `core/src/test/kotlin/com/codescene/jetbrains/core/util/ConsumerGuardTest.kt` (≤ 90)
- Modify: `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceReviewService.kt` (≤ 185)
- Modify: `src/main/kotlin/com/codescene/jetbrains/platform/watch/CsIdeWatchService.kt` (≤ 175)

**Interfaces:**
- Consumes: `ILogger`, platform `Log`, `com.intellij.openapi.diagnostic.ControlFlowException`.
- Produces: `class ConsumerGuard(logger: ILogger, service: String, isControlFlow: (Throwable) -> Boolean = { false })` with `fun <T : Any> handle(event: T, block: (T) -> Unit)`.

**`ConsumerGuard.handle`.** Calls `block(event)`. A `kotlin.coroutines.cancellation.CancellationException` is rethrown. Any other `Exception` for which `isControlFlow` is true is rethrown. Any other `Exception` is logged with `logger.warn("${event::class.simpleName} failed: ${e.message ?: e::class.java.name}", service)` and swallowed. `Error`s are not caught.

**Platform.** Each service holds one guard declared before its `init` block:

- `WorkspaceReviewService`: `ConsumerGuard(Log, "WorkspaceReviewService") { it is ControlFlowException }`. Move the `when (event)` body of the consumer into `private fun handleIncoming(event: IncomingReviewEvent)`; the loop becomes `for (event in incomingEvents) guard.handle(event, ::handleIncoming)`.
- `CsIdeWatchService`: `ConsumerGuard(Log, "CsIdeWatchService") { it is ControlFlowException }`. Move the `when (event)` body into `private fun handleEvent(event: WatchServiceEvent)`; the loop becomes `for (event in events) guard.handle(event, ::handleEvent)`. `sendWatch` keeps its own try/catch and retry.

- [ ] **Step 1: Write the failing tests**

`ConsumerGuardTest` uses a recording `ILogger` (an object that appends `message to service` for `warn` and ignores the rest):

1. `a failing event is logged and the next event still runs`: `handle("a") { throw IllegalStateException("boom") }`, then `handle("b") { handled += it }`. `handled == ["b"]`; warns are exactly `["String failed: boom" to "svc"]`.
2. `an exception without a message logs its class name`: `handle(1) { throw IllegalStateException() }` logs `"Int failed: java.lang.IllegalStateException"`.
3. `cancellation is rethrown and not logged`: a `CancellationException` from the block propagates (`assertThrows`); no warns.
4. `control-flow exceptions are rethrown and not logged`: with `isControlFlow = { it is UnsupportedOperationException }`, an `UnsupportedOperationException` propagates; no warns.
5. `errors are not caught`: an `AssertionError` from the block propagates; no warns.
6. `a successful event logs nothing`: the block runs once with the event; no warns.

Run: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.util.ConsumerGuardTest"`. Expected: FAIL (class missing).

- [ ] **Step 2: Implement the guard and wire both services**

- [ ] **Step 3: Build and test**

Run: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.util.ConsumerGuardTest"`, then `.\gradlew.bat :core:test :test`, ktlint for core and `src/`, class size.

Expected: all pass.

- [ ] **Step 4: Safeguard and commit**

Commit `fix: keep cs-ide event consumers alive after a failing event`.

---

### Task 3: Delete baseline resolution and baseline caches

**Files:**

Delete (main, 4):
- `core/src/main/kotlin/com/codescene/jetbrains/core/review/BaselineReviewCacheService.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/contracts/IBaselineReviewCacheService.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/util/GitReflogUtils.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/review/PlatformBaselineReviewCacheService.kt`

Delete (tests, 2):
- `core/src/test/kotlin/com/codescene/jetbrains/core/review/BaselineReviewCacheServiceTest.kt`
- `core/src/test/kotlin/com/codescene/jetbrains/core/util/GitReflogUtilsTest.kt`

Modify:
- `core/src/main/kotlin/com/codescene/jetbrains/core/contracts/IGitService.kt` (≤ 10): remove `getBranchCreationCommitCode` and `getBranchCreationCommitHash`.
- `src/main/kotlin/com/codescene/jetbrains/platform/git/Git4IdeaGitService.kt` (≤ 110): becomes `class Git4IdeaGitService(val project: Project) : IGitService` (no `@JvmOverloads` constructor, no `injectedMainLineBranchResolver`, no `gitCommandExecutor`, no `mainLineBranchResolver`). Delete `getBranchCreationCommitCode` (with its KDoc), `getBranchCreationCommitHash`, `getBaselineCommitSha`, `resolveHeadCommitSha`, `findMergeBaseWithMain`, `getBranchCreationCommitFromReflog`, `getRefLog` and the imports only they used (`resolveClosestMainLineMergeBase`, `parseBranchCreationCommitFromReflog`, `Repository`, `Git`, `GitCommand`). Keep `service`, `RepositoryContext`, the companion, `getRepoRelativePath`, `getRepoRoot`, `isIgnored`, `resolveRepository`, `findDeepestMatchingRepository`, `getRepositoryContext` unchanged.
- `core/src/main/kotlin/com/codescene/jetbrains/core/review/FileEventHandler.kt` (≤ 50): constructor `(deltaCache, reviewCache)`; remove the two `baselineReviewCache.updateKey` calls. In the existing `handleDelete` comment change only "All three caches are content-based" to "Both caches are content-based".
- `src/main/kotlin/com/codescene/jetbrains/platform/listeners/FileEventProcessor.kt` (≤ 119): `FileEventHandler(serviceProvider.deltaCacheService, serviceProvider.reviewCacheService)`.
- `src/main/kotlin/com/codescene/jetbrains/platform/di/CodeSceneProjectServiceProvider.kt` (≤ 58): remove `baselineReviewCacheService` and its two imports.
- `core/src/main/kotlin/com/codescene/jetbrains/core/util/CliCacheUtils.kt` (≤ 10): delete `resolveCliCacheFileName` and `resolveBaselineCliCacheFileName`; keep `isExpiredCliCacheEntry` unchanged.
- `src/main/kotlin/com/codescene/jetbrains/platform/util/AceFileRefresh.kt` (≤ 68): both `cliFileName` values become `normalizedPath`; drop the import.
- `src/main/kotlin/com/codescene/jetbrains/platform/util/AceEntryOrchestrator.kt` (≤ 265): `val fileName = filePath`, or pass `filePath` to `CodeParams` directly; drop the import.
- `benchmarks/src/jmh/kotlin/com/codescene/jetbrains/benchmarks/BenchmarkFixtures.kt` (≤ 105): `currentReviewPath` returns `repoRoot.resolve(relativePath).toString()`; `baselineReviewPath` returns `"benchmark-base:" + repoRoot.resolve(relativePath).toString()`; drop both imports.

Test updates:
- `core/src/test/kotlin/com/codescene/jetbrains/core/review/FileEventHandlerTest.kt` (≤ 62): construct with two caches; remove every `baselineReviewCache` mock and verify. The six tests keep their names and their delta and review cache assertions; the two delete tests still assert `setIncludeInCodeHealthMonitor(path, false)` and no `updateKey` or `invalidate` on either cache.
- `core/src/test/kotlin/com/codescene/jetbrains/core/util/CliCacheUtilsTest.kt` (≤ 35): delete the four `resolve*` tests; keep the two `isExpiredCliCacheEntry` tests unchanged.
- `core/src/test/kotlin/com/codescene/jetbrains/core/testdoubles/StubGitService.kt` (≤ 28) and the private `CliGitService` in `core/src/test/kotlin/com/codescene/jetbrains/core/util/GitIgnoreSupportTest.kt` (≤ 178): remove the two overrides.

**Interfaces:**
- Consumes: Task 1 (no remaining user of `baselineReviewCacheService` or the three-argument `FileEventHandler` outside `FileEventProcessor`).
- Produces: `IGitService { getRepoRelativePath; getRepoRoot; isIgnored }`; `FileEventHandler(deltaCache: IDeltaCacheService, reviewCache: IReviewCacheService)`; `Git4IdeaGitService(project: Project)`.

`Git4IdeaChangeLister` keeps its own private `resolveHeadCommitSha` and `findMergeBaseWithMain` and does not call `Git4IdeaGitService`, so it compiles unchanged until Task 5.

- [ ] **Step 1: Delete and edit**

- [ ] **Step 2: Prove nothing refers to them**

Run: `rg -n "BaselineReviewCache|baselineReviewCache|GitReflogUtils|parseBranchCreationCommitFromReflog|resolveBaselineCliCacheFileName|resolveCliCacheFileName|getBranchCreationCommit|getBaselineCommitSha|getRefLog|injectedMainLineBranchResolver" src core/src benchmarks`

Expected: no output. (`resolveHeadCommitSha` and `findMergeBaseWithMain` still appear in `Git4IdeaChangeLister.kt`; that is expected until Task 5.)

- [ ] **Step 3: Build and test**

Run: `.\gradlew.bat :core:test :test :benchmarks:jmhJar`, then ktlint for core and `src/`, then class size.

Expected: all pass; `Git4IdeaGitServiceTest`, `CodeSmellUtilsTest`, `GitIgnoreSupportTest` and the ACE tests are green without other edits.

- [ ] **Step 4: Safeguard and commit**

Commit `refactor: delete baseline resolution and caches`.

---

### Task 4: `runIde` smoke of the ungated state

Run by the coordinator with the user, on the integration branch after Tasks 1–3 are merged. No code changes. The result of every line is recorded under PKG-33 in the packages doc and committed.

- [ ] **Step 1: Prepare**

Kill a leftover sandbox IDE, `.\gradlew.bat --stop`, and in a worktree copy `C:\Git\codescene-jetbrains\src\main\resources\cs-cwf` into `src\main\resources\cs-cwf` (gitignored). Then `.\gradlew.bat runIde`. The log is `build\idea-sandbox\IC-2024.3.7\log\idea.log`. Open `C:\Temp\cs12077-smoke\repo-a` on branch `feature` and wait for the first inventory push. Note the log length before each check, so each C line reads only new lines.

- [ ] **Step 2: User checks (ask one at a time)**

- U1. Edits: typing in an open `.kt` file updates annotations, Code Vision and the monitor. In `src/AceProbe.java`, the ACE button produces a refactoring.
- U2. Saves: `Ctrl+S` after an edit keeps the annotations, and no second `CodeScene analysis` indicator appears for the same content.
- U3. External edit: with an unopened, unmodified tracked file (for example `src/App.kt`), append a line from a shell outside the IDE: it appears in the monitor within a minute. `git checkout -- src/App.kt` from the shell removes it.
- U4. Untracked files: a new `src/Smoke.kt` with a code smell created from the shell appears in the monitor; deleting it removes it.
- U5. `.gitignore`: adding `src/Untracked.kt` to `.gitignore` (in the IDE, then save) removes that file from the monitor; removing the line brings it back.
- U6. Branch switch, IDE: with an unsaved edit in an open editor, switch to `other` with the branch widget and back to `feature`. Annotations stay and the monitor shows each branch's changes.
- U7. Branch switch, terminal: with an unsaved edit and focus kept in the IDE, run `git checkout other` and `git checkout feature` in the IDE's built-in terminal. The unsaved editor keeps its annotations.
- U8. Rename and delete: renaming an open modified file in the project view moves its monitor entry; deleting it removes the entry.
- U9. Two projects: open `repo-b` in a second window; both update. Close `repo-b` with File | Close Project; `repo-a` keeps updating.

- [ ] **Step 3: Coordinator checks**

- C1. Exactly one `cs-ide` child: `Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*clojure.main*codescene.cli.ide*' }` returns one process whose `ParentProcessId` is the sandbox IDE.
- C2. No deleted class logs: `Select-String -Path idea.log -Pattern 'GitChangeObserver|VfsEventBridge|GitRepoStateListener|CodesceneRepoFilesListener|GitIgnoreFilesListener|SavedFilesTracker|BaselineReviewCache'` returns nothing new.
- C3. After U3's append, a `cs-ide inventory for <repo-a>: <n+1> files (push)` line; after the restore, `<n>`.
- C4. After U5's ignore, a push with one file fewer; after removing the line, one more.
- C5. After U6, `HEAD moved in <repo-a>; reseeding 0 unsaved documents` (the IDE saved first); after U7, `reseeding 1 unsaved documents`.
- C6. `Stop-Process -Id <cs-ide pid> -Force`: `cs-ide JSON-RPC closed` or `cs-ide server exited` is logged, then within 35 s `cs-ide watch started for <repo-a> … session=2` and a new push. C1 again returns one process. The user confirms U1 still works.
- C7. After U9's close, `cs-ide watch stopped for <repo-b> (sent=true)`.
- C8. No exception mentioning `WorkspaceReviewService`, `CsIdeWatchService`, `WorkspaceWatchCoordinator`, `FileEventProcessor` or `Git4IdeaGitService`, and no ` failed: ` lines from `ConsumerGuard`. Any that appear are listed with their stack.
- C9. `Poll found` lines continue every 9 s (the poller is still in place).
- C10. After exiting the IDE, the C1 query returns nothing.

- [ ] **Step 4: Record and commit**

Write each U and C result under PKG-33 in `docs/superpowers/plans/2026-10-14-phase4-agent-packages.md` (`git add -f`). Commit `docs: record CS-12077 phase 4 smoke`. A failing line blocks Task 5 until fixed in its own package.

---

### Task 5: Delete the poller and the plugin's git listing (CI-gated)

Starts only when the CI gate in Resolved decisions is met and Task 4 is recorded. The coordinator checks the gate:

```powershell
gh pr checks 194
gh run list --workflow Build --branch feature/cs-12077-cs-ide-json-rpc --limit 5 --json databaseId,headSha,conclusion
gh run view <databaseId> --json jobs --jq '.jobs[] | select(.name | startswith("Test")) | {name, conclusion}'
git merge-base --is-ancestor 407e97a <headSha>
```

All three `Test` jobs report `success` and the last command exits 0.

**Files:**

Delete (main, 14):
- `src/main/kotlin/com/codescene/jetbrains/platform/git/PeriodicChangeListerService.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/git/Git4IdeaChangeLister.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/git/Git4IdeaCommandExecutor.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/git/GitCommandExecutor.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/git/GitLineHandlerExt.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/git/MainLineBranchResolver.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/git/GitChangeLister.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/git/CommittedChangesCommand.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/git/FileSystemAdapter.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/git/ClosestMainLineMergeBase.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/git/MainLineBranchContext.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/git/CodesceneRepoConfig.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/contracts/IGitChangeLister.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/cleanup/StaleItemCleanup.kt`

Delete (tests, 21):
- `src/test/kotlin/com/codescene/jetbrains/platform/git/`: `Git4IdeaChangeListerTest.kt`, `Git4IdeaChangeListerFeatureBranchTest.kt`, `Git4IdeaChangeListerWindowsPathTest.kt`, `Git4IdeaChangeListerTestFixture.kt`, `Git4IdeaTestSupport.kt`, `MainLineBranchResolverTest.kt`
- `core/src/test/kotlin/com/codescene/jetbrains/core/git/`: `GitChangeListerTest.kt`, `GitCommittedChangesTest.kt`, `GitStatusChangesTest.kt`, `GitUntrackedFilesTest.kt`, `TestGitChangeLister.kt`, `TestGitChangeListerForCommitted.kt`, `TestGitChangeListerForStatus.kt`, `TestRepoMainLineContext.kt`, `GitTestSupport.kt`, `MainLineBranchContextTest.kt`, `ClosestMainLineMergeBaseTest.kt`, `CodesceneRepoConfigTest.kt`, `CommittedChangesCommandTest.kt`, `FileSystemAdapterTest.kt`
- `core/src/test/kotlin/com/codescene/jetbrains/core/cleanup/StaleItemCleanupTest.kt`

Modify:
- `src/main/kotlin/com/codescene/jetbrains/platform/listeners/ProjectStartupActivity.kt` (≤ 157): remove the `PeriodicChangeListerService` import and the two lines that get and start it. `startWatching()` stays where it is. Remove the `service` import only if ktlint reports it unused.
- `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceReviewService.kt` (≤ 182): remove `isWatched`.
- `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceWatchCoordinator.kt` (≤ 206): remove `isWatched`.
- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceWatch.kt` (≤ 195): remove `isWatched`. In `setScopes`, for each removed key, `roots.remove(key)`; when the removed state's `inventory` is not null, add `WatchEffect.SyncMonitor(removed.localRoot, emptySet())` to the effects, before any effects for remaining roots. `inventory(repoRoot)` stays (tests use it).
- `core/src/main/kotlin/com/codescene/jetbrains/core/git/GitPathUtils.kt` (≤ 40): delete `WorkspacePrefix`, `createWorkspacePrefix`, `isFileInWorkspace`, `convertGitPathToAbsolutePath`, `convertGitPathToWorkspacePath` and the `java.io.File` and `java.nio.file.Paths` imports. Keep `pathComparisonKey`, `pathCacheKey`, `pathFileName`, `LOG_FULL_PATHS_PROPERTY`, `logFullPathsEnabled`, `pathForLog`, `isPathUnderRoot`.
- `core/src/test/kotlin/com/codescene/jetbrains/core/git/GitPathUtilsTest.kt` (≤ 65): delete the seven `createWorkspacePrefix` and `isFileInWorkspace` tests and the temp-repo `setup` / `teardown` (they only served those tests, and `GitTestSupport` is deleted). The six `pathComparisonKey`, `pathCacheKey` and `pathForLog` tests stay unchanged.
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceWatchScopeTest.kt` (≤ 115):
  - `a removed root is no longer resolved or watched` becomes `a removed root syncs the monitor to empty and is no longer resolved`: after a push of `{"a.kt"}`, `setScopes(emptyMap())` returns exactly `[SyncMonitor("/repo", emptySet())]`, `resolve("/repo", "a.kt")` is null and `inventory("/repo")` is null.
  - New `a removed root without an inventory emits nothing`: `setScopes({"/repo": ["."]})`, then `setScopes(emptyMap())` returns an empty list.
  - New `a removed root matches its key on Windows`: with `windows = true`, scopes `{"C:/repo": ["."]}` and a push for `"c:\\Repo"`, `setScopes(emptyMap())` returns `[SyncMonitor("C:/repo", emptySet())]`.
  - `isWatched becomes true after the first push or fetch` becomes `the inventory is set by the first push or fetch`, asserting `inventory("/repo")` is null before, `{"a.kt"}` after the push, and `{"b.kt"}` after the fetch on the fresh watch.
- `CHANGELOG.md`: under `## [Unreleased]`, in the existing `- **Changed**` list after the CS-12077 line, add:
  - `` - the `cs-ide` server watches git roots and decides which files differ from the baseline; the plugin no longer polls git or lists changes itself (CS-12077) ``
  - `` - every untracked supported file appears in the Code Health Monitor, not at most five per directory (CS-12077) ``
  - `` - the baseline is the merge-base with `baseline_branch` from `.codescene/config.json`, else `origin/HEAD`, `origin/main`, `origin/master`, `main` or `master`; `develop`, `trunk`, `dev` and `development` are no longer probed and there is no reflog fallback (CS-12077) ``
- `DECISIONLOG.md`: TOC item `8. [DEC-2026-10-14-001: Let cs-ide own git watching and baselines](#dec-2026-10-14-001-let-cs-ide-own-git-watching-and-baselines)` and a new entry at the end in the existing format (`---`, heading, `[Back to Top](#table-of-contents)`, Date 2026-10-14, Decision Maker(s) Martin Säfsten, Status Accepted):
  - Decision: the plugin no longer observes git, lists changed files, resolves baselines or caches baseline reviews. The `cs-ide` server watches each git root, keeps the changed-file inventory and scores files against its own merge-base. The plugin submits only editor buffers and prunes the monitor from the server's inventory.
  - Context: Phase 3 ran the watch beside the 9 s poller until `WatchInventoryParityLiveTest` proved both produced the same files on Linux, Windows and macOS. Two implementations of change detection and baseline resolution had to agree.
  - Rationale: one implementation shared with the other IDE integrations; no git subprocesses from the plugin every 9 s; ignore, rules, config and HEAD changes handled where the inventory lives.
  - Consequences: every untracked supported file is listed. Merge-base candidates follow the server (`baseline_branch`, `origin/HEAD`, `origin/main`, `origin/master`, `main`, `master`); a repository whose main line is only `develop`, `trunk`, `dev` or `development` without `origin/HEAD` falls back to HEAD with every tracked file in the inventory unless `baseline_branch` is set. No reflog fallback. After a `.gitignore` or `.codescene/*.json` change, unsaved buffers keep their score until their next edit. Files without an editor are reviewed only by the watch.
  - Alternatives Considered: keep the poller as a fallback (rejected: two sources of truth and the poller's git load); keep the plugin's merge-base order and send `baseline-revision` (rejected: diverges from the other IDEs; the server's `baseline_branch` covers custom main lines).
  - Related Decisions: `DEC-2026-10-13-001`.

**Interfaces:**
- Consumes: the CI gate, Task 4's record.
- Produces: `WorkspaceWatch` without `isWatched`; `setScopes` may return `SyncMonitor(root, emptySet())` for removed roots. `WorkspaceWatchCoordinator` and `WorkspaceReviewService` without `isWatched`. The coordinator needs no change for the new effect; it already runs `SyncMonitor` through `InventoryMonitorSync.sync(root, paths, openFiles)`.

- [ ] **Step 1: Write the failing `WorkspaceWatchScopeTest` changes**

Run: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.WorkspaceWatchScopeTest"`. Expected: FAIL on the removed-root sync.

- [ ] **Step 2: Delete, edit and update records**

- [ ] **Step 3: Prove nothing refers to them**

Run: `rg -n "PeriodicChangeListerService|Git4IdeaChangeLister|Git4IdeaCommandExecutor|GitCommandExecutor|createGitLineHandler|MainLineBranchResolver|MainLineBranchContext|resolveClosestMainLineMergeBase|MAIN_LINE_BRANCH_NAMES|mainLineRefsInProbeOrder|parseBaselineBranchFromConfig|committedChangesLogArgs|FileSystemAdapter|IGitChangeLister|MAX_UNTRACKED_FILES_PER_LOCATION|shouldReviewFile|StaleItemCleanup|createWorkspacePrefix|WorkspacePrefix|isFileInWorkspace|convertGitPathTo|GitTestSupport|isWatched" src core/src benchmarks`

Expected: no output.

- [ ] **Step 4: Build and test**

Run: `.\gradlew.bat :core:test :test :benchmarks:jmhJar`, then ktlint for core and `src/`, then class size.

Expected: all pass, including `WatchLiveTest`, `WatchInventoryParityLiveTest`, `InventoryMonitorSyncTest` and `WorkspaceWatchInventoryTest` unchanged.

- [ ] **Step 5: Safeguard and commit**

Check `CHANGELOG.md` and `DECISIONLOG.md` have no BOM and that "Säfsten" is UTF-8 (`C3 A4`). Commit `refactor: delete the change poller and git listing`.

---

### Task 6: Drop disk review submissions

**Files:**
- Modify: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewPipeline.kt` (≤ 225)
- Modify: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewMatching.kt` (≤ 132)
- Modify: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewSubmitter.kt` (≤ 30)
- Modify: `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceReviewService.kt` (≤ 160)
- Modify: `src/main/kotlin/com/codescene/jetbrains/platform/api/CachedReviewService.kt` (≤ 118)
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewPipelineTest.kt` (≤ 300), `WorkspaceReviewPipelineFixture.kt` (≤ 93), `WorkspaceReviewPathKeyTest.kt` (≤ 75), `WorkspaceReviewWatchStateTest.kt` (≤ 130), `WorkspaceReviewWatchResultsTest.kt` (≤ 125), `WorkspaceReviewSubmitterTest.kt` (≤ 80)

**Interfaces:**
- Consumes: Task 5 (no poller).
- Produces:
  - `WorkspaceReviewPipeline.submit(repoRoot: String, buffers: List<BufferSubmission>)`. `DiskSubmission` and `queueDisk` are gone. `onProgress` only forwards to `sink.progress`.
  - `WorkspaceReviewMatching.shouldSend(state: PathState?, gitBlobSha: String): Boolean` (no `buffer` parameter): true when there is no state, the SHA differs, or the current submission is not a `SubmittedBuffer`; otherwise `!inFlight && !accepted`.
  - `failureMatches(state, key)`: true only when the current submission is a `SubmittedBuffer` and `key.id == submission.id`.
  - `afterReview(state) = state.copy(accepted = true)`.
  - `releasedByIdleProgress` is deleted.
  - `SubmittedDisk`, `observedState`, `awaitingDelta`, `acceptsResult`, `afterDelta` and `afterSessionLost` are unchanged.
  - `WorkspaceReviewSubmitter(pipeline, locate, isSupported)` with only `submitEditor`; `RepoLocation` stays.
  - `WorkspaceReviewService` loses `submitPaths` and the `openText` / `readBytes` lambdas (and the `LocalFileSystem` import if unused). `CachedReviewService` loses `reviewByPath`.

**Invariant.** A `SubmittedDisk` is created only by `observedState` (watch results) and is never in flight: `acceptIdLess` returns early while a buffer is in flight, `observedState` copies a non-in-flight state, and nothing else sets `inFlight` on it. The simplifications above rely on that.

**`CachedReviewService.replayQueuedReview(filePath)`.** With an editor that is not disposed: log `Replaying queued review with editor file=<name>` and `review(editor)` (unchanged). Otherwise log `Dropping queued review without editor file=<name>` at info and return.

- [ ] **Step 1: Update the tests**

`WorkspaceReviewPipelineFixture`: delete `disk(sha)`.

`WorkspaceReviewPipelineTest`: delete the `disk` path and `disk()` helper, and drop the third `submit` argument everywhere. Changes:
- `buffer and disk go in one call and resubmitting in-flight content does not send` becomes `resubmitting in-flight buffer content does not send`: two `submit` calls with `buffer("a")` send exactly one call `[ReviewFileWire(app, "id1", "a")]`.
- `accepted content is left out of a later batch` becomes `accepted buffer content is not sent again`: after `fileReview("id1", sha("a"))` and `deltaReview("id1", sha("a"))`, submitting `buffer("a")` sends nothing (`sent.size == 1`) and `activePaths()` is empty.
- `reviewFailed without an id clears only the in-flight disk submit` becomes `reviewFailed without an id is ignored`: with `buffer("a")` in flight, `reviewFailed(null, app)` leaves `activePaths() == {"/repo/src/App.kt"}` and records no failure.
- `progress count 0 releases disk paths so the same SHA sends again` becomes `progress is forwarded and count 0 does not release a buffer`: with `buffer("a")` in flight, progress `1` then `0` records counts `[1, 0]` and `activePaths()` still holds the buffer.
- `id-less results are accepted only for the disk SHA` and `an accepted disk SHA is not sent again but a buffer with that SHA is` are deleted (watch-result acceptance is covered by `WorkspaceReviewWatchResultsTest` and `WorkspaceReviewWatchStateTest`).
- `sessionLost resends content whose delta was not accepted and keeps accepted deltas`: submit `buffer("a")` and `BufferSubmission("/repo/src/Other.kt", "src/Other.kt", "b")` in one call (ids `id1`, `id2`); accept `fileReview("id1", sha("a"))` only, and both `fileReview` and `deltaReview` for `id2` (path `src/Other.kt`). After `sessionLost()`, the last event is `SESSION_LOST` and `activePaths()` is empty; submitting both buffers again sends exactly `[ReviewFileWire(app, "id3", "a")]`.
- Every other test keeps its assertions.

`WorkspaceReviewPathKeyTest`:
- `disk backslashes and watch forward slashes share one path state on Windows` becomes `a backslash buffer and a forward-slash watch result share one path state on Windows`: submit `BufferSubmission(winDisk, rel, "buf")`; accept `fileReview("id1", sha("buf"))` and `deltaReview("id1", sha("buf"))` for `rel` on `winRoot`; then an id-less `fileReview` for `rel` with `"disk-sha"` while `observe` returns `ObservedFile(winSlash, winRoot, CurrentFile("disk-sha", null))`. Two reviews are presented. Submitting `BufferSubmission(winDisk, rel, "buf")` again sends one more call, and `activePaths()` is exactly `{winDisk}` (one state, not two).
- `buffer and disk forms collapse to the latest submission path on Windows` becomes `buffer path forms collapse to the latest submission path on Windows`: `BufferSubmission(winSlash, rel, "buf")` then `BufferSubmission(winDisk, rel, "buf2")` leave `activePaths() == {winDisk}`.
- `paths differing only by case stay separate when not on Windows` keeps its assertions.

`WorkspaceReviewWatchStateTest`:
- `an accepted watch result dedups the same disk SHA but not a buffer` becomes `a buffer with the SHA of an accepted watch result is sent`: after the accepted id-less review and delta for `sha("a")`, `submit("/repo", [buffer("a")])` sends exactly `[ReviewFileWire(app, "id1", "a")]`.
- `sessionLost keeps an observed file whose delta was accepted` becomes `after sessionLost a null delta no longer pairs with a review accepted before it`: observe `sha("a")`, accept an id-less `fileReview` for `sha("a")` without a delta, call `sessionLost()`, then a null id-less `deltaReview` presents nothing.
- The other tests drop the third `submit` argument and keep their assertions.

`WorkspaceReviewWatchResultsTest`: `a disk SHA that is no longer current is dropped and the current one is accepted` becomes `a result for bytes that changed after an accepted watch result is dropped and the current one is accepted`: with current `d1`, an id-less `fileReview` for `d1` presents once; with current `d2`, a `fileReview` for `d1` presents nothing more and one for `d2` presents the second review. The other tests drop the third `submit` argument.

`WorkspaceReviewSubmitterTest`: the `submitter(...)` helper loses `openText` and `bytes`; `diskPath` and `fileReviewParams` go if unused. Delete `submitPaths batches open buffer and disk file in one repo`, `disk submit accepts id-less fileReview only for git blob sha of bytes`, `submitPaths uses separate submit calls per repo root` and `submitPaths skips unsupported unlocated and unreadable paths`. Keep `submitEditor sends one buffer`. Add `submitEditor skips unsupported and unlocated paths`: `submitEditor(unsupportedPath, "x")` and `submitEditor(outsidePath, "x")` (supported but `locate` returns null) send nothing.

Run: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.WorkspaceReview*"`. Expected: compile FAIL until Step 2.

- [ ] **Step 2: Implement**

- [ ] **Step 3: Prove nothing refers to them**

Run: `rg -n "DiskSubmission|queueDisk|submitPaths|reviewByPath|releasedByIdleProgress" src core/src`, then `rg -n "readBytes|contentsToByteArray" src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceReviewService.kt`

Expected: no output from either. (`WorkspaceWatchCoordinator` keeps its own `WorkspaceFileReader` with `openText` and `readBytes`.)

- [ ] **Step 4: Build and test**

Run: `.\gradlew.bat :core:test :test`, then ktlint for core and `src/`, then class size.

Expected: all pass, including `WorkspaceReviewLiveTest` and `WatchLiveTest` unchanged.

- [ ] **Step 5: Safeguard and commit**

Commit `refactor: drop disk review submissions`.

---

### Task 7: Final `runIde` smoke

Run by the coordinator with the user after Tasks 5 and 6 are merged. Same preparation as Task 4. Record every line under PKG-36 and commit `docs: record CS-12077 phase 4 final smoke`.

- [ ] **Step 1: User checks (ask one at a time)**

- U1–U9 from Task 4, unchanged.
- U10. Removed root: in `repo-a`, add `C:\Temp\cs12077-smoke\repo-b` as a second content root (File | Project Structure | Modules | Add Content Root). If IntelliJ reports an unregistered VCS root, register it (Settings | Version Control | Directory Mappings). Within a minute its modified and untracked files appear in the monitor. Remove that content root: its entries leave the monitor except a file that is open in an editor.
- U11. Unopened files only: close every editor, edit `src/Util.kt` from a shell; its monitor entry updates without opening it.

- [ ] **Step 2: Coordinator checks**

- C1–C8 and C10 from Task 4, unchanged.
- C9 (replaced). During 60 s idle, no `Poll found`, `Removing stale item`, `Git4IdeaChangeLister` or `PeriodicChangeListerService` lines, and no new `cs-ide inventory` or `watch started` lines.
- C11. After U10's add, `cs-ide watch started for <repo-b>` and a push for it; after the remove, `cs-ide watch stopped for <repo-b> (sent=true)` and `monitor sync for <repo-b>: hidden=<n>` with `n` > 0.
- C12. No `Replaying queued review by path` lines (deleted); any `Dropping queued review without editor` lines are listed.

- [ ] **Step 3: Record and commit**

---

## Spec coverage

| Spec item (Phase 4) | Task |
| --- | --- |
| Delete `GitChangeObserver*`, `VfsEventBridge`, `GitRepoStateListener`, `SavedFilesTrackerAdapter`, `CodesceneRepoFilesListener`, `GitIgnoreFilesListener` | 1 |
| Delete `PeriodicChangeListerService`, `Git4IdeaChangeLister`, `StaleItemCleanup` once the parity test passes in CI | 5 (gate), 7 (smoke) |
| Delete `MainLineBranchResolver`, `ClosestMainLineMergeBase`, `CodesceneRepoConfig`, baseline parts of `Git4IdeaGitService` | 3 (service, reflog), 5 (resolver chain, kept for the lister until then) |
| Delete `BaselineReviewCacheService`, `CliCacheUtils` | 3 (`isExpiredCliCacheEntry` stays for ACE cache pruning) |
| Delete disk submissions (decision) | 6 |
| Monitor pruning without the poller | 5 (removed-root sync), existing `InventoryMonitorSync` |
| Keep `FileEditorLifecycleListener`, dirty-buffer seeding, project-content filters | Global Constraints; 3 keeps `isIgnored`, `getRepoRoot`, `getRepoRelativePath` |
| Consumer loops survive a failing event (Phase 3 follow-up) | 2 |
| CHANGELOG and DECISIONLOG | 5 |
| No regression for edits, saves, branch switch, untracked files, `.gitignore`, project close, server restart | 4, 7 |
