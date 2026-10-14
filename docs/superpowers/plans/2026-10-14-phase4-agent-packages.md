# CS-12077 Phase 4: Agent work packages

Use this document to assign **one package per agent**: one branch, **one commit**, clear scope. Full task detail lives in `2026-10-14-phase4-legacy-deletion.md`. Architecture and decisions: `../specs/2026-10-12-cs-ide-json-rpc-migration-design.md` (Phase 4).

**Integration branch:** `feature/cs-12077-cs-ide-json-rpc`

**Branch strategy:** same as Phases 2 and 3. Nothing merges to master until every planned phase is done. Bring master changes in with `git merge origin/master` (never rebase or force-push the shared branch). `docs/superpowers/` stays on this branch and is removed in a final commit just before the project merges to master.

**Gradle:** set `JAVA_HOME` to JDK 17 before any `./gradlew` (Gradle 8.14 does not run on JDK 25). On this machine that JDK is `C:\Users\marti\.jdks\graalvm-jdk-17.0.12`. Use a 10-minute timeout.

**Agents:** composer-2.5 only; never substitute another model. PKG-33 and PKG-36 are run by the coordinator with the user, not by an agent. PKG-36A and PKG-36B are smoke-failure fixes run by agents before PKG-36 resumes.

**Every package:**

- `make format-check class-size-mine` passes (ktlint clean; changed `.kt` files ≤ 325 lines, every line counted). Fix lint findings before anything else. `make` hangs on Windows, so there run the equivalents directly: `.\gradlew.bat :core:ktlintCheck -PktlintFailOnError=true` (add `:ktlintCheck` when touching `src/`; max line length 120) and `bb -f .github/check-class-size-mine.clj`. The same applies to `make iter`: run the relevant `gradlew` test tasks instead.
- Respect the file budgets in the plan. Every assertion listed in the plan task is required. Deleting a class deletes the tests that exist only for it; do not delete or weaken a test of a kept class to make the build pass.
- The `rg` step of the plan task returns what the plan says before the package is done.
- CodeScene MCP `pre_commit_code_health_safeguard` before commit.
- No `Co-authored` text anywhere (commits, hand-offs, docs). Agents may add such a trailer on their own, so check `git log -1 --format=%B` after committing and amend it away.
- No UTF-8 BOM in changed files: `Format-Hex -Path <file> -Count 3` must not show `EF BB BF`.
- Never merge, fast-forward or push the integration branch, and never commit in `C:\Git\codescene-jetbrains`. Work only in your own worktree and branch; the coordinator merges.
- After a rebase, check for conflict markers (`git diff --check HEAD~1`) before declaring the package done.
- Do not delete gated items before PKG-34 (plan, Global Constraints). `plugin.xml` does not change in any package.
- Before any Gradle run, kill a leftover sandbox IDE, which locks `core/build/libs/*.jar`: `Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*idea-sandbox*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }`.

**Merging:** the coordinator rebases each package branch onto the current integration tip and fast-forwards it (`git merge --ff-only`), so the integration branch keeps one commit per package. A package whose base is not merged yet may stack on the base package's branch and is rebased once that merges. Agents have claimed green builds that were not, so after every package the coordinator re-runs ktlint for core and `src/`, `bb -f .github/check-class-size-mine.clj`, and `.\gradlew.bat :core:test :test --rerun` on the rebased branch, and checks the first bytes of every changed file for a BOM, before fast-forwarding. Fast-forwarding `feature/cs-12077-cs-ide-json-rpc` itself to its final Phase 4 state needs the user's explicit approval. Nobody pushes; the user decides when.

**Waves:** parallel Gradle builds exhausted the Kotlin daemon (out of memory) in Phase 2. Run `.\gradlew.bat --stop` between waves, and do not start more than three packages at once.

**CI gate (PKG-34):** #194 has a `Build` run whose head contains `407e97a` and whose `Test` jobs on `ubuntu-latest`, `windows-latest` and `macos-latest` all succeeded (plan Task 5 lists the `gh` commands). The user pushes; the coordinator only reads CI. "CodeScene Code Coverage (master)" is reported, not gated.

---

## Merge order

```text
Wave 1:  PKG-30 observer deletion   ∥   PKG-31 consumer guard
              |                              |
Wave 2:  PKG-32 baseline deletion            |      (32 needs 30; 31 may still run)
              \                             /
Wave 3:        PKG-33 runIde smoke, ungated state   (needs 30, 31, 32; coordinator + user)

   ── CI gate: #194 Test green on ubuntu, windows, macOS for a head containing 407e97a ──

Wave 4:  PKG-34 poller and git listing deletion     (needs 33 and the gate)
              |
Wave 5:  PKG-35 disk submission deletion            (needs 34; may stack on its branch)
              |
Wave 6:  PKG-36 final runIde smoke starts           (needs 35; coordinator + user)
              |
         U3 exposes two independent fixes
              |
Wave 7:  PKG-36A dirty-document matching  ∥  PKG-36B native monitor paths
              \                                  /
Wave 8:        PKG-36 final runIde smoke resumes
```

PKG-30 and PKG-31 start together on the integration tip. PKG-32 starts once PKG-30 is merged. PKG-33 runs once 30–32 are merged; it can run while CI is still pending. PKG-34 is the only package that waits for the CI gate itself. PKG-35 is gated by dependency: until PKG-34 deletes the poller, the poller still calls `submitPaths`. PKG-35 can stack on PKG-34's branch and start once PKG-34 has its commit.
PKG-36A and PKG-36B branch from PKG-35 and run in parallel. Both must merge before the final smoke resumes.

