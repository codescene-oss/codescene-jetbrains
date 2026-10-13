# CS-12077 Phase 3: Agent work packages

Use this document to assign **one package per agent**: one branch, **one commit**, clear scope. Full task detail lives in `2026-10-13-phase3-watch-inventory.md`. Architecture and decisions: `../specs/2026-10-12-cs-ide-json-rpc-migration-design.md` (Phase 3).

**Integration branch:** `feature/cs-12077-cs-ide-json-rpc`

**Branch strategy:** same as Phase 2. Nothing merges to master until every planned phase is done. Bring master changes in with `git merge origin/master` (never rebase or force-push the shared branch). `docs/superpowers/` stays on this branch and is removed in a final commit just before the project merges to master.

**Gradle:** set `JAVA_HOME` to JDK 17 before any `./gradlew` (Gradle 8.14 does not run on JDK 25). On this machine that JDK is `C:\Users\marti\.jdks\graalvm-jdk-17.0.12`.

**Every package:**

- `make format-check class-size-mine` passes (ktlint clean; changed `.kt` files ≤ 325 lines, every line counted). Fix lint findings before anything else. `make` hangs on Windows, so there run the equivalents directly: `.\gradlew.bat :core:ktlintCheck -PktlintFailOnError=true` (add `:ktlintCheck` when touching `src/`) and `bb -f .github/check-class-size-mine.clj`. The same applies to `make iter`: run the relevant `gradlew` test tasks instead.
- Respect the file budgets in the plan. When a test file would pass its budget, split it the way the plan says, not by trimming assertions. Every assertion listed in the plan task is required.
- CodeScene MCP `pre_commit_code_health_safeguard` before commit.
- No `Co-authored` trailers; no new code comments. Agents may add such a trailer on their own, so check `git log -1 --format=%B` after committing and amend it away.
- Never merge, fast-forward or push the integration branch, and never commit in `C:\Git\codescene-jetbrains`. Work only in your own worktree and branch; the coordinator merges.
- After a rebase, check for conflict markers (`git diff --check HEAD~1`) before declaring the package done.
- Threads: nothing new runs on the JSON-RPC reader thread or the EDT. Callbacks only enqueue (plan, Global Constraints).
- Never send `baseline-revision`. Do not delete Phase 4 items (baseline code, `BaselineReviewCacheService`, `StaleItemCleanup`, `PeriodicChangeListerService`, git observers).
- Before any Gradle run, kill a leftover sandbox IDE, which locks `core/build/libs/*.jar`: `Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*idea-sandbox*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }`.

**Merging:** the coordinator rebases each package branch onto the current integration tip and fast-forwards it (`git merge --ff-only`), so the integration branch keeps one commit per package. A package whose base is not merged yet may stack on the base package's branch and is rebased once that merges.

**Waves:** parallel Gradle builds exhausted the Kotlin daemon (out of memory) in Phase 2. Run `.\gradlew.bat --stop` between waves, and do not start more than three packages at once.

---

## Merge order

```text
Wave 1:  PKG-20 server sessions  ∥  PKG-21 watch wire + scopes  ∥  PKG-22 pipeline watch results
                 \                     /        \
                  \                   /          \
Wave 2:   PKG-26 live tests      PKG-23 registry  ∥  PKG-24 project watch + monitor sync
          (needs 20, 21)         (needs 21)          (needs 21)
                                      \               /
Wave 3:                    PKG-25 platform wiring   (needs 20–24; the only runIde package)
```

PKG-20, PKG-21 and PKG-22 start together on the integration tip. PKG-23, PKG-24 and PKG-26 start once PKG-21 is merged (PKG-26 also needs PKG-20). PKG-26 is parallel with PKG-22–25 and can merge before or after PKG-25.

---

## PKG-20 — Server sessions

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-server-sessions` |
| **Base** | Integration branch tip |
| **Commit message** | `feat(cli): track cs-ide server sessions` |
| **Plan tasks** | Task 1 |
| **Parallel with** | PKG-21, PKG-22 |

### Scope

**Modify**

- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/CsIdeServer.kt` (≤ 315 lines)
- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/CsIdeNotificationHub.kt`

**Create**

- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/CsIdeServerSessionTest.kt` (≤ 230 lines)

### Out of scope

Watch JSON, the pipeline, `CsIdeServerService` and every other platform file. Do not change `request`, the backoff policy or startup timing. Do not edit `CsIdeServerTest.kt` (292 lines) or `CsIdeServerNotifyTest.kt`.

