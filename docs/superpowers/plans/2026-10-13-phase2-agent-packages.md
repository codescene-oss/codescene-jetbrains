# CS-12077 Phase 2: Agent work packages

Use this document to assign **one package per agent**: one branch, **one commit**, clear scope. Full task detail lives in `2026-10-13-phase2-workspace-review.md`. Architecture and decisions: `../specs/2026-10-12-cs-ide-json-rpc-migration-design.md` (Phase 2).

**Integration branch:** `feature/cs-12077-cs-ide-json-rpc`

**Branch strategy:** this is the feature branch for the whole CS-12077 project (all phases). Nothing merges to master until every planned phase is done. Later phases add their packages to this branch the same way. Bring master changes in with `git merge origin/master` (never rebase or force-push the shared branch). `docs/superpowers/` stays on this branch and is removed in a final commit just before the project merges to master.

**Test releases:** from a clean checkout of this branch, `make test-release BUMP=minor` (or `bb --classpath .github -m release test minor`) creates an annotated tag `v0.6.0-test.<sha>` on HEAD; `git push origin <tag>` runs the Release workflow, which builds the plugin and creates a GitHub pre-release with the zip without publishing to the marketplace. It requires `pluginVersion` to equal the latest stable tag reachable from HEAD (currently `v0.5.8`).

**Gradle:** set `JAVA_HOME` to JDK 17 before any `./gradlew` (Gradle 8.14 does not run on JDK 25). On this machine that JDK is `C:\Users\marti\.jdks\graalvm-jdk-17.0.12`.

**Every package:**

- `make format-check class-size-mine` passes (ktlint clean; changed `.kt` files ≤ 325 lines). Fix lint findings before anything else. `make` hangs on Windows, so there run the equivalents directly: `.\gradlew.bat :core:ktlintCheck -PktlintFailOnError=true` (add `:ktlintCheck` when touching `src/`) and `bb -f .github/check-class-size-mine.clj`. The same applies to `make iter`: run the relevant `gradlew` test tasks instead.
- CodeScene MCP `pre_commit_code_health_safeguard` before commit.
- No `Co-authored` trailers; no new code comments. Cursor's agent adds `Co-authored-by: Cursor` on its own, so check `git log -1 --format=%B` after committing and amend it away.
- Never merge, fast-forward or push the integration branch, and never commit in `C:\Git\codescene-jetbrains`. Work only in your own worktree and branch; the coordinator merges.
- After a rebase, check for conflict markers (`git diff --check HEAD~1`) before declaring the package done.
- A package that rewires platform call sites also runs the `runIde` smoke checklist in the plan. On Windows that is `.\gradlew.bat runIde`, not `make run-ide`.

**Merging:** the coordinator rebases each package branch onto the current integration tip and fast-forwards it (`git merge --ff-only`), so the integration branch keeps one commit per package. A package whose base is not merged yet may stack on the base package's branch and is rebased once that merges.

---

## Merge order

```text
PKG-10 notices  ∥  PKG-11 server notify  ∥  PKG-14 delta lookup
        \                 /
         \               /
     PKG-12 pipeline     PKG-13 reviewFiles client   (12 needs 10; 13 needs 10 and 11; 12 ∥ 13)
                    \              /
                     \            /
                      PKG-15 presenter   (needs 12, 13, 14)
                             |
                      PKG-16 cut-over    (needs 15; the only runIde package)
```

PKG-12 and PKG-13 are parallel with each other. PKG-14 stays parallel with both until PKG-15. PKG-10, PKG-11 and PKG-14 can start together on the integration tip.

---

