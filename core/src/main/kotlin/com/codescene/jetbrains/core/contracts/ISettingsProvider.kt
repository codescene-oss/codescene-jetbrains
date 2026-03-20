package com.codescene.jetbrains.core.contracts

import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings

interface ISettingsProvider {
    fun currentState(): CodeSceneGlobalSettings

    fun updateTelemetryConsent(hasAccepted: Boolean)

    fun updateAceStatus(status: AceStatus)

    fun updateAceAcknowledged(acknowledged: Boolean)
}