### Interfaces (must match plan Task 1)

- `CsIdeServer.notifyInSession(method, params): Long`, `notify(method, params)` (delegates), `notifyIfRunning(method, params): Boolean`, `liveSession(): Long`, `cooldownRemainingMs(): Long`, `addSessionStartListener((Long) -> Unit): AutoCloseable`
- `CsIdeNotificationHub.addSessionStartListener`, `sessionStarted(session)`

### Definition of done

- [ ] Session ids start at 1 and increase per spawn; `detach` resets the live id to 0; `notifyInSession` returns the id of the session it wrote to.
- [ ] Start listeners run on the reader thread after `recordMetadata` and before `dispatch` and the start signal; a throwing listener is logged and does not block startup or other listeners.
- [ ] `notifyIfRunning` never spawns, ignores the backoff, returns false when closed, not running or the write fails.
- [ ] All 11 tests from plan Task 1 step 1, plus `CsIdeServerTest`, `CsIdeServerNotifyTest`, `CsIdeServerEdgeCaseTest` unchanged and passing: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.CsIdeServer*"`.
- [ ] ktlint, class-size, single commit, MCP safeguard clean. No `runIde`.

### Agent prompt (copy-paste)

```text
Implement PKG-20 for CS-12077 on branch feature/cs-12077-server-sessions from feature/cs-12077-cs-ide-json-rpc.
Read docs/superpowers/plans/2026-10-13-phase3-watch-inventory.md (Global Constraints and Task 1) and docs/superpowers/plans/2026-10-13-phase3-agent-packages.md PKG-20.
TDD: write CsIdeServerSessionTest with every test listed in Task 1 step 1, see it fail to compile, then change CsIdeServer.kt and CsIdeNotificationHub.kt only.
Start listeners run on the JSON-RPC reader thread and must only enqueue; never call request or notify from one.
CsIdeServer.kt must stay at or below 315 lines. Do not edit the existing server tests.
One commit: feat(cli): track cs-ide server sessions.
JAVA_HOME=JDK 17 (C:\Users\marti\.jdks\graalvm-jdk-17.0.12). On Windows use gradlew.bat, not make.
ktlint and bb -f .github/check-class-size-mine.clj, then pre_commit_code_health_safeguard before commit.
Do not merge or push the integration branch. Do not commit in C:\Git\codescene-jetbrains.
Check git log -1 --format=%B and amend away any Co-authored-by trailer.
```

---

## PKG-21 — Watch wire and scopes

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-watch-wire` |
| **Base** | Integration branch tip |
| **Commit message** | `feat(cli): add cs-ide watch wire and scopes` |
| **Plan tasks** | Task 2 |
| **Parallel with** | PKG-20, PKG-22 |

### Scope

**Create**

- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WatchClient.kt` (≤ 110)
- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WatchScopes.kt` (≤ 90)
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WatchClientTest.kt` (≤ 170)
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WatchScopesTest.kt` (≤ 150)

**Modify**

- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspacePaths.kt` (`rootKey`, `relativeTo`)
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspacePathsTest.kt` (six added assertions, ≤ 85 lines)

### Out of scope

`CsIdeServer` (the client takes lambdas, so it compiles without PKG-20), `CsIdeServerService`, the pipeline, the registry, platform.

### Interfaces (must match plan Task 2)

- `WatchInventory(repoRoot, files)`, `WatchNotices.INVENTORY_CHANGED`, `WatchNotices.inventory(params)`
- `WatchClient(notifyInSession, notifyIfRunning, transport)`: `watchFiles(repoRoot, relativePaths): Long`, `stopWatchFiles(repoRoot): Boolean`, `getWatchInventory(repoRoot): CompletableFuture<WatchInventory>`, `WatchClient.isNotWatched(error)`
- `WatchScopes.scopes(gitRoots, contentRoots, windows)`, `WatchScopes.collapse(paths, windows)`
- `WorkspacePaths.rootKey(root, windows)`, `WorkspacePaths.relativeTo(root, path, windows)`

### Definition of done

- [ ] `watchFiles` params have exactly the fields `repo-root` and `relative-paths`; `stopWatchFiles` params have exactly `repo-root` and go through `notifyIfRunning`.
- [ ] `getWatchInventory` parses `{repoRoot, files}` with slash-normalized paths and fails on a malformed result; `isNotWatched` is true only for the `Repository is not watched` RPC error.
- [ ] Scopes: nearest git root, `.` at or above the root, collapse, sort, sibling-prefix safety, Windows case and slashes; all nine `WatchScopesTest` cases.
- [ ] `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.WatchClientTest" --tests "com.codescene.jetbrains.core.cli.WatchScopesTest" --tests "com.codescene.jetbrains.core.cli.WorkspacePathsTest"` passes.
- [ ] ktlint, class-size, single commit, MCP safeguard clean. No `runIde`.

