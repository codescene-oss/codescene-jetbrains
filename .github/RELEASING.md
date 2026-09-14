# Releasing CodeScene for JetBrains

Releases are created from annotated Git tags. GitHub Actions validates the tag, builds the plugin with the tagged version, and distributes the resulting ZIP.

## Requirements

- Install [Babashka](https://babashka.org).
- Start from a clean Git worktree.
- Keep `pluginVersion` in `gradle.properties` equal to the latest reachable stable tag.
- Set `VISUAL` or `EDITOR`, or make `code` available on `PATH`, before creating a stable release.

## Stable releases

Choose the semantic-version increment explicitly:

```bash
make release BUMP=patch
make release BUMP=minor
make release BUMP=major
```

The command:

1. Verifies that `pluginVersion` matches the latest stable `vX.Y.Z` tag.
2. Calculates the next stable version.
3. Updates `gradle.properties`.
4. Generates a `CHANGELOG.md` section from commits since the latest stable tag.
5. Opens the changelog for review.
6. Creates a `chore(release): vX.Y.Z` commit and annotated `vX.Y.Z` tag.

Enter `q` at the confirmation prompt to abort. Both `gradle.properties` and `CHANGELOG.md` are restored.

Push the release commit and tag:

```bash
git push --follow-tags
```

GitHub Actions requires the stable tag version to equal the committed `pluginVersion`. A valid stable tag publishes the plugin to JetBrains Marketplace and creates a normal GitHub release.

## Test releases

The default increment is patch:

```bash
make test-release
```

Minor and major test releases can be requested explicitly:

```bash
make test-release BUMP=minor
make test-release BUMP=major
```

The command derives the next version from the committed stable `pluginVersion`, appends `-test.<short-sha>`, and creates an annotated tag such as `v0.5.6-test.a1b2c3d`. It does not change `gradle.properties` or `CHANGELOG.md`.

Push the exact tag printed by the command:

```bash
git push origin v0.5.6-test.a1b2c3d
```

GitHub Actions verifies that:

- the tag matches `vX.Y.Z-test.<hex-sha>`;
- the SHA suffix identifies the tagged commit;
- `pluginVersion` matches the latest reachable stable tag;
- the test base is exactly the next patch, minor, or major version.

The test version is injected with `-PreleaseVersion`, so the ZIP filename and plugin descriptor both contain the full test version. Test builds are GitHub prereleases, are never marked as the latest release, and are never published to JetBrains Marketplace.

## Release notes

GitHub generates release notes from explicit comparison baselines:

- stable releases compare with the previous reachable stable tag;
- the first test release for a version compares with the latest reachable stable tag;
- later test releases for the same version compare with the preceding same-base test tag.

Test prereleases also prepend a cumulative link comparing the latest stable tag with the current test tag.

## Installing a test release

1. Open the test release on the repository's GitHub Releases page.
2. Download `CodeScene-X.Y.Z-test.<short-sha>.zip`.
3. In the JetBrains IDE, open **Settings | Plugins**.
4. Open the gear menu and choose **Install Plugin from Disk**.
5. Select the downloaded ZIP and restart the IDE if requested.
6. Open the CodeScene plugin details in **Settings | Plugins** and verify that the displayed version matches the GitHub tag.

Artifacts uploaded by the regular build workflow are CI diagnostics. Tagged GitHub prereleases are the supported tester distribution channel.

## Validation failures

Malformed tags, mismatched versions, dirty worktrees, duplicate tags, and SHA mismatches stop the release. Correct the source state and create a new valid tag; do not bypass the validation in GitHub Actions.
