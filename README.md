# CodeScene JetBrains Plugin

<!-- Plugin description -->
**The Codescene JetBrains plugin** provides in-editor recommendations and suggested improvements, helping you write maintainable code and proactively address potential issues.
<!-- Plugin description end -->

### Table of contents

- [Getting started](#getting-started)
- [Gradle configuration](#gradle-configuration)
- [Plugin template structure](#plugin-template-structure)
- [Plugin configuration file](#plugin-configuration-file)
- [Testing](#testing)
  - [Functional tests](#functional-tests)
  - [Code coverage](#code-coverage)
  - [UI tests](#ui-tests)
- [Predefined Run/Debug configurations](#predefined-rundebug-configurations)
- [Continuous integration](#continuous-integration) based on GitHub Actions
  - [Dependencies management](#dependencies-management) with Dependabot
  - [Changelog maintenance](#changelog-maintenance) with the Gradle Changelog Plugin
  - [Release flow](#release-flow) using GitHub Releases
  - [Plugin signing](#plugin-signing) with your private certificate
  - [Publishing the plugin](#publishing-the-plugin) with the Gradle IntelliJ Plugin
- [Useful links](#useful-links)
- [License](#license)


## Getting started

### Clone the repository

Run the following command to clone the repository:

```bash
git clone git@github.com:empear-analytics/codescene-jetbrains.git
```

### Set JDK version to 17

After opening your project in *IntelliJ IDEA Community Edition* or *IntelliJ IDEA Ultimate*, the next step is to set the proper <kbd>SDK</kbd> to Java in version `17` within the [Project Structure settings][docs:project-structure-settings].

![Project Structure — SDK][file:project-structure-sdk.png] 

### Build the project

To install dependencies and build the project, run the following Gradle command:

```bash
./gradlew build
```

You can also run the build task from the Gradle menu in your IDE.

### Run the plugin

#### Gradle configuration

The recommended method for plugin development involves using the [Gradle][gradle] setup with the [gradle-intellij-plugin][gh:gradle-intellij-plugin] installed.
The `gradle-intellij-plugin` makes it possible to run the IDE with the plugin and publish it to JetBrains Marketplace.

A project built using the IntelliJ Platform Plugin Template includes a Gradle configuration already set up. To run the project, start the *Run Plugin* task:

![Run/Debug configurations][file:run-debug-configurations.png]

> **Note**
>
> Make sure to always upgrade to the latest version of `gradle-intellij-plugin`.


Alternatively, you can run the following command:

```bash
./gradlew runIde
```

### Environment variables

Environment variables used by the current project are related to the [plugin signing](#plugin-signing) and [publishing](#publishing-the-plugin).

| Environment variable name | Description                                                                                                  |
|---------------------------|--------------------------------------------------------------------------------------------------------------|
| `PRIVATE_KEY`             | Certificate private key, should contain: `-----BEGIN RSA PRIVATE KEY----- ... -----END RSA PRIVATE KEY-----` |
| `PRIVATE_KEY_PASSWORD`    | Password used for encrypting the certificate file.                                                           |
| `CERTIFICATE_CHAIN`       | Certificate chain, should contain: `-----BEGIN CERTIFICATE----- ... -----END CERTIFICATE----`                |
| `PUBLISH_TOKEN`           | Publishing token generated in your JetBrains Marketplace profile dashboard.                                  |


## Project structure

The CodeScene JetBrains project has the following content structure:

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
│   │   └── resources/      Resources - plugin.xml, icons, messages
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
├── LICENSE                 License, MIT by default
├── README.md               README
└── settings.gradle.kts     Gradle project settings
```

In addition to the configuration files, the most crucial part is the `src` directory, which contains our implementation and the manifest for our plugin – [plugin.xml][file:plugin.xml].

## Testing - TBD

[Testing plugins][docs:testing-plugins] is an essential part of the plugin development to make sure that everything works as expected between IDE releases and plugin refactorings.
The CodeScene JetBrains plugin project provides integration of two testing approaches – functional and UI tests.

### Functional tests

//TBD

> **Note**
> 
> Run your tests using predefined *Run Tests* configuration or by invoking the `./gradlew check` Gradle task.

### UI tests

//TBD

> **Note**
> 
> Run IDE for UI tests using predefined *Run IDE for UI Tests* and then *Run Tests* configurations or by invoking the `./gradlew runIdeForUiTests` and `./gradlew check` Gradle tasks.

## Predefined Run/Debug configurations

Within the default project structure, there is a `.run` directory provided containing predefined *Run/Debug configurations* that expose corresponding Gradle tasks:

| Configuration name   | Description                                                                                                                                                                   |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Run Plugin           | Runs [`:runIde`][gh:gradle-intellij-plugin-runIde] Gradle IntelliJ Plugin task. Use the *Debug* icon for plugin debugging.                                                    |
| Run Verifications    | Runs [`:runPluginVerifier`][gh:gradle-intellij-plugin-runPluginVerifier] Gradle IntelliJ Plugin task to check the plugin compatibility against the specified IntelliJ IDEs.   |
| Run Tests            | Runs [`:test`][gradle:lifecycle-tasks] Gradle task.                                                                                                                           |
| Run IDE for UI Tests | Runs [`:runIdeForUiTests`][gh:intellij-ui-test-robot] Gradle IntelliJ Plugin task to allow for running UI tests within the IntelliJ IDE running instance.                     |

> **Note**
> 
> You can find the logs from the running task in the `idea.log` tab.
>
> ![Run/Debug configuration logs][file:run-logs.png]

## Continuous integration - TBD

Continuous integration depends on [GitHub Actions][gh:actions], a set of workflows that make it possible to automate your testing and release process.

In the `.github/workflows` directory, you can find definitions for the following GitHub Actions workflows:

- [Build](.github/workflows/build.yml)
  - Triggered on `push` and `pull_request` events.
  - Runs the *Gradle Wrapper Validation Action* to verify the wrapper's checksum.
  - Runs the `verifyPlugin` and `test` Gradle tasks.
  - Builds the plugin with the `buildPlugin` Gradle task and provides the artifact for the next jobs in the workflow.
  - Verifies the plugin using the *IntelliJ Plugin Verifier* tool.
  - Prepares a draft release of the GitHub Releases page for manual verification.
- [Release](.github/workflows/release.yml)
  - Triggered on `released` event.
  - Updates `CHANGELOG.md` file with the content provided with the release note.
  - Signs the plugin with a provided certificate before publishing.
  - Publishes the plugin to JetBrains Marketplace using the provided `PUBLISH_TOKEN`.
  - Sets publish channel depending on the plugin version, i.e. `1.0.0-beta` -> `beta` channel.
  - Patches the Changelog and commits.
- [Run UI Tests](.github/workflows/run-ui-tests.yml)
  - Triggered manually.
  - Runs for macOS, Windows, and Linux separately.
  - Runs `runIdeForUiTests` and `test` Gradle tasks.
- [Template Cleanup](.github/workflows/template-cleanup.yml)
  - Triggered once on the `push` event when a new template-based repository has been created.
  - Overrides the scaffold with files from the `.github/template-cleanup` directory.
  - Overrides JetBrains-specific sentences or package names with ones specific to the target repository.
  - Removes redundant files.

### Dependencies management

This project depends on Gradle plugins and external libraries.

All plugins and dependencies used by Gradle are managed with [Gradle version catalog][gradle:version-catalog], which defines versions and coordinates of dependencies in the [`gradle/libs.versions.toml`][file:libs.versions.toml] file.

> **Note**
>
> To add a new dependency to the project, in the `dependencies { ... }` block, add:
> 
> ```kotlin
> dependencies {
>   implementation(libs.annotations)
> }
> ```
> 
> and define the dependency in the [`gradle/libs.versions.toml`][file:libs.versions.toml] file as follows:
> ```toml
> [versions]
> annotations = "24.0.1"
> 
> [libraries]
> annotations = { group = "org.jetbrains", name = "annotations", version.ref = "annotations" }
> ```

Keeping the project in good shape and having all the dependencies up-to-date requires time and effort, but it is possible to automate that process using [Dependabot][gh:dependabot].

### Changelog maintenance - TBD

When releasing an update, it is essential to let your users know what the new version offers.
The best way to do this is to provide release notes.

The changelog is a curated list that contains information about any new features, fixes, and deprecations.
When they're provided, these lists are available in a few different places:
- the [CHANGELOG.md](./CHANGELOG.md) file,
- the [Releases page][gh:releases],
- the *What's new* section of JetBrains Marketplace Plugin page,
- and inside the Plugin Manager's item details.

### Plugin signing - TBD

Plugin Signing is a mechanism introduced in the 2021.2 release cycle to increase security in [JetBrains Marketplace](https://plugins.jetbrains.com) and all of our IntelliJ-based IDEs.

JetBrains Marketplace signing is designed to ensure that plugins aren't modified over the course of the publishing and delivery pipeline.

The current project provides a predefined plugin signing configuration that lets you sign and publish your plugin from the Continuous Integration (CI) and local environments.
All the configuration related to the signing should be provided using [environment variables](#environment-variables).

To find out how to generate signing certificates, check the [Plugin Signing][docs:plugin-signing] section in the IntelliJ Platform Plugin SDK documentation.

> **Note**
>
> Remember to encode your secret environment variables using `base64` encoding to avoid issues with multi-line values.

### Publishing the plugin - TBD

> **Tip**
> 
> Make sure to follow all guidelines listed in [Publishing a Plugin][docs:publishing] to follow all recommended and required steps.

Releasing a plugin to [JetBrains Marketplace](https://plugins.jetbrains.com) is a straightforward operation that uses the `publishPlugin` Gradle task provided by the [gradle-intellij-plugin][gh:gradle-intellij-plugin-docs].
In addition, the [Release](.github/workflows/release.yml) workflow automates this process by running the task when a new release appears in the GitHub Releases section.

> **Note**
> 
> Set a suffix to the plugin version to publish it in the custom repository channel, i.e. `v1.0.0-beta` will push your plugin to the `beta` [release channel][docs:release-channel].

The authorization process relies on the `PUBLISH_TOKEN` secret environment variable, specified in the _Secrets_ section of the repository _Settings_.

You can get that token in your JetBrains Marketplace profile dashboard in the [My Tokens][jb:my-tokens] tab.

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
[docs:publishing]: https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate
[docs:release-channel]: https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate#specifying-a-release-channel
[docs:plugin-signing]: https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate
[docs:project-structure-settings]: https://www.jetbrains.com/help/idea/project-settings-and-structure.html
[docs:testing-plugins]: https://plugins.jetbrains.com/docs/intellij/testing-plugins.html?from=IJPluginTemplate

[file:libs.versions.toml]: ./gradle/libs.versions.toml
[file:project-structure-sdk.png]: ./.github/readme/project-structure-sdk.png
[file:plugin.xml]: ./src/main/resources/META-INF/plugin.xml
[file:run-debug-configurations.png]: ./.github/readme/run-debug-configurations.png
[file:run-logs.png]: ./.github/readme/run-logs.png

[gh:actions]: https://help.github.com/en/actions
[gh:code-samples]: https://github.com/JetBrains/intellij-sdk-code-samples
[gh:dependabot]: https://docs.github.com/en/free-pro-team@latest/github/administering-a-repository/keeping-your-dependencies-updated-automatically
[gh:gradle-intellij-plugin]: https://github.com/JetBrains/gradle-intellij-plugin
[gh:gradle-intellij-plugin-docs]: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
[gh:gradle-intellij-plugin-runIde]: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#tasks-runide
[gh:gradle-intellij-plugin-runPluginVerifier]: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#tasks-runpluginverifier
[gh:intellij-ui-test-robot]: https://github.com/JetBrains/intellij-ui-test-robot
[gh:releases]: https://github.com/JetBrains/intellij-platform-plugin-template/releases

[gradle]: https://gradle.org
[gradle:kotlin-dsl]: https://docs.gradle.org/current/userguide/kotlin_dsl.html
[gradle:lifecycle-tasks]: https://docs.gradle.org/current/userguide/java_plugin.html#lifecycle_tasks
[gradle:version-catalog]: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog