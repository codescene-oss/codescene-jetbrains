# CS-12077 Phase 1: Agent work packages

Use this document to assign **one package per agent**: one branch, **one commit**, clear scope. Full task detail lives in `2026-10-12-phase1-cs-ide-transport.md`. Architecture and decisions: `../specs/2026-10-12-cs-ide-json-rpc-migration-design.md`.

**Integration branch:** `feature/cs-12077-cs-ide-json-rpc`

**Gradle:** set `JAVA_HOME` to JDK 17 before any `./gradlew` (Gradle 8.14 does not run on JDK 25).

**Every package:**

- `make format-check class-size-mine` passes (ktlint clean; changed `.kt` files ≤ 325 lines). Fix lint findings before anything else. `make` hangs on Windows, so there run the equivalents directly: `.\gradlew.bat :core:ktlintCheck -PktlintFailOnError=true` (add `:ktlintCheck` when touching `src/`) and `bb -f .github/check-class-size-mine.clj`. The same applies to `make iter`: run the relevant `gradlew` test tasks instead.
- CodeScene MCP `pre_commit_code_health_safeguard` before commit.
- No `Co-authored` trailers; no new code comments. Cursor's agent adds `Co-authored-by: Cursor` on its own, so check `git log -1 --format=%B` after committing and amend it away.
- Never merge, fast-forward or push the integration branch, and never commit in `C:\Git\codescene-jetbrains`. Work only in your own worktree and branch; the coordinator merges.
- After a rebase, check for conflict markers (`git diff --check HEAD~1`) before declaring the package done.

**Merging:** the coordinator rebases each package branch onto the current integration tip and fast-forwards it (`git merge --ff-only`), so the integration branch keeps one commit per package. A package whose base is not merged yet may stack on the base package's branch and is rebased once that merges.

---

## Merge order

```text
[DONE] PKG-0 transport
   ↓
[DONE] PKG-1 launch + trust  ∥  [DONE] PKG-3 typed client  ∥  [DONE] PKG-7 macOS spike
   ↓
[DONE] PKG-2 supervisor (needs PKG-1)
   ↓
[DONE] PKG-4 live contract  ∥  PKG-5a platform service  ∥  PKG-6 benchmarks   (each needs PKG-1, 2, 3)
   ↓
PKG-5b platform cut-over (needs PKG-5a; PKG-4 recommended first)
   ↓
PKG-8 verification + changelog
```

PKG-3 does not depend on PKG-1 or PKG-2: `CsIdeTransport` is a `fun interface` that PKG-3 owns, and PKG-5a adapts the supervisor to it with `CsIdeTransport(server::request)`. `CsIdeServer` does not implement it.

---

## PKG-0 — Transport (DONE)

| Field | Value |
| --- | --- |
| **Status** | Merged on integration branch |
| **Commit** | `f270fa3` — `feat(core): add LSP framing, RpcJson and JsonRpcConnection (CS-12077)` |
| **Files** | `LspFraming.kt`, `RpcJson.kt`, `JsonRpcConnection.kt`, `LspFramingTest.kt`, `RpcJsonTest.kt`, `JsonRpcConnectionTest.kt`, `TestPipes.kt` |
| **Verify** | `./gradlew :core:test --tests "com.codescene.jetbrains.core.cli.*"` |

---

## PKG-1 — Launch command, environment, trust store

| Field | Value |
| --- | --- |
| **Status** | DONE — `cfaece3` `feat(cli): add cs-ide launch command and environment`. PATH and proxy rules as in Task 3. |
| **Branch** | `feature/cs-12077-launch-env` |
| **Base** | Integration branch tip (PKG-0) |
| **Commit message** | `feat(cli): add cs-ide launch command and environment` |
| **Plan tasks** | Task 3 |

### Scope (create only)

- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/CsIdeLaunch.kt`
- `core/src/main/kotlin/com/codescene/jetbrains/core/cli/CsIdeTrustStore.kt`
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/CsIdeLaunchTest.kt`
- `core/src/test/kotlin/com/codescene/jetbrains/core/cli/CsIdeTrustStoreTest.kt`

### Out of scope

Process spawn, `CsIdeServer`, `CsIdeClient`, platform `CertificateManager`, any `platform/` or service rewrites.

### Interfaces (must match Phase 1 plan Task 3)

- `CsIdeProxy`, `CsIdeLaunchEnvironment`, `CsIdeLaunchSpec`
- `CsIdeLaunch`: `MAX_HEAP`, `javaExecutable`, `extensionApiJar`, `defaultThreads`, `command`, `environment`, `spec`
- `CsIdeTrustStore`: `PASSWORD`, `write`, `jvmArgs`, `jdkDefaultIssuers`

### Definition of done

- [ ] Unit tests: Windows `java.exe` vs Unix `java`; missing executable → `IllegalStateException` with path; `defaultThreads(8)==4`, `defaultThreads(1)==1`; command order `java --enable-native-access=ALL-UNNAMED -Xmx2g <extra> -cp <jar> clojure.main -m codescene.cli.ide server --threads N`; env overlay; PATH prefix once (case-insensitive on Windows, `;` vs `:`); proxy sets `HTTPS_PROXY`/`HTTP_PROXY` and `NO_PROXY` only when not already set; `extensionApiJar()` is existing `.jar`.
- [ ] Trust store: PKCS12 with `PASSWORD` contains all certs; `jvmArgs` includes store path, type PKCS12, password.
- [ ] `./gradlew :core:test --tests "*.cli.CsIdeLaunchTest" --tests "*.cli.CsIdeTrustStoreTest"` passes.
- [ ] `make format-check class-size-mine` passes.
- [ ] Single commit; MCP safeguard clean.

### Agent prompt (copy-paste)

```text
Implement PKG-1 for CS-12077 on branch feature/cs-12077-launch-env from feature/cs-12077-cs-ide-json-rpc.
Read docs/superpowers/plans/2026-10-12-phase1-cs-ide-transport.md Task 3 and docs/superpowers/plans/2026-10-12-agent-packages.md PKG-1.
TDD: tests first, then CsIdeLaunch.kt and CsIdeTrustStore.kt only. One commit: feat(cli): add cs-ide launch command and environment.
Do not touch supervisor, client, platform, or integration tests.
JAVA_HOME=JDK17 for Gradle. make format-check class-size-mine, then pre_commit_code_health_safeguard before commit.
```

---

