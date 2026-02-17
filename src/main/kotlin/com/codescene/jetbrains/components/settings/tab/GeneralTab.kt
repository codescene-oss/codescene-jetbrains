package com.codescene.jetbrains.components.settings.tab

import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_ACE
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.config.global.AceStatus
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.flag.RuntimeFlags
import com.codescene.jetbrains.notifier.AceStatusRefreshNotifier
import com.codescene.jetbrains.services.api.AceService
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.util.Constants.AI_PRINCIPLES_URL
import com.codescene.jetbrains.util.Constants.CONTACT_URL
import com.codescene.jetbrains.util.Constants.DOCUMENTATION_URL
import com.codescene.jetbrains.util.Constants.GREEN
import com.codescene.jetbrains.util.Constants.RED
import com.codescene.jetbrains.util.Constants.SUPPORT_URL
import com.codescene.jetbrains.util.Constants.TERMS_AND_CONDITIONS_URL
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.TelemetryEvents
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.border.AbstractBorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeneralTab : Configurable {
    private var statusButton = getStatusButton()
    private lateinit var status: AceStatus
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        subscribeToAceStatusRefreshEvent()
    }

    override fun getDisplayName(): String = UiLabelsBundle.message("generalTitle")

    override fun isModified(): Boolean = false

    override fun apply() {
        // No settings to change in this tab
    }

    private val more =
        listOf(
            UiLabelsBundle.message("documentation") to DOCUMENTATION_URL,
            UiLabelsBundle.message("termsAndPolicies") to TERMS_AND_CONDITIONS_URL,
            UiLabelsBundle.message("aiPrinciples") to AI_PRINCIPLES_URL,
            UiLabelsBundle.message("contactCodeScene") to CONTACT_URL,
            UiLabelsBundle.message("supportTicket") to SUPPORT_URL,
        )

    override fun createComponent(): JPanel =
        JPanel().apply {
            layout = BorderLayout()

            if (RuntimeFlags.aceFeature) add(getAceSection(), BorderLayout.NORTH)
            add(getMoreSection(), BorderLayout.CENTER)
        }

    private fun getAceSection() =
        JPanel().apply {
            layout = BorderLayout()

            border =
                IdeBorderFactory.createTitledBorder(
                    UiLabelsBundle.message("status"),
                    true,
                    JBUI.insetsRight(10),
                )

            add(
                JLabel(UiLabelsBundle.message("ace")).apply {
                    icon = CODESCENE_ACE
                },
                BorderLayout.WEST,
            )

            add(statusButton, BorderLayout.EAST)
            add(Box.createVerticalStrut(20), BorderLayout.SOUTH)
        }

    private fun getStatusButton(): JButton {
        status = CodeSceneGlobalSettingsStore.getInstance().state.aceStatus
        val button = JButton(status.name)
        colorButton(button, status)

        // disabling button on first click to prevent multiple clicks
        button.addActionListener {
            if (status == AceStatus.ERROR) {
                button.isEnabled = false
                scope.launch {
                    try {
                        AceService.getInstance().runPreflight(true)
                    } finally {
                        button.isEnabled = true
                    }
                }
            }
        }

        return button
    }

    private fun colorButton(
        button: JButton,
        status: AceStatus,
    ) {
        val buttonColor: JBColor =
            when (status) {
                AceStatus.ERROR -> RED
                AceStatus.SIGNED_IN -> GREEN
                AceStatus.SIGNED_OUT -> JBColor.ORANGE
                AceStatus.DEACTIVATED -> JBColor.GRAY
                AceStatus.OFFLINE -> JBColor.LIGHT_GRAY
                AceStatus.OUT_OF_CREDITS -> JBColor.YELLOW
            }

        button.foreground = buttonColor
    }

    private fun refreshStatusButton() {
        status = CodeSceneGlobalSettingsStore.getInstance().state.aceStatus
        statusButton.text = status.name
        colorButton(statusButton, status)
    }

    private fun getMoreSection() =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            border =
                IdeBorderFactory.createTitledBorder(
                    UiLabelsBundle.message("more"),
                    true,
                    JBUI.insetsRight(10),
                )

        /*
        TODO: uncomment when we have more than 1 section:
                val message = UiLabelsBundle.message("more")
                val icon = IconUtil.scale(globeIcon, null, 1.2f)
                add(Box.createVerticalStrut(10))
                addHeader(message, icon)
         */
            add(Box.createVerticalStrut(10))

            more.forEach {
                add(createRoundedPanel(it.first, it.second))
                add(Box.createVerticalStrut(10))
            }
        }

    class RoundedBorder(
        private val radius: Int,
    ) : AbstractBorder() {
        // @codescene(disable:"Excess Number of Function Arguments")
        override fun paintBorder(
            c: Component?,
            g: Graphics,
            x: Int,
            y: Int,
            width: Int,
            height: Int,
        ) {
            val g2d = g as Graphics2D

            g2d.color = JBColor.LIGHT_GRAY
            g2d.fillRoundRect(x, y, width - 1, height - 1, radius, radius)
            g2d.color = JBColor.LIGHT_GRAY
            g2d.stroke = BasicStroke(2f)
            g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius)
        }

        override fun getBorderInsets(c: Component?) = JBUI.insets(0, 10)
    }

    private fun createRoundedPanel(
        text: String,
        link: String,
    ): JPanel {
        val padding = 3
        val maxHeight = 35

        return JPanel(BorderLayout(padding, padding)).apply {
            isOpaque = false
            border = RoundedBorder(20)
            minimumSize = Dimension(Int.MAX_VALUE, maxHeight)
            maximumSize = Dimension(Int.MAX_VALUE, maxHeight)
            preferredSize = Dimension(Int.MAX_VALUE, maxHeight)

            add(
                JLabel(text).apply {
                    horizontalAlignment = SwingConstants.LEFT
                },
                BorderLayout.WEST,
            )

            add(
                JLabel(AllIcons.General.ChevronRight).apply {
                    horizontalAlignment = SwingConstants.RIGHT
                },
                BorderLayout.EAST,
            )

            addMouseListener(createLinkMouseListener(link))
        }
    }

    private fun createLinkMouseListener(link: String): MouseAdapter =
        object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                try {
                    val uri = URI(link)
                    if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(uri)

                    TelemetryService.getInstance().logUsage(
                        TelemetryEvents.OPEN_LINK,
                        mutableMapOf<String, Any>(Pair("url", uri)),
                    )
                } catch (e: Exception) {
                    Log.warn("Unable to open link. Error message: ${e.message}")
                }
            }

            override fun mouseEntered(e: MouseEvent?) {
                (e?.component as? JComponent)?.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            }

            override fun mouseExited(e: MouseEvent?) {
                (e?.component as? JComponent)?.cursor = Cursor.getDefaultCursor()
            }
        }

    private fun subscribeToAceStatusRefreshEvent() {
        ApplicationManager
            .getApplication()
            .messageBus
            .connect()
            .subscribe(
                AceStatusRefreshNotifier.TOPIC,
                object : AceStatusRefreshNotifier {
                    override fun refresh() {
                        refreshStatusButton()
                    }
                },
            )
    }
}
