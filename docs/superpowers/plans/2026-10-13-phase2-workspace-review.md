# Phase 2: workspace review pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Editor and path reviews go through one `cs-ide/reviewFiles` notification per batch, and the plugin applies `fileReview`, `deltaReview`, `reviewFailed` and `reviewProgress` instead of calling `cs-ide/review` or `cs-ide/delta`.

**Architecture:** Core owns the git-blob SHA, the notice parser, the correlation pipeline and the `reviewFiles` JSON. `CsIdeServer` gains `notify` plus handlers that survive respawn. A project service submits buffers (a live `Document`, with a UUID and the document text) and disk files (no id, no content). Accepted reviews fill the existing review cache. Accepted deltas fill the existing delta cache, which is what the monitor reads. One background indicator follows `reviewProgress`.

**Tech Stack:** Kotlin 1.9 / JVM 17 toolchain, Jackson from the extension API fat jar, JUnit 4 + MockK, IntelliJ Platform 2024.3+.

Spec: `docs/superpowers/specs/2026-10-12-cs-ide-json-rpc-migration-design.md` (Phase 2). Packages: `docs/superpowers/plans/2026-10-13-phase2-agent-packages.md`.

## Global Constraints

- Business logic in `core`; platform code is a thin adapter.
- No new code comments; keep existing ones.
- Every changed `.kt` file stays at or below 325 lines (`bb -f .github/check-class-size-mine.clj`).
- Outbound JSON keys are kebab-case; inbound payloads are normalized from camelCase.
- No new runtime dependencies.
- No request or notification handler work on the EDT, and no handler work on the JSON-RPC reader thread.
- `reviewFiles` is a notification. Never wait for a response to it.
- Do not send `baseline-revision`, `watchFiles` or `stopWatchFiles` in this phase.
- One commit per task. Do not commit the failing test on its own.
- On Windows, `make` hangs. Set `JAVA_HOME` to `C:\Users\marti\.jdks\graalvm-jdk-17.0.12` and run `.\gradlew.bat`. ktlint is `.\gradlew.bat :core:ktlintCheck -PktlintFailOnError=true`, plus `:ktlintCheck` when the change touches `src/`. Class size is `bb -f .github/check-class-size-mine.clj`.
- CodeScene MCP `pre_commit_code_health_safeguard` before each commit.
- No `Co-authored` trailers. After committing, `git log -1 --format=%B` must not contain one; amend it away if it appears.
- Never merge or push `feature/cs-12077-cs-ide-json-rpc`, and never commit in `C:\Git\codescene-jetbrains`.

## Resolved decisions

- **Progress indicator.** Phase 1 saw a background indicator for about 30 files every 6–9 seconds. The poll interval is `PeriodicChangeListerService.INTERVAL_MS` (9000). Each polled file called `PathBasedReviewHandler`, which wraps review, delta and baseline review in `withBackgroundProgress`. Phase 2 removes those per-file indicators. The poller stays until Phase 3, but it does not send a file whose current git-blob SHA was already accepted, so an unchanged poll does not emit `reviewProgress`. One indicator shows `CodeScene analysis (N remaining)` while `count > 0` and disappears when `count` is 0. Phase 3 replaces the poller and does not redesign the indicator.
- **Ids.** Buffers carry a UUID. Disk `reviewFiles` entries omit `id` and `content`, and the server then omits `id` on `fileReview` and `deltaReview` (`reviewFailed` sends `id: null`). Phase 2 accepts those id-less results after the SHA check. Phase 3 watch results are id-less too and reuse this matcher.
- **Open documents.** A loaded `Document` is always sent as a buffer, even when it is unmodified. Disk bytes are sent only when no `Document` exists. That keeps the scored text equal to the text the annotator displays, including when the file charset or newlines differ from UTF-8.
- **Baseline.** The server computes the merge-base. The plugin does not send `baseline-revision` and does not call `reviewBaseline` or `getBranchCreationCommitCode` on the review path. Repos that previously fell back to the reflog can show a different delta. Phase 4 deletes the client baseline code; this phase only stops calling it.
- **Request methods stay.** `CsIdeClient.review`, `delta` and `reviewDelta` remain for benchmarks and the Phase 1 live contract. ACE `fns-to-refactor` and `refactor` stay on requests.

## File structure

`core/src/main/kotlin/com/codescene/jetbrains/core/cli/`

