package com.codescene.jetbrains.platform.settings.tab

import com.codescene.jetbrains.core.flag.RuntimeFlags
import com.codescene.jetbrains.core.util.Constants.TELEMETRY_EVENTS_URL
import com.codescene.jetbrains.core.util.Constants.TELEMETRY_SAMPLES_URL
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.api.AceService
import com.codescene.jetbrains.platform.settings.CodeSceneGlobalSettingsStore
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Suppress("DialogTitleCapitalization")
class SettingsTab : BoundConfigurable(UiLabelsBundle.message("settingsTitle")) {
    private val settings = CodeSceneGlobalSettingsStore.getInstance().currentState()
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun createPanel(): DialogPanel {
        return panel {
            row {
                checkBox(UiLabelsBundle.message("enableCodeLenses"))
                    .bindSelected(settings::enableCodeLenses)
                    .comment(UiLabelsBundle.message("enableCodeLensesComment"))
            }

            groupRowsRange(UiLabelsBundle.message("ace")) {
                row {
                    checkBox("Ace acknowledged")
                        .bindSelected(settings::aceAcknowledged)
                        .comment(
                            "For testing purposes. This should only be visible when FEATURE_CWF_DEVMODE is enabled.",
                        )
                        .visible(RuntimeFlags.isDevMode)
                }

                row {
                    checkBox(UiLabelsBundle.message("enableAutoRefactor"))
                        .bindSelected(settings::enableAutoRefactor)
                        .comment(UiLabelsBundle.message("enableAutoRefactorComment"))
                }

                row(UiLabelsBundle.message("aceAuthToken")) {
                    passwordField()
                        .align(Align.FILL)
                        .resizableColumn()
                        .comment(UiLabelsBundle.message("aceAuthTokenComment"))
                        .bindText(settings::aceAuthToken)
                }
            }

            // TODO: verify naming of this section, currently just a placeholder
            groupRowsRange(UiLabelsBundle.message("cloudConnection")) {
                row(UiLabelsBundle.message("serverUrl")) {
                    textField()
                        .align(Align.FILL)
                        .resizableColumn()
                        .bindText(settings::serverUrl)
                }
            }.visible(false)

            groupRowsRange(UiLabelsBundle.message("statistics")) {
                row {
                    cell(telemetryDescriptionTextArea())
                        .align(Align.FILL)
                }
                row {
                    cell(telemetrySampleLink(TELEMETRY_SAMPLES_URL, UiLabelsBundle.message("dataSamples")))
                }
                row {
                    cell(telemetrySampleLink(TELEMETRY_EVENTS_URL, UiLabelsBundle.message("eventsList")))
                }
                row {
                    checkBox(UiLabelsBundle.message("telemetryCheckbox"))
                        .bindSelected(settings::telemetryConsentGiven)
                }
            }
        }
    }

    override fun apply() {
        val previousState = settings.copy()
        super.apply()
        CodeSceneGlobalSettingsStore.getInstance().notifyIfStateChanged(previousState)
        val onlyTelemetryConsentChanged =
            previousState.copy(telemetryConsentGiven = settings.telemetryConsentGiven) == settings
        if (!onlyTelemetryConsentChanged) {
            scope.launch {
                AceService.getInstance().runPreflight(true)
            }
        }
    }
}
