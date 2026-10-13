# CS-12077 macOS spike: agent prompt

Run this prompt with an agent opened in your local clone of `codescene-jetbrains` on macOS.

```text
You are in my local clone of codescene-jetbrains on macOS. Run the CS-12077 macOS cs-ide watcher spike from here and report the result. Do not change any code or commit anything.

Context: the JetBrains plugin now runs analysis in a `cs-ide server` child JVM started from the IDE's bundled JBR. Phase 3 adds a file watcher that needs JNA's native library (libjnidispatch). The question is whether the signed JBR inside the IDE's .app can load it, either with JNA's default unpack or using the IDE's bundled copy. The handoff script tests both.

Steps:
1. Prepare the branch:
   - Run git status. If there are local changes, stop and ask me before switching branches.
   - Then: git fetch origin && git checkout feature/cs-12077-cs-ide-json-rpc && git pull.
   - Confirm that .github/cs-ide-macos-handoff.sh and .github/cs-ide-macos-spike.clj exist.
2. Prerequisites:
   - A JetBrains IDE in /Applications or ~/Applications, with the CodeScene plugin installed from the marketplace. Plugin 0.5.8 bundles api-1.0-fdef705.jar.
   - Babashka (bb): if missing, run brew install borkdude/brew/babashka.
3. From the repo root, run .github/cs-ide-macos-handoff.sh.
   - It picks the first JetBrains IDE it finds. If several are listed, prefer IntelliJ IDEA and pass its .app path as the first argument.
   - If it cannot find the extension API jar, search ~/Library/Application Support/JetBrains/*/plugins/*/lib/ and ~/.gradle/caches for api-1.0-fdef705.jar, then rerun with API_JAR=<path>.
   - Each spike run takes at most about 2.5 minutes. Don't interrupt it.
4. Afterwards, check that no cs-ide server is left running: pgrep -fl codescene.cli.ide should print nothing. If something is listed, report it and kill it.
5. If a run fails with an error from the script itself rather than PASS/FAIL (for example a bash error, a path problem or a missing tool), fix only the environment (install the tool, pass the right path) and rerun. Do not edit the scripts. If you can't get a verdict, report the exact error output.
6. Leave the repo clean: the report file cs-ide-macos-spike-report.md is written in the repo root and is not tracked. Show me its contents, then delete it so the working tree stays clean.

Report back:
- The full contents of cs-ide-macos-spike-report.md.
- For each of the two runs (default JNA unpack, and IDE-bundled libjnidispatch): PASS or FAIL, plus any LinkageError or UnsatisfiedLinkError lines.
- The verdict:
  - Default unpack passes: Phase 3 needs no special JNA setup.
  - Only the bundled run passes: Phase 3 adds -Djna.boot.library.path=<ide>/lib/jna/<arch> -Djna.nounpack=true.
  - Both fail: Phase 3 is blocked. Include the entitlements line and the errors.
- Whether any cs-ide process was left running.
```