## PKG-10 — Review notices

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-review-notices` |
| **Base** | Integration branch tip |
| **Commit message** | `feat(cli): parse cs-ide review notifications` |
| **Plan tasks** | Task 1 |
| **Parallel with** | PKG-11, PKG-14 |

### Scope (create only)

- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/GitBlobSha.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspacePaths.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewNotices.kt`
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/GitBlobShaTest.kt`
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspacePathsTest.kt`
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewNoticesTest.kt`

### Out of scope

`CsIdeServer`, the pipeline, `reviewFiles` JSON, platform, delta cache.

### Interfaces (must match Phase 2 plan Task 1)

- `GitBlobSha.ofBytes`, `GitBlobSha.ofString`
- `WorkspacePaths.sameRoot`, `WorkspacePaths.normalizeRel`
- `ReviewFileWire`, `ReviewFilesSender`
- `FileReviewNotice`, `DeltaReviewNotice`, `ReviewFailedNotice`, `ReviewProgressNotice`
- `WorkspaceReviewNotices.fileReview`, `deltaReview`, `reviewFailed`, `reviewProgress`
- `progressTitle`

### Definition of done

- [ ] SHA vectors: `"a"` → `2e65efe2a145dda7ee51d1741299f848e5bf752e`, `""` → `e69de29bb2d1d6434b8b29ae775ad8c2e48c5391`, `"ü"` → `99fe40df261f7d4afd1391fe2739b2c7466fe968` (2 UTF-8 bytes), `"a\n"` → `78981922613b2afb6025042ff6bd878ac1994e85`.
- [ ] Parsers accept camelCase and kebab-case, treat missing and null `id` as null, return null without `path` or `repoRoot`, and map `Review` / `Delta` after reading blob SHAs.
- [ ] `progressTitle` is null at count 0 and `CodeScene analysis (N remaining)` otherwise.
- [ ] `./gradlew :core:test --tests "com.codescene.jetbrains.core.cli.GitBlobShaTest" --tests "com.codescene.jetbrains.core.cli.WorkspacePathsTest" --tests "com.codescene.jetbrains.core.cli.WorkspaceReviewNoticesTest"` passes.
- [ ] ktlint, class-size (each new file ≤ 325), single commit, MCP safeguard clean.

### Agent prompt (copy-paste)

```text
Implement PKG-10 for CS-12077 on branch feature/cs-12077-review-notices from feature/cs-12077-cs-ide-json-rpc.
Read docs/superpowers/plans/2026-10-13-phase2-workspace-review.md Task 1 and docs/superpowers/plans/2026-10-13-phase2-agent-packages.md PKG-10.
TDD: tests first, then GitBlobSha.kt, WorkspacePaths.kt and WorkspaceReviewNotices.kt only.
One commit: feat(cli): parse cs-ide review notifications.
Do not touch CsIdeServer, the pipeline, platform, or the delta cache.
JAVA_HOME=JDK 17 (C:\Users\marti\.jdks\graalvm-jdk-17.0.12). On Windows use gradlew.bat, not make.
ktlint and bb -f .github/check-class-size-mine.clj, then pre_commit_code_health_safeguard before commit.
Do not merge or push the integration branch. Do not commit in C:\Git\codescene-jetbrains.
Check git log -1 --format=%B and amend away any Co-authored-by trailer.
```

---

## PKG-11 — Server notifications

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-server-notify` |
| **Base** | Integration branch tip |
| **Commit message** | `feat(cli): deliver cs-ide server notifications` |
| **Plan tasks** | Task 2 |
| **Parallel with** | PKG-10, PKG-14 |

### Scope

- Create `core/src/main/kotlin/com/codescene/jetbrains/core/cli/CsIdeNotificationHub.kt`
- Modify `core/src/main/kotlin/com/codescene/jetbrains/core/cli/CsIdeServer.kt`
- Create `core/src/test/kotlin/com/codescene/jetbrains/core/cli/CsIdeServerNotifyTest.kt`

### Out of scope

`WorkspaceReviewNotices.kt`, `WorkspaceReviewClient`, the pipeline, platform. Do not add tests to `CsIdeServerTest.kt` (it is already 292 lines).

### Interfaces (must match Phase 2 plan Task 2)

- `CsIdeServer.notify(method, params)`
- `CsIdeServer.addNotificationHandler(handler): AutoCloseable`
- `CsIdeServer.addSessionListener(listener): AutoCloseable`

### Definition of done

