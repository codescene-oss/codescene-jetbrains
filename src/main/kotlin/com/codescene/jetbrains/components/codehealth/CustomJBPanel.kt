package com.codescene.jetbrains.components.codehealth

import com.codescene.jetbrains.util.Log
import com.intellij.ui.components.JBPanel
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

class CustomJBPanel : JBPanel<JBPanel<*>>() {
    init {
        setupVisibilityListener()
    }

    private fun setupVisibilityListener() {
        // Add component listener
        addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent) {
                // Handle panel becoming visible
                handlePanelVisible()
            }

            override fun componentHidden(e: ComponentEvent) {
                // Handle panel becoming hidden
                handlePanelHidden()
            }
        })

//        // Alternative method using property change listener
//        addPropertyChangeListener("visible") { evt ->
//            val isVisible = evt.newValue as Boolean
//            if (isVisible) {
//                handlePanelVisible()
//            } else {
//                handlePanelHidden()
//            }
//        }
    }

    private fun handlePanelVisible() {
        // Add your logic here when panel becomes visible
        println("Telemetry: Panel became visible")
    }

    private fun handlePanelHidden() {
        // Add your logic here when panel becomes hidden
        println("Telemetry: Panel became hidden")
    }
}