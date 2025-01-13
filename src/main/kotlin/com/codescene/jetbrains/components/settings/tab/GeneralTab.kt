package com.codescene.jetbrains.components.settings.tab

import com.codescene.jetbrains.CodeSceneIcons.STATUS
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.util.Constants.AI_PRINCIPLES_URL
import com.codescene.jetbrains.util.Constants.CONTACT_URL
import com.codescene.jetbrains.util.Constants.DOCUMENTATION_URL
import com.codescene.jetbrains.util.Constants.SUPPORT_URL
import com.codescene.jetbrains.util.Constants.TERMS_AND_CONDITIONS_URL
import com.codescene.jetbrains.util.Log
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.Configurable
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.*
import javax.swing.border.AbstractBorder

class GeneralTab : Configurable {
    override fun getDisplayName(): String = UiLabelsBundle.message("generalTitle")
    private val globeIcon = IconUtil.colorize(AllIcons.Actions.InlayGlobe, JBUI.CurrentTheme.IconBadge.INFORMATION)
    private val usersIcon = IconUtil.colorize(AllIcons.General.User, JBUI.CurrentTheme.IconBadge.INFORMATION)
    private val statusIcon = IconUtil.colorize(STATUS, JBUI.CurrentTheme.IconBadge.INFORMATION)

    private val account = listOf(
        UiLabelsBundle.message("upgradeToPro") to "", //TBD
        UiLabelsBundle.message("signIn") to "" //TBD
    )

    private val status = listOf(
        UiLabelsBundle.message("ace") to "", //TBD
        UiLabelsBundle.message("codeHealthAnalysis") to "" //TBD
    )

    private val more = listOf(
        UiLabelsBundle.message("documentation") to DOCUMENTATION_URL,
        UiLabelsBundle.message("termsAndPolicies") to TERMS_AND_CONDITIONS_URL,
        UiLabelsBundle.message("aiPrinciples") to AI_PRINCIPLES_URL,
        UiLabelsBundle.message("contactCodeScene") to CONTACT_URL,
        UiLabelsBundle.message("supportTicket") to SUPPORT_URL
    )

    override fun createComponent() = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        add(Box.createVerticalStrut(10))

        add(JPanel().apply {
            layout = FlowLayout(FlowLayout.LEFT, 5, 0)

            val statusIcon = JLabel(usersIcon)
            val status = JLabel(UiLabelsBundle.message("account")).apply {
                font = Font("Arial", Font.BOLD, 13)
            }

            add(statusIcon)
            add(status)
        })

        add(Box.createVerticalStrut(10))

        account.map {
            add(createRoundedPanel(it.first, it.second, false))
            add(Box.createVerticalStrut(10))
        }

        add(Box.createVerticalStrut(10))

        add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            add(Box.createVerticalGlue())

            add(JPanel().apply {
                layout = FlowLayout(FlowLayout.LEFT, 5, 0)

                val statusIcon = JLabel(statusIcon)
                val status = JLabel(UiLabelsBundle.message("status")).apply {
                    font = Font("Arial", Font.BOLD, 13)
                }

                add(statusIcon)
                add(status)
            })

            add(Box.createVerticalStrut(10))

            status.map {
                add(createRoundedPanel(it.first, it.second))
                add(Box.createVerticalStrut(10))
            }
            add(Box.createVerticalStrut(10))
        })

        add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            add(Box.createVerticalGlue())

            add(JPanel().apply {
                layout = FlowLayout(FlowLayout.LEFT, 5, 0)

                val statusIcon = JLabel(IconUtil.scale(globeIcon, null, 1.2f))
                val status = JLabel(UiLabelsBundle.message("more")).apply {
                    font = Font("Arial", Font.BOLD, 13)
                }

                add(statusIcon)
                add(status)
            })

            add(Box.createVerticalStrut(10))

            more.map {
                add(createRoundedPanel(it.first, it.second))
                add(Box.createVerticalStrut(10))
            }
        })
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

    private fun createRoundedPanel(text: String, link: String, isLink: Boolean = true): JPanel {
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

            if (isLink) {
                add(JLabel(AllIcons.General.ChevronRight).apply {
                    horizontalAlignment = SwingConstants.RIGHT
                }, BorderLayout.EAST)

                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        try {
                            val uri = URI(link)

                            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(uri)
                        } catch (e: Exception) {
                            Log.warn("Unable to open link: ${e.message}")
                        }
                    }

                    override fun mouseEntered(e: MouseEvent?) {
                        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    }

                    override fun mouseExited(e: MouseEvent?) {
                        cursor = Cursor.getDefaultCursor()
                    }
                })
            } else {
                add(JButton("COMING SOON").apply {
                    foreground = JBColor.BLACK
                    isContentAreaFilled = false
                    isFocusPainted = false
                    font = Font("Arial", Font.BOLD, 10)
                    border = RoundedLineBorder(JBColor.BLACK, 12)
                    isOpaque = false
                    preferredSize = Dimension(100, 5)
                    minimumSize = Dimension(100, 100)
                    maximumSize = Dimension(100, 100)
                }, BorderLayout.EAST)
            }
        }
    }
}