- [ ] `notify` writes a notification frame with no `id` after `cs-ide/start`, throws `CsIdeUnavailableException` during backoff without spawning, and resets backoff after a successful write.
- [ ] Handlers see notifications including `cs-ide/start`. A throwing handler does not block the others or the next request. Closing a registration removes only that handler.
- [ ] Session listeners run on transport close, process exit and `close()`, outside the server lock.
- [ ] `CsIdeServer.kt` stays ≤ 325 lines. The handler list is in `CsIdeNotificationHub.kt`.
- [ ] `./gradlew :core:test --tests "com.codescene.jetbrains.core.cli.CsIdeServerNotifyTest" --tests "com.codescene.jetbrains.core.cli.CsIdeServerTest"` passes.
- [ ] ktlint, class-size, single commit, MCP safeguard clean.

### Agent prompt

```text
Implement PKG-11 for CS-12077 on branch feature/cs-12077-server-notify from feature/cs-12077-cs-ide-json-rpc.
Read Task 2 in docs/superpowers/plans/2026-10-13-phase2-workspace-review.md and PKG-11 in docs/superpowers/plans/2026-10-13-phase2-agent-packages.md.
Add CsIdeServer.notify, addNotificationHandler and addSessionListener. Put the handler list in CsIdeNotificationHub.kt.
New tests go in CsIdeServerNotifyTest.kt. Do not grow CsIdeServerTest.kt.
Do not add reviewFiles JSON or the pipeline.
One commit: feat(cli): deliver cs-ide server notifications.
JAVA_HOME=JDK 17. On Windows use gradlew.bat, not make.
ktlint, class-size, pre_commit_code_health_safeguard.
Do not merge or push the integration branch. Amend away any Co-authored-by trailer.
```

---

## PKG-12 — Correlation pipeline

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-review-pipeline` |
| **Base** | Integration branch with **PKG-10** merged (or stacked on `feature/cs-12077-review-notices`) |
| **Commit message** | `feat(cli): correlate cs-ide review results` |
| **Plan tasks** | Task 3 |
| **Parallel with** | PKG-13, and PKG-14 until PKG-15 |

### Scope (create only)

- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewMatching.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewPipeline.kt`
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewPipelineTest.kt`

### Out of scope

`CsIdeServer.kt`, `WorkspaceReviewClient.kt`, platform, delta-cache classes. Use a fake `ReviewFilesSender`. Do not edit Task 1 files; consume their types.

### Definition of done

- [ ] Buffer submits carry id and content. Disk submits omit both. One `submit` sends one batch, minus entries already in flight or already accepted for that SHA.
- [ ] Id match plus git-blob SHA for buffers. Superseded ids and `cancel` drop late results. `sessionLost` allows the same content to be sent again.
- [ ] Id-less results are accepted only when the SHA matches the disk submission. A missing SHA on an id-less result is dropped.
- [ ] `activePaths()` covers buffer paths until delta or `reviewFailed`, and disk paths until the id-less result or `reviewProgress` count 0.
- [ ] Unknown methods are ignored.
- [ ] Both new production files ≤ 325 lines.
- [ ] `./gradlew :core:test --tests "com.codescene.jetbrains.core.cli.WorkspaceReviewPipelineTest"` passes.
- [ ] ktlint, class-size, single commit, MCP safeguard clean.

### Agent prompt

```text
Implement PKG-12 for CS-12077 on feature/cs-12077-review-pipeline after PKG-10 is merged (or stacked on feature/cs-12077-review-notices).
Follow Task 3 in docs/superpowers/plans/2026-10-13-phase2-workspace-review.md and PKG-12 in the Phase 2 agent packages.
Create WorkspaceReviewMatching.kt, WorkspaceReviewPipeline.kt and WorkspaceReviewPipelineTest.kt only.
Use a fake ReviewFilesSender. Do not edit CsIdeServer, WorkspaceReviewClient, or the notice types from PKG-10.
Keep each new .kt file at or under 325 lines.
One commit: feat(cli): correlate cs-ide review results.
JAVA_HOME=JDK 17. On Windows use gradlew.bat, not make.
ktlint, class-size, pre_commit_code_health_safeguard.
Do not merge or push the integration branch. Amend away any Co-authored-by trailer.
```

---

## PKG-13 — reviewFiles client

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-review-files-client` |
| **Base** | Integration branch with **PKG-10 and PKG-11** merged (or stacked on those branches) |
| **Commit message** | `feat(cli): send cs-ide reviewFiles` |
| **Plan tasks** | Task 4 |
| **Parallel with** | PKG-12, and PKG-14 until PKG-15 |