---

## PKG-30 — Observer deletion

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-delete-observers` |
| **Base** | Integration branch tip |
| **Commit message** | `refactor: delete git change observers` |
| **Plan tasks** | Task 1 |
| **Parallel with** | PKG-31 |

### Scope

**Delete (main):** `core/.../core/git/GitChangeObserver.kt`, `core/.../core/git/SavedFilesTracker.kt`, `core/.../core/contracts/ISavedFilesTracker.kt`, and in `src/main/kotlin/com/codescene/jetbrains/platform/git/`: `GitChangeObserverService.kt`, `GitChangeObserverAdapter.kt`, `VfsEventBridge.kt`, `GitRepoStateListener.kt`, `SavedFilesTrackerAdapter.kt`, `CodesceneRepoFilesListener.kt`, `GitIgnoreFilesListener.kt`.

**Delete (tests):** `core/src/test/.../core/git/`: `GitChangeObserverTest.kt`, `GitChangeObserverPrePopulationTest.kt`, `GitChangeObserverIntegrationTest.kt`, `GitChangeObserverTestSupport.kt`, `SavedFilesTrackerTest.kt`; `src/test/.../platform/git/`: `VfsEventBridgeTest.kt`, `GitRepoStateListenerTest.kt`, `CodesceneRepoFilesListenerTest.kt`, `GitIgnoreFilesListenerTest.kt`, `GitChangeObserverServiceTest.kt`, `GitChangeObserverCallbackTest.kt`, `GitChangeObserverReviewSchedulingTest.kt`.

**Modify**

- `src/main/kotlin/com/codescene/jetbrains/platform/listeners/ProjectStartupActivity.kt` (≤ 160)
- `core/src/main/kotlin/com/codescene/jetbrains/core/git/GitPathUtils.kt` (≤ 86): delete `gitRelativeComparisonKey`

### Out of scope

`PeriodicChangeListerService`, `Git4IdeaChangeLister`, `MainLineBranchResolver` (its `invalidate` becomes unused and stays until PKG-34), `FileEventHandler`, `PlatformBaselineReviewCacheService`, `IGitService`, `Git4IdeaGitService`, `FileChangeListener`, `FileEventProcessor`, `OpenFilesObserverAdapter`, `IOpenFilesObserver`, every other function in `GitPathUtils.kt`, `plugin.xml`.

### Interfaces (must match plan Task 1)

None produced. `runStartup` keeps every other call in order.

### Definition of done

- [ ] The 22 files are deleted; the Task 1 `rg` returns only the KDoc lines in `PeriodicChangeListerService.kt`.
- [ ] `.\gradlew.bat :core:test :test` passes; ktlint for core and `src/`, class-size pass.
- [ ] Single commit, MCP safeguard clean. No `runIde`.

### Agent prompt (copy-paste)

```text
Implement PKG-30 for CS-12077 on branch feature/cs-12077-delete-observers from feature/cs-12077-cs-ide-json-rpc.
Read docs/superpowers/plans/2026-10-14-phase4-legacy-deletion.md (Global Constraints and Task 1) and the PKG-30 section of docs/superpowers/plans/2026-10-14-phase4-agent-packages.md.
git rm exactly the 22 files listed, remove the observer start from ProjectStartupActivity and gitRelativeComparisonKey from GitPathUtils. Delete nothing else; the poller, lister and baseline code stay.
Run the Task 1 rg, then .\gradlew.bat :core:test :test, ktlint for core and src (max line length 120), bb -f .github/check-class-size-mine.clj.
One commit: refactor: delete git change observers.
JAVA_HOME=JDK 17 (C:\Users\marti\.jdks\graalvm-jdk-17.0.12). On Windows use gradlew.bat, not make.
pre_commit_code_health_safeguard before commit.
Do not merge or push the integration branch. Do not commit in C:\Git\codescene-jetbrains.
Check git log -1 --format=%B and amend away any Co-authored-by trailer.
```

---

## PKG-31 — Consumer guard

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-consumer-guard` |
| **Base** | Integration branch tip |
| **Commit message** | `fix: keep cs-ide event consumers alive after a failing event` |
| **Plan tasks** | Task 2 |
| **Parallel with** | PKG-30, PKG-32 |

### Scope

**Create**

- `core/src/main/kotlin/com/codescene/jetbrains/core/util/ConsumerGuard.kt` (≤ 35)
- `core/src/test/kotlin/com/codescene/jetbrains/core/util/ConsumerGuardTest.kt` (≤ 90)

**Modify**

- `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceReviewService.kt` (≤ 185)
- `src/main/kotlin/com/codescene/jetbrains/platform/watch/CsIdeWatchService.kt` (≤ 175)

### Out of scope

The pipeline, `WorkspaceWatchCoordinator`, `sendWatch`'s own retry, the notification and session registrations, `submitPaths` and `isWatched` (PKG-34 and PKG-35 remove them). No other `try`/`catch` changes.

### Interfaces (must match plan Task 2)

- `ConsumerGuard(logger: ILogger, service: String, isControlFlow: (Throwable) -> Boolean = { false })`, `fun <T : Any> handle(event: T, block: (T) -> Unit)`
- `WorkspaceReviewService.handleIncoming(event)` and `CsIdeWatchService.handleEvent(event)` (private)

### Definition of done

