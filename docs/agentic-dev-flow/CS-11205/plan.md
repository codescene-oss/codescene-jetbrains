# CS-11205: Zip Slip in build-script unzip()

**Goal:** Prevent the Gradle build-script ZIP extraction helper from writing archive entries outside the intended CWF asset output directory.

**Approach:** Keep the existing release ZIP layout handling unchanged, including the current first path-segment stripping. Add a canonical-path containment check immediately after resolving each output file so entries containing `../` chains or symlink-resolved escapes fail the Gradle task before any file or directory is created outside the target directory.

## Context

The ClickUp ticket identifies a Zip Slip issue in `build.gradle.kts`, where `fun unzip(zipFile, outputDir)` computes a relative path from each ZIP entry and writes `File(outputDir, relativePath)` without verifying that the canonical destination remains inside `outputDir`. A malicious ZIP entry such as `a/../../etc/passwd` could resolve outside `src/main/resources/cs-cwf`. The release archive is fetched from a CodeScene-controlled GitHub repo, so practical risk is low, but the build-script pattern is unsafe and should not be copied forward.

## Acceptance Criteria

- ZIP entries resolving inside the target output directory continue to extract normally.
- ZIP file entries resolving outside the target output directory fail before writing outside the directory.
- ZIP directory entries resolving outside the target output directory fail before creating directories outside the directory.
- The containment check uses canonical paths so `..` segments and symlink-resolved escapes are handled by the filesystem.
- The existing CWF download and unzip flow remains otherwise unchanged.

---

### Task 1: Guard Build-Script ZIP Extraction

**Files:**

- Modify: `build.gradle.kts`

**Intent:** Add a minimal containment guard to the existing `unzip(zipFile: File, outputDir: File)` helper. The helper should compute the output file exactly as it does today, then reject any ZIP entry whose canonical output destination is not the output directory itself and is not under the canonical output directory path.

**Key signatures/shapes:**

- `fun unzip(zipFile: File, outputDir: File)` remains the same public build-script helper.
- Add local values near `val outFile = File(outputDir, relativePath)`:
  - `val outDirCanonical = outputDir.canonicalPath`
  - `val outCanonical = outFile.canonicalPath`
- Add a `require(...)` guard before `entry.isDirectory` handling:
  - Allow when `outCanonical == outDirCanonical`.
  - Allow when `outCanonical.startsWith(outDirCanonical + File.separator)`.
  - Otherwise throw with message `Zip entry escapes target directory: ${entry.name}`.

**Test strategy:**

- Scenario: run the existing build-script formatting/checking path via `make iter`; this exercises Kotlin DSL compilation and the plugin build path that includes CWF asset handling.
- Scenario: inspect the diff to confirm the guard executes before both `outFile.mkdirs()` and `outFile.outputStream()`.
- Edge case: confirm directory entries are guarded by the same check as file entries because the check occurs before `entry.isDirectory`.

**Commit:** `CS-11205: guard build-script zip extraction`