### Agent prompt (copy-paste)

```text
Implement PKG-21 for CS-12077 on branch feature/cs-12077-watch-wire from feature/cs-12077-cs-ide-json-rpc.
Read docs/superpowers/plans/2026-10-13-phase3-watch-inventory.md (Server behaviour, Global Constraints, Task 2) and the PKG-21 section of docs/superpowers/plans/2026-10-13-phase3-agent-packages.md.
TDD: write WatchClientTest, WatchScopesTest and the WorkspacePathsTest additions first, then WatchClient.kt, WatchScopes.kt and the two WorkspacePaths functions.
Never send baseline-revision. watchFiles and stopWatchFiles are notifications; only getWatchInventory is a request.
Do not touch CsIdeServer, the pipeline or platform code.
One commit: feat(cli): add cs-ide watch wire and scopes.
JAVA_HOME=JDK 17 (C:\Users\marti\.jdks\graalvm-jdk-17.0.12). On Windows use gradlew.bat, not make.
ktlint and bb -f .github/check-class-size-mine.clj, then pre_commit_code_health_safeguard before commit.
Do not merge or push the integration branch. Do not commit in C:\Git\codescene-jetbrains.
Check git log -1 --format=%B and amend away any Co-authored-by trailer.
```

---

## PKG-22 — Pipeline accepts watch results

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-watch-results` |
| **Base** | Integration branch tip |
| **Commit message** | `feat(cli): accept cs-ide watch results` |
| **Plan tasks** | Task 3 |
| **Parallel with** | PKG-20, PKG-21 |

### Scope

**Create**

- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceFileReader.kt` (≤ 30)
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceFileReaderTest.kt` (≤ 70)
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewPipelineFixture.kt` (≤ 120)
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewWatchResultsTest.kt` (≤ 200)
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewWatchStateTest.kt` (≤ 170)

**Modify**

- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewPipeline.kt` (≤ 250)
- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewMatching.kt` (≤ 140)

### Out of scope

`WorkspaceReviewSubmitter.kt` (keeps its Phase 2 reads and constructor), `WorkspaceReviewPipelineTest.kt` (323 lines; do not edit), `WorkspaceReviewSubmitterTest.kt`, the presenter, platform.

### Interfaces (must match plan Task 3)

- `CurrentFile(gitBlobSha, text)`, `WorkspaceFileReader(openText, readBytes).current(absolutePath)`
- `ObservedFile(absolutePath, repoRoot, current)`
- `WorkspaceReviewPipeline(..., observe = { _, _ -> null })` as the last constructor parameter; `forgetAccepted(repoRoot)`

### Definition of done

- [ ] Id-less results go through one function: an in-flight buffer drops them; otherwise `observe` supplies the current file (or the Phase 2 disk submission when `observe` returns null) and `acceptsResult` decides. No second matcher.
- [ ] Results with an id never call `observe`.
- [ ] A null-result id-less delta is accepted only while the last accepted id-less review awaits its delta and the current SHA still equals that review's SHA.
- [ ] A watch result for a loaded document presents with its text; for a disk file, without content.
- [ ] `forgetAccepted` clears accepted flags for that root only, comparing roots like `sameRoot`.
- [ ] Every test from plan Task 3 step 1, plus all Phase 2 pipeline and submitter tests unchanged: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.Workspace*"`.
- [ ] ktlint, class-size, single commit, MCP safeguard clean. No `runIde`.

### Agent prompt (copy-paste)