### Scope

- Create `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewClient.kt`
- Create `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewClientTest.kt`
- Create `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewLiveTest.kt`

### Out of scope

The pipeline, platform, `CsIdeServer.kt` except calling `notify`. Do not edit PKG-10 files.

### Definition of done

- [ ] `WorkspaceReviewClient` implements `ReviewFilesSender` and notifies `cs-ide/reviewFiles` with kebab-case `repo-root` and `rel-path`. `id` and `content` are omitted when null. No `baseline-revision`. An empty list does not notify.
- [ ] Unit test covers the JSON shape.
- [ ] Live test, one server per class, sends a buffer for a real git repo and waits up to 60s for `fileReview` (matching id and `GitBlobSha.ofString`), `deltaReview` with that id, and `reviewProgress` count 0. `close()` leaves no child process.
- [ ] `./gradlew :core:test --tests "com.codescene.jetbrains.core.cli.WorkspaceReviewClientTest" --tests "com.codescene.jetbrains.core.cli.WorkspaceReviewLiveTest"` passes.
- [ ] ktlint, class-size, single commit, MCP safeguard clean. No `runIde`.

### Agent prompt

```text
Implement PKG-13 for CS-12077 on feature/cs-12077-review-files-client after PKG-10 and PKG-11 are merged.
Follow Task 4 in docs/superpowers/plans/2026-10-13-phase2-workspace-review.md and PKG-13 in the Phase 2 agent packages.
WorkspaceReviewClient notifies cs-ide/reviewFiles. Unit-test the JSON, then add WorkspaceReviewLiveTest against the real jar.
Do not edit the pipeline or CsIdeServer except by calling notify.
One commit: feat(cli): send cs-ide reviewFiles.
JAVA_HOME=JDK 17. On Windows use gradlew.bat, not make.
ktlint, class-size, pre_commit_code_health_safeguard.
Do not merge or push the integration branch. Amend away any Co-authored-by trailer.
```

---

## PKG-14 — Delta lookup

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-delta-lookup` |
| **Base** | Integration branch tip |
| **Commit message** | `feat(core): look up presented deltas by current content` |
| **Plan tasks** | Task 5 |
| **Parallel with** | PKG-10, PKG-11, PKG-12, PKG-13 |

### Scope

- Create `core/src/main/kotlin/com/codescene/jetbrains/core/delta/PresentedDelta.kt`
- Modify `DeltaCacheService.kt`, `IDeltaCacheService.kt`, `InMemoryDeltaCacheService.kt`
- Tests in `PresentedDeltaTest.kt` (and `DeltaCacheServiceTest.kt` only if that file stays ≤ 325 lines)

### Out of scope

`getCachedDelta`, Code Vision, `cli/`, platform. Do not change `get(DeltaCacheQuery)`.

### Interfaces (must match Phase 2 plan Task 5)

- `presentedDeltaContents(scoredText, oldGitBlobSha, newGitBlobSha): Pair<String, String>` as `(headContent, currentContent)`
- `getByCurrentContent(filePath, currentContent): Pair<Boolean, Delta?>` on `DeltaCacheService` and `IDeltaCacheService`

### Definition of done

- [ ] Equal blob SHAs store the same head and current string. Different SHAs store head `baseline:<old>` so `getAll()` still treats the file as content-changed when `scoreChange` is 0.
- [ ] `getByCurrentContent` matches the current string only. A null delta is a hit and is absent from `getAll()`.
- [ ] `DeltaCacheUtilsTest` still passes unchanged.
- [ ] `./gradlew :core:test --tests "com.codescene.jetbrains.core.delta.PresentedDeltaTest" --tests "com.codescene.jetbrains.core.delta.DeltaCacheServiceTest" --tests "com.codescene.jetbrains.core.delta.DeltaCacheUtilsTest"` passes.
- [ ] ktlint, class-size, single commit, MCP safeguard clean.

### Agent prompt

```text
Implement PKG-14 for CS-12077 on feature/cs-12077-delta-lookup from feature/cs-12077-cs-ide-json-rpc.
Follow Task 5 in docs/superpowers/plans/2026-10-13-phase2-workspace-review.md and PKG-14 in the Phase 2 agent packages.
Add presentedDeltaContents and DeltaCacheService.getByCurrentContent. Do not change getCachedDelta or get(DeltaCacheQuery).
Update InMemoryDeltaCacheService so it still implements IDeltaCacheService.
One commit: feat(core): look up presented deltas by current content.
JAVA_HOME=JDK 17. On Windows use gradlew.bat, not make.
ktlint, class-size, pre_commit_code_health_safeguard.
Do not merge or push the integration branch. Amend away any Co-authored-by trailer.
```

---

## PKG-15 — Presenter and progress indicator

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-review-presenter` |
| **Base** | Integration branch with **PKG-12, PKG-13 and PKG-14** merged |
| **Commit message** | `feat: present cs-ide workspace reviews` |
| **Plan tasks** | Task 6 |
| **Parallel with** | nothing; wait for 12, 13 and 14 |

