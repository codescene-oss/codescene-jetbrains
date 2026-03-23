package com.codescene.jetbrains.core.review

import com.codescene.data.ace.PreflightResponse
import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.contracts.ISettingsProvider
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.util.resolveAceFailureStatus
import com.codescene.jetbrains.core.util.resolveAcePreflightDecision

class AcePreflightOrchestrator(
    private val settingsProvider: ISettingsProvider,
    private val logger: ILogger,
    private val serviceName: String,
    private val fetchPreflight: suspend (Boolean) -> TimedResult<PreflightResponse?>,
    private val onStatusChange: (AceStatus) -> Unit,
) {
    suspend fun runPreflight(force: Boolean = false): PreflightResponse? {
        val settings = settingsProvider.currentState()
        val decision =
            resolveAcePreflightDecision(
                autoRefactorEnabled = settings.enableAutoRefactor,
                token = settings.aceAuthToken,
                force = force,
            )

        decision.skippedStatus?.let(settingsProvider::updateAceStatus)
        if (!decision.shouldRun) return null

        val bypassCache = decision.successStatus != null
        logger.debug("Getting ACE preflight data from server...", serviceName)

        return try {
            val result = fetchPreflight(bypassCache)
            logger.info(
                "Preflight info fetched from the server in ${result.elapsedMs}ms. Cache bypassed: $bypassCache",
                serviceName,
            )
            decision.successStatus?.let(onStatusChange)
            result.result
        } catch (e: Exception) {
            val newStatus = resolveAceFailureStatus(e)
            onStatusChange(newStatus)

            if (newStatus == AceStatus.OFFLINE) {
                logger.warn(
                    "Preflight request timed out or connection failed. Error message: ${e.message}",
                    serviceName,
                )
            } else {
                logger.error(
                    "Error during preflight info fetching. Error message: ${e.message}",
                    serviceName,
                )
            }
            null
        }
    }
}
