BB := bb
ifeq ($(OS),Windows_NT)
GRADLEW := .\gradlew.bat
else
GRADLEW := ./gradlew
endif

.PHONY: install-cli check-bb build test format format-check delta iter 

install-cli: check-bb
	@$(BB) -f .github/install-cli.clj

check-bb:
	@$(BB) --version

KT_FILES := $(wildcard src/main/kotlin/**/*.kt) \
            $(wildcard src/test/kotlin/**/*.kt) \
            $(wildcard core/src/main/kotlin/**/*.kt) \
            $(wildcard core/src/test/kotlin/**/*.kt)
GRADLE_FILES := $(wildcard build.gradle.kts) $(wildcard core/build.gradle.kts) $(wildcard settings.gradle.kts) $(wildcard gradle.properties) $(wildcard gradle/libs.versions.toml)

.build-timestamp: check-bb $(KT_FILES) $(GRADLE_FILES)
	@$(BB) -f .github/run-quiet.clj build.log $(GRADLEW) --rerun-tasks --warn buildPlugin
	@$(BB) -e "(println (str \"Build completed at \" (java.time.Instant/now)))" > .build-timestamp

build: .build-timestamp

test: check-bb
	@$(BB) -f .github/run-quiet.clj test.log $(GRADLEW) test

format: check-bb
	@$(BB) -f .github/run-quiet.clj format.log $(GRADLEW) ktlintFormat
	@$(BB) -f .github/run-quiet.clj format.log $(GRADLEW) core:ktlintFormat

format-check: check-bb
	@$(BB) -f .github/run-quiet.clj format-check.log $(GRADLEW) --console=plain -PktlintFailOnError=true ktlintCheck

delta: check-bb install-cli
	@$(BB) -f .github/run-quiet.clj delta.log cs delta master --error-on-warnings

iter: format-check delta build test 