| File | Responsibility |
| --- | --- |
| `GitBlobSha.kt` | `sha1("blob " + byteLength + "\0" + bytes)`, UTF-8 for strings |
| `WorkspacePaths.kt` | Slash-normalized relative paths; repository-root equality |
| `WorkspaceReviewNotices.kt` | Wire file list, parsed notifications, progress title |
| `CsIdeNotificationHub.kt` | Handler and session-listener lists |
| `WorkspaceReviewClient.kt` | `ReviewFilesSender` that writes `cs-ide/reviewFiles` |
| `WorkspaceReviewPipeline.kt` | Submit, dedup, in-flight paths, cancel, session loss |
| `WorkspaceReviewMatching.kt` | Pure accept/drop decisions |
| `WorkspaceReviewSubmitter.kt` | Document-versus-disk batching, grouped by repo root |

`core/src/main/kotlin/com/codescene/jetbrains/core/delta/`

| File | Responsibility |
| --- | --- |
| `PresentedDelta.kt` | Head/current strings stored for a server delta so `getAll()` still means "score changed or content changed" |

Modified in `core`: `CsIdeServer.kt` (`notify`, hub dispatch), `DeltaCacheService.kt` and `IDeltaCacheService.kt` (`getByCurrentContent`), `DeltaCacheUtils.kt` (`getCachedDelta` stops reading the client baseline).

`src/main/kotlin/com/codescene/jetbrains/platform/`

| File | Responsibility |
| --- | --- |
| `cli/CsIdeServerService.kt` | Exposes `notify`, `addNotificationHandler`, `addSessionListener` |
| `review/WorkspaceReviewService.kt` | Project service: pipeline, submitter, presenter, progress indicator |
| `review/WorkspaceReviewPresenter.kt` | Writes caches, refreshes UI, schedules ACE |
| `progress/ReviewProgressIndicator.kt` | One `withBackgroundProgress` task per project |

Cut-over edits `CachedReviewService.kt`, `CodeSceneService.kt`, `PeriodicChangeListerService.kt`, `DeltaCacheUtils.kt`, and the two Code Vision providers. `UpdateMonitor` already reads `activeReviewCalls`, so it stays unchanged once that set includes pipeline paths. Deletes `PathBasedReviewHandler.kt`, `CodeReviewService.kt` and `CodeDeltaService.kt` once nothing references them.

Do not delete `BaselineReviewCacheService`, `FileEventHandler`, or the baseline methods on `Git4IdeaGitService`. Phase 4 owns that. Do not send `watchFiles`.

---

### Task 1: Git blob SHA, paths and notices

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/GitBlobSha.kt`
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspacePaths.kt`
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewNotices.kt`
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/GitBlobShaTest.kt`
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspacePathsTest.kt`
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewNoticesTest.kt`

**Interfaces:**
- Produces:
  - `object GitBlobSha { fun ofBytes(bytes: ByteArray): String; fun ofString(content: String): String }`
  - `object WorkspacePaths { fun sameRoot(left: String, right: String, windows: Boolean): Boolean; fun normalizeRel(path: String): String }`
  - `data class ReviewFileWire(val relPath: String, val id: String? = null, val content: String? = null)`
  - `fun interface ReviewFilesSender { fun reviewFiles(repoRoot: String, files: List<ReviewFileWire>) }`
  - `data class FileReviewNotice(val id: String?, val relPath: String, val repoRoot: String, val review: Review, val gitBlobSha: String?)`
  - `data class DeltaReviewNotice(val id: String?, val relPath: String, val repoRoot: String, val delta: Delta?, val oldGitBlobSha: String?, val newGitBlobSha: String?)`
  - `data class ReviewFailedNotice(val id: String?, val relPath: String, val repoRoot: String, val message: String)`
  - `data class ReviewProgressNotice(val count: Int, val done: Int, val files: List<String>)`
  - `object WorkspaceReviewNotices { fun fileReview(params: JsonNode): FileReviewNotice?; fun deltaReview(params: JsonNode): DeltaReviewNotice?; fun reviewFailed(params: JsonNode): ReviewFailedNotice?; fun reviewProgress(params: JsonNode): ReviewProgressNotice? }`
  - `fun progressTitle(progress: ReviewProgressNotice): String?`

`ofString` hashes UTF-8 bytes. `progressTitle` returns null when `count == 0`, otherwise `CodeScene analysis ($count remaining)`. Parsers accept camelCase and kebab-case. A missing `id` and a JSON null `id` both become null. Return null when `path` or `repoRoot` is missing, and when `reviewProgress` is not an object. `delta` is null when `result` is null or missing. Map `result` through `RpcJson.read` onto `Review` or `Delta` after reading the SHA fields. `files` that are not an array become an empty list. Non-numeric `count` or `done` becomes 0.

- [ ] **Step 1: Write the failing tests**

