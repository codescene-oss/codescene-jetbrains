## CS-12077 macOS spike (cs-ide watcher)

- Date: 2026-10-13 10:43 CEST
- macOS: 26.4 (arm64)
- IDE: IntelliJ IDEA 2026.2.3 (IU-262.10968.63) at `/Applications/IntelliJ IDEA.app`
- JBR: openjdk version "25.0.4" 2026-07-21 OpenJDK Runtime Environment JBR-25.0.4+1-508.27-nomod (build 25.0.4+1-b508.27)
- JBR entitlements: com.apple.security.automation.apple-events com.apple.security.cs.allow-jit com.apple.security.cs.allow-unsigned-executable-memory com.apple.security.cs.disable-library-validation com.apple.security.device.audio-input com.apple.security.device.camera com.apple.security.personal-information.addressbook com.apple.security.personal-information.calendars com.apple.security.personal-information.location com.apple.security.personal-information.photos-library
- IDE quarantine attribute: 03c1;6ab248d6;Chrome;16D71B7F-4F97-448D-B5AB-6E6069C4351A
- IDE JNA directory: `/Applications/IntelliJ IDEA.app/Contents/lib/jna/aarch64`
- Extension API jar: `/Users/martinsafsten/Library/Application Support/JetBrains/IntelliJIdea2026.2/plugins/CodeScene/lib/api-1.0-fdef705.jar`

### Default JNA unpack

- Result: `PASS` (exit 0)
- No `LinkageError` or `UnsatisfiedLinkError`.

### IDE-bundled libjnidispatch

- Result: `PASS` (exit 0)
- JVM options: `-Djna.boot.library.path=/var/folders/fd/3ht8nz_501d0pdjcx03fbmpc0000gn/T/cs-ide-jna.hsm72jlUIx/jna -Djna.nounpack=true`
- No `LinkageError` or `UnsatisfiedLinkError`.

### Verdict

Default unpack passes, so Phase 3 needs no special JNA setup.

No `codescene.cli.ide` process was left running.
