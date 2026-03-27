package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.util.formatLogMessage
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
 * and adding the logger path, e.g., `#com.codescene.jetbrains.platform.util.Log`.
 * CodeScene debug lines are tagged with a service suffix such as CodeSceneCodeVision,
 * CodeSceneDeltaCache, CodeSceneReviewCache, or CodeSceneCachedReview (filter in idea.log).
 * Make sure to filter by all types of logs in the console, not just warnings.
 *
 * @see <a href="https://www.jetbrains.com/help/idea/setting-log-options.html#add_log">IntelliJ Logging Documentation</a>
 */

object Log : ILogger {
    private val logger: Logger by lazy { Logger.getInstance(Log::class.java) }

    fun info(message: String) = info(message, "")

    override fun info(
        message: String,
        service: String?,
    ) = logger.info(formatLogMessage(message, service))

    fun warn(message: String) = warn(message, "")

    override fun warn(
        message: String,
        service: String?,
    ) = logger.warn(formatLogMessage(message, service))

    fun debug(message: String) = debug(message, "")

    override fun debug(
        message: String,
        service: String?,
    ) = logger.debug(formatLogMessage(message, service))

    fun error(message: String) = error(message, "")

    override fun error(
        message: String,
        service: String?,
    ) = logger.error(formatLogMessage(message, service))
}