`GitBlobSha.ofString("a")` is `2e65efe2a145dda7ee51d1741299f848e5bf752e`. `ofString("")` is `e69de29bb2d1d6434b8b29ae775ad8c2e48c5391`. `ofString("ü")` is `99fe40df261f7d4afd1391fe2739b2c7466fe968` and equals `ofBytes` of the two UTF-8 bytes `0xC3, 0xBC` (the length in the header is 2, not 1). `ofString("a\n")` is `78981922613b2afb6025042ff6bd878ac1994e85`.

`WorkspacePaths.normalizeRel("src\\App.kt")` is `src/App.kt`. `normalizeRel("./src/App.kt")` is `src/App.kt`. `sameRoot("C:\\repo", "c:/repo/", windows = true)` is true. `sameRoot("/repo", "/Repo", windows = false)` is false. `sameRoot("/repo/", "/repo", windows = false)` is true.

Notices: a camelCase `fileReview` with `id`, `path`, `repoRoot`, `result.gitBlobSha` and a minimal review (`score`, `rawScore`, empty smell arrays) parses, and `review.rawScore` is set. The same payload in kebab-case parses. Omitting `id` yields null. A `deltaReview` with `result: null` has a null delta and null SHAs. `reviewFailed` with `"id": null` has a null id and keeps `message`. `reviewProgress` `{count: 2, done: 1, files: ["a.kt"]}` parses. `progressTitle` of count 0 is null. `progressTitle` of count 2 is `CodeScene analysis (2 remaining)`. A notice without `path` returns null.

- [ ] **Step 2: Run the tests and confirm they fail to compile**

Run: `./gradlew :core:test --tests "com.codescene.jetbrains.core.cli.GitBlobShaTest" --tests "com.codescene.jetbrains.core.cli.WorkspacePathsTest" --tests "com.codescene.jetbrains.core.cli.WorkspaceReviewNoticesTest"`

Expected: compilation failure.

- [ ] **Step 3: Implement**

```kotlin
fun ofBytes(bytes: ByteArray): String {
    val header = "blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8)
    val digest = MessageDigest.getInstance("SHA-1")
    digest.update(header)
    digest.update(bytes)
    return digest.digest().joinToString("") { "%02x".format(it) }
}
```

`ofString` is `ofBytes(content.toByteArray(Charsets.UTF_8))`. Read SHA fields with `RpcJson.text` before `RpcJson.read`.

- [ ] **Step 4: Re-run the tests**

Expected: PASS. Each new file is under 325 lines.

- [ ] **Step 5: Commit**

`feat(cli): parse cs-ide review notifications`

This task does not edit `CsIdeServer.kt` or any file under `src/`.

---

### Task 2: Server notifications

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/CsIdeNotificationHub.kt`
- Modify: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/CsIdeServer.kt`
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/CsIdeServerNotifyTest.kt`

Do not add tests to `CsIdeServerTest.kt`. That file is already 292 lines, and the 325-line cap counts every line.

**Interfaces:**
- Consumes: `JsonRpcConnection.notify`, the existing `CsIdeServer` startup path.
- Produces, on `CsIdeServer`:
  - `fun notify(method: String, params: JsonNode)`
  - `fun addNotificationHandler(handler: (String, JsonNode) -> Unit): AutoCloseable`
  - `fun addSessionListener(listener: () -> Unit): AutoCloseable`

`notify` uses the same lazy start as `request`. It throws `CsIdeUnavailableException` when the server is closed or the backoff cooldown is active, and it does not spawn during the cooldown. A successful write resets the backoff. There is no response future.

`addNotificationHandler` returns a registration whose `close()` removes that handler. Dispatch copies the handler list and invokes it outside the server lock. A throwing handler is logged and does not prevent the others from running. Dispatch includes `cs-ide/start` after startup metadata is recorded. `addSessionListener` fires when the transport closes, the process exits, or `close()` is called. Listeners run outside the server lock.

`CsIdeServer.kt` stays at or below 325 lines. The handler list lives in `CsIdeNotificationHub`.

- [ ] **Step 1: Write the failing tests** in `CsIdeServerNotifyTest.kt`, using `FakeCsIdeProcessFactory` and `FakeCsIdePeer` the same way `CsIdeServerTest` does. Duplicate the small server-builder locally; do not enlarge `CsIdeServerTest`.

Cases: after `cs-ide/start`, `notify("cs-ide/reviewFiles", params)` writes a frame whose `method` is `cs-ide/reviewFiles`, whose params equal the object passed in, and which has no `id`. A registered handler receives a following `cs-ide/fileReview` from the peer. Two handlers both receive it. Closing a registration stops that handler only. A handler that throws still lets the next `request` complete. `notify` during cooldown throws `CsIdeUnavailableException` and `factory.starts` stays 0. Killing the process runs the session listener.

- [ ] **Step 2: Run** `./gradlew :core:test --tests "com.codescene.jetbrains.core.cli.CsIdeServerNotifyTest"`. Expected: compilation failure.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Re-run** that test class and `./gradlew :core:test --tests "com.codescene.jetbrains.core.cli.CsIdeServerTest"`. Expected: PASS.
- [ ] **Step 5: Commit** `feat(cli): deliver cs-ide server notifications`

This task does not add `reviewFiles` JSON and does not edit Task 1 files.

---

### Task 3: Correlation pipeline

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewMatching.kt`
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewPipeline.kt`
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewPipelineTest.kt`

