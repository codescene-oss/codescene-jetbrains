package com.codescene.jetbrains.components

import com.codescene.jetbrains.CodeSceneIcons.CODE_SMELL_FIXED
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Constants.CONTACT_URL
import com.codescene.jetbrains.util.Constants.FREE_TRIAL_URL
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.net.URI
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.UIManager
import javax.swing.event.HyperlinkEvent

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
class FreemiumPlaceholder {
    fun getComponent() =
        JPanel().apply {
            isOpaque = false
            layout = GridBagLayout()

            val isDarkTheme = UIManager.getLookAndFeelDefaults().getBoolean("ui.theme.is.dark")
            background =
                if (isDarkTheme) {
                    ColorUtil.brighter(UIUtil.getPanelBackground(), 2)
                } else {
                    UIUtil.getPanelBackground()
                }

            border =
                JBUI.Borders.compound(
                    JBUI.Borders.empty(),
                    RoundedLineBorder(JBColor.GRAY, 12),
                )

            val constraint = getConstraints()

            constraint.gridy++
            add(Box.createVerticalStrut(5), constraint)

            addHeader(constraint)
            addSubHeader(constraint)
            addUpgradeText(constraint)
            addBenefitsList(constraint)

            constraint.gridy++
            add(Box.createVerticalStrut(9), constraint)

            addButton(constraint)
            addContactUsLabel(constraint)
        }

    private fun getConstraints() =
        GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            ipady = 20
            weightx = 1.0
            weighty = 0.0
            anchor = GridBagConstraints.NORTH
            fill = GridBagConstraints.HORIZONTAL
            insets = JBUI.insets(0, 18)
        }

    private fun JPanel.addHeader(constraint: GridBagConstraints) {
        val label =
            JLabel("Unlock deeper insights with $CODESCENE").apply {
                font = Font("Arial", Font.BOLD, 15)
                alignmentX = Component.LEFT_ALIGNMENT
                isOpaque = false
            }

        constraint.gridy++
        add(label, constraint)
    }

    private fun JPanel.addSubHeader(constraint: GridBagConstraints) {
        val text =
            getTextArea(
                UiLabelsBundle.message("getTheFullPictureOfYourCodebase"),
                Dimension(250, 20),
            )

        constraint.gridy++
        add(text, constraint)
    }

    private fun JPanel.addUpgradeText(constraint: GridBagConstraints) {
        val text =
            getTextArea(
                UiLabelsBundle.message("upgradingEmpowersYouTo"),
                Dimension(60, 15),
            )

        constraint.gridy++
        add(text, constraint)
    }

    private fun getTextArea(
        text: String,
        dimension: Dimension,
    ) = JTextArea(text).apply {
        isEditable = false
        isOpaque = false
        lineWrap = true
        wrapStyleWord = true
        foreground = JBColor.BLACK
        alignmentX = Component.LEFT_ALIGNMENT
        preferredSize = dimension
        font = Font("Arial", Font.PLAIN, 14)
    }

    private fun JPanel.addButton(constraint: GridBagConstraints) {
        val button =
            JButton("Get $CODESCENE now")
                .apply {
                    preferredSize = Dimension(250, 35)
                    border = JBUI.Borders.empty()
                    addActionListener { Desktop.getDesktop().browse(URI(FREE_TRIAL_URL)) }
                }.apply {
                    putClientProperty("JButton.backgroundColor", ColorUtil.fromHex("#3f6dc7"))
                    putClientProperty("JButton.textColor", ColorUtil.fromHex("#ffffff"))
                }

        constraint.gridy++
        add(button, constraint)

        constraint.gridy++
        add(Box.createVerticalStrut(5), constraint)
    }

    private fun JPanel.addBenefitsList(constraint: GridBagConstraints) {
        val items =
            listOf(
                UiLabelsBundle.message("smarterFasterDecisions"),
                UiLabelsBundle.message("longTermMaintainability"),
                UiLabelsBundle.message("boostDevelopmentSpeed"),
            )

        constraint.ipady = 0
        items.forEach { item ->
            val labelItem =
                JLabel(item, CODE_SMELL_FIXED, JLabel.LEFT).apply {
                    isOpaque = false
                    alignmentX = Component.LEFT_ALIGNMENT
                    font = Font("Arial", Font.PLAIN, 14)
                    foreground = JBColor.GRAY
                    border = JBUI.Borders.empty(5, 0)
                }

            constraint.gridy++
            add(labelItem, constraint)
        }
    }

    private fun JPanel.addContactUsLabel(constraint: GridBagConstraints) {
        val contactLink = getContactLink()
        val editorPane =
            JEditorPane("text/html", contactLink.first).apply {
                isEditable = false
                isOpaque = false
                alignmentX = Component.CENTER_ALIGNMENT
                font = Font("Arial", Font.PLAIN, 13)
                preferredSize = Dimension(250, 45)
                addHyperlinkListener { e ->
                    if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                        val href = e.description
                        val action = contactLink.second[href]
                        action?.run()
                    }
                }
            }

        constraint.gridy++
        constraint.ipady = 15

        add(editorPane, constraint)
    }

    @Suppress("ktlint:standard:string-template-indent")
    private fun getContactLink(): Pair<String, Map<String, Runnable>> {
        val href2linkAction = mutableMapOf<String, Runnable>()
        val actionId = "contact.us"

        href2linkAction[actionId] =
            Runnable {
                try {
                    Desktop.getDesktop().browse(URI(CONTACT_URL))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        return """
        <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; font-size: 13pt; color: ${ColorUtil.toHex(JBColor.GRAY)}; }
                    .centered { text-align: center; }
                </style>
            </head>
            <body>
                <div class="centered">
                    Existing customer? <a href="$actionId">Contact us</a> for access.
                </div>
            </body>
        </html>
            """.trimIndent() to href2linkAction
    }
}
