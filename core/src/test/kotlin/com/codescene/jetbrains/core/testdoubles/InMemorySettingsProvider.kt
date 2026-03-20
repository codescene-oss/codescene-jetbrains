package com.codescene.jetbrains.core.testdoubles

import com.codescene.jetbrains.core.contracts.ISettingsProvider
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings

class InMemorySettingsProvider(
    private var settings: CodeSceneGlobalSettings = CodeSceneGlobalSettings(),
) : ISettingsProvider {
    override fun currentState(): CodeSceneGlobalSettings = settings

    override fun updateTelemetryConsent(hasAccepted: Boolean) {
        settings = settings.copy(telemetryConsentGiven = hasAccepted)
    }

    override fun updateAceStatus(status: AceStatus) {
        settings.aceStatus = status
    }

    override fun updateAceAcknowledged(acknowledged: Boolean) {
        settings = settings.copy(aceAcknowledged = acknowledged)
    }
}