```text
Implement PKG-22 for CS-12077 on branch feature/cs-12077-watch-results from feature/cs-12077-cs-ide-json-rpc.
Read docs/superpowers/plans/2026-10-13-phase3-watch-inventory.md (Resolved decisions on current bytes and null deltas, Task 3) and the PKG-22 section of docs/superpowers/plans/2026-10-13-phase3-agent-packages.md.
TDD: create WorkspaceReviewPipelineFixture, WorkspaceFileReaderTest, WorkspaceReviewWatchResultsTest and WorkspaceReviewWatchStateTest with every listed test, see them fail, then add WorkspaceFileReader.kt and change WorkspaceReviewPipeline.kt and WorkspaceReviewMatching.kt.
Use the acceptIdLess shape from the plan. Do not add a second matcher, and do not edit WorkspaceReviewPipelineTest.kt or the submitter.
Run every Workspace* core test; the Phase 2 tests must pass unchanged.
One commit: feat(cli): accept cs-ide watch results.
JAVA_HOME=JDK 17 (C:\Users\marti\.jdks\graalvm-jdk-17.0.12). On Windows use gradlew.bat, not make.
ktlint and bb -f .github/check-class-size-mine.clj, then pre_commit_code_health_safeguard before commit.
Do not merge or push the integration branch. Do not commit in C:\Git\codescene-jetbrains.
Check git log -1 --format=%B and amend away any Co-authored-by trailer.
```

---

## PKG-23 — Watch registry

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-watch-registry` |
| **Base** | **PKG-21** merged |
| **Commit message** | `feat(cli): keep one cs-ide watch per git root` |
| **Plan tasks** | Task 4 |
| **Parallel with** | PKG-24, PKG-26 |

### Scope (create only)

- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WatchRegistry.kt` (≤ 170)
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WatchRegistryTest.kt` (≤ 230)

### Out of scope

`WatchScopes.kt`, `WorkspacePaths.kt` (use what PKG-21 added; do not edit), `CsIdeServer`, platform.

### Interfaces (must match plan Task 4)

- `WatchCommand.Watch(repoRoot, relativePaths)`, `WatchCommand.Stop(repoRoot)`
- `WatchRegistry(windows, clock)`: `setProject`, `removeProject`, `sent`, `stopped`, `sessionStarted`, `pending`, `lost`, `hasDesired`; `FIRST_RETRY_MS`, `MAX_RETRY_MS`, `LOST_REWATCH_INTERVAL_MS`, `retryDelayMs`

### Definition of done

- [ ] Desired paths per root are the collapsed union across projects; roots keyed by `rootKey`, displayed in the first project's form.
- [ ] `Stop` only for roots that were sent and that no project wants; a shrinking union sends `Watch`.
- [ ] `sessionStarted(k)` and `pending(live)` re-emit roots not sent in that session; `lost` is limited to once per 60 s per root.
- [ ] All 12 tests from plan Task 4: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.WatchRegistryTest"`.
- [ ] ktlint, class-size, single commit, MCP safeguard clean. No `runIde`.

### Agent prompt (copy-paste)

```text
Implement PKG-23 for CS-12077 on branch feature/cs-12077-watch-registry, based on the integration branch after PKG-21 is merged (stack on feature/cs-12077-watch-wire if it is not merged yet).
Read docs/superpowers/plans/2026-10-13-phase3-watch-inventory.md (Resolved decisions on one watch per git root and session loss, Task 4) and the PKG-23 section of docs/superpowers/plans/2026-10-13-phase3-agent-packages.md.
TDD: write WatchRegistryTest with every listed test, see it fail, then create WatchRegistry.kt. The registry is not thread-safe; only the application consumer calls it.
Do not edit WatchScopes.kt or WorkspacePaths.kt.
One commit: feat(cli): keep one cs-ide watch per git root.
JAVA_HOME=JDK 17 (C:\Users\marti\.jdks\graalvm-jdk-17.0.12). On Windows use gradlew.bat, not make.
ktlint and bb -f .github/check-class-size-mine.clj, then pre_commit_code_health_safeguard before commit.
Do not merge or push the integration branch. Do not commit in C:\Git\codescene-jetbrains.
Check git log -1 --format=%B and amend away any Co-authored-by trailer.
```

---

## PKG-24 — Project watch state and monitor sync

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-workspace-watch` |
| **Base** | **PKG-21** merged |
| **Commit message** | `feat(cli): track cs-ide watch inventory per project` |
| **Plan tasks** | Task 5 |
| **Parallel with** | PKG-23, PKG-26 |

### Scope (create only)

- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceWatch.kt` (≤ 230)
- `core/src/main/kotlin/com/codescene/jetbrains/core/cleanup/InventoryMonitorSync.kt` (≤ 50)
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceWatchScopeTest.kt` (≤ 190)
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceWatchInventoryTest.kt` (≤ 210)
- `core/src/test/kotlin/com/codescene/jetbrains/core/cleanup/InventoryMonitorSyncTest.kt` (≤ 150)

