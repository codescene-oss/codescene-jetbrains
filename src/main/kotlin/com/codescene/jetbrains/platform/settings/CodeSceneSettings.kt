package com.codescene.jetbrains.platform.settings

import com.codescene.jetbrains.core.telemetry.buildSettingsVisibilityTelemetryData
import com.codescene.jetbrains.core.util.Constants.CODESCENE
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.platform.settings.tab.AboutTab
import com.codescene.jetbrains.platform.settings.tab.SettingsTab
import com.codescene.jetbrains.platform.telemetry.TelemetryService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.Configurable.Composite
import com.intellij.ui.components.JBTabbedPane
import java.awt.event.HierarchyEvent
import javax.swing.JComponent

class CodeSceneSettings : Composite, Configurable {
    private val settingsTab = SettingsTab()
    private val aboutTab = AboutTab()

    private val childConfigurables: Array<Configurable> =
        arrayOf(
            settingsTab,
            aboutTab,
        )

    override fun getConfigurables(): Array<Configurable> = childConfigurables

    override fun createComponent(): JComponent =
        JBTabbedPane().apply {
            childConfigurables
                .mapNotNull { configurable ->
                    configurable.createComponent()?.let { component ->
                        addTab(configurable.displayName, component)
                    }
                }

            TelemetryService.getInstance().logUsage(TelemetryEvents.OPEN_SETTINGS)

            addHierarchyListener { event ->
                // Check if the SHOWING_CHANGED bit is affected
                if (event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
                    TelemetryService.getInstance().logUsage(
                        TelemetryEvents.SETTINGS_VISIBILITY,
                        buildSettingsVisibilityTelemetryData(this.isShowing),
                    )
                }
            }
        }

    override fun isModified() = settingsTab.isModified

    override fun apply() {
        if (settingsTab.isModified) settingsTab.apply()
    }

    override fun reset() {
        settingsTab.reset()
    }

    override fun disposeUIResources() {
        childConfigurables.forEach { it.disposeUIResources() }
    }

    override fun getDisplayName(): String = CODESCENE
}