## PKG-2 — Process abstraction, backoff, supervisor (DONE)

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-supervisor` |
| **Base** | Integration branch with **PKG-1** merged (or stacked on `feature/cs-12077-launch-env`) |
| **Commit message** | `feat(cli): add cs-ide server supervisor` |
| **Plan tasks** | Task 4 |
| **Status** | DONE — `799bd00`. Process tree kill destroys descendants on every OS; `close()` and process exit abort a pending start immediately; exit/transport callbacks ignore stale processes; shutdown runs outside the state lock; stderr is logged at debug. |

### Scope

- `CsIdeProcess.kt`, `CsIdeBackoff.kt`, `CsIdeServer.kt`
- `CsIdeBackoffTest.kt`, `CsIdeServerTest.kt`, `FakeCsIdeProcess.kt` (test helper)

### Out of scope

`CsIdeClient`, `CsIdeTransport`, live jar tests, platform wiring. `CsIdeUnavailableException` already exists in `CsIdeErrors.kt` (PKG-3); use it, do not redeclare it.

### Definition of done

- [ ] Fake-process tests: lazy start; resolve after `cs-ide/start`; startup timeout → `CsIdeUnavailableException` + stderr tail + destroy; exit fails pending; respawn after backoff; cooldown fail-fast without spawn; success resets backoff; `close()` stdin → grace → `destroyTree`; requests after close fail.
- [ ] `SystemCsIdeProcessFactory` uses `CsIdeLaunchSpec` from PKG-1.
- [ ] `./gradlew :core:test --tests "*.cli.CsIdeBackoffTest" --tests "*.cli.CsIdeServerTest"` passes.
- [ ] `make format-check class-size-mine` passes.
- [ ] Single commit; MCP safeguard clean.

### Agent prompt

```text
Implement PKG-2 for CS-12077 on feature/cs-12077-supervisor after PKG-1 is merged (or stacked on feature/cs-12077-launch-env).
Follow 2026-10-12-phase1-cs-ide-transport.md Task 4 and 2026-10-12-agent-packages.md PKG-2.
Use fake processes only in tests; no live server in this package. CsIdeServer does not implement CsIdeTransport.
CsIdeUnavailableException already exists in CsIdeErrors.kt; use it, do not redeclare it.
Do not merge or push the integration branch; check git log -1 for a Co-authored-by trailer and amend it away.
make format-check class-size-mine, then pre_commit_code_health_safeguard.
One commit: feat(cli): add cs-ide server supervisor.
```

---

## PKG-3 — Typed client and error mapping

| Field | Value |
| --- | --- |
| **Status** | DONE — `2b5bbe2` `feat(cli): add typed cs-ide client`. Client takes `ReviewParams`/`CodeParams`/`CacheParams` (see Task 5); `CsIdeUnavailableException` lives in `CsIdeErrors.kt`. |
| **Branch** | `feature/cs-12077-client` |
| **Base** | Integration branch tip (PKG-0); parallel with PKG-1 and PKG-2 |
| **Commit message** | `feat(cli): add typed cs-ide client` |
| **Plan tasks** | Task 5 |

### Scope

- `CsIdeClient.kt` (includes `fun interface CsIdeTransport`), `CsIdeErrors.kt`
- `CsIdeClientTest.kt`, `CsIdeErrorsTest.kt`, `IdeServerFixture.kt` (port from old branch `c8f6b1c` as needed)

### Out of scope

`CsIdeServer.kt` and anything else from PKG-1/PKG-2. Tests drive the client through `CsIdeTransport(connection::request)` over a `JsonRpcConnection` to the fixture.

### Definition of done

- [ ] `CsIdeTransport` + `CsIdeClient` with all methods from plan Task 5; kebab-case params on wire; preflight cached for `fnsToRefactor`.
- [ ] Every blocking client call rethrows failures through `CsIdeErrors.unwrap`; a test proves a `"java.net.ConnectException: ..."` RPC error surfaces from a client call as `ConnectException`.
- [ ] `IdeServerFixture` in-process fake; tests for each RPC method and `CsIdeErrors.unwrap` (ConnectException, 401 message, ExecutionException).
- [ ] `./gradlew :core:test --tests "*.cli.CsIdeClientTest" --tests "*.cli.CsIdeErrorsTest"` passes.
- [ ] `make format-check class-size-mine` passes.
- [ ] Single commit; no platform changes.

### Agent prompt

```text
Implement PKG-3 for CS-12077 on feature/cs-12077-client from the integration branch tip (does not need PKG-1 or PKG-2).
Task 5 in 2026-10-12-phase1-cs-ide-transport.md; package brief PKG-3.
CsIdeTransport is a fun interface owned by this package; do not create or edit CsIdeServer.kt.
Port IdeServerFixture from feature/ide-daemon-json-rpc c8f6b1c if helpful.
make format-check class-size-mine, then pre_commit_code_health_safeguard.
One commit: feat(cli): add typed cs-ide client.
```

---

## PKG-4 — Live contract test (DONE)

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-live-contract` |
| **Base** | Integration branch with **PKG-1, PKG-2, PKG-3** merged |
| **Commit message** | `test(cli): replace ExtensionAPI integration tests with live cs-ide contract` |
| **Plan tasks** | Task 6 |
| **Status** | DONE — `1a3077e`. Seven live tests in `CsIdeLiveContractTest` (fixtures in `CsIdeLiveContractFixtures`); a null rules delta counts as no degradation instead of skipping the test. |

