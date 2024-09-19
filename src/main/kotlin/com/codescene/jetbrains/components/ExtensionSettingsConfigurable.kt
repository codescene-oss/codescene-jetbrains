package com.codescene.jetbrains.components

import com.codescene.jetbrains.components.settings.tab.SettingsTab
import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class ExtensionSettingsConfigurable : Configurable {
    private var extensionSettingsComponent = SettingsTab()

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return "Extension Settings Example"
    }

    override fun createComponent(): JComponent {
        return extensionSettingsComponent.createComponent()
    }

    override fun isModified(): Boolean {
        return extensionSettingsComponent.isModified
    }

    override fun apply() {
        return extensionSettingsComponent.apply()
    }

    override fun reset() {
        extensionSettingsComponent.reset()
    }
}