package com.codescene.jetbrains.components.controlCenter.panel

import com.intellij.ui.dsl.builder.panel
import javax.swing.JPanel

fun createRulesPanel(): JPanel {
    return panel {
        row("Rules") {
            label("This is the Rules section placeholder.")
        }
    }
}