**Interfaces:**
- Consumes: `ReviewFilesSender`, `ReviewFileWire`, `WorkspaceReviewNotices`, `GitBlobSha`, `WorkspacePaths` from Task 1.
- Produces:
  - `data class BufferSubmission(val absolutePath: String, val relPath: String, val content: String)`
  - `data class DiskSubmission(val absolutePath: String, val relPath: String, val gitBlobSha: String)`
  - `data class PresentedReview(val absolutePath: String, val relPath: String, val repoRoot: String, val content: String?, val review: Review, val gitBlobSha: String?, val elapsedMs: Long)`
  - `data class PresentedDelta(val absolutePath: String, val relPath: String, val repoRoot: String, val scoredContent: String?, val delta: Delta?, val oldGitBlobSha: String?, val newGitBlobSha: String?, val elapsedMs: Long)`
  - `interface WorkspaceReviewSink { fun presentReview(event: PresentedReview); fun presentDelta(event: PresentedDelta); fun reviewFailed(event: ReviewFailedNotice); fun progress(event: ReviewProgressNotice); fun sessionLost() }`
  - `class WorkspaceReviewPipeline(sender: ReviewFilesSender, sink: WorkspaceReviewSink, newId: () -> String = { java.util.UUID.randomUUID().toString() }, windows: Boolean = false, clock: () -> Long = System::currentTimeMillis)` with `fun submit(repoRoot: String, buffers: List<BufferSubmission>, disks: List<DiskSubmission>)`, `fun cancel(absolutePath: String)`, `fun onNotification(method: String, params: JsonNode)`, `fun sessionLost()`, `fun activePaths(): Set<String>`

`content` on a presented disk review is null. `scoredContent` on a presented disk delta is null. Buffer presentations carry the document text. `elapsedMs` is `clock() - submittedAt`, or 0 when the pipeline has no start time.

Rules, all covered by tests with a recording sender and sink:

- A buffer submit sends one `ReviewFileWire` with a new id, the normalized relative path, and the content. The pipeline hashes the content with `GitBlobSha.ofString`.
- One `submit` with a buffer and a disk file calls the sender once, with both wire entries.
- A file that is already in flight or already accepted for the same SHA is left out of that call. When every file is left out, the sender is not called.
- A second submit of the same repo, path and content does not call the sender.
- A disk submit sends a wire entry with null id and null content. A second submit with the same `gitBlobSha` does not call the sender.
- `fileReview` with that id and the same SHA calls `presentReview` once.
- `fileReview` with a different SHA does not present, and a later submit of new content sends again.
- After a second submit, a `fileReview` for the first id does not present and does not clear the second id.
- `deltaReview` with the matching id and `newGitBlobSha` presents the delta. A null `result` still presents, with a null delta.
- `reviewFailed` for the current id does not present and removes the path from `activePaths()`. `reviewFailed` for an older id does not remove the newer path.
- An id-less `fileReview` whose SHA equals the disk submission SHA presents with null content. A different SHA does not present. An id-less notice with a null SHA does not present.
- A disk path stays in `activePaths()` until the id-less result or a `reviewProgress` with `count == 0`. After count 0 with no result, submitting the same SHA sends again.
- A buffer path stays in `activePaths()` until its delta or its `reviewFailed`, not merely until `fileReview`.
- `cancel` drops a later `fileReview` and `deltaReview` for that id. `sessionLost` clears in-flight state, calls `sink.sessionLost()`, and the same buffer content is sent again on the next submit.
- A notice whose `repoRoot` fails `WorkspacePaths.sameRoot` is dropped. With `windows = true`, `C:\repo` matches `c:/repo`.

`onNotification` ignores methods other than the four review methods, so a later `watchInventoryChanged` can be handled by another listener without this class rejecting it. Both new files stay at or below 325 lines. Put the accept/drop decisions in `WorkspaceReviewMatching.kt` and the maps in `WorkspaceReviewPipeline.kt`.

