# CS-12077 Phase 1: Agent work packages

Use this document to assign **one package per agent**: one branch, **one commit**, clear scope. Full task detail lives in `2026-10-12-phase1-cs-ide-transport.md`. Architecture and decisions: `../specs/2026-10-12-cs-ide-json-rpc-migration-design.md`.

**Integration branch:** `feature/cs-12077-cs-ide-json-rpc`

**Gradle:** set `JAVA_HOME` to JDK 17 before any `./gradlew` (Gradle 8.14 does not run on JDK 25).

**Every package:** run CodeScene MCP `pre_commit_code_health_safeguard` before commit; no `Co-authored` trailers; no new code comments; changed `.kt` files ≤ 325 lines (`make class-size-mine`).

---

## Merge order

```text
[DONE] PKG-0 transport
   ↓
PKG-1 launch + trust
   ↓
PKG-2 supervisor
   ↓
PKG-3 typed client
   ↓
PKG-4 live contract  ∥  PKG-6 benchmarks  ∥  PKG-7 macOS spike
   ↓
PKG-5 platform wiring (recommended after PKG-4)
   ↓
PKG-8 verification + changelog
```

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
| **Branch** | `feature/cs-12077-launch-env` |
| **Base** | Tip of `feature/cs-12077-cs-ide-json-rpc` after PKG-0 |
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
- [ ] Single commit; MCP safeguard clean.

### Agent prompt (copy-paste)

```text
Implement PKG-1 for CS-12077 on branch feature/cs-12077-launch-env from feature/cs-12077-cs-ide-json-rpc.
Read docs/superpowers/plans/2026-10-12-phase1-cs-ide-transport.md Task 3 and docs/superpowers/plans/2026-10-12-agent-packages.md PKG-1.
TDD: tests first, then CsIdeLaunch.kt and CsIdeTrustStore.kt only. One commit: feat(cli): add cs-ide launch command and environment.
Do not touch supervisor, client, platform, or integration tests.
JAVA_HOME=JDK17 for Gradle. pre_commit_code_health_safeguard before commit.
```

---

## PKG-2 — Process abstraction, backoff, supervisor

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-supervisor` |
| **Base** | Integration branch with **PKG-1** merged |
| **Commit message** | `feat(cli): add cs-ide server supervisor` |
| **Plan tasks** | Task 4 |

### Scope

- `CsIdeProcess.kt`, `CsIdeBackoff.kt`, `CsIdeServer.kt`
- `CsIdeBackoffTest.kt`, `CsIdeServerTest.kt`, `FakeCsIdeProcess.kt` (test helper)

### Out of scope

`CsIdeClient`, live jar tests, platform wiring.

### Definition of done

- [ ] Fake-process tests: lazy start; resolve after `cs-ide/start`; startup timeout → `CsIdeUnavailableException` + stderr tail + destroy; exit fails pending; respawn after backoff; cooldown fail-fast without spawn; success resets backoff; `close()` stdin → grace → `destroyTree`; requests after close fail.
- [ ] `SystemCsIdeProcessFactory` uses `CsIdeLaunchSpec` from PKG-1.
- [ ] `./gradlew :core:test --tests "*.cli.CsIdeBackoffTest" --tests "*.cli.CsIdeServerTest"` passes.
- [ ] Single commit; MCP safeguard clean.

### Agent prompt

```text
Implement PKG-2 for CS-12077 on feature/cs-12077-supervisor after PKG-1 is merged.
Follow 2026-10-12-phase1-cs-ide-transport.md Task 4 and 2026-10-12-agent-packages.md PKG-2.
Use fake processes only in tests; no live server in this package.
One commit: feat(cli): add cs-ide server supervisor.
```

---

## PKG-3 — Typed client and error mapping

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-client` |
| **Base** | Integration branch with **PKG-2** merged |
| **Commit message** | `feat(cli): add typed cs-ide client` |
| **Plan tasks** | Task 5 |

### Scope

- `CsIdeClient.kt`, `CsIdeErrors.kt`
- `CsIdeClientTest.kt`, `CsIdeErrorsTest.kt`, `IdeServerFixture.kt` (port from old branch `c8f6b1c` as needed)

