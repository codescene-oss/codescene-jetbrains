package com.codescene.jetbrains.components.settings.tab

import com.codescene.jetbrains.CodeSceneIcons.LOGO
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Constants.CODESCENE_URL
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.options.Configurable
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import java.time.Year
import javax.swing.*

class AboutTab : Configurable {
    override fun getDisplayName(): String = UiLabelsBundle.message("aboutTitle")

    private val textArea = JTextArea(UiLabelsBundle.message("codeSceneDescription")).apply {
        lineWrap = true
        isEditable = false
        wrapStyleWord = true
        alignmentX = Component.LEFT_ALIGNMENT
        preferredSize = Dimension(450, 50)
        font = Font("Arial", Font.PLAIN, 15)
    }

    private val leftPanel = JLabel().apply { icon = LOGO }

    private val header = JLabel(CODESCENE).apply { font = Font("Arial", Font.BOLD, 18) }

    private val copyRight = JLabel("© ${Year.now().value} CodeScene AB")

    private val link = JLabel("<html><a href='$CODESCENE_URL'>www.codescene.com</a></html>").apply {
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                try {
                    Desktop.getDesktop().browse(URI.create(CODESCENE_URL))
                } catch (e: Exception) {
                    Log.error("Unable to open $CODESCENE_URL: ${e.stackTraceToString()}")
                }
            }
        })
    }

    private val aboutComponents = listOf<JComponent>(header, textArea, copyRight, link)

    private val rightPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        aboutComponents.map {
            add(Box.createVerticalStrut(10))
            add(it)
        }
    }

    override fun createComponent() = JPanel().apply {
        layout = FlowLayout(FlowLayout.LEFT)

        add(leftPanel)
        add(Box.createHorizontalStrut(10))
        add(rightPanel)
    }

    override fun isModified(): Boolean = false

    override fun apply() {
        // No settings to change in this tab
    }
}