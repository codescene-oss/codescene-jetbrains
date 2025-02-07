# CodeScene - Code Analysis

<!-- Plugin description -->
[CodeScene](https://www.codescene.com) - the only code analysis tool with a proven business impact.

## Code Health Monitor

Track code health in real-time as you work by using the Monitor.

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
proper <kbd>SDK</kbd> to Java in version `17` within the [Project Structure settings][docs:project-structure-settings].

![Project Structure — SDK][file:project-structure-sdk.png]

### Build the project

To install dependencies and build the project, run the following Gradle command:

```bash
./gradlew build
```

You can also run the build task from the Gradle menu in your IDE.

### Run the plugin

#### Gradle configuration

The recommended method for plugin development involves using the [Gradle][gradle] setup with
the [gradle-intellij-plugin][gh:gradle-intellij-plugin] installed.
The `gradle-intellij-plugin` makes it possible to run the IDE with the plugin and publish it to JetBrains Marketplace.

A project built using the IntelliJ Platform Plugin Template includes a Gradle configuration already set up. To run the
project, start the *Run Plugin* task:

![Run/Debug configurations][file:run-debug-configurations.png]

> **Note**
>
> Make sure to always upgrade to the latest version of `gradle-intellij-plugin`.

Alternatively, you can run the following command:

```bash
./gradlew runIde
```

## Documentation management

The documentation for the extension, including details about code smells and other features, is maintained in a
centralized repository: the *IDE Protocol repository*. This ensures consistency and reduces redundancy across different
IDE extensions (e.g., Visual Studio Code).

To fetch the latest documentation for local development, run the following command:

```bash
./gradlew fetchDocs
```

**Note:** Access to the documentation repository currently requires appropriate permissions.

## Project structure

The CodeScene project has the following content structure:

```
.
├── .github/                GitHub Actions workflows and Dependabot configuration files
├── .run/                   Predefined Run/Debug Configurations
├── build/                  Output build directory
├── gradle
│   ├── wrapper/            Gradle Wrapper
│   └── libs.versions.toml  Gradle version catalog
├── src                     Plugin sources
│   ├── main
│   │   ├── kotlin/         Kotlin production sources
│   │   └── resources/      Resources - plugin.xml, icons, messages, docs
│   └── test
│       ├── kotlin/         Kotlin test sources
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

In addition to the configuration files, the most crucial part is the `src` directory, which contains our implementation
and the manifest for our plugin – [plugin.xml][file:plugin.xml].

## Predefined Run/Debug configurations

Within the default project structure, there is a `.run` directory provided containing predefined *Run/Debug
configurations* that expose corresponding Gradle tasks:

| Configuration name   | Description                                                                                                                                                                 |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Run Plugin           | Runs [`:runIde`][gh:gradle-intellij-plugin-runIde] Gradle IntelliJ Plugin task. Use the *Debug* icon for plugin debugging.                                                  |
| Run Verifications    | Runs [`:runPluginVerifier`][gh:gradle-intellij-plugin-runPluginVerifier] Gradle IntelliJ Plugin task to check the plugin compatibility against the specified IntelliJ IDEs. |
| Run Tests            | Runs [`:test`][gradle:lifecycle-tasks] Gradle task.                                                                                                                         |
| Run IDE for UI Tests | Runs [`:runIdeForUiTests`][gh:intellij-ui-test-robot] Gradle IntelliJ Plugin task to allow for running UI tests within the IntelliJ IDE running instance.                   |

> **Note**
>
> You can find the logs from the running task in the `idea.log` tab.
>
> ![Run/Debug configuration logs][file:run-logs.png]

## Useful links

- [IntelliJ Platform SDK Plugin SDK][docs]
- [Gradle IntelliJ Plugin Documentation][gh:gradle-intellij-plugin-docs]
- [Kotlin UI DSL][docs:kotlin-ui-dsl]
- [IntelliJ SDK Code Samples][gh:code-samples]
- [GitHub Actions][gh:actions]

## License

See LICENSE file.

[docs]: https://plugins.jetbrains.com/docs/intellij?from=IJPluginTemplate

[docs:kotlin-ui-dsl]: https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html?from=IJPluginTemplate

[docs:project-structure-settings]: https://www.jetbrains.com/help/idea/project-settings-and-structure.html

[file:project-structure-sdk.png]: ./.github/readme/project-structure-sdk.png

[file:plugin.xml]: ./src/main/resources/META-INF/plugin.xml

[file:run-debug-configurations.png]: ./.github/readme/run-debug-configurations.png

[file:run-logs.png]: ./.github/readme/run-logs.png

[gh:actions]: https://help.github.com/en/actions

[gh:code-samples]: https://github.com/JetBrains/intellij-sdk-code-samples

[gh:gradle-intellij-plugin]: https://github.com/JetBrains/gradle-intellij-plugin

[gh:gradle-intellij-plugin-docs]: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html

[gh:gradle-intellij-plugin-runIde]: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#tasks-runide

[gh:gradle-intellij-plugin-runPluginVerifier]: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#tasks-runpluginverifier

[gradle]: https://gradle.org

[gradle:lifecycle-tasks]: https://docs.gradle.org/current/userguide/java_plugin.html#lifecycle_tasks