### Definition of done

- [ ] `CsIdeTransport` + `CsIdeClient` with all methods from plan Task 5; kebab-case params on wire; preflight cached for `fnsToRefactor`.
- [ ] `IdeServerFixture` in-process fake; tests for each RPC method and `CsIdeErrors.unwrap` (ConnectException, 401 message, ExecutionException).
- [ ] `./gradlew :core:test --tests "*.cli.CsIdeClientTest" --tests "*.cli.CsIdeErrorsTest"` passes.
- [ ] Single commit; no platform changes.

### Agent prompt

```text
Implement PKG-3 for CS-12077 on feature/cs-12077-client after PKG-2 merged.
Task 5 in 2026-10-12-phase1-cs-ide-transport.md; package brief PKG-3.
Port IdeServerFixture from feature/ide-daemon-json-rpc c8f6b1c if helpful.
One commit: feat(cli): add typed cs-ide client.
```

---

## PKG-4 — Live contract test

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-live-contract` |
| **Base** | Integration branch with **PKG-3** merged |
| **Commit message** | `test(cli): replace ExtensionAPI integration tests with live cs-ide contract` |
| **Plan tasks** | Task 6 |

### Scope

- Create `CsIdeLiveContractTest.kt`
- Delete `ExtensionApiIntegrationTest.kt`, `ExtensionApiCodeHealthRulesIntegrationTest.kt`, `ExtensionApiFnToRefactorDeltaOverloadTest.kt`
- Modify `core/build.gradle.kts` only for `CS_IDE_JAVA` → `csIde.java`

### Definition of done

- [ ] Port scenarios: Kotlin review/delta/fns-to-refactor; rules review and rules delta via `cs-ide/delta` without repo-path; `deviceId()`; clean `close()` / no orphan process.
- [ ] `@BeforeClass` one server per class; Java from `csIde.java` or test JVM.
- [ ] `./gradlew :core:test --tests "*.cli.CsIdeLiveContractTest"` passes.
- [ ] Single commit; no `platform/` edits.

### Agent prompt

```text
Implement PKG-4 for CS-12077 on feature/cs-12077-live-contract after PKG-3 merged.
Task 6 in phase1 plan; replace the three ExtensionApi* integration tests with CsIdeLiveContractTest.
Only touch core tests and core/build.gradle.kts for csIde.java.
One commit: test(cli): replace ExtensionAPI integration tests with live cs-ide contract.
```

---

## PKG-5 — Platform wiring

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-platform` |
| **Base** | **PKG-3** required; **PKG-4** recommended merged first |
| **Commit message** | `feat: route analysis through the cs-ide JSON-RPC server` |
| **Plan tasks** | Task 7 |

### Scope

**Create**

- `src/main/kotlin/com/codescene/jetbrains/platform/cli/CsIdeServerService.kt`
- `src/main/kotlin/com/codescene/jetbrains/platform/cli/CsIdeLaunchEnvironmentProvider.kt`

**Modify**

- `core/.../review/BaseService.kt`, `core/.../util/DeviceIdStore.kt`
- `CodeReviewService.kt`, `CodeDeltaService.kt`, `AceService.kt`
- `TelemetryService.kt`, `CodeSceneApplicationServiceProvider.kt`, `AboutTab.kt`
- `BaseServiceTest.kt`, `DeviceIdStoreTest.kt`

**Delete**

- `ClassLoaderScope.kt`, `ClassLoaderScopeTest.kt`

### Definition of done

- [ ] Eight call sites use `CsIdeServerService.getInstance().client()`; no `runWithClassLoaderChange` / in-process `ExtensionAPI` on those paths.
- [ ] `runTimed` replaces classloader timing; `DeviceIdStore` no failure cache; telemetry + About tab off EDT for device id.
- [ ] Launch env: `EnvironmentUtil`, git dir on PATH, HTTP proxy env, trust store at `PathManager.getSystemPath()/codescene/cs-ide-truststore.p12`.
- [ ] `./gradlew test` passes.
- [ ] Single commit. **Do not run in parallel with PKG-4** if both would touch the same files (PKG-4 should not touch platform).