- [ ] **Step 1: Write the failing tests** listed above.
- [ ] **Step 2: Run** `./gradlew :core:test --tests "com.codescene.jetbrains.core.cli.WorkspaceReviewPipelineTest"`. Expected: compilation failure.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Re-run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(cli): correlate cs-ide review results`

Depends on Task 1 only. Does not edit `CsIdeServer.kt` or `WorkspaceReviewClient.kt`.

---

### Task 4: reviewFiles client and live check

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewClient.kt`
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewClientTest.kt`
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewLiveTest.kt`

**Interfaces:**
- Consumes: `ReviewFilesSender`, `ReviewFileWire` from Task 1, and `CsIdeServer.notify` from Task 2.
- Produces: `class WorkspaceReviewClient(private val notify: (String, JsonNode) -> Unit) : ReviewFilesSender`

`reviewFiles` notifies method `cs-ide/reviewFiles`. Params are an object node with `repo-root` and `files`. Each file has `rel-path`. Add `id` only when it is non-null. Add `content` only when it is non-null. Do not add `baseline-revision`. An empty file list does not notify.

- [ ] **Step 1: Write the unit test** against a recording `notify` function. One file with id and content produces those kebab-case keys. One file with null id and null content produces only `rel-path`. An empty list does not call `notify`.
- [ ] **Step 2: Run** `./gradlew :core:test --tests "com.codescene.jetbrains.core.cli.WorkspaceReviewClientTest"`. Expected: compilation failure.
- [ ] **Step 3: Implement the client.** Re-run the unit test. Expected: PASS.
- [ ] **Step 4: Write the live test.** Start one server the way `CsIdeLiveContractTest.startServer` does (`CsIdeLaunch.spec`, `SystemCsIdeProcessFactory`, `csIde.java` or the test JVM, warm it up with `deviceId()`). Build `WorkspaceReviewClient(server::notify)`. Use `CsIdeLiveContractFixtures.prepareGitRepoWithoutRules` with `simpleKotlinCode` at `ReviewSubject.kt`. Register a notification handler before sending. Send one buffer wire for `ReviewSubject.kt` with a fixed id and content `simpleKotlinCode + "\n"`. Wait up to 60 seconds for a `fileReview` whose id matches and whose `gitBlobSha` equals `GitBlobSha.ofString` of that content, then a `deltaReview` with the same id, then a `reviewProgress` with `count == 0`. Close the server and assert the child pid is dead, using the same `ProcessHandle` wait as `CsIdeLiveContractTest`.
- [ ] **Step 5: Run** `./gradlew :core:test --tests "com.codescene.jetbrains.core.cli.WorkspaceReviewLiveTest"`. Expected: PASS.
- [ ] **Step 6: Commit** `feat(cli): send cs-ide reviewFiles`

Depends on Tasks 1 and 2. Does not edit the pipeline.

---

### Task 5: Delta cache lookup without a client baseline

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/delta/PresentedDelta.kt`
- Modify: `core/src/main/kotlin/com/codescene/jetbrains/core/delta/DeltaCacheService.kt`
- Modify: `core/src/main/kotlin/com/codescene/jetbrains/core/contracts/IDeltaCacheService.kt`
- Modify: `core/src/test/kotlin/com/codescene/jetbrains/core/testdoubles/InMemoryDeltaCacheService.kt`
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/delta/PresentedDeltaTest.kt`
- Test: extend `core/src/test/kotlin/com/codescene/jetbrains/core/delta/DeltaCacheServiceTest.kt` only if it stays at or below 325 lines; otherwise put the new cases in `PresentedDeltaTest.kt`

**Interfaces:**
- Produces:
  - `fun presentedDeltaContents(scoredText: String?, oldGitBlobSha: String?, newGitBlobSha: String?): Pair<String, String>` returning `(headContent, currentContent)`
  - `fun getByCurrentContent(filePath: String, currentContent: String): Pair<Boolean, Delta?>` on `DeltaCacheService` and `IDeltaCacheService`

`currentContent` is `scoredText` when it is non-null, otherwise `"blob:${newGitBlobSha.orEmpty()}"`. `headContent` equals `currentContent` when `oldGitBlobSha == newGitBlobSha` (both null counts as equal). Otherwise `headContent` is `"baseline:${oldGitBlobSha.orEmpty()}"`.

`getByCurrentContent` hits when the stored current-content SHA-256 equals `sha256(currentContent)`, and ignores the head hash. A hit returns the stored delta, which may be null. A miss returns `false` to null.

