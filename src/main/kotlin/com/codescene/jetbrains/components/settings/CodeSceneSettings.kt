package com.codescene.jetbrains.components.settings

import com.codescene.jetbrains.components.settings.tab.AboutTab
import com.codescene.jetbrains.components.settings.tab.GeneralTab
import com.codescene.jetbrains.components.settings.tab.SettingsTab
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.Configurable.Composite
import com.intellij.ui.components.JBTabbedPane
import javax.swing.JComponent

class CodeSceneSettings : Composite, Configurable {
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
    }

    override fun isModified(): Boolean = settingsTab.isModified

    override fun apply() {
        if (settingsTab.isModified) {
            settingsTab.apply()
        }
    }

    override fun reset() = settingsTab.reset()

    override fun disposeUIResources() = childConfigurables.forEach { it.disposeUIResources() }

    override fun getDisplayName(): String = CODESCENE
}