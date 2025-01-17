package com.codescene.jetbrains.util

import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.openapi.diagnostic.Logger

/**
 * Logs are written to `idea.log`, located in the `build/idea-sandbox/system/log/`
 * directory during plugin development.
 *
 * To view logs, configure the log file in your Run/Debug configuration by
 * specifying the path and alias to display in the console.
 *
 * To view debug logs specifically, enable the appropriate debug settings
 * in the IDE by navigating to Help > Diagnostic Tools > Debug Log Settings
 * and adding the logger path, e.g., `#com.codescene.jetbrains.util.Log`.
 * Make sure to filter by all types of logs in the console, not just warnings.
 *
 * @see <a href="https://www.jetbrains.com/help/idea/setting-log-options.html#add_log">IntelliJ Logging Documentation</a>
 */

object Log {
    private val logger: Logger by lazy { Logger.getInstance(Log::class.java) }
    private const val PREFIX = "$CODESCENE -"

    fun info(message: String, service: String? = "") = logger.info("$PREFIX $service $message")

    fun warn(message: String, service: String? = "") = logger.warn("$PREFIX $service $message")

    fun debug(message: String, service: String? = "") = logger.debug("$PREFIX $service $message")

    fun error(message: String, service: String? = "") = logger.error("$PREFIX $service $message")
}