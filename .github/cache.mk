# cache.mk - Caching macro for arbitrary program execution
#
# Usage:
#   $(call call_cached,CACHE_KEY,COMMAND)
#
# Parameters:
#   CACHE_KEY - Unique identifier for caching this specific command execution
#   COMMAND   - The command to execute (with arguments)
#
# Cache structure:
#   .cache/CACHE_KEY/<SHA256-of-command>/stdout
#   .cache/CACHE_KEY/<SHA256-of-command>/stderr
#   .cache/CACHE_KEY/<SHA256-of-command>/exitcode
#
# To invalidate cache:
#   make clean-cache

CACHE_DIR := .cache

$(CACHE_DIR):
	@$(BB) -e "(require '[babashka.fs :as fs]) (fs/create-dirs \"$(CACHE_DIR)\")"

define call_cached
	@$(BB) -f .github/cache.clj $(1) '$(2)' $(CACHE_DIR)
endef

.PHONY: clean-cache
clean-cache:
	@$(BB) -e "(require '[babashka.fs :as fs]) (when (fs/exists? \"$(CACHE_DIR)\") (fs/delete-tree \"$(CACHE_DIR)\"))"
