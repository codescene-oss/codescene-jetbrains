package com.codescene.jetbrains.core.testdoubles

import com.codescene.jetbrains.core.contracts.ISettingsProvider
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings

class InMemorySettingsProvider(
    private var settings: CodeSceneGlobalSettings = CodeSceneGlobalSettings(),
    private var aceAuthToken: String = "",
) : ISettingsProvider {
    override fun currentState(): CodeSceneGlobalSettings = settings

    override fun getAceAuthToken(): String = aceAuthToken

    override fun setAceAuthToken(token: String) {
        aceAuthToken = token
        settings = settings.copy(aceTokenConfigured = token.isNotBlank())
    }

    override fun updateTelemetryConsent(hasAccepted: Boolean) {
        settings = settings.copy(telemetryConsentGiven = hasAccepted)
    }

    override fun updateTelemetryNoticeShown(shown: Boolean) {
        settings = settings.copy(telemetryNoticeShown = shown)
    }

    override fun updateAceStatus(status: AceStatus) {
        settings = settings.copy(aceStatus = status)
    }

    override fun updateAceAcknowledged(acknowledged: Boolean) {
        settings = settings.copy(aceAcknowledged = acknowledged)
    }
}