### Out of scope

`StaleItemCleanup` (Phase 4 deletes it; do not change or reuse it), the delta cache, the pipeline, the registry, platform.

### Interfaces (must match plan Task 5)

- `WatchEffect.FetchInventory(repoRoot, generation)`, `ScheduleConfirm(repoRoot, token, delayMs)`, `ReportLost(repoRoot)`, `SyncMonitor(repoRoot, absolutePaths)`, `Reseed(repoRoot)`
- `ResolvedPath(repoRoot, absolutePath)`
- `WorkspaceWatch(windows)` with every function in plan Task 5 and `CONFIRM_DELAY_MS = 10_000`
- `InventoryMonitorSync(deltaCacheService, logger).sync(repoRoot, inventoryPaths, openPaths): Int`

### Definition of done

- [ ] Pushes replace the inventory wholesale, filtered to the project scope; an empty push also fetches.
- [ ] Confirm tokens: only the latest token fetches; a push clears it. Fetch results are dropped when a push arrived after the fetch was issued (generation).
- [ ] Not-watched fetch failure reports the root as lost; HEAD changes after the first sighting emit `Reseed` then a confirm.
- [ ] `WorkspaceWatch` guards its state with one lock.
- [ ] Monitor sync hides only entries under the root outside inventory ∪ open files, compares by `pathCacheKey`, and re-includes inventory paths.
- [ ] Every test from plan Task 5, plus `StaleItemCleanupTest` unchanged: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.WorkspaceWatch*" --tests "com.codescene.jetbrains.core.cleanup.*"`.
- [ ] ktlint, class-size, single commit, MCP safeguard clean. No `runIde`.

### Agent prompt (copy-paste)

```text
Implement PKG-24 for CS-12077 on branch feature/cs-12077-workspace-watch, based on the integration branch after PKG-21 is merged (stack on feature/cs-12077-watch-wire if it is not merged yet).
Read docs/superpowers/plans/2026-10-13-phase3-watch-inventory.md (Resolved decisions on inventory, lost watch, HEAD moves and inventory consumers; Task 5) and the PKG-24 section of docs/superpowers/plans/2026-10-13-phase3-agent-packages.md.
TDD: write WorkspaceWatchScopeTest, WorkspaceWatchInventoryTest and InventoryMonitorSyncTest with every listed test, see them fail, then create WorkspaceWatch.kt and InventoryMonitorSync.kt.
WorkspaceWatch returns effects and performs no I/O. Do not change or reuse StaleItemCleanup.
One commit: feat(cli): track cs-ide watch inventory per project.
JAVA_HOME=JDK 17 (C:\Users\marti\.jdks\graalvm-jdk-17.0.12). On Windows use gradlew.bat, not make.
ktlint and bb -f .github/check-class-size-mine.clj, then pre_commit_code_health_safeguard before commit.
Do not merge or push the integration branch. Do not commit in C:\Git\codescene-jetbrains.
Check git log -1 --format=%B and amend away any Co-authored-by trailer.
```

---