Do not change `getCachedDelta` in this task. It still takes `IGitService` until Task 7. Do not change `get(DeltaCacheQuery)`.

- [ ] **Step 1: Write the failing tests.**

`presentedDeltaContents("text", "aaa", "aaa")` has equal head and current, both `"text"`. `presentedDeltaContents("text", "aaa", "bbb")` has head `"baseline:aaa"` and current `"text"`. `presentedDeltaContents(null, null, "abc")` has current `"blob:abc"` and head `"baseline:"` (a file that is new since the merge-base counts as changed). `presentedDeltaContents(null, "old", "abc")` has head `"baseline:old"`.

Put a `DeltaCacheEntry` built from those strings with a `Delta` whose `scoreChange` is 0 and empty finding lists. `getByCurrentContent(path, "text")` hits when the SHAs differed. `getByCurrentContent(path, "other")` misses. `getAll()` contains the path when the SHAs differed and the score change is 0, and does not contain it when the SHAs were equal and the score change is 0. A null delta is a hit for `getByCurrentContent` and is absent from `getAll()`.

- [ ] **Step 2: Run** `./gradlew :core:test --tests "com.codescene.jetbrains.core.delta.PresentedDeltaTest" --tests "com.codescene.jetbrains.core.delta.DeltaCacheServiceTest"`. Expected: compilation failure (`getByCurrentContent` is missing, and `InMemoryDeltaCacheService` no longer implements the interface).
- [ ] **Step 3: Implement.** Delegate the new method from `InMemoryDeltaCacheService` to its `DeltaCacheService`.
- [ ] **Step 4: Re-run** those two test classes and `./gradlew :core:test --tests "com.codescene.jetbrains.core.delta.DeltaCacheUtilsTest"`. Expected: PASS. `getCachedDelta` behaviour is unchanged.
- [ ] **Step 5: Commit** `feat(core): look up presented deltas by current content`

Parallel with Tasks 1–4. Does not edit `cli/` or `src/`.

---

### Task 6: Platform presenter, submitter and progress indicator

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewSubmitter.kt`
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/WorkspaceReviewSubmitterTest.kt`
- Create: `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceReviewPresenter.kt`
- Create: `src/main/kotlin/com/codescene/jetbrains/platform/review/WorkspaceReviewService.kt`
- Create: `src/main/kotlin/com/codescene/jetbrains/platform/progress/ReviewProgressIndicator.kt`
- Modify: `src/main/kotlin/com/codescene/jetbrains/platform/cli/CsIdeServerService.kt`

Nothing calls `WorkspaceReviewService` yet. Do not edit `CachedReviewService` or `PathBasedReviewHandler`.

**Interfaces:**
- Consumes: `WorkspaceReviewPipeline`, `WorkspaceReviewClient`, `presentedDeltaContents`, `progressTitle`, `CsIdeServer.notify` and the handler registration from Task 2.
- Produces:
  - `data class RepoLocation(val repoRoot: String, val relPath: String)`
  - `class WorkspaceReviewSubmitter(private val pipeline: WorkspaceReviewPipeline, private val locate: (String) -> RepoLocation?, private val openText: (String) -> String?, private val readBytes: (String) -> ByteArray?, private val isSupported: (String) -> Boolean)` with `fun submitEditor(absolutePath: String, content: String)` and `fun submitPaths(paths: List<String>)`
  - `class WorkspaceReviewService` (project `@Service`, `Disposable`) exposing `fun submitEditor(absolutePath: String, content: String)`, `fun submitPaths(paths: List<String>)`, `fun cancel(absolutePath: String)`, `fun activePaths(): Set<String>`
  - `CsIdeServerService.notify`, `addNotificationHandler`, `addSessionListener` delegating to the server

`submitEditor` locates the repo and submits one buffer. It does nothing when `locate` returns null or `isSupported` is false.

`submitPaths` groups by `repoRoot`. A path whose `openText` returns non-null is a buffer with that text. A path whose `openText` returns null and whose `readBytes` returns bytes is a disk submission hashed with `GitBlobSha.ofBytes`. A path with no bytes is skipped. Unsupported paths are skipped. One `pipeline.submit` call per repo root that has at least one file.

`WorkspaceReviewPresenter` implements `WorkspaceReviewSink` for one project:

