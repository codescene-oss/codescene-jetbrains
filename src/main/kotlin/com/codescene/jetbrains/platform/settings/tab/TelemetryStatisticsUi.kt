package com.codescene.jetbrains.platform.settings.tab

import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.util.Log
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.JLabel
import javax.swing.JTextArea

internal fun telemetryDescriptionTextArea(): JTextArea =
    JTextArea(UiLabelsBundle.message("telemetryDescription")).apply {
        font = UIUtil.getFont(UIUtil.FontSize.NORMAL, Font.getFont("Arial"))
        isOpaque = false
        isEditable = false
        lineWrap = true
        preferredSize = Dimension(100, 85)
        wrapStyleWord = true
    }

internal fun telemetrySampleLink(
    url: String,
    label: String,
): JLabel =
    JLabel("<html><a href=\"$url\">$label</a>").apply {
        alignmentX = Component.LEFT_ALIGNMENT
        addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    if (e?.source is JLabel) {
                        try {
                            Desktop.getDesktop().browse(URI(url))
                        } catch (ex: Exception) {
                            Log.error("Failed to open URL [$url] on label click. Error message: ${ex.message}")
                        }
                    }
                }
            },
        )
        border = JBUI.Borders.empty(0, 0, 10, 0)
    }
