BB := bb
GRADLEW := $(shell $(BB) -e "(require '[babashka.fs :as fs]) (println (if (fs/windows?) \".\\\\gradlew.bat\" \"./gradlew\"))")

include .github/cache.mk
include .github/sha.mk

CACHE_KEY = $(eval CACHE_KEY := $$(call get_cache_key))$(CACHE_KEY)

.PHONY: build test test-mine format-mine format-all format-check format-check-mine delta install-cli iter clean-cache check-bb

check-bb:
	@$(BB) --version

KT_FILES := $(wildcard src/main/kotlin/*/*.kt) \
            $(wildcard src/main/kotlin/*/*/*.kt) \
            $(wildcard src/main/kotlin/*/*/*/*.kt) \
            $(wildcard src/main/kotlin/*/*/*/*/*.kt) \
            $(wildcard src/main/kotlin/*/*/*/*/*.kt) \
            $(wildcard src/test/kotlin/*/*.kt) \
            $(wildcard src/test/kotlin/*/*/*.kt) \
            $(wildcard src/test/kotlin/*/*/*/*.kt)
GRADLE_FILES := $(wildcard build.gradle.kts) $(wildcard settings.gradle.kts) $(wildcard gradle.properties) $(wildcard gradle/libs.versions.toml)

.build-timestamp: check-bb $(KT_FILES) $(GRADLE_FILES)
	@$(BB) -f .github/run-quiet.clj build.log bb -f .github/gradlew-run.clj buildPlugin
	@$(BB) -e "(println (str \"Build completed at \" (java.time.Instant/now)))" > .build-timestamp

build: .build-timestamp

test: check-bb build
	@$(BB) -f .github/run-quiet.clj test.log bb -f .github/cache.clj $(CACHE_KEY) '$(GRADLEW) check' $(CACHE_DIR)

test-mine: check-bb build
	@$(BB) -f .github/run-quiet.clj test-mine.log bb -f .github/cache.clj $(CACHE_KEY) '$(BB) -f .github/test-mine.clj' $(CACHE_DIR)

format-mine: check-bb
	@$(BB) -f .github/run-quiet.clj format.log bb -f .github/cache.clj $(CACHE_KEY) '$(BB) -f .github/format-mine.clj' $(CACHE_DIR)

format-all: check-bb
	@$(BB) -f .github/run-quiet.clj format.log bb -f .github/cache.clj $(CACHE_KEY) '$(GRADLEW) ktlintFormat' $(CACHE_DIR)

format-check: check-bb
	@$(BB) -f .github/run-quiet.clj format-check.log bb -f .github/cache.clj $(CACHE_KEY) '$(GRADLEW) ktlintCheck' $(CACHE_DIR)

format-check-mine: check-bb
	@$(BB) -f .github/run-quiet.clj format-check.log bb -f .github/format-check-mine.clj

install-cli: check-bb
	@$(BB) -f .github/install-cli.clj

delta: check-bb install-cli
	@$(BB) -f .github/run-quiet.clj delta.log bb -f .github/cache.clj $(CACHE_KEY) 'cs delta master --error-on-warnings' $(CACHE_DIR)

iter: build delta test-mine format-check-mine