### Scope

- Create `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewSubmitter.kt`
- Create `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewSubmitterTest.kt`
- Create `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceReviewPresenter.kt`
- Create `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceReviewService.kt`
- Create `src/main/kotlin/com/codescene/jetbrains/platform/progress/ReviewProgressIndicator.kt`
- Modify `src/main/kotlin/com/codescene/jetbrains/platform/cli/CsIdeServerService.kt` only to delegate `notify`, `addNotificationHandler` and `addSessionListener`

### Out of scope

`CachedReviewService`, `PathBasedReviewHandler`, `CodeReviewService`, `CodeDeltaService`, `PeriodicChangeListerService`, `getCachedDelta`. No existing call site changes, so no `runIde` smoke test.

### Definition of done

- [ ] `WorkspaceReviewSubmitterTest`: open document text is a buffer; raw bytes are a disk file hashed with `GitBlobSha.ofBytes`; paths group by repo; unsupported, unlocated and unreadable paths are skipped.
- [ ] `WorkspaceReviewService` is a project `@Service` and `Disposable`. It hops off the JSON-RPC reader thread before touching the pipeline. It registers a notification handler and a session listener and disposes both.
- [ ] Presenter writes the review cache only when it has document text, writes the delta cache through `presentedDeltaContents` and `completeDeltaAnalysis`, refreshes UI, and schedules ACE only for buffer results.
- [ ] One background indicator uses `progressTitle`. Count 0 clears it. No per-file indicator is added.
- [ ] `./gradlew :core:test --tests "com.codescene.jetbrains.core.cli.WorkspaceReviewSubmitterTest"` and `./gradlew test` pass.
- [ ] ktlint (`:core:ktlintCheck` and `:ktlintCheck`), class-size, single commit, MCP safeguard clean.

### Agent prompt

```text
Implement PKG-15 for CS-12077 on feature/cs-12077-review-presenter after PKG-12, PKG-13 and PKG-14 are merged.
Follow Task 6 in docs/superpowers/plans/2026-10-13-phase2-workspace-review.md and PKG-15 in the Phase 2 agent packages.
Add the submitter, presenter, project service and single reviewProgress indicator. Delegate notify and listeners from CsIdeServerService.
Do not change CachedReviewService, PathBasedReviewHandler, or getCachedDelta. Nothing calls the new service yet, so do not run runIde.
Hop off the JSON-RPC reader thread before calling the pipeline.
One commit: feat: present cs-ide workspace reviews.
JAVA_HOME=JDK 17. On Windows use gradlew.bat, not make.
ktlint for core and src, class-size, pre_commit_code_health_safeguard.
Do not merge or push the integration branch. Amend away any Co-authored-by trailer.
```

---

