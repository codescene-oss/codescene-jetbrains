package com.codescene.jetbrains.components.codehealth.detail

import com.codescene.jetbrains.components.codehealth.detail.slider.CustomSlider
import com.codescene.jetbrains.util.resolveHealthBadge
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.*

class CodeHealthPanelBuilder(private val details: CodeHealthDetails) {
    fun getPanel() = JPanel().apply {
        val isCodeHealth = details.type == CodeHealthDetailsType.HEALTH

        layout = GridBagLayout()
        border = JBUI.Borders.empty(0, 10, 10, 10)

        val constraint = getGridBagConstraints()

        addHeader(constraint)
        addSubHeader(constraint)

        addSeparator(constraint)

        if (isCodeHealth) {
            addCodeHealthHeader(constraint)
            if (details.healthData!!.subText.isNotEmpty()) addHealthDecline(constraint)
            addSlider(constraint)
        }

        addBody(constraint)

        if (isCodeHealth) {
            addLink(constraint)
        }
    }

    private fun JPanel.addHeader(constraint: GridBagConstraints) {
        constraint.gridy = 0
        constraint.gridx = 0
        constraint.gridwidth = 3

        add(JLabel("<html><h2>${details.header}</h2></html>"), constraint)
    }

    private fun JPanel.addSubHeader(constraint: GridBagConstraints) {
        constraint.gridy = 1
        constraint.gridx = 0
        constraint.gridwidth = 1
        constraint.ipadx = 15
        constraint.ipady = 15

        add(JLabel(details.subHeader.fileName).apply { icon = details.subHeader.fileIcon }, constraint)
        constraint.ipadx = 0

        constraint.gridx = 1
        add(JLabel(details.subHeader.codeSmell).apply { icon = details.subHeader.codeSmellIcon }, constraint)
        constraint.ipady = 0
    }

    private fun JPanel.addSeparator(constraint: GridBagConstraints) {
        constraint.gridy = 2
        constraint.gridx = 0
        constraint.gridwidth = 3
        constraint.ipady = 5

        add(JSeparator(), constraint)
        constraint.ipady = 0
    }

    private fun JPanel.addCodeHealthHeader(constraint: GridBagConstraints) {
        val score = details!!.healthData!!.header
        constraint.gridy = 3
        constraint.gridx = 0

        val badgeDetails = resolveHealthBadge(score.toDouble())

        add(JPanel().apply {
            layout = FlowLayout(FlowLayout.LEFT)
            add(JLabel("<html><h1>$score</h1></html>"), constraint)

            add(Box.createHorizontalStrut(5))

            add(JButton(badgeDetails.first).apply {
                foreground = badgeDetails.second
                isContentAreaFilled = false
                isFocusPainted = false
                border = RoundedLineBorder(badgeDetails.second, 12)
                isOpaque = false
            })
        }, constraint)
    }

    private fun JPanel.addSlider(constraint: GridBagConstraints) {
        constraint.gridy = 5
        constraint.gridx = 0
        constraint.ipady = 30
        constraint.weightx = 1.0
        constraint.gridwidth = 3

        add(CustomSlider(details.healthData!!.header.toDouble()), constraint)

        constraint.weightx = 0.0
    }

    private fun JPanel.addBody(constraint: GridBagConstraints) {
        var currentRow = 6

        details.body.forEach { item ->
            constraint.ipady = 20
            constraint.gridy = currentRow++
            constraint.gridx = 0
            constraint.gridwidth = 3

            add(JLabel("<html><h3>${item.heading}</h3></html>"), constraint)

            constraint.ipady = 0
            constraint.gridy = currentRow++
            constraint.gridx = 0
            constraint.gridwidth = 3
            constraint.weightx = 1.0

            add(JLabel("<html>${item.body}</html>"), constraint)
        }
    }

    private fun JPanel.addHealthDecline(constraint: GridBagConstraints) {
        constraint.gridy = 4
        constraint.gridx = 0
        constraint.gridwidth = 3

        add(JLabel(details.healthData!!.subText).apply {
            foreground = JBColor.GRAY
        }, constraint)
    }

    private fun JPanel.addLink(constraint: GridBagConstraints) {
        constraint.gridy = 8
        constraint.gridx = 0
        constraint.gridwidth = 3
        constraint.ipady = 15

        val url =
            "https://codescene.com/product/code-health#:~:text=Code%20Health%20is%20an%20aggregated,negative%20outcomes%20for%20your%20project"

        val linkLabel =
            JLabel("<html><a href='$url'>Learn more about Code Health Analysis</a></html>").apply {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            }

        linkLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                Desktop.getDesktop().browse(URI.create(url))
            }
        })

        add(linkLabel, constraint)
    }

    private fun getGridBagConstraints() = GridBagConstraints().apply {
        fill = GridBagConstraints.HORIZONTAL
        insets = JBUI.emptyInsets()
        weightx = 0.0
        weighty = 0.0
    }
}