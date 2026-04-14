# CodeScene - Code Analysis and Refactoring



[CodeScene](https://www.codescene.com) - the only code analysis tool with a proven business impact.

## Code Health Monitor

Track code health in real-time as you work. The Monitor highlights code smells and shows you opportunities where to improve your code.

## Code Health Analysis

CodeScene’s [Code Health](https://codescene.io/docs/guides/technical/code-health.html) metric is the software industry’s
only code-level metric with proven business impact, measured through fact-based, winning research. It’s a metric that
you can trust.

The extension analyses and scores your code as you type, and adds diagnostic items that highlights
any code smells.

### Language support

CodeScene
supports [most popular languages](https://codescene.io/docs/usage/language-support.html#supported-programming-languages).

## Code Smells

Code smells often lead to issues such as increased technical debt, more bugs, and reduced overall quality of the
software.

You can find detailed information for each code smell by either clicking the corresponding code vision in the editor, by
examining the diagnostics (squigglies or in the Problems view), or by using the corresponding intention action (light
bulb).

## Android Studio Support

To use the CodeScene plugin in Android Studio, you must enable JCEF (Java Chromium Embedded Framework). The plugin
relies on JCEF, but it is not bundled with the default IDE runtime. Follow these steps to enable JCEF and view
documentation: [Markdown Editor and Preview Not Working in Android Studio](https://stackoverflow.com/questions/69171807/markdown-editor-and-preview-not-working-in-android-studio).

## Rider Support

The plugin is expected to work with Rider, but the level of support has not been fully assessed. Some features may
function as intended, while others could have limitations. If you encounter any issues, please open a support ticket
[here](https://supporthub.codescene.com/kb-tickets/new).

## Do you want to keep using CodeScene ACE?

CodeScene ACE, our AI-powered refactoring agent, was free during beta but will now be offered as an add-on to
the extension. If you’re interested in continuing to use CodeScene ACE or would like to share feedback,
[reach out](https://codescene.com/contact-us-about-codescene-ace) to our Sales team.



## Table of contents

- [Getting started](#getting-started)
- [Documentation management](#documentation-management)
- [Project structure](#project-structure)
- [Predefined Run/Debug configurations](#predefined-rundebug-configurations)
- [Useful links](#useful-links)
- [License](#license)

## Getting started

### Clone the repository

Run the following command to clone the repository:

```bash
git clone git@github.com:empear-analytics/codescene-jetbrains.git
```

### Set JDK version to 17

After opening your project in *IntelliJ IDEA Community Edition* or *IntelliJ IDEA Ultimate*, the next step is to set the
proper SDK to Java in version `17` within the [Project Structure settings](https://www.jetbrains.com/help/idea/project-settings-and-structure.html).

Project Structure — SDK

### Build the project

To install dependencies and build the project, run the following Gradle command:

```bash
./gradlew build
```

You can also run the build task from the Gradle menu in your IDE.

### Make targets (requires Babashka)

The Makefile provides development targets for build, test, format, and CodeScene delta analysis. [Babashka](https://babashka.org) must be installed first.


| Target                        | Description                                        |
| ----------------------------- | -------------------------------------------------- |
| `make build`                  | Build the plugin (cached)                          |
| `make test`                   | Run all tests                                      |
| `make format`                 | Format all Kotlin files with ktlint (both modules) |
| `make format-check`           | Check format of all Kotlin files                   |
| `make delta`                  | Run CodeScene delta analysis (requires `cs` CLI)   |
| `make install-cli`            | Install CodeScene CLI (`cs`)                       |
| `make iter`                   | Run format-check, delta, build, and test           |
| `make bump-version BUMP=patch | minor                                              |
| `make release`                | Prepare a stable release commit and tag            |
| `make test-release`           | Create a tagged test release from `HEAD`           |


### Release commands

The release flow is tag-driven. Prepare the release locally, then push the annotated tag and let GitHub Actions build from that tag.

- `make bump-version BUMP=minor`
  - increments the current base version in `gradle.properties`
  - does not create a commit, tag, or GitHub release
  - lets the branch move to the next planned stable base version before any release is cut
- `make release`
  - uses the existing base version from `gradle.properties`
  - generates a draft release section from commits since the latest non-test tag (skips `*-test.<sha>` tags)
  - filters out `Merge*` and `chore*` commits
  - groups conventional commits into `Added`, `Fixed`, and `Changed`
  - opens `CHANGELOG.md` for manual cleanup
  - creates a release commit and an annotated `v<baseVersion>` tag
- `make test-release`
  - keeps `gradle.properties` unchanged
  - derives a version like `<baseVersion>-test.<shortSha>`
  - uses the current `CHANGELOG.md` `Unreleased` section as the test release notes
  - creates an annotated `v<baseVersion>-test.<shortSha>` tag on the current commit
  - does not modify or open `CHANGELOG.md`

Push prepared tags with:

```bash
git push --follow-tags
```

Set `VISUAL` or `EDITOR`, or make sure `code` is available on `PATH`, before running the release commands.

### Example release checklist

1. Start the next release line with `make bump-version BUMP=minor` or `make bump-version BUMP=major`.
2. Commit the base version change when appropriate for the branch.
3. Create GitHub-only tester drops during development with `make test-release`, then `git push --follow-tags`.
4. When the release is ready, run `make release`.
5. Review and clean up `CHANGELOG.md` in the editor that opens.
6. Push the release commit and tag with `git push --follow-tags`.

### Run the plugin

#### Gradle configuration

The recommended method for plugin development involves using the [Gradle](https://gradle.org) setup with
the [gradle-intellij-plugin](https://github.com/JetBrains/gradle-intellij-plugin) installed.
The `gradle-intellij-plugin` makes it possible to run the IDE with the plugin and publish it to JetBrains Marketplace.

A project built using the IntelliJ Platform Plugin Template includes a Gradle configuration already set up. To run the
project, start the *Run Plugin* task:

Run/Debug configurations

> **Note**
>
> Make sure to always upgrade to the latest version of `gradle-intellij-plugin`.

Alternatively, you can run the following command:

```bash
./gradlew runIde
```

## Webview Framework (CWF) management

The Centralized Webviews Framework (CWF) used by the extension, which provides shared webview components across all supported IDEs, is maintained in a private repository.

To fetch the latest CWF necessary for **local** development, run the following command:

```bash
./gradlew fetchCwf
```

**Note:** Access to the CWF repository requires appropriate permissions.

## Feature Flags

This plugin supports simple feature flags that can be controlled at build time or when running the plugin in the IDE sandbox.
Feature flags allow you to enable or disable experimental functionality without modifying the source code.

### How feature flags work

Feature flags are defined in `src/main/resources/feature-flags.properties` using placeholders.

During the build, Gradle replaces these placeholders with actual values. This is done in the `processResources` task.

The generated file is packaged inside the plugin. At runtime, the plugin reads these values using the `RuntimeFlags` object.

### Using feature flags during development (runIde)

To test feature flags locally using the JetBrains Gradle plugin, pass the flags when launching the sandboxed IDE:

```bash
./gradlew runIde -PFEATURE_CWF_DEVMODE=true
```

### Using feature flags when building the plugin (buildPlugin)

Feature flags can also be set during the plugin build so that the resulting plugin artifact contains the desired configuration. For example:

```bash
./gradlew buildPlugin -PFEATURE_CWF_DEVMODE=false
```

If a property is not provided, it defaults to false.


| Flag name           | Description                                                                        |
| ------------------- | ---------------------------------------------------------------------------------- |
| FEATURE_CWF_DEVMODE | Allows easier debugging of CWF payloads, enables opening devtools in the webviews. |


## Project structure

The CodeScene project uses a multi-module architecture with a clear separation between core business logic and platform-specific code:

```
.
├── .github/                GitHub Actions workflows and Dependabot configuration files
├── .run/                   Predefined Run/Debug Configurations
├── build/                  Output build directory
├── core/                   Core module - platform-independent business logic
│   ├── src/main/kotlin/    Core production sources (contracts, models, services)
│   └── src/test/kotlin/    Core tests and test doubles
├── gradle
│   ├── wrapper/            Gradle Wrapper
│   └── libs.versions.toml  Gradle version catalog
├── src                     Platform module - IntelliJ Platform integration
│   ├── main
│   │   ├── kotlin/         Platform-specific Kotlin sources
│   │   └── resources/      Resources - plugin.xml, icons, messages, docs
│   └── test
│       ├── kotlin/         Platform-specific test sources
│       └── testData/       Test data used by tests
├── .gitignore              Git ignoring rules
├── build.gradle.kts        Gradle configuration
├── CHANGELOG.md            Full change history
├── DECISIONLOG.md          Full decision history
├── gradle.properties       Gradle configuration properties
├── gradlew                 *nix Gradle Wrapper script
├── gradlew.bat             Windows Gradle Wrapper script
├── LICENSE                 License
├── README.md               README
└── settings.gradle.kts     Gradle project settings
```

### Architecture overview

- `**core/**` - Contains all platform-independent business logic, including:
  - `contracts/` - Interfaces for dependency inversion (services, caches, etc.)
  - `models/` - Data models, settings, and view data
  - `review/` - Code review and analysis orchestration
  - `delta/` - Delta analysis services
  - `handler/` - Message routing and action handling
  - `mapper/` - Data transformation logic
  - `util/` - Shared utilities
  - `testdoubles/` - In-memory implementations for testing
- `**src/**` - Contains IntelliJ Platform-specific code in `com.codescene.jetbrains.platform`:
  - IDE service implementations
  - Editor integrations (annotators, code vision, intentions)
  - UI components (settings, tool windows, webviews)
  - Platform-specific utilities

The plugin manifest is located at [plugin.xml](./src/main/resources/META-INF/plugin.xml).

## Predefined Run/Debug configurations

Within the default project structure, there is a `.run` directory provided containing predefined *Run/Debug
configurations* that expose corresponding Gradle tasks:


| Configuration name   | Description                                                                                                                                                                                                                           |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Run Plugin           | Runs `[:runIde](https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#tasks-runide)` Gradle IntelliJ Plugin task. Use the *Debug* icon for plugin debugging.                                                  |
| Run Verifications    | Runs `[:runPluginVerifier](https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#tasks-runpluginverifier)` Gradle IntelliJ Plugin task to check the plugin compatibility against the specified IntelliJ IDEs. |
| Run Tests            | Runs `[:test](https://docs.gradle.org/current/userguide/java_plugin.html#lifecycle_tasks)` Gradle task.                                                                                                                               |
| Run IDE for UI Tests | Runs [`:runIdeForUiTests`][gh:intellij-ui-test-robot] Gradle IntelliJ Plugin task to allow for running UI tests within the IntelliJ IDE running instance.                                                                             |


> **Note**
>
> You can find the logs from the running task in the `idea.log` tab.
>
> Run/Debug configuration logs

## Useful links

- [IntelliJ Platform SDK Plugin SDK](https://plugins.jetbrains.com/docs/intellij?from=IJPluginTemplate)
- [Gradle IntelliJ Plugin Documentation](https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html)
- [Kotlin UI DSL](https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html?from=IJPluginTemplate)
- [IntelliJ SDK Code Samples](https://github.com/JetBrains/intellij-sdk-code-samples)
- [GitHub Actions](https://help.github.com/en/actions)

## License

See LICENSE file.