- [ ] `CancellationException` and control-flow exceptions are rethrown; `Error`s propagate; other exceptions log `<EventClass> failed: <message or class>` with the service name and the loop continues.
- [ ] Both guards use `{ it is ControlFlowException }` and are declared before the `init` block that launches the consumer.
- [ ] All six `ConsumerGuardTest` tests from plan Task 2 pass: `.\gradlew.bat :core:test --tests "com.codescene.jetbrains.core.util.ConsumerGuardTest"`; then `.\gradlew.bat :core:test :test`.
- [ ] ktlint for core and `src/`, class-size, single commit, MCP safeguard clean. No `runIde`.

### Agent prompt (copy-paste)

```text
Implement PKG-31 for CS-12077 on branch feature/cs-12077-consumer-guard from feature/cs-12077-cs-ide-json-rpc.
Follow Task 2 in docs/superpowers/plans/2026-10-14-phase4-legacy-deletion.md and the PKG-31 section of docs/superpowers/plans/2026-10-14-phase4-agent-packages.md.
Write ConsumerGuardTest first (six tests), see it fail, then add ConsumerGuard in core/util and use it in the consumer loops of WorkspaceReviewService and CsIdeWatchService by extracting their when-blocks into private functions. Change nothing else in those files.
Run .\gradlew.bat :core:test :test, ktlint for core and src (max line length 120), bb -f .github/check-class-size-mine.clj.
One commit: fix: keep cs-ide event consumers alive after a failing event.
JAVA_HOME=JDK 17 (C:\Users\marti\.jdks\graalvm-jdk-17.0.12). On Windows use gradlew.bat, not make.
pre_commit_code_health_safeguard before commit.
Do not merge or push the integration branch. Do not commit in C:\Git\codescene-jetbrains.
Check git log -1 --format=%B and amend away any Co-authored-by trailer.
```

---

## PKG-32 — Baseline deletion

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-delete-baseline` |
| **Base** | **PKG-30** merged |
| **Commit message** | `refactor: delete baseline resolution and caches` |
| **Plan tasks** | Task 3 |
| **Parallel with** | PKG-31 |

### Scope

**Delete (main):** `core/.../core/review/BaselineReviewCacheService.kt`, `core/.../core/contracts/IBaselineReviewCacheService.kt`, `core/.../core/util/GitReflogUtils.kt`, `src/.../platform/review/PlatformBaselineReviewCacheService.kt`.

**Delete (tests):** `core/src/test/.../core/review/BaselineReviewCacheServiceTest.kt`, `core/src/test/.../core/util/GitReflogUtilsTest.kt`.

**Modify**

- `core/src/main/kotlin/com/codescene/jetbrains/core/contracts/IGitService.kt` (≤ 10)
- `src/main/kotlin/com/codescene/jetbrains/platform/git/Git4IdeaGitService.kt` (≤ 110)
- `core/src/main/kotlin/com/codescene/jetbrains/core/review/FileEventHandler.kt` (≤ 50)
- `src/main/kotlin/com/codescene/jetbrains/platform/listeners/FileEventProcessor.kt` (≤ 119)
- `src/main/kotlin/com/codescene/jetbrains/platform/di/CodeSceneProjectServiceProvider.kt` (≤ 58)
- `core/src/main/kotlin/com/codescene/jetbrains/core/util/CliCacheUtils.kt` (≤ 10)
- `src/main/kotlin/com/codescene/jetbrains/platform/util/AceFileRefresh.kt` (≤ 68)
- `src/main/kotlin/com/codescene/jetbrains/platform/util/AceEntryOrchestrator.kt` (≤ 265)
- `benchmarks/src/jmh/kotlin/com/codescene/jetbrains/benchmarks/BenchmarkFixtures.kt` (≤ 105)

**Test updates:** `FileEventHandlerTest.kt` (≤ 62), `CliCacheUtilsTest.kt` (≤ 35), `testdoubles/StubGitService.kt` (≤ 28), `util/GitIgnoreSupportTest.kt` (≤ 178).

### Out of scope

`isExpiredCliCacheEntry` and `PlatformCliCacheService` (live ACE cache pruning), `Git4IdeaChangeLister` (its private merge-base helpers stay until PKG-34), `MainLineBranchResolver`, `Git4IdeaCommandExecutor`, `GitCommandExecutor`, `GitLineHandlerExt.kt`, `ClosestMainLineMergeBase.kt`, `MainLineBranchContext.kt`, `CodesceneRepoConfig.kt`, `Git4IdeaGitServiceTest.kt` (it only covers kept methods; it must pass unchanged), `HomeViewData.commitBaseline`, `MessageTypes.COMMIT_BASELINE`.

### Interfaces (must match plan Task 3)

- `IGitService { getRepoRelativePath(filePath); getRepoRoot(filePath); isIgnored(filePath) }`
- `class Git4IdeaGitService(val project: Project) : IGitService`
- `FileEventHandler(deltaCache: IDeltaCacheService, reviewCache: IReviewCacheService)`

### Definition of done

- [ ] The six files are deleted and the Task 3 `rg` returns nothing.
- [ ] ACE passes the absolute path as the CLI file name at all three call sites; the benchmark's baseline path is `"benchmark-base:" + path`.
- [ ] The existing `handleDelete` comment is kept; only "All three caches are" becomes "Both caches are".
- [ ] `.\gradlew.bat :core:test :test :benchmarks:jmhJar` passes; ktlint for core and `src/`, class-size pass.
- [ ] Single commit, MCP safeguard clean. No `runIde`.

### Agent prompt (copy-paste)

```text
Implement PKG-32 for CS-12077 on branch feature/cs-12077-delete-baseline after PKG-30 is merged into feature/cs-12077-cs-ide-json-rpc.
Follow Task 3 in docs/superpowers/plans/2026-10-14-phase4-legacy-deletion.md and the PKG-32 section of docs/superpowers/plans/2026-10-14-phase4-agent-packages.md.
Delete the four main and two test files, shrink IGitService to getRepoRelativePath/getRepoRoot/isIgnored, strip the baseline code from Git4IdeaGitService, make FileEventHandler take two caches, and inline the CLI cache-name helpers in ACE and the benchmark. Keep isExpiredCliCacheEntry. Do not touch Git4IdeaChangeLister or the main-line resolver chain.
Run the Task 3 rg, then .\gradlew.bat :core:test :test :benchmarks:jmhJar, ktlint for core and src (max line length 120), bb -f .github/check-class-size-mine.clj.
One commit: refactor: delete baseline resolution and caches.
JAVA_HOME=JDK 17 (C:\Users\marti\.jdks\graalvm-jdk-17.0.12). On Windows use gradlew.bat, not make.
pre_commit_code_health_safeguard before commit.
Do not merge or push the integration branch. Do not commit in C:\Git\codescene-jetbrains.
Check git log -1 --format=%B and amend away any Co-authored-by trailer.
```

---

## PKG-33 — runIde smoke, ungated state

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-phase4-smoke` |
| **Base** | **PKG-30, PKG-31, PKG-32** merged |
| **Commit message** | `docs: record CS-12077 phase 4 smoke` |
| **Plan tasks** | Task 4 |
| **Parallel with** | Waiting for the CI gate |