### Scope

- Create `CsIdeLiveContractTest.kt`
- Delete `ExtensionApiIntegrationTest.kt`, `ExtensionApiCodeHealthRulesIntegrationTest.kt`, `ExtensionApiFnToRefactorDeltaOverloadTest.kt`
- Modify `core/build.gradle.kts` only for `CS_IDE_JAVA` → `csIde.java`

### Definition of done

- [ ] Port scenarios: Kotlin review/delta/fns-to-refactor; rules review and rules delta via `cs-ide/delta` without repo-path; `deviceId()`; clean `close()` / no orphan process.
- [ ] `@BeforeClass` one server per class; Java from `csIde.java` or test JVM; client built with `CsIdeTransport(server::request)`.
- [ ] `./gradlew :core:test --tests "*.cli.CsIdeLiveContractTest"` passes.
- [ ] `make format-check class-size-mine` passes.
- [ ] Single commit; no `platform/` edits.

### Agent prompt

```text
Implement PKG-4 for CS-12077 on feature/cs-12077-live-contract after PKG-1, PKG-2 and PKG-3 merged.
Task 6 in phase1 plan; replace the three ExtensionApi* integration tests with CsIdeLiveContractTest.
Only touch core tests and core/build.gradle.kts for csIde.java.
make format-check class-size-mine, then pre_commit_code_health_safeguard.
One commit: test(cli): replace ExtensionAPI integration tests with live cs-ide contract.
```

---

## PKG-5a — Platform server service (not wired)

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-platform-service` |
| **Base** | Integration branch with **PKG-1, PKG-2, PKG-3** merged |
| **Commit message** | `feat: add cs-ide server service and launch environment provider` |
| **Plan tasks** | Task 7, steps 1–2 |

### Scope (create only)

- `src/main/kotlin/com/codescene/jetbrains/platform/cli/CsIdeServerService.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/cli/CsIdeLaunchEnvironmentProvider.kt`

### Out of scope

Every existing call site, `BaseService`, `DeviceIdStore`, `ClassLoaderScope`. Nothing calls the new service yet.

### Definition of done

- [ ] `CsIdeServerService` is an app-level `@Service`, `Disposable`; owns one `CsIdeServer` and a `CsIdeClient(CsIdeTransport(server::request))`; `dispose()` closes the server.
- [ ] Launch env: `EnvironmentUtil`, git dir on PATH, HTTP proxy env, trust store at `PathManager.getSystemPath()/codescene/cs-ide-truststore.p12`; every step `runCatching` and logged.
- [ ] `./gradlew test` passes; `make format-check class-size-mine` passes.
- [ ] Single commit; MCP safeguard clean.

### Agent prompt

```text
Implement PKG-5a for CS-12077 on feature/cs-12077-platform-service after PKG-1, PKG-2 and PKG-3 merged.
Task 7 steps 1-2 in phase1 plan: add CsIdeServerService and CsIdeLaunchEnvironmentProvider only. Do not change any existing call site.
make format-check class-size-mine, then pre_commit_code_health_safeguard.
One commit: feat: add cs-ide server service and launch environment provider.
```

---

## PKG-5b — Platform cut-over

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-platform` |
| **Base** | **PKG-5a** required; **PKG-4** recommended merged first |
| **Commit message** | `feat: route analysis through the cs-ide JSON-RPC server` |
| **Plan tasks** | Task 7, steps 3–6 |

### Scope

**Modify**

- `core/.../review/BaseService.kt`, `core/.../util/DeviceIdStore.kt`
- `CodeReviewService.kt`, `CodeDeltaService.kt`, `AceService.kt`
- `TelemetryService.kt`, `CodeSceneApplicationServiceProvider.kt`, `AboutTab.kt`
- `BaseServiceTest.kt`, `DeviceIdStoreTest.kt`

