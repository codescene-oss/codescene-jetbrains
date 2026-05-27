# CodeScene - Code Analysis and Refactoring

<!-- Plugin description -->
[CodeScene](https://www.codescene.com) is the only code analysis tool with a **proven business impact**. 

## What is CodeScene for JetBrains? ##
It serves as a **safeguard** against introducing code changes that could negatively affect your business outcomes.

CodeScene promotes **healthy, maintainable code** by providing clear, actionable insights and guidance on how to improve your codebase.

With an add-on **Automatic Refactoring with ACE**, CodeScene can even automate parts of the improvement process—helping developers enhance code quality and maintainability faster and more accurately.

By using CodeScene, developers can spend less time deciphering and refactoring code, and more time focusing on what truly matters: **solving problems and delivering value**.

## The CodeScene CodeHealth™ metric ##
CodeScene’s [CodeHealth™](https://codescene.io/docs/guides/technical/code-health.html) metric is the software industry’s only code-level metric with proven business impact, measured through fact-based, winning research. It’s a metric that you can trust. The extension analyses and scores your code as you type, and adds diagnostic items that highlights any [code smells](#code-smells).
CodeScene supports [most popular languages](https://codescene.io/docs/usage/language-support.html#supported-programming-languages).

## Safeguarding your code changes ##
Instant feedback is vital for maintaining high-quality code during development. By analyzing code as it’s written, developers may identify issues immediately. This interactive feedback loop encourages better coding habits, speeds up learning, and reduces the need for extensive rework later. 

Continuous feedback fosters a sense of flow and confidence, as you instantly can see the impact of your changes on overall code health. In short, interactive monitoring turns code quality from a delayed review process into a **continuous, integrated part of writing code**, ensuring long-term maintainability and faster, safer development.

The **Code Health Monitor** flags for drops in code health in real time and offers instant recommendations to keep your code maintainable.

> **_NOTE:_** _The Code Health Monitor is currently available to all users for a limited time period. However, this capability will become accessible only to CodeScene customers in future_.

## Automatic Refactoring in ACE ##
[CodeScene ACE](https://codescene.io/docs/auto-refactor/index.html) helps you with the hardest part of software development: maintaining and improving existing code. While other AI tools focus on code generation, CodeScene ACE fixes technical debt and refactors code smells directly in your IDE. With fact-based metrics, ACE focuses on the impactful improvements you can do now to simplify your tasks. Maintain momentum, and let ACE handle the heavy lifting for you.


> **_NOTE:_** _CodeScene ACE is an AI-Powered service that is not accessible by default. All AI technology is hosted seperately, and can only be utilized by consent of your organisation. It must be explicitly activated in order to become available via the CodeScene IDE extension. If you are interested in purchasing ACE for your organisation or just want to conduct a trial, please contact [Sales](https://codescene.com/product/talk-to-sales)_.

## Overview of Available Features ##
| Feature | Description |
|---------|-------------|
| **Code Health Monitor*** | The Code Health Monitor continuously tracks changes in your code, highlighting any improvements or degradations you introduce. Each file displays both its previous and current Code Health score, along with a clear delta value showing the overall change. You can easily see the impact of your modifications at the file or function level, including the status of any associated Code Smells—whether you’ve introduced new ones or resolved existing issues. With this level of visibility, there’s no longer any excuse for allowing Code Health to decline in your codebase. |
| **Automatic Refactoring**** | <div>CodeScene ACE enables automatic refactoring within the CodeScene IDE extension. Code Smells detected during code analysis can be flagged for automatic refactoring, and any changes identified by the Code Health Monitor that have refactoring potential are also considered. ACE selects the most suitable AI model for each task and validates the results to ensure that only relevant and high-quality refactorings are presented. Each suggested refactoring includes a confidence evaluation and highlights any potential concerns, giving developers clear guidance and control over improvements to their code.</div><p></p><div>These are the languages that ACE can refactor:</div><ul><li>Java</li><li>C#</li><li>C++</li><li>JavaScript</li><li>TypeScript</li><li>React additional support (works with JavaScript and TypeScript)</li></ul><div>ACE is currently capable to refactor a subset of Code Smells that may be detected:</div><ul><li>Complex Conditional</li><li>Bumpy Road Ahead</li><li>Complex Method</li><li>Deep, Nested Complexity</li><li>Large Method</li></ul> |
| **Inline Code Smell detection** | Code smells often lead to issues such as increased technical debt, more bugs, and reduced overall quality of the software. You can find detailed information for each code smell by either clicking the corresponding inline action notation in the editor, by examining the diagnostics (squigglies, Code Vision or in the Problems view).|
| **Refactoring Guidance** | Our mission is to educate and raise awareness about Code Health and its impact on your efficiency as a developer. The CodeScene extension equips you with rich insights into Code Smells and provides clear, actionable guidance on how to address issues that may exist in your codebase. We include relevant examples that illustrate the essence of Code Smells, along with common patterns and practical solutions to help you write cleaner, more maintainable code. |
| **Problems View** | When a file is opened in the editor, it is instantly scanned for existing Code Health issues. All discovered issues are then listed as warnings in the IDE Problems View. This way you instantly get an overview of all opportunities a file has for improvements. |
| **Custom Code Health rules** | To customize the code analysis you can either use local [Code Comment Directives](https://codescene.io/docs/guides/technical/code-health.html#disable-local-smells-via-code-comment-directives) or create a `code-health-rules.json` file which applies to the entire project. |


_* Available time-limited for non CodeScene customers._

_** Paid add-on feature._

## Android Studio Support

To use the CodeScene plugin in Android Studio, you must enable JCEF (Java Chromium Embedded Framework). The plugin
relies on JCEF, but it is not bundled with the default IDE runtime. Follow these steps to enable JCEF and view
documentation: [Markdown Editor and Preview Not Working in Android Studio](https://stackoverflow.com/questions/69171807/markdown-editor-and-preview-not-working-in-android-studio).

## Rider Support

The plugin is expected to work with Rider, but the level of support has not been fully assessed. Some features may
function as intended, while others could have limitations. If you encounter any issues, please open a support ticket
[here](https://supporthub.codescene.com/kb-tickets/new).

<!-- Plugin description end -->

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