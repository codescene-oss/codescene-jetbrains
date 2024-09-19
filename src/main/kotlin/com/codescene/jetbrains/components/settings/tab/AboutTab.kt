package com.codescene.jetbrains.components.settings.tab

import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class AboutTab : Configurable {
    override fun getDisplayName(): String = "About"

    override fun createComponent(): JComponent = panel {
        row("About CodeScene") {
            label("This is the About section placeholder.")
        }
    }

    override fun isModified(): Boolean = false

    override fun apply() {
        // No settings to change in this tab
    }
}