## PKG-25 — Platform wiring

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-watch-wiring` |
| **Base** | **PKG-20, PKG-21, PKG-22, PKG-23, PKG-24** merged |
| **Commit message** | `feat: watch git roots through cs-ide` |
| **Plan tasks** | Task 6 |
| **Parallel with** | PKG-26 only |

### Scope

**Modify**

- `src/main/kotlin/com/codescene/jetbrains/platform/cli/CsIdeServerService.kt` (≤ 80)
- `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceReviewService.kt` (≤ 200)
- `src/main/kotlin/com/codescene/jetbrains/platform/listeners/ProjectStartupActivity.kt` (one call added)
- `src/main/kotlin/com/codescene/jetbrains/platform/git/PeriodicChangeListerService.kt` (cleanup guarded by `isWatched`)

**Create**

- `src/main/kotlin/com/codescene/jetbrains/platform/watch/CsIdeWatchService.kt` (≤ 170)
- `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceWatchCoordinator.kt` (≤ 210)

**One-line test updates:** none expected (no platform test references these files). If any test stops compiling, fix it in this package and name it in the hand-off.

### Out of scope

Every core file (report a core gap instead of patching it here), `ReviewProgressIndicator` (no new indicator), the presenter, `Git4IdeaChangeLister`, `GitChangeObserverService`, `StaleItemCleanup`, `plugin.xml` (both services use `@Service`).

### Interfaces (must match plan Task 6)

- `CsIdeServerService.watchClient()`, `addSessionStartListener`, `liveSession`, `cooldownRemainingMs`
- `CsIdeWatchService.setProjectScopes`, `removeProject`, `reportLost`, `addWatchSentListener`, `getInstance()`
- `WorkspaceWatchCoordinator(project, scope, enqueue, reseed)`: `start`, `handle`, `onNotification`, `observe`, `isWatched`, `dispose`; `WatchEvent`
- `WorkspaceReviewService.startWatching()`, `isWatched(repoRoot)`

### Definition of done

- [ ] Every listener, request completion and timer only enqueues; the project consumer and the application consumer do all work on `Dispatchers.IO`.
- [ ] Topics: `ModuleRootListener.TOPIC`, `VcsRepositoryManager.VCS_REPOSITORY_MAPPING_UPDATED`, `GitRepository.GIT_REPO_CHANGE`, all through `project.messageBus.connect(WorkspaceReviewService)`.
- [ ] Current bytes for `observe`: `getCachedDocument(file)?.text`, else `Files.readAllBytes`.
- [ ] Session loss retries after 2 s, then `retryDelayMs(cooldownRemainingMs())`; session start resends; project dispose enqueues `removeProject` via `serviceIfCreated`; application dispose sends nothing.
- [ ] Poller keeps `submitPaths` and skips `StaleItemCleanup` only while its git root is watched.
- [ ] `rg "withBackgroundProgress" src/main` still lists only `ReviewProgressIndicator.kt` and `IntelliJProgressService.kt`.
- [ ] `.\gradlew.bat test`, ktlint for core and `src/`, class-size pass.
- [ ] **`runIde` smoke checklist** (this is the only Phase 3 package that runs it). Before `runIde`: kill a leftover sandbox IDE, copy `C:\Git\codescene-jetbrains\src\main\resources\cs-cwf` into the worktree's `src\main\resources\cs-cwf` (gitignored), and run `.\gradlew.bat --stop` if another wave just built. Report the result of every line from plan Task 6 step 4:
  - User in the IDE: U1 unopened changes listed, U2 one indicator then gone, U3 30 s idle without indicator, U4 buffer edits and ACE, U5 external untracked file appears and restored file disappears, U6 branch switch with an unsaved editor, U7 two projects and closing one.
  - Coordinator from shell and `build\idea-sandbox\IC-2024.3.7\log\idea.log`: C1 exactly one `cs-ide` child (command line contains `clojure.main` and `codescene.cli.ide`), C2 `watch started` and inventory size, C3 external edit push, C4 kill and recovery with `session=2`, C5 reseed line, C6 `watch stopped` on project close, C7 idle has no new inventory or watch lines, C8 no exceptions or retry loops, C9 nothing left after IDE exit.
- [ ] Single commit, MCP safeguard clean.

### Agent prompt (copy-paste)

```text
Implement PKG-25 for CS-12077 on branch feature/cs-12077-watch-wiring after PKG-20 to PKG-24 are merged into feature/cs-12077-cs-ide-json-rpc.
Follow Task 6 in docs/superpowers/plans/2026-10-13-phase3-watch-inventory.md and the PKG-25 section of docs/superpowers/plans/2026-10-13-phase3-agent-packages.md.
Platform stays thin: no decisions outside core. Every callback (message bus, JSON-RPC handler, session listener, request completion, delay) only calls trySend; the project and application consumers on Dispatchers.IO do the work.
Do not change core files, ReviewProgressIndicator, the presenter, Git4IdeaChangeLister, GitChangeObserverService or StaleItemCleanup.
Run .\gradlew.bat test, ktlint for core and src, class-size.
Then kill any leftover sandbox IDE, copy src\main\resources\cs-cwf from C:\Git\codescene-jetbrains, run .\gradlew.bat runIde, and work through the U lines with the user and the C lines from the shell and build\idea-sandbox\IC-2024.3.7\log\idea.log. Report every line.
One commit: feat: watch git roots through cs-ide.
JAVA_HOME=JDK 17 (C:\Users\marti\.jdks\graalvm-jdk-17.0.12). Do not use make on Windows.
pre_commit_code_health_safeguard before commit.
Do not merge or push the integration branch. Do not commit in C:\Git\codescene-jetbrains.
Check git log -1 --format=%B and amend away any Co-authored-by trailer.
```

---

## PKG-26 — Live watch and parity tests

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-watch-live-tests` |
| **Base** | **PKG-20 and PKG-21** merged |
| **Commit message** | `test(cli): pin cs-ide watch and inventory parity` |
| **Plan tasks** | Task 7 |
| **Parallel with** | PKG-22, PKG-23, PKG-24, PKG-25 |

