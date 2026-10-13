# Phase 1: cs-ide transport and request methods Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every in-process `ExtensionAPI`/`DevToolsAPI` call in the plugin goes through one supervised `cs-ide server` JSON-RPC child process launched from the `codescene.extension:api` jar on the IDE's JBR.

**Architecture:** A Kotlin client in `core/cli` (framing, connection, launch, supervisor, typed client), owned by an application-level platform service that supplies the IDE-specific launch environment (shell env, git on PATH, proxy env, trust store). Existing services keep their signatures and caches; only the call into the jar changes.

**Tech Stack:** Kotlin 1.9 / JVM 17 toolchain, Jackson from the extension API fat jar, JUnit 4 + MockK, IntelliJ Platform 2024.3+.

Spec: `docs/superpowers/specs/2026-10-12-cs-ide-json-rpc-migration-design.md`.

## Global Constraints

- Business logic in `core`; platform code is a thin adapter.
- No new code comments; keep existing ones.
- Every changed `.kt` file stays at or below 325 lines (`make class-size-mine`).
- Outbound JSON keys are kebab-case; inbound payloads are normalized from camelCase.
- No new runtime dependencies.
- No request on the EDT.
- Every task's commit passes `make format-check class-size-mine`; lint findings are fixed first.
- `make iter` (10-minute timeout) passes before the PR.
- CodeScene MCP `pre_commit_code_health_safeguard` before each commit, `analyze_change_set` before the PR.
- No `Co-authored` trailers.
- Gradle runs on JDK 17 (`JAVA_HOME` must point to a 17 JDK; Gradle 8.14 cannot run on 25).

---

## File Structure

`core/src/main/kotlin/com/codescene/jetbrains/core/cli/`

| File | Responsibility |
| --- | --- |
| `LspFraming.kt` | Content-Length framing (bytes) |
| `RpcJson.kt` | Shared mapper, camelCase to kebab-case normalization, typed reads |
| `JsonRpcConnection.kt` | Request/response correlation, notifications, cancellation, ordered writes, transport failure |
| `CsIdeLaunch.kt` | Java executable, jar path, thread count, command line, environment (PATH, proxy env) |
| `CsIdeTrustStore.kt` | Writes a PKCS12 trust store from certificates and returns its JVM args |
| `CsIdeProcess.kt` | Process abstraction (`CsIdeProcess`, `CsIdeProcessFactory`) and the `java.lang.Process` implementation with tree kill |
| `CsIdeServer.kt` | Supervisor: lazy start, `cs-ide/start` wait, stderr tail, backoff, respawn, close |
| `CsIdeBackoff.kt` | Restart backoff state machine |
| `CsIdeClient.kt` | `CsIdeTransport` and typed requests mirroring the old `ExtensionAPI` surface |
| `CsIdeErrors.kt` | Maps JSON-RPC errors to exceptions existing helpers understand |

`src/main/kotlin/com/codescene/jetbrains/platform/cli/`

| File | Responsibility |
| --- | --- |
| `CsIdeServerService.kt` | App-level `@Service`, `Disposable`; owns `CsIdeServer` and `CsIdeClient(CsIdeTransport(server::request))` |
| `CsIdeLaunchEnvironmentProvider.kt` | Builds `CsIdeLaunchEnvironment` from `EnvironmentUtil`, `GitExecutableManager`, `ProxySettings`, `CertificateManager` |

Modified: `BaseService.kt`, `DeviceIdStore.kt`, `CodeReviewService.kt`, `CodeDeltaService.kt`, `AceService.kt`, `TelemetryService.kt`, `CodeSceneApplicationServiceProvider.kt`, `AboutTab.kt`, benchmarks.
Deleted: `ClassLoaderScope.kt`, `ClassLoaderScopeTest.kt`, `ExtensionApi*Test.kt` (3 files).
New script: `.github/cs-ide-macos-spike.clj`.

---

### Task 1: Framing and JSON

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/LspFraming.kt`, `RpcJson.kt`
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/LspFramingTest.kt`, `RpcJsonTest.kt`

**Interfaces:**
- Produces:
  - `object LspFraming { fun write(output: OutputStream, body: ByteArray); fun read(input: InputStream): ByteArray? }`
  - `object RpcJson { val mapper: ObjectMapper; fun objectNode(): ObjectNode; fun tree(value: Any?): JsonNode; fun kebabKey(key: String): String; fun toKebabNode(node: JsonNode): JsonNode; fun <T> read(node: JsonNode, type: Class<T>): T; fun <T> read(node: JsonNode, type: TypeReference<T>): T }`

