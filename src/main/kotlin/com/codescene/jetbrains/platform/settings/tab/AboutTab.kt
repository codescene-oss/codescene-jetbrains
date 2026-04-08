package com.codescene.jetbrains.platform.settings.tab

import com.codescene.jetbrains.core.util.Constants.CODESCENE
import com.codescene.jetbrains.core.util.Constants.CODESCENE_URL
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.icons.CodeSceneIcons.LOGO
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI.Borders
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import java.time.Year
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea

class AboutTab : BoundConfigurable(UiLabelsBundle.message("aboutTitle")) {
    override fun createPanel() =
        DialogPanel(BorderLayout(0, 20)).apply {
            border = Borders.empty(10)
            add(aboutSection, BorderLayout.NORTH)
            add(deviceIdSection, BorderLayout.SOUTH)
        }

    private val aboutSection =
        JPanel().apply {
            layout = FlowLayout(FlowLayout.LEFT)
            add(JLabel(LOGO))
            add(Box.createHorizontalStrut(10))
            add(
                JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)

                    getAboutComponents().forEach {
                        add(Box.createVerticalStrut(10))
                        add(it)
                    }
                },
            )
        }

    private val deviceIdSection =
        JTextArea().apply {
            text = "device-id: ${CodeSceneApplicationServiceProvider.getInstance().deviceIdStore.get()}"
            foreground = JBColor.GRAY
            isEditable = false
            isOpaque = false
        }

    private fun getAboutComponents(): List<JComponent> {
        val header = JLabel(CODESCENE).apply { font = Font("Arial", Font.BOLD, 18) }
        val copyRight = JLabel("© ${Year.now().value} CodeScene AB")
        val link =
            JLabel("<html><a href='$CODESCENE_URL'>www.codescene.com</a></html>").apply {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addMouseListener(
                    object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent) {
                            try {
                                Desktop.getDesktop().browse(URI.create(CODESCENE_URL))
                            } catch (e: Exception) {
                                Log.error("Unable to open $CODESCENE_URL. Error message: ${e.message}")
                            }
                        }
                    },
                )
            }
        val aboutText =
            JTextArea(UiLabelsBundle.message("codeSceneDescription")).apply {
                lineWrap = true
                isEditable = false
                wrapStyleWord = true
                alignmentX = Component.LEFT_ALIGNMENT
                preferredSize = Dimension(450, 50)
                font = Font("Arial", Font.PLAIN, 15)
            }

        return listOf<JComponent>(header, aboutText, copyRight, link)
    }
}