- `presentReview` with non-null content calls `completeReviewAnalysis` on the project's `reviewCacheService` and `telemetryService`, with that content, the review, and `elapsedMs`, then `UIRefreshService.refreshUI(absolutePath, CodeSceneCodeVisionProvider.getProviders())`. Null content does not write the review cache.
- `presentDelta` calls `adaptDeltaResult`, then `completeDeltaAnalysis` on the project's `deltaCacheService` (the platform service, so `put` still updates the monitor) with the head and current strings from `presentedDeltaContents(event.scoredContent, event.oldGitBlobSha, event.newGitBlobSha)`. The current string is that current string, and the old string is the head string. Then refresh UI when content was non-null.
- When the presented review is non-null and content is non-null, launch `refreshAceFromReview` on the project IO scope. When the adapted delta is non-null and content is non-null, launch `refreshAceFromDelta`. Do not call ACE for disk results that have no document text.
- `progress` forwards to `ReviewProgressIndicator`. `sessionLost` tells the indicator that the title is null.
- `reviewFailed` logs the message and does not write caches.

`ReviewProgressIndicator` holds one coroutine. On the first non-null `progressTitle`, start `withBackgroundProgress(project, title, cancellable = false)`. Inside the block, set `ProgressManager.getInstance().progressIndicator?.text` to the latest title whenever a new notice arrives. If that indicator is null, end the block and start a new one so the title shown to the user is the latest `progressTitle`. Return from the block when the title becomes null, which removes the indicator. A later non-zero count starts a new block. Do not start a block for count 0.

`WorkspaceReviewService` builds the client with `CsIdeServerService.getInstance()::notify`, the pipeline, the submitter, and the presenter. `openText` reads `FileDocumentManager.getInstance().getCachedDocument` (not `getDocument`, which loads one) inside a read action and returns null when there is no document. `readBytes` reads the virtual file bytes inside a read action. `locate` uses `Git4IdeaGitService.getRepoRoot` and `getRepoRelativePath`, then `WorkspacePaths.normalizeRel`. `isSupported` is `isPathSupportedForReview`. Register the pipeline as a notification handler and register `pipeline.sessionLost()` as a session listener. Dispose both registrations. Both callbacks from `CsIdeServer` run on the JSON-RPC reader thread. The service must hop to `Dispatchers.IO` before `pipeline.onNotification` and before `pipeline.sessionLost`. The pipeline and the presenter then run on that IO dispatcher.

- [ ] **Step 1: Write `WorkspaceReviewSubmitterTest`.** Two paths in one repo and one open document produce one `submit` with one buffer and one disk entry, and the disk SHA is `GitBlobSha.ofBytes` of the supplied bytes. Two repos produce two `submit` calls. An unsupported path and a path with no location are omitted. `submitEditor` sends one buffer and no disk entry. A missing byte array is omitted.
- [ ] **Step 2: Run** `./gradlew :core:test --tests "com.codescene.jetbrains.core.cli.WorkspaceReviewSubmitterTest"`. Expected: compilation failure.
- [ ] **Step 3: Implement the submitter, then the platform types.**
- [ ] **Step 4: Run** `./gradlew :core:test --tests "com.codescene.jetbrains.core.cli.WorkspaceReviewSubmitterTest"` and `./gradlew test`. Expected: PASS. No `runIde` in this task: no existing call site uses the service yet.
- [ ] **Step 5: Commit** `feat: present cs-ide workspace reviews`

Depends on Tasks 3, 4 and 5.

---

### Task 7: Cut over the review path

**Files:**
- Modify: `src/main/kotlin/com/codescene/jetbrains/platform/api/CachedReviewService.kt`
- Modify: `src/main/kotlin/com/codescene/jetbrains/platform/api/CodeSceneService.kt`
- Modify: `src/main/kotlin/com/codescene/jetbrains/platform/git/PeriodicChangeListerService.kt`
- Modify: `core/src/main/kotlin/com/codescene/jetbrains/core/delta/DeltaCacheUtils.kt`
- Modify: `core/src/test/kotlin/com/codescene/jetbrains/core/delta/DeltaCacheUtilsTest.kt`
- Modify: `src/main/kotlin/com/codescene/jetbrains/platform/editor/codeVision/CodeSceneCodeVisionProvider.kt`
- Modify: `src/main/kotlin/com/codescene/jetbrains/platform/editor/codeVision/providers/CodeHealthCodeVisionProvider.kt`
- Delete, after a repo search shows no remaining references: `src/main/kotlin/com/codescene/jetbrains/platform/api/PathBasedReviewHandler.kt`, `src/main/kotlin/com/codescene/jetbrains/platform/api/CodeReviewService.kt`, `src/main/kotlin/com/codescene/jetbrains/platform/api/CodeDeltaService.kt`

**Interfaces:**
- Consumes: `WorkspaceReviewService` from Task 6, `getByCurrentContent` from Task 5.
- Produces: `fun getCachedDelta(filePath: String, fileContent: String, deltaCacheService: IDeltaCacheService): Pair<Boolean, Delta?>`