- [ ] Write tests: framing round trip with a multi-byte body (`Content-Length` counts bytes), `read` returns null on EOF, header case-insensitive; `kebabKey("fileLevelCodeSmells") == "file-level-code-smells"`, `kebabKey("nippyB64") == "nippy-b64"`, `toKebabNode` recurses objects in arrays, `read(node, Review::class.java)` maps a camelCase server review (`rawScore`, `highlightRange`, `score`).
- [ ] Run `./gradlew :core:test --tests "*.cli.LspFramingTest" --tests "*.cli.RpcJsonTest"`; expect compilation failure.
- [ ] Port `LspFraming` from `c8f6b1c`; implement `RpcJson` with `mapper = ExtensionAPI.createObjectMapper()`.
- [ ] Re-run; expect PASS. Commit `feat(cli): add LSP framing and JSON normalization`.

### Task 2: JSON-RPC connection

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/JsonRpcConnection.kt`
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/JsonRpcConnectionTest.kt`

**Interfaces:**
- Produces:
  - `class JsonRpcException(val rpcCode: Int?, message: String, cause: Throwable? = null) : RuntimeException`
  - `class JsonRpcConnection(input, output, onNotification: (String, JsonNode) -> Unit, onClosed: (Throwable) -> Unit) : AutoCloseable` with `fun request(method: String, params: JsonNode): CompletableFuture<JsonNode>`, `fun notify(method: String, params: JsonNode)`, `fun failPending(error: Throwable)`, `override fun close()`.

- [ ] Tests over `PipedInputStream`/`PipedOutputStream` with a scripted peer: responses correlate by id under concurrency (two requests answered in reverse order); a notification reaches `onNotification`; an `error` reply fails the future with `JsonRpcException(-32603, message)`; cancelling the future sends `$/cancelRequest` with the id and a late reply is ignored; writes keep submission order; peer EOF fails pending futures and calls `onClosed`; a server-to-client request gets a `-32601` error reply.
- [ ] Run; expect failure. Implement (reader daemon thread, `ConcurrentHashMap<Long, CompletableFuture>`, single write lock). Run; expect PASS. Commit `feat(cli): add JSON-RPC connection`.

### Task 3: Launch command, environment and trust store

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/CsIdeLaunch.kt`, `CsIdeTrustStore.kt`
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/CsIdeLaunchTest.kt`, `CsIdeTrustStoreTest.kt`

**Interfaces:**
- Produces:
  - `data class CsIdeProxy(val host: String, val port: Int, val exceptions: String?)`
  - `data class CsIdeLaunchEnvironment(val shellEnvironment: Map<String, String> = emptyMap(), val pathPrefix: List<String> = emptyList(), val proxy: CsIdeProxy? = null, val extraJvmArgs: List<String> = emptyList())`
  - `data class CsIdeLaunchSpec(val command: List<String>, val environment: Map<String, String>)`
  - `object CsIdeLaunch { const val MAX_HEAP: String = "-Xmx2g"; fun javaExecutable(javaHome: Path, osName: String): Path; fun extensionApiJar(): Path; fun defaultThreads(cpus: Int): Int; fun command(java: Path, jar: Path, threads: Int, extraJvmArgs: List<String>): List<String>; fun environment(processEnvironment: Map<String, String>, launch: CsIdeLaunchEnvironment, windows: Boolean): Map<String, String>; fun spec(launch: CsIdeLaunchEnvironment, javaHome: Path, jar: Path, threads: Int, osName: String, processEnvironment: Map<String, String>): CsIdeLaunchSpec }`
  - `object CsIdeTrustStore { const val PASSWORD: String = "changeit"; fun write(certificates: Collection<X509Certificate>, target: Path): Path; fun jvmArgs(path: Path): List<String>; fun jdkDefaultIssuers(): List<X509Certificate> }`

- [ ] Tests: `java.exe` on Windows, `java` elsewhere; missing executable throws `IllegalStateException` naming the path; `defaultThreads(8) == 4`, `defaultThreads(1) == 1`; command order is `java --enable-native-access=ALL-UNNAMED -Xmx2g <extra> -cp <jar> clojure.main -m codescene.cli.ide server --threads N`; shell env overlays process env; `pathPrefix` is moved to the front once (existing occurrences removed), matching `Path` case-insensitively on Windows with `;`, `PATH` with `:` elsewhere, comparing entries as unquoted strings so quoted entries never throw; proxy sets `HTTPS_PROXY`/`HTTP_PROXY=http://host:port` only when no `http(s)_proxy` variable exists, and `NO_PROXY` from exceptions only when no `no_proxy` variable exists; `extensionApiJar()` points at an existing `.jar`. Trust store: written file loads as PKCS12 with `PASSWORD` and contains every certificate; `jvmArgs` lists `trustStore`, `trustStoreType=PKCS12`, `trustStorePassword`.
- [ ] Run; expect failure. Implement. Run; expect PASS. Commit `feat(cli): add cs-ide launch command and environment`.