### Agent prompt

```text
Implement PKG-5 for CS-12077 on feature/cs-12077-platform after PKG-3 (and preferably PKG-4) merged.
Task 7 in phase1 plan; full platform cut-over, delete ClassLoaderScope.
One commit: feat: route analysis through the cs-ide JSON-RPC server.
Full ./gradlew test must pass.
```

---

## PKG-6 — Benchmarks

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-benchmarks` |
| **Base** | **PKG-3** merged (parallel with PKG-4/7) |
| **Commit message** | `perf: benchmark the cs-ide RPC path` |
| **Plan tasks** | Task 8 |

### Scope

- `benchmarks/src/jmh/kotlin/com/codescene/jetbrains/benchmarks/*.kt`
- Optional: `.github/workflows/benchmarks.yml` job name only

### Definition of done

- [ ] `@State` + `@Setup(Level.Trial)` / `@TearDown` shared `CsIdeClient` + server; keep benchmark class names.
- [ ] `./gradlew :benchmarks:jmhJar` compiles.
- [ ] Single commit; no platform service required if benchmarks use core launch helpers.

### Agent prompt

```text
Implement PKG-6 for CS-12077 on feature/cs-12077-benchmarks after PKG-3 merged.
Switch JMH benchmarks from ExtensionAPI to CsIdeClient per Task 8.
One commit: perf: benchmark the cs-ide RPC path.
```

---

## PKG-7 — macOS watcher spike script

| Field | Value |
| --- | --- |
| **Branch** | `feature/cs-12077-macos-spike` |
| **Base** | Any tip with transport (no Kotlin deps on PKG-1+) |
| **Commit message** | `chore: add cs-ide macOS watcher spike script` |
| **Plan tasks** | Task 9 |

### Scope

- Create `.github/cs-ide-macos-spike.clj` only

### Definition of done

- [ ] Babashka script: args `<java> <jar>`; temp git repo; wait `cs-ide/start`; `watchFiles`; edit file; wait 60s for `fileReview`; PASS/FAIL + LinkageError lines; exit 0 on PASS.
- [ ] Run once on Windows with IDE JBR + extension API jar to validate script logic.
- [ ] Single commit; no Kotlin changes.

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
| **Base** | All PKGs 1–7 merged (5 required for product completeness) |
| **Commit message** | `docs: record cs-ide JSON-RPC decision` |
| **Plan tasks** | Task 10 |

### Scope

- `DECISIONLOG.md`, `CHANGELOG.md` (Unreleased)
- Fix any issues found by `make iter` and CodeScene `analyze_change_set` vs `master`

### Definition of done

- [ ] `make iter` (10 min timeout) green.
- [ ] `analyze_change_set` — no unaddressed Code Health regressions.
- [ ] PR description includes manual `make run-ide` smoke checklist (review, ACE, telemetry, device id, one child java, kill recovery, clean exit).
- [ ] Single commit for docs/decision log unless iter fixes require a separate fix commit (prefer one docs commit + fix commits only if necessary).

### Agent prompt

```text
Run PKG-8 after all implementation packages merged: make iter, CodeScene analyze_change_set vs master, DECISIONLOG + CHANGELOG.
One commit: docs: record cs-ide JSON-RPC decision.
Document smoke test results in PR, not necessarily in repo.
```

---

## Conflict matrix (parallel work)

| Packages | Parallel? | Notes |
| --- | --- | --- |
| PKG-4 + PKG-6 + PKG-7 | Yes | Different paths |
| PKG-4 + PKG-5 | Yes | Different paths; merge 4 before 5 if you want contract green before cut-over |
| PKG-5 + PKG-6 | Yes | Different modules |
| PKG-1 + PKG-2 | No | 2 needs 1 |
| PKG-2 + PKG-3 | No | 3 needs 2 |

---

## Docs already on integration branch

| Commit | Content |
| --- | --- |
| `24e5152` | Design spec |
| `8fe9430` | Phase 1 implementation plan |
| (this file) | Agent packages — commit after add |