**Delete**

- `ClassLoaderScope.kt`, `ClassLoaderScopeTest.kt`

### Definition of done

- [ ] Eight call sites use `CsIdeServerService.getInstance().client()`; no `runWithClassLoaderChange` / in-process `ExtensionAPI` on those paths.
- [ ] `runTimed` replaces classloader timing, and the classloader KDoc on `runWithClassLoaderChange` is removed with it; `DeviceIdStore` no failure cache; telemetry + About tab off EDT for device id.
- [ ] `./gradlew test` passes; `make format-check class-size-mine` passes.
- [ ] Manual `make run-ide` smoke test, results listed in the commit's PR/hand-off note: review shows annotations, code vision and monitor; ACE preflight and refactor; telemetry sent; device id stable in About tab; exactly one `java` child; killing it recovers on the next action; closing the IDE leaves no orphan.
- [ ] Single commit. PKG-4 must not touch platform files.

### Agent prompt

```text
Implement PKG-5b for CS-12077 on feature/cs-12077-platform after PKG-5a (and preferably PKG-4) merged.
Task 7 steps 3-6 in phase1 plan; full platform cut-over, delete ClassLoaderScope and the classloader KDoc in BaseService.
Full ./gradlew test and make format-check class-size-mine must pass, then run the make run-ide smoke checklist and report the results.
pre_commit_code_health_safeguard before commit.
One commit: feat: route analysis through the cs-ide JSON-RPC server.
```

---