### Task 4: Process abstraction, backoff and supervisor

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/CsIdeProcess.kt`, `CsIdeBackoff.kt`, `CsIdeServer.kt`
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/CsIdeBackoffTest.kt`, `CsIdeServerTest.kt`, `FakeCsIdeProcess.kt`

**Interfaces:**
- Consumes: `JsonRpcConnection`, `CsIdeLaunchSpec`, `ILogger`, `CsIdeUnavailableException` (already in `CsIdeErrors.kt` from Task 5; do not redeclare it).
- Produces:
  - `interface CsIdeProcess { val stdin: OutputStream; val stdout: InputStream; val stderr: InputStream; val pid: Long; fun isAlive(): Boolean; fun onExit(): CompletableFuture<Int>; fun destroyTree() }`
  - `fun interface CsIdeProcessFactory { fun start(): CsIdeProcess }` and `class SystemCsIdeProcessFactory(private val spec: () -> CsIdeLaunchSpec) : CsIdeProcessFactory`
  - `class CsIdeBackoff(initialMs: Long = 1_000, maxMs: Long = 30_000) { fun remainingMs(now: Long): Long; fun recordFailure(now: Long); fun reset() }`
  - `data class CsIdeServerMetadata(val sha: String, val version: String)`
  - `class CsIdeServer(processFactory, logger, startupTimeoutMs = 30_000, shutdownGraceMs = 3_000, backoff = CsIdeBackoff(), clock = System::currentTimeMillis) : AutoCloseable` with `fun request(method: String, params: JsonNode): CompletableFuture<JsonNode>`, `fun metadata(): CsIdeServerMetadata?`, `fun stderrTail(): String`, `override fun close()`. It does not implement `CsIdeTransport` (Task 5); consumers adapt it with `CsIdeTransport(server::request)`.

- [ ] Tests: backoff doubles to the cap and resets; first request starts the process and resolves after `cs-ide/start`; no `cs-ide/start` within the timeout fails with `CsIdeUnavailableException` containing the stderr tail and destroys the process; an exit fails pending requests and the next request respawns after the backoff; requests inside the cooldown fail fast without spawning; a successful response resets the backoff; `close()` closes stdin, waits for the grace period, then destroys the tree; requests after `close()` fail.
- [ ] Run; expect failure. Implement. Run; expect PASS. Commit `feat(cli): add cs-ide server supervisor`.

### Task 5: Typed client and error mapping

