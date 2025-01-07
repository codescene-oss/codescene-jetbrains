package com.codescene.jetbrains.components.codehealth.detail

import com.codescene.jetbrains.components.codehealth.detail.slider.CustomSlider
import com.codescene.jetbrains.components.layout.ResponsiveLayout
import com.codescene.jetbrains.data.CodeSmell
import com.codescene.jetbrains.data.HighlightRange
import com.codescene.jetbrains.services.CodeSceneDocumentationService
import com.codescene.jetbrains.util.CodeHealthDetails
import com.codescene.jetbrains.util.CodeHealthDetailsType
import com.codescene.jetbrains.util.Constants.CODE_HEALTH_URL
import com.codescene.jetbrains.util.Paragraph
import com.codescene.jetbrains.util.resolveHealthBadge
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.*

class CodeHealthPanelBuilder(private val details: CodeHealthDetails, private val project: Project) {
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
            if (details.healthData!!.status.isNotEmpty()) addHealthDecline(constraint)
            addSlider(constraint)
        }

        addBody(constraint)

        if (isCodeHealth) addLink(constraint)
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
        constraint.gridwidth = 3
        constraint.fill = GridBagConstraints.HORIZONTAL
        constraint.weightx = 1.0
        constraint.ipady = 15

        val subHeaderPanel = JPanel(ResponsiveLayout()).apply {
            add(JLabel(details.subHeader.fileName).apply { icon = details.subHeader.fileIcon })
            add(JLabel(details.subHeader.status).apply { icon = details.subHeader.statusIcon })
        }

        add(subHeaderPanel, constraint)

        constraint.ipady = 0
        constraint.weightx = 0.0
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
        val score = details.healthData!!.score
        constraint.gridy = 3
        constraint.gridx = 0

        val badgeDetails = resolveHealthBadge(score)

        add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)

            constraint.insets = JBUI.insetsTop(16)

            add(JLabel(score.toString()).apply {
                font = Font("Arial", Font.BOLD, 24)
            }, constraint)

            add(Box.createHorizontalStrut(8), constraint)

            add(JButton(badgeDetails.first).apply {
                foreground = badgeDetails.second
                isContentAreaFilled = false
                isFocusPainted = false
                border = RoundedLineBorder(badgeDetails.second, 12)
                isOpaque = false
            })
        }, constraint)

        constraint.insets = JBUI.emptyInsets()
    }

    private fun JPanel.addSlider(constraint: GridBagConstraints) {
        constraint.gridy = 5
        constraint.gridx = 0
        constraint.ipady = 30
        constraint.weightx = 1.0
        constraint.gridwidth = 3

        add(CustomSlider(details.healthData!!.score), constraint)

        constraint.weightx = 0.0
    }

    private fun splitString(input: String, delimiter: String, limit: Int = 2): Pair<String, String> {
        val parts = input.split(delimiter, limit = limit)

        return Pair(parts[0], parts[1])
    }

    private fun JPanel.addBody(constraint: GridBagConstraints) {
        var currentRow = 6

        details.body.forEach { item ->
            constraint.ipady = 20
            constraint.gridy = currentRow++
            constraint.gridx = 0
            constraint.gridwidth = 3

            if (details.type == CodeHealthDetailsType.FUNCTION) addFunctionTitle(item, constraint)
            else addTitle(item, constraint)

            constraint.ipady = 0
            constraint.gridy = currentRow++
            constraint.gridx = 0
            constraint.gridwidth = 3
            constraint.weightx = 1.0

            add(JLabel("<html>${item.body}</html>"), constraint)

            constraint.gridy = currentRow++
            add(Box.createVerticalStrut(15), constraint)
        }
    }

    private fun JPanel.addHealthDecline(constraint: GridBagConstraints) {
        constraint.gridy = 4
        constraint.gridx = 0

        constraint.insets = JBUI.insetsTop(8)
        add(JLabel(details.healthData!!.status).apply {
            foreground = JBColor.GRAY
        }, constraint)

        constraint.insets = JBUI.emptyInsets()
    }

    private fun JPanel.addFunctionTitle(item: Paragraph, constraint: GridBagConstraints) {
        add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = JBUI.Borders.empty()

            val (first, second) = splitString(item.heading, " ")
            val firstLabel = JLabel(first).apply {
                font = Font("Arial", Font.BOLD, JBUI.Fonts.label().size)
                icon = item.icon
            }

            add(firstLabel)

            add(Box.createHorizontalStrut(7))

            val secondLabel = JLabel(second).apply {
                font = Font("Arial", Font.BOLD, JBUI.Fonts.label().size)
                foreground = JBUI.CurrentTheme.Link.FOCUSED_BORDER_COLOR

                addMouseListener(
                    getMouseAdapter(
                        project, CodeSmell(
                            category = "Complex Conditional", highlightRange = HighlightRange(
                                startLine = 1, startColumn = 1, endLine = 1, endColumn = 1
                            ), details = ""
                        )
                    )
                )
            }

            add(secondLabel)
        }, constraint)
    }

    private fun getMouseAdapter(
        project: Project, codeSmell: CodeSmell
    ) = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            if (editor != null) {
                CodeSceneDocumentationService.getInstance(project).openDocumentationPanel(editor, codeSmell)
            }
        }

        override fun mouseEntered(e: MouseEvent) {
            e.component.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }

        override fun mouseExited(e: MouseEvent) {
            e.component.cursor = Cursor.getDefaultCursor()
        }

    }

    private fun JPanel.addTitle(item: Paragraph, constraint: GridBagConstraints) {
        constraint.insets = JBUI.insetsTop(15)

        add(JLabel(item.heading).apply {
            icon = item.icon
            font = Font("Arial", Font.BOLD, JBUI.Fonts.label().size)
        }, constraint)

        constraint.insets = JBUI.emptyInsets()
    }

    private fun JPanel.addLink(constraint: GridBagConstraints) {
        constraint.gridy = 8
        constraint.gridx = 0
        constraint.gridwidth = 3
        constraint.ipady = 15

        val linkLabel =
            JLabel("<html><a href='$CODE_HEALTH_URL'>Learn more about Code Health Analysis</a></html>").apply {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            }

        linkLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                Desktop.getDesktop().browse(URI.create(CODE_HEALTH_URL))
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