## PKG-6 — Benchmarks

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-benchmarks` |
| **Base** | **PKG-1, PKG-2, PKG-3** merged (parallel with PKG-4/5a) |
| **Commit message** | `perf: benchmark the cs-ide RPC path` |
| **Plan tasks** | Task 8 |

### Scope

- `benchmarks/src/jmh/kotlin/com/codescene/jetbrains/benchmarks/*.kt`
- `benchmarks/build.gradle.kts` only to pass the resolved extension API jar path as a system property
- `.github/workflows/benchmarks.yml`: job/step names and the benchmark action `name:` input

### Definition of done

- [ ] `@State` + `@Setup(Level.Trial)` / `@TearDown` shared `CsIdeClient` + server; keep benchmark class names.
- [ ] The server is launched from the real `codescene.extension:api` jar, not the JMH fat jar that `ExtensionAPI`'s `codeSource` resolves to under `:benchmarks:jmh`; pass the jar to `CsIdeLaunch.spec` explicitly.
- [ ] `benchmark-action` `name:` changed to `JetBrains Plugin - cs-ide RPC` so the RPC path starts a new series instead of tripping `fail-on-alert` against in-process history.
- [ ] A short real run passes, e.g. `./gradlew :benchmarks:jmh -Pjmh.includes=ExtensionApiReviewBenchmark` with reduced iterations, not just `:benchmarks:jmhJar`.
- [ ] `make format-check class-size-mine` passes.
- [ ] Single commit; no platform service required if benchmarks use core launch helpers.

### Agent prompt

```text
Implement PKG-6 for CS-12077 on feature/cs-12077-benchmarks after PKG-1, PKG-2 and PKG-3 merged.
Switch JMH benchmarks from ExtensionAPI to CsIdeClient per Task 8.
Launch the server from the real extension API jar (passed from benchmarks/build.gradle.kts), not the JMH fat jar.
Rename the benchmark-action series to "JetBrains Plugin - cs-ide RPC". Prove it with a short real :benchmarks:jmh run.
make format-check class-size-mine, then pre_commit_code_health_safeguard.
One commit: perf: benchmark the cs-ide RPC path.
```

---

## PKG-7 — macOS watcher spike script (DONE)

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-macos-spike` |
| **Base** | Integration branch tip; no dependencies, start any time |
| **Commit message** | `chore: add cs-ide macOS watcher spike script` |
| **Plan tasks** | Task 9 |
| **Status** | DONE — `a3a0ba1`. PASS on Windows with the IDE JBR. The script waits for the first `watchInventoryChanged` before editing (the server requeues the inventory on `watchFiles`). The Mac run is still pending (PKG-8). |

### Scope

- Create `.github/cs-ide-macos-spike.clj` only

### Definition of done

- [ ] Babashka script: args `<java> <jar>`; temp git repo; wait `cs-ide/start`; `watchFiles`; edit file; wait 60s for `fileReview`; PASS/FAIL + LinkageError lines; exit 0 on PASS.
- [ ] Run once on Windows with IDE JBR + extension API jar to validate script logic.
- [ ] Single commit; no Kotlin changes.

The agent only proves the script works. The Phase 3 go/no-go run on a Mac (Dock-launched IDE's JBR) is a **manual step owned by the coordinator**, tracked in PKG-8.

### Agent prompt

```text
Implement PKG-7 for CS-12077: .github/cs-ide-macos-spike.clj per Task 9.
Verify script on Windows with local java + extension-api jar.
One commit: chore: add cs-ide macOS watcher spike script.
```

---

## PKG-8 — Verification and project records

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-verify` |
| **Base** | All of PKG-1 to PKG-7 merged |
| **Commit message** | `docs: record cs-ide JSON-RPC decision` |
| **Plan tasks** | Task 10 |

### Scope

- `DECISIONLOG.md`, `CHANGELOG.md` (Unreleased)
- Fix any issues found by `make iter` and CodeScene `analyze_change_set` vs `master`

### Definition of done

- [ ] `make iter` (10 min timeout) green.
- [ ] `analyze_change_set` — no unaddressed Code Health regressions.
- [ ] Re-run the PKG-5b smoke checklist on the integration tip; PR description lists the results plus the Mac and proxy checks still pending.
- [ ] Coordinator has run `.github/cs-ide-macos-spike.clj` on a Mac and recorded PASS/FAIL in the PR (gates Phase 3, not this PR).
- [ ] Coordinator decides whether `docs/superpowers/` ships to master. `docs` is gitignored and these files were force-added; if they should not ship, remove them in a final commit before opening the PR.
- [ ] Single commit for docs/decision log unless iter fixes require a separate fix commit (prefer one docs commit + fix commits only if necessary).

### Agent prompt

```text
Run PKG-8 after all implementation packages merged: make iter, CodeScene analyze_change_set vs master, DECISIONLOG + CHANGELOG.
Re-run the PKG-5b smoke checklist on the integration tip.
One commit: docs: record cs-ide JSON-RPC decision.
Document smoke test results in PR, not necessarily in repo.
```

---

## Conflict matrix (parallel work)

| Packages | Parallel? | Notes |
| --- | --- | --- |
| PKG-1 + PKG-3 + PKG-7 | Yes | Different files; PKG-3 owns `CsIdeTransport` |
| PKG-2 + PKG-3 | Yes | PKG-3 does not touch `CsIdeServer.kt` |
| PKG-1 + PKG-2 | No | 2 needs `CsIdeLaunchSpec` from 1 (may stack) |
| PKG-4 + PKG-5a + PKG-6 | Yes | Different paths and modules |
| PKG-4 + PKG-5b | Yes | Different paths; merge 4 before 5b if you want contract green before cut-over |
| PKG-5a + PKG-5b | No | 5b wires the service 5a creates |
| PKG-5b + PKG-6 | Yes | Different modules |

---

## Docs on integration branch

`docs` is gitignored; these files are force-added (`git add -f`), and any new doc needs the same.

| Commit | Content |
| --- | --- |
| `24e5152` | Design spec |
| `8fe9430` | Phase 1 implementation plan |
| `972063a` | Agent packages (this file) |