### Scope (create only)

- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/CsIdeLiveServer.kt` (≤ 90)
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WatchGitFixture.kt` (≤ 120)
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WatchLiveTest.kt` (≤ 250)
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WatchInventoryParityLiveTest.kt` (≤ 250)

### Out of scope

All main sources. `WorkspaceReviewLiveTest.kt` and `CsIdeLiveContractFixtures.kt` stay as they are (reading `simpleKotlinCode` and `complexKotlinCode` from the fixture is fine). Platform tests, including `Git4IdeaChangeListerFeatureBranchTest`.

### Interfaces (must match plan Task 7)

- `CsIdeLiveServer.start(): CsIdeServer`, `CsIdeLiveServer.stop(server)`
- `WatchGitFixture(root)`: `init`, `write`, `delete`, `commitAll`, `checkoutNew`, `setOriginHead`

### Definition of done

- [ ] `WatchLiveTest` covers the five tests in plan Task 7, including push-before-results ordering, the disk-byte SHA, 15 s idle without progress, the null delta after restoring the baseline, and stop followed by `isNotWatched`.
- [ ] `WatchInventoryParityLiveTest` has the six rows of the parity table with the exact expected sets; the many-untracked row asserts all ten files.
- [ ] `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.cli.WatchLiveTest" --tests "com.codescene.jetbrains.core.cli.WatchInventoryParityLiveTest"` passes. A failing parity row is reported with the server's set, not edited to pass.
- [ ] ktlint, class-size, single commit, MCP safeguard clean. No `runIde`.

### Agent prompt (copy-paste)

```text
Implement PKG-26 for CS-12077 on branch feature/cs-12077-watch-live-tests, based on the integration branch after PKG-20 and PKG-21 are merged.
Follow Task 7 in docs/superpowers/plans/2026-10-13-phase3-watch-inventory.md and the PKG-26 section of docs/superpowers/plans/2026-10-13-phase3-agent-packages.md.
Create the four test files only. Start the real server from the resolved codescene.extension:api jar like WorkspaceReviewLiveTest (CS_IDE_JAVA honoured), one server per class, and register handlers before sending.
Expected parity sets come from the plan's table. If the server disagrees, report its set; do not change the expectation.
One commit: test(cli): pin cs-ide watch and inventory parity.
JAVA_HOME=JDK 17 (C:\Users\marti\.jdks\graalvm-jdk-17.0.12). On Windows use gradlew.bat, not make.
ktlint and bb -f .github/check-class-size-mine.clj, then pre_commit_code_health_safeguard before commit.
Do not merge or push the integration branch. Do not commit in C:\Git\codescene-jetbrains.
Check git log -1 --format=%B and amend away any Co-authored-by trailer.
```

---

## Conflict matrix

| Packages | Parallel? | Shared files |
| --- | --- | --- |
| PKG-20 + PKG-21 + PKG-22 | Yes | none. 21's `WatchClient` takes lambdas, so it does not need 20 |
| PKG-23 + PKG-24 | Yes, after 21 | none. Neither edits `WorkspacePaths.kt` or `WatchScopes.kt` |
| PKG-26 + PKG-22 / 23 / 24 / 25 | Yes, after 20 and 21 | none. 26 creates test files only |
| PKG-21 + PKG-23 / 24 | No | 23 and 24 consume `WatchInventory`, `rootKey` and `collapse` from 21. Stack on 21 if it is not merged |
| PKG-20 + PKG-26 | No | 26 calls `notifyInSession` and `notifyIfRunning` from 20 |
| PKG-22 + PKG-25 | No | 25 passes `observe` and calls `forgetAccepted` |
| PKG-25 + PKG-20 / 21 / 22 / 23 / 24 | No | 25 wires all of them into the platform |

---

## Docs on the integration branch

`docs` is gitignored. These files are force-added (`git add -f`), and any new doc needs the same. `docs/superpowers/` stays on `feature/cs-12077-cs-ide-json-rpc` until the final commit before the project merges to master.
