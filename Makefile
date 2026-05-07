BB := bb
ifeq ($(OS),Windows_NT)
NULL := NUL
IDEA_LOG := build\idea-sandbox\*\log\idea.log
else
NULL := /dev/null
IDEA_LOG := build/idea-sandbox/*/log/idea.log
endif

.PHONY: install-cli check-bb build test benchmarks format format-check delta iter coverage-summary bump-version release test-release class-size-mine run-ide kill-ide logs rm-nul

install-cli: check-bb
	@$(BB) -f .github/install-cli.clj

check-bb:
	@$(BB) --version > $(NULL) 2>&1

KT_FILES := $(wildcard src/main/kotlin/**/*.kt) \
            $(wildcard src/test/kotlin/**/*.kt) \
            $(wildcard core/src/main/kotlin/**/*.kt) \
            $(wildcard core/src/test/kotlin/**/*.kt) \
            $(wildcard benchmarks/src/jmh/kotlin/**/*.kt)
GRADLE_FILES := $(wildcard build.gradle.kts) $(wildcard core/build.gradle.kts) $(wildcard benchmarks/build.gradle.kts) $(wildcard settings.gradle.kts) $(wildcard gradle.properties) $(wildcard gradle/libs.versions.toml)

.build-timestamp: check-bb $(KT_FILES) $(GRADLE_FILES)
ifeq ($(OS),Windows_NT)
	@$(BB) -f .github/run-quiet.clj build.log "gradle buildPlugin" cmd //c ".\\gradlew.bat --rerun-tasks --warn buildPlugin"
else
	@$(BB) -f .github/run-quiet.clj build.log "gradle buildPlugin" ./gradlew --rerun-tasks --warn buildPlugin
endif
	@$(BB) -e "(println (str \"Build completed at \" (java.time.Instant/now)))" > .build-timestamp

build: .build-timestamp

test: kill-ide check-bb build
ifeq ($(OS),Windows_NT)
	@$(BB) -f .github/run-quiet.clj test.log "gradle test" cmd //c ".\\gradlew.bat test"
else
	@$(BB) -f .github/run-quiet.clj test.log "gradle test" ./gradlew test
endif

benchmarks: check-bb
	@$(BB) -f .github/run-quiet.clj benchmarks.log "gradle benchmarks" $(GRADLEW) :benchmarks:jmh

format: check-bb
ifeq ($(OS),Windows_NT)
	@$(BB) -f .github/run-quiet.clj format.log "gradle ktlintFormat" cmd //c ".\\gradlew.bat ktlintFormat"
	@$(BB) -f .github/run-quiet.clj format.log "gradle core:ktlintFormat" cmd //c ".\\gradlew.bat core:ktlintFormat"
else
	@$(BB) -f .github/run-quiet.clj format.log "gradle ktlintFormat" ./gradlew ktlintFormat
	@$(BB) -f .github/run-quiet.clj format.log "gradle core:ktlintFormat" ./gradlew core:ktlintFormat
endif

format-check: check-bb
ifeq ($(OS),Windows_NT)
	@$(BB) -f .github/run-quiet.clj format-check.log "gradle ktlintCheck" cmd //c ".\\gradlew.bat --console=plain -PktlintFailOnError=true ktlintCheck"
else
	@$(BB) -f .github/run-quiet.clj format-check.log "gradle ktlintCheck" ./gradlew --console=plain -PktlintFailOnError=true ktlintCheck
endif

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

kill-ide:
ifeq ($(OS),Windows_NT)
	@powershell -ExecutionPolicy Bypass -File .github/kill-ide.ps1 > $(NULL) 2>&1 || exit 0
else
	@.github/kill-ide.sh > $(NULL) 2>&1 || true
endif

run-ide:
ifeq ($(OS),Windows_NT)
	@powershell -Command "Remove-Item -Force -ErrorAction SilentlyContinue '$(IDEA_LOG)'"
	cmd //c ".\\gradlew.bat runIde"
else
	@rm -f $(IDEA_LOG)
	./gradlew runIde
endif

iter: format format-check class-size-mine delta test

logs:
ifeq ($(OS),Windows_NT)
	@powershell -Command "Get-Content (Get-ChildItem '$(IDEA_LOG)')[0].FullName | Select-String -SimpleMatch 'codescene.jetbrains'; Get-Content -Wait -Tail 0 (Get-ChildItem '$(IDEA_LOG)')[0].FullName | Select-String -SimpleMatch 'codescene.jetbrains'"
else
	@tail -n +1 -f $(IDEA_LOG) | grep -F "codescene.jetbrains"
endif

rm-nul:
	@rm -f NUL