`CodeSceneService.reviewFile` passes `showProgress = false` into `ReviewOrchestrator.reviewFile`. The debounce and the in-flight job stay, so typing still waits 325 ms and does not open a per-file indicator.

`CachedReviewService.review` and `reviewFromCodeVision` keep the debounce and then call `WorkspaceReviewService.submitEditor` with the document text. `reviewByPath` calls `submitPaths(listOf(filePath))` and does not debounce. `activeReviewCalls` is the orchestrator set plus `WorkspaceReviewService.activePaths()`. `cancelFileReview` cancels both the orchestrator job and `WorkspaceReviewService.cancel`. Delete `performCachedReview`, `handleDelta` and `getBaselineScore` from this class.

`PeriodicChangeListerService.pollChangedFiles` collects the changed paths and calls `submitPaths` once. It does not call `reviewByPath` in a loop.

`getCachedDelta` calls `deltaCacheService.getByCurrentContent(filePath, fileContent)` and does not take `IGitService`. Update both Code Vision call sites and `DeltaCacheUtilsTest`. The test that stored head `"old"` and current `"current"` still hits when queried with `"current"`. Drop `StubGitService` from that test.

Leave `BaselineReviewCacheService`, the git baseline methods, `resolveDeltaExecutionPlan` and `FileEventHandler` in place.

- [ ] **Step 1: Update `DeltaCacheUtilsTest` to the new signature and run it.** Expected: compilation failure.
- [ ] **Step 2: Change `getCachedDelta` and the two Code Vision call sites.** Re-run `./gradlew :core:test --tests "com.codescene.jetbrains.core.delta.DeltaCacheUtilsTest"`. Expected: PASS.
- [ ] **Step 3: Rewire `CachedReviewService`, `CodeSceneService` and `PeriodicChangeListerService`.** Delete the three unused classes only after `rg` shows no remaining references, including tests.
- [ ] **Step 4: Run** `./gradlew test`, `.\gradlew.bat :core:ktlintCheck :ktlintCheck -PktlintFailOnError=true`, and `bb -f .github/check-class-size-mine.clj`. Expected: PASS. Every changed `.kt` file is at or below 325 lines. `CachedReviewService.kt` should shrink; if it would exceed 325, move the editor-replay helper into `WorkspaceReviewService` rather than adding another review implementation.
- [ ] **Step 5: Smoke-test with `.\gradlew.bat runIde`** (do not use `make run-ide` on Windows). Record each line in the hand-off:

  - Open a supported file: annotations and Code Vision appear, and exactly one `java` child is running.
  - Edit the file: the decorations follow the buffer, and the monitor updates from the delta.
  - A changed file that is not open appears in the monitor after the poll or a git refresh.
  - The background indicator is a single `CodeScene analysis (N remaining)` entry while a batch runs, and it is gone when the batch finishes.
  - Leave the IDE open for 20 seconds on an unchanged tree: the indicator does not come back.
  - ACE preflight and refactor still run for a supported file.
  - Kill the `java` child: the next edit starts a new one and reviews again.
  - Close the IDE: no leftover `java` child.

- [ ] **Step 6: Commit** `feat: review files through cs-ide reviewFiles`

Depends on Task 6. This is the only task that rewires the platform call sites.

---

## Spec coverage

| Spec requirement | Task |
| --- | --- |
| Buffer `reviewFiles` with UUID, content, kebab-case, no `baseline-revision` | 1, 3, 4 |
| Disk files without id or content | 3, 4, 6 |
| `fileReview` / `deltaReview` / `reviewFailed` / `reviewProgress` | 1, 3 |
| Match by id; drop stale buffer results by git-blob SHA | 3 |
| Id-less results accepted only after the SHA check | 3 |
| UTF-8 blob SHA for buffers, raw bytes for disk | 1, 6 |
| Unsupported files are not waited on | 6 (`isPathSupportedForReview` before send) |
| Monitor fed from `deltaReview`; no client baseline review | 5, 6, 7 |
| Null delta is a cache hit and stays off the monitor | 5, 6 |
| `reviewProgress` with `count: 0` clears one indicator | 1, 6, 7 |
| Unchanged 9s poll does not raise progress | 3, 7 |
| Notifications leave the server, off the reader thread | 2, 6 |
| Session loss allows the same content to be sent again | 2, 3, 6 |
| Phase 3 can add `watchFiles` without a second matcher | 3 ignores unknown methods; id-less SHA check already exists |
| `CsIdeClient` request methods and ACE requests stay | no task deletes them |
