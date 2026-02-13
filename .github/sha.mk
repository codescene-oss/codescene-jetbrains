# sha.mk - Cache key computation
#
# Computes a cache key based on the content of all .kt, .kts, gradle.properties,
# and libs.versions.toml files (both tracked and untracked). Uses git object hashes.
#
# The cache key format is: <files-sha-12> (12 characters)

define get_cache_key
$(shell $(BB) -f .github/get-cache-key.clj)
endef