**Files:**
- Create: `core/src/main/kotlin/com/codescene/jetbrains/core/cli/CsIdeClient.kt`, `CsIdeErrors.kt`
- Test: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/CsIdeClientTest.kt`, `CsIdeErrorsTest.kt`, `IdeServerFixture.kt`

**Interfaces:**
- Consumes: `JsonRpcConnection` (tests only). Does not depend on Task 3 or Task 4, so it can be built in parallel with them.
- Produces:
  - `fun interface CsIdeTransport { fun request(method: String, params: JsonNode): CompletableFuture<JsonNode> }` (tests use `CsIdeTransport(connection::request)`; Task 7 uses `CsIdeTransport(server::request)`)
  - `class CsIdeClient(transport: CsIdeTransport, timeoutMs: Long = 300_000)` with blocking calls:
    - `fun review(params: ReviewParams, cache: CacheParams): Review?` (returns null without a request when `params.code` is null, as the in-process API did)
    - `fun delta(oldScore: String?, newScore: String?): Delta?`
    - `fun reviewDelta(old: ReviewParams, new: ReviewParams, cache: CacheParams): Delta?`
    - `fun preflight(force: Boolean): PreflightResponse?`
    - `fun fnsToRefactor(code: CodeParams, cache: CacheParams, codeSmells: List<CodeSmell>): List<FnToRefactor>`
    - `fun fnsToRefactor(code: CodeParams, cache: CacheParams, delta: Delta): List<FnToRefactor>`
    - `ReviewParams`, `CodeParams` and `CacheParams` are the `ExtensionAPI` records the call sites already build.
    - `fun refactor(fn: FnToRefactor, options: RefactoringOptions?): RefactorResponse?`
    - `fun telemetry(event: TelemetryEvent): Int`
    - `fun deviceId(): String`
  - `object CsIdeErrors { fun unwrap(error: Throwable): Exception }` and `class CsIdeUnavailableException(message: String, cause: Throwable? = null) : RuntimeException`, both in `CsIdeErrors.kt`

- [ ] Tests against `IdeServerFixture` (in-process fake over pipes, ported from `c8f6b1c`, records the last params per method): each method sends kebab-case params (`file-content`, `cache-path`, `repo-path`, `old-score`, `fn-to-refactor`, `skip-cache`, `token`, `code-smells`, `delta-result`); camelCase results map to the jar models; `review(code = null)` sends nothing and returns null; `reviewDelta` sends two reviews then `delta` with their raw scores; `fnsToRefactor` sends the last preflight response once `preflight` succeeded; the `delta` null result returns null; `CsIdeErrors` maps `"java.net.ConnectException: ..."` to `ConnectException`, keeps `"Request failed [401]"` in the message, and unwraps `ExecutionException`; every blocking client call rethrows through `CsIdeErrors.unwrap`, proven by a fixture RPC error `"java.net.ConnectException: ..."` surfacing from a client call as `ConnectException`; `CsIdeUnavailableException` passes through unchanged.
- [ ] Run; expect failure. Implement. Run; expect PASS. Commit `feat(cli): add typed cs-ide client`.

### Task 6: Live contract test

**Files:**
- Create: `core/src/test/kotlin/com/codescene/jetbrains/core/cli/CsIdeLiveContractTest.kt`
- Delete: `core/src/test/kotlin/com/codescene/jetbrains/core/review/ExtensionApiIntegrationTest.kt`, `ExtensionApiCodeHealthRulesIntegrationTest.kt`, `ExtensionApiFnToRefactorDeltaOverloadTest.kt`
- Modify: `core/build.gradle.kts` (forward `CS_IDE_JAVA` as `csIde.java`)

- [ ] Port the scenarios: Kotlin review, delta, fns-to-refactor from review smells and from delta, rules review, and rules delta through `cs-ide/delta` without `repo-path`; plus `deviceId()` non-blank and a clean `close()` with no surviving child process. One server per test class (`@BeforeClass`), Java from `csIde.java` or the test JVM, client built with `CsIdeTransport(server::request)`.
- [ ] Run `./gradlew :core:test --tests "*.cli.CsIdeLiveContractTest"`; expect PASS. Commit `test(cli): replace ExtensionAPI integration tests with live cs-ide contract`.

### Task 7: Platform wiring

**Files:**
- Create: `src/main/kotlin/com/codescene/jetbrains/platform/cli/CsIdeServerService.kt`, `CsIdeLaunchEnvironmentProvider.kt`
- Modify: `core/.../review/BaseService.kt`, `core/.../util/DeviceIdStore.kt`, `platform/api/CodeReviewService.kt`, `CodeDeltaService.kt`, `AceService.kt`, `platform/telemetry/TelemetryService.kt`, `platform/di/CodeSceneApplicationServiceProvider.kt`, `platform/settings/tab/AboutTab.kt`
- Delete: `core/.../util/ClassLoaderScope.kt`, `core/src/test/.../util/ClassLoaderScopeTest.kt`
- Test: `BaseServiceTest.kt`, `DeviceIdStoreTest.kt`

**Interfaces:**
- Consumes: `CsIdeServer`, `CsIdeClient`, `CsIdeLaunch`, `CsIdeTrustStore`.
- Produces: `CsIdeServerService.getInstance().client(): CsIdeClient`; `BaseService.runTimed(action): TimedResult<T>`; `DeviceIdStore(logger, deviceIdProvider)`.

Two commits. Steps 1–2 add the service without wiring it in; steps 3–6 cut over.

- [ ] Step 1: Add the two platform files. `CsIdeServerService` owns one `CsIdeServer` and a `CsIdeClient(CsIdeTransport(server::request))` and closes the server in `dispose()`. The provider: env from `EnvironmentUtil.getEnvironmentMap()`; `pathPrefix` from the parent of `GitExecutableManager.getInstance().pathToGit` when it is an absolute path; proxy from `ProxySettings.getInstance().getProxyConfiguration()` when it is a `StaticProxyConfiguration` with `ProxyProtocol.HTTP`; trust store from `CertificateManager.getInstance().trustManager.acceptedIssuers` plus `CsIdeTrustStore.jdkDefaultIssuers()`, written to `PathManager.getSystemPath()/codescene/cs-ide-truststore.p12`. Every step is `runCatching` and logged.
- [ ] Step 2: Run `./gradlew test` and `make format-check class-size-mine`; expect PASS. Commit `feat: add cs-ide server service and launch environment provider`.
- [ ] Step 3: Update `BaseServiceTest` to `runTimed` (timing and exception logging, no classloader lines) and `DeviceIdStoreTest` (no classloader; a failed lookup is retried on the next call, a success is cached). Run; expect failure.
- [ ] Step 4: Implement `runTimed`, the `DeviceIdStore` change, delete `ClassLoaderScope`. Remove the classloader KDoc on `runWithClassLoaderChange` together with the method, since it no longer describes the code.
- [ ] Step 5: Rewire the eight call sites through `CsIdeServerService.getInstance().client()`. Telemetry: move event building, including the device id, inside `scope.launch`. About tab: load the device id on a pooled thread and update the label on the EDT.
- [ ] Step 6: Run `./gradlew test` and `make format-check class-size-mine`; expect PASS. Run the manual `make run-ide` smoke test: review shows annotations, code vision and monitor; ACE preflight and refactor; telemetry sent; stable device id in the About tab; exactly one `java` child; killing it recovers on the next action; closing the IDE leaves no orphan. Record the results in the hand-off. Commit `feat: route analysis through the cs-ide JSON-RPC server`.

### Task 8: Benchmarks

**Files:**
- Modify: `benchmarks/src/jmh/kotlin/com/codescene/jetbrains/benchmarks/*.kt`, `benchmarks/build.gradle.kts` (pass the extension API jar path), `.github/workflows/benchmarks.yml` (job/step names and the benchmark action `name:`)

- [ ] Replace `ExtensionAPI.*` with a shared `CsIdeClient` from a `@State(Scope.Benchmark)` server started in `@Setup(Level.Trial)` and closed in `@TearDown`. Keep benchmark class names.
- [ ] Under `:benchmarks:jmh`, `ExtensionAPI`'s `codeSource` is the JMH fat jar (built with `duplicateClassesStrategy = WARN`), so `CsIdeLaunch.extensionApiJar()` must not be used. Resolve the `codescene.extension:api` jar from the `jmh` configuration in `benchmarks/build.gradle.kts`, pass it to the forked JVM as a system property, and hand it to `CsIdeLaunch.spec` explicitly.
- [ ] Change the benchmark action `name:` from `JetBrains Plugin - ExtensionAPI` to `JetBrains Plugin - cs-ide RPC`. The RPC path adds IPC and serialization overhead, and comparing it to the in-process series would trip `fail-on-alert` on the first master run.
- [ ] Run a short real benchmark, e.g. `./gradlew :benchmarks:jmh -Pjmh.includes=ExtensionApiReviewBenchmark` with reduced iterations; compiling `:benchmarks:jmhJar` alone is not enough. Commit `perf: benchmark the cs-ide RPC path`.

### Task 9: macOS spike script

**Files:**
- Create: `.github/cs-ide-macos-spike.clj`

- [ ] Babashka script: args `<java> <jar>`; creates a temp git repo with one `.kt` file and a commit; starts the server; waits for `cs-ide/start`; sends `cs-ide/watchFiles`; edits the file; waits up to 60 s for `cs-ide/fileReview`; prints `PASS` or `FAIL` plus any `LinkageError`/`UnsatisfiedLinkError` stderr lines; closes stdin. Exit code 0 on PASS.
- [ ] Run it on Windows against the IntelliJ JBR to prove the script itself works. Commit `chore: add cs-ide macOS watcher spike script`.
- [ ] Manual, owned by the coordinator (not an agent): run the script on a Mac against an installed IDE's JBR and record PASS/FAIL in the Phase 1 PR. This gates Phase 3, not Phase 1. The task has no code dependencies and can start immediately.

### Task 10: Verification and records

- [ ] `make iter` (10-minute timeout).
- [ ] CodeScene MCP `analyze_change_set` against `master`; fix any Code Health regressions.
- [ ] Re-run the Task 7 smoke test on the integration tip. Dock-launched macOS PATH and proxy checks are listed for a Mac and a proxy environment.
- [x] `docs/superpowers/` does not ship to master. The branch is the feature branch for all phases; the docs are removed in a final commit before the whole project merges.
- [ ] `DECISIONLOG.md` entry and `CHANGELOG.md` Unreleased line. Commit `docs: record cs-ide JSON-RPC decision`.
