package com.codescene.jetbrains.featureflag

import java.util.*

/**
 * Centralized feature flags for the plugin.
 *
 * Reads properties from two sources:
 * 1. `release-flags.properties` – the default flags shipped with the plugin, used in production.
 * 2. `local-flags.properties` – optional local overrides for development. This file is gitignored.
 *
 * Local flags override release flags if present.
 */

object FeatureFlagManager {
    private val flags: Properties = Properties().apply {
        this@FeatureFlagManager.javaClass.classLoader.getResourceAsStream("release-flags.properties")?.use { load(it) }
        this@FeatureFlagManager.javaClass.classLoader.getResourceAsStream("local-flags.properties")?.use { load(it) }
    }

    fun isEnabled(key: String, default: Boolean = false) = flags.getProperty(key)?.toBoolean() ?: default

    fun getString(key: String, default: String? = null) = flags.getProperty(key) ?: default

    fun getInt(key: String, default: Int = 0) = flags.getProperty(key)?.toIntOrNull() ?: default
}