Run by the coordinator with the user. No code changes. The commit only fills in the results below (`git add -f`).

### Scope

Prepare as in plan Task 4 step 1 (kill the sandbox, `--stop`, copy `cs-cwf`, `runIde`, open `C:\Temp\cs12077-smoke\repo-a` on `feature`). Ask the user each U line with AskQuestion, one at a time. The coordinator runs each C line from the shell and `build\idea-sandbox\IC-2024.3.7\log\idea.log`, reading only lines written since the check started.

### Out of scope

Any code change. A failure is reported, then fixed in its own package.

### Interfaces

None. Uses `C:\Temp\cs12077-smoke` (`repo-a`, `repo-b`) as described in the plan's Smoke decision.

### U checks (user, in the IDE)

- [ ] U1. Edits update annotations, Code Vision and the monitor; ACE refactors in `src/AceProbe.java`.
- [ ] U2. `Ctrl+S` keeps annotations; no second indicator for the same content.
- [ ] U3. External append to unopened `src/App.kt` shows it in the monitor; `git checkout -- src/App.kt` removes it.
- [ ] U4. New untracked `src/Smoke.kt` from the shell appears; deleting it removes it.
- [ ] U5. Adding `src/Untracked.kt` to `.gitignore` removes it from the monitor; removing the line brings it back.
- [ ] U6. Branch widget switch `feature` → `other` → `feature` with an unsaved edit: annotations stay, monitor shows each branch.
- [ ] U7. Built-in terminal `git checkout other` / `git checkout feature` with an unsaved edit: the editor keeps its annotations.
- [ ] U8. Rename then delete an open modified file in the project view: the monitor entry follows, then goes.
- [ ] U9. Open `repo-b` in a second window, both update; close it; `repo-a` keeps updating.

### C checks (coordinator)

- [ ] C1. Exactly one `cs-ide` child of the sandbox IDE.
- [ ] C2. No log lines from deleted classes (`GitChangeObserver|VfsEventBridge|GitRepoStateListener|CodesceneRepoFilesListener|GitIgnoreFilesListener|SavedFilesTracker|BaselineReviewCache`).
- [ ] C3. U3 gives pushes with `n+1` then `n` files.
- [ ] C4. U5 gives pushes with one fewer, then one more file.
- [ ] C5. U6 logs `reseeding 0 unsaved documents`; U7 logs `reseeding 1 unsaved documents`.
- [ ] C6. Killing the `cs-ide` child logs the close or exit, then `watch started … session=2` and a push within 35 s; one child again; U1 still works.
- [ ] C7. U9's close logs `cs-ide watch stopped for <repo-b> (sent=true)`.
- [ ] C8. No exceptions mentioning `WorkspaceReviewService`, `CsIdeWatchService`, `WorkspaceWatchCoordinator`, `FileEventProcessor` or `Git4IdeaGitService`; no `ConsumerGuard` ` failed: ` lines (list any with stack).
- [ ] C9. `Poll found` lines continue every 9 s.
- [ ] C10. Nothing left after exiting the IDE.

### Results

Run 2026-10-14 13:26–13:59 on `feature/cs-12077-phase4` at `1706ee4` (PKG-30 `8d67260`, PKG-31 `f1f316a`, PKG-32 `1706ee4`), sandbox IC-2024.3.7, `cs-ide` 1.0.74-SNAPSHOT (`efcaeed`). First push: `repo-a` 6 files.

