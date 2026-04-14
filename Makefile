BB := bb
ifeq ($(OS),Windows_NT)
GRADLEW := .\gradlew.bat
NULL := NUL
else
GRADLEW := ./gradlew
NULL := /dev/null
endif

.PHONY: install-cli check-bb build test format format-check delta iter coverage-summary bump-version release test-release class-size-mine

install-cli: check-bb
	@$(BB) -f .github/install-cli.clj

check-bb:
	@$(BB) --version > $(NULL) 2>&1

KT_FILES := $(wildcard src/main/kotlin/**/*.kt) \
            $(wildcard src/test/kotlin/**/*.kt) \
            $(wildcard core/src/main/kotlin/**/*.kt) \
            $(wildcard core/src/test/kotlin/**/*.kt)
GRADLE_FILES := $(wildcard build.gradle.kts) $(wildcard core/build.gradle.kts) $(wildcard settings.gradle.kts) $(wildcard gradle.properties) $(wildcard gradle/libs.versions.toml)

.build-timestamp: check-bb $(KT_FILES) $(GRADLE_FILES)
	@$(BB) -f .github/run-quiet.clj build.log "gradle buildPlugin" $(GRADLEW) --rerun-tasks --warn buildPlugin
	@$(BB) -e "(println (str \"Build completed at \" (java.time.Instant/now)))" > .build-timestamp

build: .build-timestamp

test: check-bb build
	@$(BB) -f .github/run-quiet.clj test.log "gradle test" $(GRADLEW) test

format: check-bb
	@$(BB) -f .github/run-quiet.clj format.log "gradle ktlintFormat" $(GRADLEW) ktlintFormat
	@$(BB) -f .github/run-quiet.clj format.log "gradle core:ktlintFormat" $(GRADLEW) core:ktlintFormat

format-check: check-bb
	@$(BB) -f .github/run-quiet.clj format-check.log "gradle ktlintCheck" $(GRADLEW) --console=plain -PktlintFailOnError=true ktlintCheck

delta: check-bb install-cli
	@$(BB) -f .github/run-quiet.clj delta.log "cs delta" cs delta master --error-on-warnings

coverage-summary: check-bb
	@$(BB) -f .github/coverage-summary.clj


bump-version: check-bb
	@$(BB) .github/release.clj bump-version "$(BUMP)"

release: check-bb
	@$(BB) .github/release.clj stable

test-release: check-bb
	@$(BB) .github/release.clj test

class-size-mine: check-bb
	@$(BB) -f .github/check-class-size-mine.clj

iter: format format-check class-size-mine delta test
