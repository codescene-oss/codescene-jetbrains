package com.codescene.jetbrains.components.settings.tab

import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_ACE
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.notifier.AceStatusRefreshNotifier
import com.codescene.jetbrains.services.AceService
import com.codescene.jetbrains.services.telemetry.TelemetryService
import com.codescene.jetbrains.util.Constants.CONTACT_URL
import com.codescene.jetbrains.util.Constants.DOCUMENTATION_URL
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
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.*
import javax.swing.border.AbstractBorder

class GeneralTab : Configurable {
    private var statusButton = getStatusButton()

    init {
        subscribeToAceStatusRefreshEvent()
    }

    override fun getDisplayName(): String = UiLabelsBundle.message("generalTitle")

    private val more = listOf(
        UiLabelsBundle.message("documentation") to DOCUMENTATION_URL,
        UiLabelsBundle.message("termsAndPolicies") to TERMS_AND_CONDITIONS_URL,
//        UiLabelsBundle.message("aiPrinciples") to AI_PRINCIPLES_URL, TODO: uncomment when ACE capabilities are added
        UiLabelsBundle.message("contactCodeScene") to CONTACT_URL,
        UiLabelsBundle.message("supportTicket") to SUPPORT_URL
    )

    override fun createComponent(): JPanel {
        return JPanel().apply {
            layout = BorderLayout()

            add(getAceSection(), BorderLayout.NORTH)
            add(getMoreSection(), BorderLayout.CENTER)
        }
    }

    private fun getAceSection() = JPanel().apply {
        layout = BorderLayout()

        border = IdeBorderFactory.createTitledBorder(
            UiLabelsBundle.message("status"),
            true,
            JBUI.insetsRight(10)
        )

        add(JLabel(UiLabelsBundle.message("ace")).apply {
            icon = CODESCENE_ACE
        }, BorderLayout.WEST)

        add(statusButton, BorderLayout.EAST)
        add(Box.createVerticalStrut(20), BorderLayout.SOUTH)
    }

    private fun getStatusButton(): JButton {
        val status = AceService.getInstance().getStatus()
        val button = JButton(status.name)

        return button
    }

    private fun refreshStatusButton() {
        val newStatus = AceService.getInstance().getStatus()
        statusButton.text = newStatus.name
    }

    private fun getMoreSection() = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        border = IdeBorderFactory.createTitledBorder(
            UiLabelsBundle.message("more"),
            true,
            JBUI.insetsRight(10)
        )

        /*
        TODO: uncomment when we have more than 1 section:
                val message = UiLabelsBundle.message("more")
                val icon = IconUtil.scale(globeIcon, null, 1.2f)
                add(Box.createVerticalStrut(10))
                addHeader(message, icon)
         */
        add(Box.createVerticalStrut(10))

        more.map {
            add(createRoundedPanel(it.first, it.second))
            add(Box.createVerticalStrut(10))
        }
    }

    private fun JPanel.addHeader(message: String, icon: Icon) {
        val panel = JPanel().apply {
            layout = FlowLayout(FlowLayout.LEFT, 5, 0)

            val statusIcon = JLabel(icon)
            val status = JLabel(message).apply {
                font = Font("Arial", Font.BOLD, 13)
            }

            add(statusIcon)
            add(status)
        }

        add(panel)
    }

    override fun isModified(): Boolean = false

    override fun apply() {
        // No settings to change in this tab
    }

    class RoundedBorder(private val radius: Int) : AbstractBorder() {
        override fun paintBorder(c: Component?, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
            val g2d = g as Graphics2D

            g2d.color = JBColor.LIGHT_GRAY
            g2d.fillRoundRect(x, y, width - 1, height - 1, radius, radius)
            g2d.color = JBColor.LIGHT_GRAY
            g2d.stroke = BasicStroke(2f)
            g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius)
        }

        override fun getBorderInsets(c: Component?) = JBUI.insets(0, 10)
    }

    private fun createRoundedPanel(text: String, link: String): JPanel {
        val padding = 3
        val maxHeight = 35

        return JPanel(BorderLayout(padding, padding)).apply {
            isOpaque = false
            border = RoundedBorder(20)
            minimumSize = Dimension(Int.MAX_VALUE, maxHeight)
            maximumSize = Dimension(Int.MAX_VALUE, maxHeight)
            preferredSize = Dimension(Int.MAX_VALUE, maxHeight)

            add(JLabel(text).apply {
                horizontalAlignment = SwingConstants.LEFT
            }, BorderLayout.WEST)

            add(JLabel(AllIcons.General.ChevronRight).apply {
                horizontalAlignment = SwingConstants.RIGHT
            }, BorderLayout.EAST)

            addMouseListener(createLinkMouseListener(link))
        }
    }

    private fun createLinkMouseListener(link: String): MouseAdapter {
        return object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                try {
                    val uri = URI(link)
                    if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(uri)

                    TelemetryService.getInstance().logUsage(
                        TelemetryEvents.OPEN_LINK,
                        mutableMapOf<String, Any>(Pair("url", uri))
                    )
                } catch (e: Exception) {
                    Log.warn("Unable to open link: ${e.message}")
                }
            }

            override fun mouseEntered(e: MouseEvent?) {
                (e?.component as? JComponent)?.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            }

            override fun mouseExited(e: MouseEvent?) {
                (e?.component as? JComponent)?.cursor = Cursor.getDefaultCursor()
            }
        }
    }

    private fun subscribeToAceStatusRefreshEvent() {
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            AceStatusRefreshNotifier.TOPIC,
            object : AceStatusRefreshNotifier {
                override fun refresh() {
                    Log.warn("Refreshing ACE status in Settings General tab...")

                    refreshStatusButton()
                }
            })
    }


}