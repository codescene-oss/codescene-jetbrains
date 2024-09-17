package com.codescene.jetbrains.components.controlCenter.panel

import com.intellij.ui.dsl.builder.panel
import javax.swing.JPanel

fun createAboutPanel(): JPanel {
    return panel {
        row("About CodeScene") {
            label("This is the About section placeholder.")
        }
    }
}