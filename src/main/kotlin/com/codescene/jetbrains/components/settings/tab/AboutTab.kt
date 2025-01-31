package com.codescene.jetbrains.components.settings.tab

import com.codescene.jetbrains.CodeSceneIcons.LOGO
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Constants.CODESCENE_URL
import com.codescene.jetbrains.util.Constants.TELEMETRY_SAMPLES_URL
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.ui.JBUI.Borders
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import java.time.Year
import javax.swing.*

class AboutTab : BoundConfigurable(UiLabelsBundle.message("aboutTitle")) {
    private lateinit var telemetryCheckbox: JCheckBox
    private val settings = CodeSceneGlobalSettingsStore.getInstance().state

    override fun createPanel() = DialogPanel(BorderLayout(0, 20)).apply {
        border = Borders.empty(10)
        add(aboutSection, BorderLayout.NORTH)
        add(telemetrySection, BorderLayout.CENTER)
    }

    override fun isModified() = (settings.telemetryConsentGiven != telemetryCheckbox.isSelected)

    override fun apply() {
        settings.telemetryConsentGiven = telemetryCheckbox.isSelected
    }

    private val aboutSection = JPanel().apply {
        layout = FlowLayout(FlowLayout.LEFT)
        add(JLabel(LOGO))
        add(Box.createHorizontalStrut(10))
        add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            getAboutComponents().map {
                add(Box.createVerticalStrut(10))
                add(it)
            }
        })
    }

    private val telemetrySection = JPanel().apply {
        layout = GridBagLayout()
        border = IdeBorderFactory.createTitledBorder(
            UiLabelsBundle.message("statistics"),
            true,
            Insets(0, 0, 0, 10)
        )

        val gbc = GridBagConstraints().apply {
            gridx = 0
            fill = GridBagConstraints.HORIZONTAL

            weightx = 1.0
            weighty = 0.0

            ipady = 0
            ipadx = 0

            insets = Insets(0, 0, 0, 0)
            anchor = GridBagConstraints.NORTHWEST
        }

        gbc.gridy = 0
        add(getTelemetryDescription(), gbc)

        gbc.gridy = 1
        gbc.ipady = 10
        add(getSamplesLabel(), gbc)

        gbc.gridy = 2
        gbc.weighty = 1.0
        add(getTelemetryConsentCheckbox(), gbc)
    }

    private fun getTelemetryDescription() = JTextArea(UiLabelsBundle.message("telemetryDescription")).apply {
        font = UIUtil.getFont(UIUtil.FontSize.NORMAL, Font.getFont("Arial"))
        isOpaque = false
        isEditable = false
        lineWrap = true
        preferredSize = Dimension(100, 85)
        wrapStyleWord = true
    }

    private fun getSamplesLabel() = JLabel("<html><a href=\"$TELEMETRY_SAMPLES_URL\">Telemetry samples</a>").apply {
        alignmentX = Component.LEFT_ALIGNMENT
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (e?.source is JLabel) {
                    try {
                        Desktop.getDesktop().browse(URI(TELEMETRY_SAMPLES_URL))
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        })
    }

    private fun getTelemetryConsentCheckbox() = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        border = Borders.empty()

        telemetryCheckbox = JCheckBox(UiLabelsBundle.message("telemetryCheckbox")).apply {
            isFocusable = false
            alignmentX = Component.LEFT_ALIGNMENT
            isSelected = settings.telemetryConsentGiven
            margin = Insets(0, 0, 0, 0)
        }

        add(telemetryCheckbox)
    }

    private fun getAboutComponents(): List<JComponent> {
        val header = JLabel(CODESCENE).apply { font = Font("Arial", Font.BOLD, 18) }
        val copyRight = JLabel("© ${Year.now().value} CodeScene AB")
        val link = JLabel("<html><a href='$CODESCENE_URL'>www.codescene.com</a></html>").apply {
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
        val aboutText = JTextArea(UiLabelsBundle.message("codeSceneDescription")).apply {
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