- [x] U1. Pass. Edits update annotations, Code Vision and the monitor; ACE refactors in `src/AceProbe.java`.
- [x] U2. Pass. `Ctrl+S` keeps annotations; no second indicator.
- [x] U3. Pass. The appended `src/App.kt` appeared in the monitor; `git checkout -- src/App.kt` removed it. `App.kt` was open in an editor during the check (`Replaying queued review with editor file=App.kt`), so this did not exercise the unopened path; PKG-36 U11 covers it.
- [x] U4. Pass. Untracked `src/Smoke.kt` appeared; deleting it removed it.
- [x] U5. Pass. Ignoring `src/Untracked.kt` removed it; removing the line brought it back.
- [x] U6. Pass. Branch widget `feature` → `other` → `feature`: annotations stay, monitor follows.
- [x] U7. Pass. Built-in terminal checkouts kept the editor's annotations. Checkouts run from the coordinator's shell with focus kept in the IDE also kept them.
- [x] U8. Pass. Rename and delete of an open modified file (`src/Util.kt`) in the project view: entry followed, then went.
- [x] U9. Pass. `repo-b` in a second window: both update; after File | Close Project, `repo-a` keeps updating.

- [x] C1. Pass. One `cs-ide` child (pid 13624) of the sandbox IDE (pid 25740); still one with both projects open.
- [x] C2. Pass. No lines from deleted classes.
- [x] C3. Partial, expected. The append gave `7 files (push)` (13:32:43). The restore gave no push back to 6: the server keeps a touched file in the inventory until a rescan, and reviews it again (workspace README, "Inventory"). The rescore returned a null delta, which removed the entry. The next rescan (U5's `.gitignore` change) dropped `App.kt` from the inventory. The plan's expectation is wrong; the behaviour is the server's documented one.
- [x] C4. Pass. Ignore: `5 files (push)` with `hidden=1` (the rescan also dropped the stale `App.kt`, 7 → 5); removing the line: `6 files (push)`.
- [x] C5. Pass. U6 logged `reseeding 0 unsaved documents` twice. The built-in terminal (U7) also logged `reseeding 0`: IntelliJ saves documents when its terminal takes focus ("Save files when switching to a different application or a built-in terminal", on by default), so the plan's U7 premise does not hold. With the edit kept unsaved and both checkouts run from the coordinator's shell while focus stayed in the IDE, both HEAD moves logged `reseeding 1 unsaved documents` (13:50:28, 13:50:38).
- [x] C6. Pass. `Stop-Process` on pid 13624 at 13:56:38: `cs-ide JSON-RPC closed: JSON-RPC stream ended`, `cs-ide server started` at 13:56:45, `watch started for repo-a … session=2`, `5 files (push)` at 13:56:46. One child again (pid 5916). U1 still works.
- [x] C7. Pass. `cs-ide watch stopped for C:/temp/cs12077-smoke/repo-b (sent=true)` (13:56:10).
- [x] C8. Pass. No exceptions mentioning the five classes, no ` failed: ` lines, no `ERROR` lines. The only recurring WARN is IntelliJ's `RegionUrlMapper` `SecurityException: setContextClassLoader` (sandbox, unrelated).
- [x] C9. Pass. `Poll found` every ~9 s through the end (13:58:40).
- [x] C10. Pass. No `cs-ide` and no sandbox process after exit.

Plan corrections found during Wave 1–3 (for PKG-34 onward): `Format-Hex` has no `-Count` on Windows PowerShell 5.1 (read the first bytes with `[IO.File]::ReadAllBytes`); the sandbox-kill filter `*idea-sandbox*` also matches Gradle test workers of other worktrees and killed one `:test` run, so exclude `*GradleWorkerMain*` and `*GradleDaemon*`; the Task 3 `rg` still matches `injectedMainLineBranchResolver` in `Git4IdeaChangeLister.kt` until Task 5.

### Definition of done

- [ ] Every U and C line has a result. A failing line is fixed in its own package (branch, one commit) before PKG-34 starts, and its line is re-run.

---

## PKG-34 — Poller and git listing deletion (CI-gated)

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-delete-poller` |
| **Base** | **PKG-33** recorded and the **CI gate** met |
| **Commit message** | `refactor: delete the change poller and git listing` |
| **Plan tasks** | Task 5 |
| **Parallel with** | None (PKG-35 may stack on it) |

### Scope

**Delete (main):** in `src/main/.../platform/git/`: `PeriodicChangeListerService.kt`, `Git4IdeaChangeLister.kt`, `Git4IdeaCommandExecutor.kt`, `GitCommandExecutor.kt`, `GitLineHandlerExt.kt`, `MainLineBranchResolver.kt`; in `core/src/main/.../core/git/`: `GitChangeLister.kt`, `CommittedChangesCommand.kt`, `FileSystemAdapter.kt`, `ClosestMainLineMergeBase.kt`, `MainLineBranchContext.kt`, `CodesceneRepoConfig.kt`; `core/.../core/contracts/IGitChangeLister.kt`; `core/.../core/cleanup/StaleItemCleanup.kt`.

**Delete (tests):** in `src/test/.../platform/git/`: `Git4IdeaChangeListerTest.kt`, `Git4IdeaChangeListerFeatureBranchTest.kt`, `Git4IdeaChangeListerWindowsPathTest.kt`, `Git4IdeaChangeListerTestFixture.kt`, `Git4IdeaTestSupport.kt`, `MainLineBranchResolverTest.kt`; in `core/src/test/.../core/git/`: `GitChangeListerTest.kt`, `GitCommittedChangesTest.kt`, `GitStatusChangesTest.kt`, `GitUntrackedFilesTest.kt`, `TestGitChangeLister.kt`, `TestGitChangeListerForCommitted.kt`, `TestGitChangeListerForStatus.kt`, `TestRepoMainLineContext.kt`, `GitTestSupport.kt`, `MainLineBranchContextTest.kt`, `ClosestMainLineMergeBaseTest.kt`, `CodesceneRepoConfigTest.kt`, `CommittedChangesCommandTest.kt`, `FileSystemAdapterTest.kt`; `core/src/test/.../core/cleanup/StaleItemCleanupTest.kt`.

**Modify**

- `src/main/kotlin/com/codescene/jetbrains/platform/listeners/ProjectStartupActivity.kt` (≤ 157)
- `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceReviewService.kt` (≤ 182)
- `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceWatchCoordinator.kt` (≤ 206)
- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceWatch.kt` (≤ 195)
- `core/src/main/kotlin/com/codescene/jetbrains/core/git/GitPathUtils.kt` (≤ 40)
- `core/src/test/kotlin/com/codescene/jetbrains/core/git/GitPathUtilsTest.kt` (≤ 65)
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceWatchScopeTest.kt` (≤ 115)
- `CHANGELOG.md`, `DECISIONLOG.md` (no BOM; keep UTF-8)

### Out of scope

`InventoryMonitorSync` and its test, `WorkspaceWatchInventoryTest`, `WatchEffect.kt`, `WatchLiveTest`, `WatchInventoryParityLiveTest`, the disk-submission path (PKG-35), `.codescene/code-health-rules.json` (deferred), the nested-root sync (deferred), `OpenFilesObserverAdapter`, `Git4IdeaGitService`, `plugin.xml`.

### Interfaces (must match plan Task 5)

- `WorkspaceWatch.setScopes` returns `SyncMonitor(removed.localRoot, emptySet())` for each removed root that had an inventory, before effects for remaining roots. `isWatched` is gone from `WorkspaceWatch`, `WorkspaceWatchCoordinator` and `WorkspaceReviewService`.

### Definition of done

- [ ] The coordinator recorded the gate (run id, head SHA, three `Test` conclusions) in the hand-off before the agent starts.
- [ ] The 35 files are deleted; the Task 5 `rg` returns nothing.
- [ ] The four `WorkspaceWatchScopeTest` changes from plan Task 5, and the six kept `GitPathUtilsTest` tests, pass.
- [ ] CHANGELOG has the three lines; DECISIONLOG has TOC item 8 and `DEC-2026-10-14-001` in the existing format.
- [ ] `.\gradlew.bat :core:test :test :benchmarks:jmhJar` passes, including both Phase 3 live tests; ktlint for core and `src/`, class-size pass.
- [ ] Single commit, MCP safeguard clean. No `runIde`.

### Agent prompt (copy-paste)

```text
Implement PKG-34 for CS-12077 on branch feature/cs-12077-delete-poller from feature/cs-12077-cs-ide-json-rpc (after PKG-33 is recorded and the coordinator confirmed the #194 CI gate).
Follow Task 5 in docs/superpowers/plans/2026-10-14-phase4-legacy-deletion.md and the PKG-34 section of docs/superpowers/plans/2026-10-14-phase4-agent-packages.md.
Write the WorkspaceWatchScopeTest changes first and see the removed-root test fail. Then git rm the 35 files, remove the poller start and the three isWatched layers, make WorkspaceWatch.setScopes sync removed roots to empty, trim GitPathUtils and GitPathUtilsTest, and add the CHANGELOG lines and the DECISIONLOG entry exactly as the plan describes.
Do not touch the disk-submission path, InventoryMonitorSync or the live tests.
Run the Task 5 rg, then .\gradlew.bat :core:test :test :benchmarks:jmhJar, ktlint for core and src (max line length 120), bb -f .github/check-class-size-mine.clj.
One commit: refactor: delete the change poller and git listing.
JAVA_HOME=JDK 17 (C:\Users\marti\.jdks\graalvm-jdk-17.0.12). On Windows use gradlew.bat, not make.
pre_commit_code_health_safeguard before commit.
Do not merge or push the integration branch. Do not commit in C:\Git\codescene-jetbrains.
Check git log -1 --format=%B and amend away any Co-authored-by trailer.
```

---

## PKG-35 — Disk submission deletion

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-drop-disk-submissions` |
| **Base** | **PKG-34** merged, or stacked on its branch |
| **Commit message** | `refactor: drop disk review submissions` |
| **Plan tasks** | Task 6 |
| **Parallel with** | None |

### Scope

**Modify**

- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewPipeline.kt` (≤ 225)
- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewMatching.kt` (≤ 132)
- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewSubmitter.kt` (≤ 30)
- `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceReviewService.kt` (≤ 160)
- `src/main/kotlin/com/codescene/jetbrains/platform/api/CachedReviewService.kt` (≤ 118)

**Test updates** (`core/src/test/kotlin/com/codescene/jetbrains/core/cli/`): `WorkspaceReviewPipelineTest.kt` (≤ 300), `WorkspaceReviewPipelineFixture.kt` (≤ 93), `WorkspaceReviewPathKeyTest.kt` (≤ 75), `WorkspaceReviewWatchStateTest.kt` (≤ 130), `WorkspaceReviewWatchResultsTest.kt` (≤ 125), `WorkspaceReviewSubmitterTest.kt` (≤ 80).

### Out of scope

`SubmittedDisk`, `observedState`, `awaitingDelta`, `acceptsResult`, `afterDelta`, `afterSessionLost`, `WorkspaceFileReader`, `WorkspaceWatchCoordinator`, the presenter, the `REVIEW_PROGRESS` constant (deferred), `FileEditorLifecycleListener`, Code Vision.

### Interfaces (must match plan Task 6)

- `WorkspaceReviewPipeline.submit(repoRoot, buffers)`
- `WorkspaceReviewMatching.shouldSend(state, gitBlobSha)`, `failureMatches` (buffers only), `afterReview = copy(accepted = true)`; `releasedByIdleProgress` deleted
- `WorkspaceReviewSubmitter(pipeline, locate, isSupported).submitEditor(absolutePath, content)`
- `CachedReviewService.replayQueuedReview` replays only with a live editor; otherwise logs `Dropping queued review without editor file=<name>`

### Definition of done

- [ ] Every test rename and assertion listed in plan Task 6 step 1 is in place; the two deleted pipeline tests and four deleted submitter tests are gone; `submitEditor skips unsupported and unlocated paths` is added.
- [ ] Both Task 6 `rg` commands return nothing.
- [ ] `.\gradlew.bat :core:test :test` passes, including `WorkspaceReviewLiveTest` and `WatchLiveTest`; ktlint for core and `src/`, class-size pass.
- [ ] Single commit, MCP safeguard clean. No `runIde`.

### Agent prompt (copy-paste)

```text
Implement PKG-35 for CS-12077 on branch feature/cs-12077-drop-disk-submissions, based on PKG-34 (merged into feature/cs-12077-cs-ide-json-rpc, or stacked on feature/cs-12077-delete-poller).
Follow Task 6 in docs/superpowers/plans/2026-10-14-phase4-legacy-deletion.md and the PKG-35 section of docs/superpowers/plans/2026-10-14-phase4-agent-packages.md.
Update the six test files first exactly as Task 6 step 1 lists (renames and assertions), then remove DiskSubmission, queueDisk, the disks parameter, both submitPaths, reviewByPath and the disk-in-flight matcher rules. SubmittedDisk stays for watch results.
Do not change observedState, awaitingDelta, acceptsResult, afterDelta, afterSessionLost or WorkspaceWatchCoordinator.
Run both Task 6 rg commands, then .\gradlew.bat :core:test :test, ktlint for core and src (max line length 120), bb -f .github/check-class-size-mine.clj.
One commit: refactor: drop disk review submissions.
JAVA_HOME=JDK 17 (C:\Users\marti\.jdks\graalvm-jdk-17.0.12). On Windows use gradlew.bat, not make.
pre_commit_code_health_safeguard before commit.
Do not merge or push the integration branch. Do not commit in C:\Git\codescene-jetbrains.
Check git log -1 --format=%B and amend away any Co-authored-by trailer.
```

---

## PKG-36A — Match watch results against dirty documents only

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-watch-dirty-documents` |
| **Base** | **PKG-35** merged |
| **Commit message** | `fix: match watch results against unsaved buffers` |
| **Parallel with** | PKG-36B |

### Scope and design

The server hashes disk bytes for watch results. A loaded but saved IntelliJ `Document` can be stale after an external write and always has LF line endings, so its hash can reject a valid disk result. `WorkspaceFileReader` must use loaded text only when the document is unsaved; otherwise it hashes raw disk bytes. Unsaved editor reviews remain authoritative.

**Modify**

- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceFileReader.kt`
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceFileReaderTest.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceWatchCoordinator.kt`
- `docs/superpowers/specs/2026-10-12-cs-ide-json-rpc-migration-design.md`
- `DECISIONLOG.md`

### Required test-first cycle

1. Introduce an explicit loaded-text value carrying `text` and `unsaved`; update `WorkspaceFileReader.openText` to return it.
2. Add `saved loaded text uses bytes` first. Supply CRLF bytes and LF loaded text with `unsaved = false`; expect the raw-byte SHA, `text = null`, and prove `readBytes` was called. Run that single test and capture the expected failure before production changes.
3. Update the existing loaded-text and empty-text tests to mark those documents unsaved. They continue to prove unsaved text wins and is hashed.
4. Implement the minimum reader logic: loaded text wins only when `unsaved`; otherwise read disk bytes.
5. In `WorkspaceWatchCoordinator`, obtain the cached document and pair its text with `FileDocumentManager.isDocumentUnsaved(document)`.
6. Correct the JSON-RPC migration spec's loaded-document rule and add a decision recording why saved loaded documents use disk bytes.

### Definition of done

- [ ] Red/green evidence for `saved loaded text uses bytes`.
- [ ] `.\gradlew.bat :core:test :test` passes; ktlint for core and `src/`, class-size pass.
- [ ] No `DIAG` logging, no `plugin.xml` change, no UTF-8 BOM.
- [ ] Single commit, MCP safeguard clean.

---

## PKG-36B — Native monitor path separators

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-native-monitor-paths` |
| **Base** | **PKG-35** merged |
| **Commit message** | `fix: use native monitor path separators` |
| **Parallel with** | PKG-36A |

### Scope and design

Keep cache keys, watch paths and resolver inputs unchanged. Normalize only the absolute file path sent to the Code Health Monitor: backslashes on Windows and forward slashes on macOS/Linux. The platform supplies its separator to core mapping; the original path remains the input to the ACE resolver.

**Modify**

- `core/src/main/kotlin/com/codescene/jetbrains/core/mapper/CodeHealthMonitorMapper.kt`
- `core/src/test/kotlin/com/codescene/jetbrains/core/mapper/CodeHealthMonitorMapperTest.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/webview/util/UpdateMonitor.kt`

### Required test-first cycle

1. Add mapper tests first for a mixed-separator absolute path:
   - separator `'\\'` produces `C:\repo\src\File.kt`;
   - separator `'/'` produces `/repo/src/File.kt`;
   - the function-to-refactor resolver receives the original unmodified path.
2. Run the focused mapper tests and capture the expected failure before production changes.
3. Give `CodeHealthMonitorMapper` an injected path separator and normalize only `File(fileName = ...)` in `getFileDeltaData`.
4. Construct the shared mapper in `UpdateMonitor` with `java.io.File.separatorChar`.

### Definition of done

- [ ] Red/green evidence for both platform separators and unchanged resolver input.
- [ ] `.\gradlew.bat :core:test :test` passes; ktlint for core and `src/`, class-size pass.
- [ ] Cache paths and `deltaResults` are not mutated; no `plugin.xml` change, no UTF-8 BOM.
- [ ] Single commit, MCP safeguard clean.

---

## PKG-36 — Final runIde smoke

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-phase4-final-smoke` |
| **Base** | **PKG-36A** and **PKG-36B** merged |
| **Commit message** | `docs: record CS-12077 phase 4 final smoke` |
| **Plan tasks** | Task 7 |
| **Parallel with** | None |

Run by the coordinator with the user, prepared as PKG-33. The commit only fills in the results below.

### Out of scope

Any code change. A failure is reported, then fixed in its own package.

### Interfaces

None. Same repositories as PKG-33.

### U checks (user, in the IDE)

- [ ] U1–U9 as in PKG-33.
- [ ] U10. Add `repo-b` as a second content root of `repo-a` (register the VCS root if asked): its changes appear; remove the content root: its entries leave the monitor except an open file.
- [ ] U11. With every editor closed, a shell edit to `src/Util.kt` updates its monitor entry.

### C checks (coordinator)

- [ ] C1–C8 and C10 as in PKG-33.
- [ ] C9. 60 s idle: no `Poll found`, `Removing stale item`, `Git4IdeaChangeLister` or `PeriodicChangeListerService` lines, no new inventory or `watch started` lines.
- [ ] C11. U10 logs `watch started for <repo-b>` and a push, then `watch stopped for <repo-b> (sent=true)` and `monitor sync for <repo-b>: hidden=<n>` with `n` > 0.
- [ ] C12. No `Replaying queued review by path` lines; any `Dropping queued review without editor` lines are listed.

### Results

_To be filled in by PKG-36._

### Definition of done

- [ ] Every U and C line has a result. Failures are fixed in their own package and re-run. After this, the coordinator asks the user before the final fast-forward of `feature/cs-12077-cs-ide-json-rpc`.

---

## Conflict matrix

| Packages | Parallel? | Shared files |
| --- | --- | --- |
| PKG-30 + PKG-31 | Yes | none. 30 edits `ProjectStartupActivity` and `GitPathUtils`; 31 edits `WorkspaceReviewService` and `CsIdeWatchService` |
| PKG-31 + PKG-32 | Yes | none |
| PKG-30 + PKG-32 | No | 30 deletes the observer files and tests that use `baselineReviewCacheService`, the three-argument `FileEventHandler`, `IBaselineReviewCacheService` and `MockGitService : IGitService`. Stack 32 on 30 |
| PKG-33 + PKG-30 / 31 / 32 | No | 33 smokes their merged state |
| PKG-34 + PKG-30 | No (sequential) | `ProjectStartupActivity`, `GitPathUtils` (different lines) |
| PKG-34 + PKG-31 | No (sequential) | `WorkspaceReviewService` |
| PKG-35 + PKG-34 | No | the poller calls `submitPaths` until 34; both edit `WorkspaceReviewService`. Stack 35 on 34 |
| PKG-35 + PKG-31 | No (sequential) | `WorkspaceReviewService` |
| PKG-36A + PKG-36B | Yes | none. 36A edits the watch reader and coordinator; 36B edits monitor mapping |
| PKG-36A + PKG-36 | No | final smoke must verify the saved-document fix |
| PKG-36B + PKG-36 | No | final smoke must verify native monitor paths |

---

## Deferred (not in Phase 4)

- Nested-root monitor flapping: with `/p` and `/p/sub` both watched, `/p`'s sync hides `/p/sub` entries until `/p/sub`'s next sync or the next idle progress. Fix idea: pass the project's nested watched roots to `InventoryMonitorSync.sync` as exclusions.
- Inventory push logging for other projects' roots (`WorkspaceWatchCoordinator.onNotification` logs before the scope check).
- One shared `cs-ide/reviewProgress` constant (pipeline companion and coordinator literal).
- `.codescene/code-health-rules.json` `WorkspaceWatch` exemptions: `Global Conditionals` and `String Heavy Function Arguments` duplicate the global fallback; only `Primitive Obsession` is a real exemption.
- Phase 3 smoke C4: log the process exit code when the stream closes first.
- `HomeViewData.commitBaseline` / `MessageTypes.COMMIT_BASELINE` webview protocol fields.

---

## Docs on the integration branch

`docs` is gitignored. These files are force-added (`git add -f`), and any new doc needs the same. `docs/superpowers/` stays on `feature/cs-12077-cs-ide-json-rpc` until the final commit before the project merges to master.
