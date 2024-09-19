package com.codescene.jetbrains.components.settings

import com.codescene.jetbrains.components.settings.tab.AboutTab
import com.codescene.jetbrains.components.settings.tab.GeneralTab
import com.codescene.jetbrains.components.settings.tab.RulesTab
import com.codescene.jetbrains.components.settings.tab.SettingsTab
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.Configurable.Composite
import com.intellij.ui.components.JBTabbedPane
import javax.swing.JComponent

class CodeSceneSettings : Composite, Configurable {
    private val settingsTab = SettingsTab()
    private val aboutTab = AboutTab()
    private val generalTab = GeneralTab()
    private val rulesTab = RulesTab()

    private val childConfigurables: Array<Configurable> = arrayOf(
        generalTab,
        settingsTab,
        aboutTab,
        rulesTab
    )

    override fun getConfigurables(): Array<Configurable> {
        return childConfigurables
    }

    override fun createComponent(): JComponent = JBTabbedPane().apply {
        childConfigurables
            .mapNotNull { configurable ->
                configurable.createComponent()?.let { component ->
                    addTab(configurable.displayName, component)
                }
            }
    }

    override fun isModified(): Boolean {
        return settingsTab.isModified
    }

    override fun apply() {
        if (settingsTab.isModified) {
            settingsTab.apply()
        }
    }

    override fun reset() {
        settingsTab.reset()
    }

    override fun disposeUIResources() {
        childConfigurables.forEach { it.disposeUIResources() }
    }

    override fun getDisplayName(): String {
        return "CodeScene"
    }
}