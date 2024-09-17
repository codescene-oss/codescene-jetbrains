package com.codescene.jetbrains.components.controlCenter.panel

import com.intellij.ui.dsl.builder.panel
import javax.swing.JPanel

fun createGeneralPanel(): JPanel {
    return panel {
        row("General") {
            label("This is the General section placeholder.")
        }
    }
}