## PKG-16 — Review path cut-over

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-review-cutover` |
| **Base** | **PKG-15** merged |
| **Commit message** | `feat: review files through cs-ide reviewFiles` |
| **Plan tasks** | Task 7 |

### Scope

**Modify**

- `CachedReviewService.kt`, `CodeSceneService.kt`, `PeriodicChangeListerService.kt`
- `DeltaCacheUtils.kt`, `DeltaCacheUtilsTest.kt`
- `CodeSceneCodeVisionProvider.kt`, `CodeHealthCodeVisionProvider.kt`

**Delete after `rg` shows no remaining references**

- `PathBasedReviewHandler.kt`, `CodeReviewService.kt`, `CodeDeltaService.kt`

### Out of scope

`BaselineReviewCacheService`, `FileEventHandler`, `Git4IdeaGitService` baseline methods, `resolveDeltaExecutionPlan`, `watchFiles`, `CsIdeClient.review` / `delta` / `reviewDelta`.

### Definition of done

- [ ] Editor reviews debounce, then `submitEditor` with the document text. `reviewByPath` and the 9s poll call `submitPaths`. The poll submits the whole set once per tick, not once per file.
- [ ] `showProgress` is false on that path. `activeReviewCalls` includes `WorkspaceReviewService.activePaths()`. Cancel drops both the debounce job and the pipeline entry.
- [ ] `getCachedDelta(filePath, fileContent, deltaCacheService)` uses `getByCurrentContent` and does not call git. Both Code Vision call sites and `DeltaCacheUtilsTest` match.
- [ ] No review path calls `cs-ide/review`, `cs-ide/delta`, or `reviewBaseline`.
- [ ] `./gradlew test`, ktlint for core and `src/`, and class-size pass. Changed files ≤ 325 lines.
- [ ] `.\gradlew.bat runIde` smoke checklist from Task 7 step 5, with the result of each line written in the hand-off:
  - annotations, Code Vision and monitor update from a buffer edit
  - an unopened changed file reaches the monitor
  - one `CodeScene analysis (N remaining)` indicator, gone when the batch finishes
  - 20 seconds on an unchanged tree does not bring the indicator back
  - ACE still runs
  - one `java` child; killing it recovers on the next edit; IDE exit leaves none
- [ ] Single commit, MCP safeguard clean.

### Agent prompt

```text
Implement PKG-16 for CS-12077 on feature/cs-12077-review-cutover after PKG-15 is merged.
Follow Task 7 in docs/superpowers/plans/2026-10-13-phase2-workspace-review.md and PKG-16 in the Phase 2 agent packages.
Point CachedReviewService and PeriodicChangeListerService at WorkspaceReviewService. Pass showProgress false.
Change getCachedDelta to getByCurrentContent and update both Code Vision providers.
Delete PathBasedReviewHandler, CodeReviewService and CodeDeltaService only when rg shows they are unused.
Do not delete baseline cache types or send watchFiles.
Run ./gradlew test, ktlint, class-size, then .\gradlew.bat runIde and report every smoke-checklist line.
One commit: feat: review files through cs-ide reviewFiles.
JAVA_HOME=JDK 17 (C:\Users\marti\.jdks\graalvm-jdk-17.0.12). Do not use make on Windows.
pre_commit_code_health_safeguard before commit.
Do not merge or push the integration branch. Amend away any Co-authored-by trailer.
```

---

## Conflict matrix

| Packages | Parallel? | Shared files |
| --- | --- | --- |
| PKG-10 + PKG-11 + PKG-14 | Yes | none |
| PKG-12 + PKG-13 | Yes, after their bases | none. 12 does not edit notices or `CsIdeServer`. 13 does not edit the pipeline |
| PKG-12 + PKG-14 | Yes | none |
| PKG-13 + PKG-14 | Yes | none |
| PKG-11 + PKG-13 | No | 13 calls `notify` added by 11. Stack 13 on 11 if 11 is not merged |
| PKG-10 + PKG-12 | No | 12 consumes types from 10. Stack 12 on 10 if 10 is not merged |
| PKG-10 + PKG-13 | No | 13 consumes `ReviewFileWire` and `ReviewFilesSender` from 10 |
| PKG-15 + PKG-12 / 13 / 14 | No | 15 wires those three together and edits `CsIdeServerService.kt` |
| PKG-15 + PKG-16 | No | 16 is the first caller of `WorkspaceReviewService` |
| PKG-14 + PKG-16 | No | both can touch `DeltaCacheUtils.kt` only if 14 is merged first. 14 must not edit that file |
| PKG-16 + any earlier package | No | cut-over |

---

## Docs on the integration branch

`docs` is gitignored. These files are force-added (`git add -f`), and any new doc needs the same. `docs/superpowers/` stays on `feature/cs-12077-cs-ide-json-rpc` until the final commit before the project merges to master.
