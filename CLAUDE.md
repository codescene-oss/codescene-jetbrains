This project is a Jetbrains extension using intelliJPlatform, written in Kotlin.

Business and core logic should be implemented in the `core` sub-project, while any IntelliJ Platform dependent code belongs in `com.codescene.jetbrains.platform`. Keep code in the platform project to a minimum.

Don't add comments to any code you add, however keep any existing comments you find.

All commands for building, testing, linting etc are maintained at the Makefile. They're configured to not emit much output unless they fail; that's normal. Use a 10 minute timeout.

When you deem it necessary, run `make iter` which runs tests and linters related to the current set of Git changes.

If a linting issue is found, you must prioritize fixing it before resuming your previous intent.

This project uses the git4idea library. You can study its source at ~/intellij-community/plugins/git4idea . If that directory doesn't exist, git clone https://github.com/JetBrains/intellij-community and switch to tag idea/233.15619.7
