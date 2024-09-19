package com.codescene.jetbrains.components.settings.tab

import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class RulesTab : Configurable {
    override fun getDisplayName(): String = "Rules"

    override fun createComponent(): JComponent = panel {
        row("Rules") {
            label("This is the Rules section placeholder.")
        }
    }

    override fun isModified(): Boolean = false

    override fun apply() {
        // No settings to change in this tab
    }
}