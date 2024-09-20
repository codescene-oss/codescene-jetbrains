package com.codescene.jetbrains.components.settings.tab

import com.codescene.jetbrains.UiLabelsBundle
import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class GeneralTab : Configurable {
    override fun getDisplayName(): String = UiLabelsBundle.message("generalTitle")

    override fun createComponent(): JComponent = panel {
        row("General") {
            label("This is the General section placeholder.")
        }
    }

    override fun isModified(): Boolean = false

    override fun apply() {
        // No settings to change in this tab
    }
}