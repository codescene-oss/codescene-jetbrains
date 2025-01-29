package com.codescene.jetbrains.components.settings

import com.codescene.jetbrains.components.settings.tab.AboutTab
import com.codescene.jetbrains.components.settings.tab.GeneralTab
import com.codescene.jetbrains.components.settings.tab.SettingsTab
import com.codescene.jetbrains.services.telemetry.TelemetryService
import com.codescene.jetbrains.util.Constants
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Log
import com.intellij.designer.designSurface.ComponentSelectionListener
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.Configurable.Composite
import com.intellij.ui.components.JBTabbedPane
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.HierarchyEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.JComponent

class CodeSceneSettings : Composite, Configurable, FocusListener {
    private val settingsTab = SettingsTab()
    private val aboutTab = AboutTab()
    private val generalTab = GeneralTab()

    private val childConfigurables: Array<Configurable> = arrayOf(
        generalTab,
        settingsTab,
        aboutTab,
    )

    override fun getConfigurables(): Array<Configurable> = childConfigurables

    override fun createComponent(): JComponent = JBTabbedPane().apply {
        childConfigurables
            .mapNotNull { configurable ->
                configurable.createComponent()?.let { component ->
                    addTab(configurable.displayName, component)
                }
            }

        TelemetryService.getInstance().logUsage("${Constants.TELEMETRY_EDITOR_TYPE}/${Constants.TELEMETRY_OPEN_SETTINGS}")

        addHierarchyListener { event ->
            // Check if the SHOWING_CHANGED bit is affected
            if (event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
                TelemetryService.getInstance().logUsage(
                    "${Constants.TELEMETRY_EDITOR_TYPE}/${Constants.TELEMETRY_SETTINGS_VISIBILITY}",
                    mutableMapOf<String, Any>(Pair("visible", this.isShowing)))
            }
        }
    }

    override fun isModified(): Boolean = settingsTab.isModified

    override fun apply() {
        if (settingsTab.isModified) {
            settingsTab.apply()
        }
        Log.warn("Telemetry event logged: apply() method")
    }

    override fun reset() {
        settingsTab.reset()
//        TelemetryService.getInstance().logUsage(
//            "${Constants.TELEMETRY_EDITOR_TYPE}/${Constants.TELEMETRY_SETTINGS_VISIBILITY}",
//            mutableMapOf<String, Any>(Pair("visible", true)))
    }

    override fun disposeUIResources() {
        childConfigurables.forEach { it.disposeUIResources() }
//        TelemetryService.getInstance().logUsage(
//            "${Constants.TELEMETRY_EDITOR_TYPE}/${Constants.TELEMETRY_SETTINGS_VISIBILITY}",
//            mutableMapOf<String, Any>(Pair("visible", false)))
    }

    override fun getDisplayName(): String = CODESCENE

    override fun focusGained(e: FocusEvent?) {
        Log.warn("Telemetry event logged: {visible: ${e?.component?.isVisible}}")
    }

    override fun focusLost(e: FocusEvent?) {
        Log.warn("Telemetry event logged: {visible: ${e?.component?.isVisible}}")
    }
}